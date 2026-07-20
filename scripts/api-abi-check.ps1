param(
    [string]$ProjectRoot = "",

    [string]$ApiJar = "",

    [string]$BaselinePath = "",

    [string]$ReportPath = "",

    [switch]$UpdateBaseline,

    [switch]$NoFailOnMismatch
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$generator = "javap-protected-descriptors-v2"

function Resolve-OptionalPath {
    param([string]$Path)

    if (-not $Path -or $Path.Trim().Length -eq 0) {
        return ""
    }
    $resolved = Resolve-Path -LiteralPath $Path -ErrorAction SilentlyContinue
    if ($resolved) {
        return $resolved.Path
    }
    return [System.IO.Path]::GetFullPath($Path)
}

function Read-GradleProperties {
    param([string]$Path)

    $values = @{}
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $values
    }
    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        if ($line -match "^\s*#" -or $line.Trim().Length -eq 0) {
            continue
        }
        if ($line -match "^\s*([^=]+?)\s*=\s*(.*)$") {
            $values[$Matches[1].Trim()] = $Matches[2].Trim()
        }
    }
    return $values
}

function Get-StringSha256 {
    param([string]$Text)

    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($Text)
        return [System.BitConverter]::ToString($sha.ComputeHash($bytes)).Replace("-", "")
    } finally {
        $sha.Dispose()
    }
}

function Find-Javap {
    $candidates = New-Object System.Collections.Generic.List[string]
    if ($env:JAVA_HOME) {
        $candidates.Add((Join-Path $env:JAVA_HOME "bin\javap.exe"))
        $candidates.Add((Join-Path $env:JAVA_HOME "bin/javap"))
    }
    $command = Get-Command javap -ErrorAction SilentlyContinue
    if ($command) {
        $candidates.Add($command.Source)
    }
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate -PathType Leaf)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }
    throw "javap nu a fost gasit. Configureaza JAVA_HOME catre un JDK complet."
}

function Compare-SemVer {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Left,

        [Parameter(Mandatory = $true)]
        [string]$Right
    )

    $pattern = "^(\d+)\.(\d+)\.(\d+)(?:[-+].*)?$"
    if ($Left -notmatch $pattern) {
        throw "Versiune SemVer invalida: $Left"
    }
    $leftParts = @([int]$Matches[1], [int]$Matches[2], [int]$Matches[3])
    if ($Right -notmatch $pattern) {
        throw "Versiune SemVer invalida in baseline: $Right"
    }
    $rightParts = @([int]$Matches[1], [int]$Matches[2], [int]$Matches[3])
    for ($index = 0; $index -lt 3; $index++) {
        if ($leftParts[$index] -lt $rightParts[$index]) {
            return -1
        }
        if ($leftParts[$index] -gt $rightParts[$index]) {
            return 1
        }
    }
    return 0
}

function Convert-JavapBlock {
    param([string[]]$Lines)

    $normalized = @($Lines |
        Where-Object { $_.Trim().Length -gt 0 -and $_ -notmatch "^Compiled from " } |
        ForEach-Object { $_.TrimEnd() })
    if ($normalized.Count -eq 0) {
        return $null
    }
    $header = $normalized |
        Where-Object { $_ -match "^\s*public\b.*\b(class|interface|enum|record)\b" } |
        Select-Object -First 1
    if (-not $header) {
        return $null
    }
    if ($header -notmatch "\b(?:class|interface|enum|record)\s+([^\s<{]+)") {
        throw "Nu s-a putut extrage numele clasei din javap: $header"
    }
    $className = $Matches[1]
    $signature = $normalized -join "`n"
    return [pscustomobject]@{
        name = $className
        sha256 = Get-StringSha256 -Text $signature
        line_count = $normalized.Count
        signature = $signature
    }
}

function Get-PublicAbiSnapshot {
    param(
        [Parameter(Mandatory = $true)]
        [string]$JarPath,

        [Parameter(Mandatory = $true)]
        [string]$JavapPath
    )

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($JarPath)
    try {
        $classNames = @($zip.Entries |
            Where-Object {
                $_.FullName.EndsWith(".class") -and
                -not $_.FullName.StartsWith("META-INF/") -and
                $_.FullName -ne "module-info.class" -and
                $_.FullName -notmatch '\$(WhenMappings|EntriesMappings)\.class$' -and
                $_.FullName -notmatch '\$\$inlined\$' -and
                $_.FullName -notmatch '\$\d+\.class$'
            } |
            ForEach-Object { $_.FullName.Substring(0, $_.FullName.Length - 6).Replace("/", ".") } |
            Sort-Object -Unique)
    } finally {
        $zip.Dispose()
    }

    $classSignatures = New-Object System.Collections.Generic.List[object]
    $batchSize = 40
    for ($offset = 0; $offset -lt $classNames.Count; $offset += $batchSize) {
        $last = [Math]::Min($offset + $batchSize - 1, $classNames.Count - 1)
        $batch = @($classNames[$offset..$last])
        $arguments = @("-classpath", $JarPath, "-protected", "-s", "-constants") + $batch
        $output = @(& $JavapPath @arguments 2>&1)
        if ($LASTEXITCODE -ne 0) {
            throw "javap a esuat pentru batch-ul pornit la index ${offset}: $($output -join ' ')"
        }

        $current = New-Object System.Collections.Generic.List[string]
        foreach ($line in $output) {
            $text = [string]$line
            if ($text -match "^Compiled from ") {
                $block = Convert-JavapBlock -Lines $current.ToArray()
                if ($null -ne $block) {
                    $classSignatures.Add($block)
                }
                $current.Clear()
            }
            $current.Add($text)
        }
        $block = Convert-JavapBlock -Lines $current.ToArray()
        if ($null -ne $block) {
            $classSignatures.Add($block)
        }
    }

    $sorted = @($classSignatures.ToArray() | Sort-Object name)
    $snapshotParts = New-Object System.Collections.Generic.List[string]
    foreach ($classSignature in $sorted) {
        $snapshotParts.Add("## $($classSignature.name)")
        $snapshotParts.Add($classSignature.signature)
        $snapshotParts.Add("")
    }
    $snapshotText = $snapshotParts.ToArray() -join "`n"
    return [pscustomobject]@{
        class_count = $sorted.Count
        signature_sha256 = Get-StringSha256 -Text $snapshotText
        snapshot_text = $snapshotText
        classes = @($sorted | ForEach-Object {
            [pscustomobject]@{
                name = $_.name
                sha256 = $_.sha256
                line_count = $_.line_count
            }
        })
    }
}

$repoRoot = if ($ProjectRoot -and $ProjectRoot.Trim().Length -gt 0) {
    Resolve-OptionalPath -Path $ProjectRoot
} else {
    Split-Path -Parent $PSScriptRoot
}
if (-not (Test-Path -LiteralPath $repoRoot -PathType Container)) {
    throw "ProjectRoot invalid: $repoRoot"
}

$properties = Read-GradleProperties -Path (Join-Path $repoRoot "gradle.properties")
if (-not $properties.ContainsKey("apiVersion")) {
    throw "gradle.properties nu defineste apiVersion."
}
$apiVersion = [string]$properties["apiVersion"]
$javaRelease = if ($properties.ContainsKey("javaRelease")) { [string]$properties["javaRelease"] } else { "" }
[void](Compare-SemVer -Left $apiVersion -Right $apiVersion)

$apiJarPath = if ($ApiJar) {
    Resolve-OptionalPath -Path $ApiJar
} else {
    Resolve-OptionalPath -Path (Join-Path $repoRoot "ainpc-api\build\libs\ainpc-api-$apiVersion.jar")
}
if (-not (Test-Path -LiteralPath $apiJarPath -PathType Leaf)) {
    throw "JAR API lipsa: $apiJarPath"
}

$baselineFile = if ($BaselinePath) {
    Resolve-OptionalPath -Path $BaselinePath
} else {
    Resolve-OptionalPath -Path (Join-Path $repoRoot "ainpc-api\abi\ainpc-api-abi-baseline.json")
}
$reportFile = if ($ReportPath) {
    Resolve-OptionalPath -Path $ReportPath
} else {
    Resolve-OptionalPath -Path (Join-Path $repoRoot "ainpc-api\build\reports\abi\ainpc-api-abi-report.json")
}
$snapshotFile = [System.IO.Path]::ChangeExtension($reportFile, ".txt")

foreach ($directory in @(
    (Split-Path -Parent $baselineFile),
    (Split-Path -Parent $reportFile),
    (Split-Path -Parent $snapshotFile)
)) {
    if ($directory -and -not (Test-Path -LiteralPath $directory -PathType Container)) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }
}

$javap = Find-Javap
$javapVersion = ((& $javap -version 2>&1) -join " ").Trim()
$snapshot = Get-PublicAbiSnapshot -JarPath $apiJarPath -JavapPath $javap
Set-Content -LiteralPath $snapshotFile -Value $snapshot.snapshot_text -Encoding UTF8

$baseline = if (Test-Path -LiteralPath $baselineFile -PathType Leaf) {
    Get-Content -LiteralPath $baselineFile -Raw -Encoding UTF8 | ConvertFrom-Json
} else {
    $null
}

$baselineByName = @{}
if ($null -ne $baseline -and $null -ne $baseline.classes) {
    foreach ($entry in @($baseline.classes)) {
        $baselineByName[[string]$entry.name] = [string]$entry.sha256
    }
}
$actualByName = @{}
foreach ($entry in @($snapshot.classes)) {
    $actualByName[[string]$entry.name] = [string]$entry.sha256
}

$addedClasses = @($actualByName.Keys | Where-Object { -not $baselineByName.ContainsKey($_) } | Sort-Object)
$removedClasses = @($baselineByName.Keys | Where-Object { -not $actualByName.ContainsKey($_) } | Sort-Object)
$changedClasses = @($actualByName.Keys | Where-Object {
    $baselineByName.ContainsKey($_) -and $baselineByName[$_] -ne $actualByName[$_]
} | Sort-Object)

$baselineVersion = if ($null -ne $baseline) { [string]$baseline.api_version } else { "" }
$baselineSignature = if ($null -ne $baseline) { [string]$baseline.signature_sha256 } else { "" }
$versionMatches = $null -ne $baseline -and $baselineVersion -eq $apiVersion
$signatureMatches = $null -ne $baseline -and $baselineSignature -eq $snapshot.signature_sha256
$baselineUpdated = $false

if ($UpdateBaseline) {
    if ($null -ne $baseline) {
        $versionOrder = Compare-SemVer -Left $apiVersion -Right $baselineVersion
        $baselineGenerator = if ($baseline.PSObject.Properties["generator"]) { [string]$baseline.generator } else { "" }
        $generatorRefresh = $baselineGenerator -ne $generator
        if ($versionOrder -lt 0) {
            throw "apiVersion $apiVersion este mai mic decat baseline-ul $baselineVersion."
        }
        if (-not $signatureMatches -and $versionOrder -eq 0 -and -not $generatorRefresh) {
            throw "ABI-ul s-a schimbat fara bump apiVersion (ramane $apiVersion)."
        }
    }

    $baselineDocument = [pscustomobject]@{
        schema = "ainpc.api-abi-baseline.v1"
        api_version = $apiVersion
        java_release = $javaRelease
        generator = $generator
        javap_version = $javapVersion
        class_count = $snapshot.class_count
        signature_sha256 = $snapshot.signature_sha256
        classes = @($snapshot.classes)
    }
    $baselineDocument | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $baselineFile -Encoding UTF8
    $baseline = $baselineDocument
    $baselineVersion = $apiVersion
    $baselineSignature = $snapshot.signature_sha256
    $versionMatches = $true
    $signatureMatches = $true
    $addedClasses = @()
    $removedClasses = @()
    $changedClasses = @()
    $baselineUpdated = $true
}

$ok = $null -ne $baseline -and $versionMatches -and $signatureMatches
$report = [pscustomobject]@{
    schema = "ainpc.api-abi-report.v1"
    generated_at = (Get-Date).ToString("o")
    ok = $ok
    api_version = $apiVersion
    java_release = $javaRelease
    generator = $generator
    javap_version = $javapVersion
    api_jar = $apiJarPath
    baseline_path = $baselineFile
    baseline_exists = $null -ne $baseline
    baseline_updated = $baselineUpdated
    baseline_api_version = $baselineVersion
    version_matches = $versionMatches
    signature_matches = $signatureMatches
    class_count = $snapshot.class_count
    signature_sha256 = $snapshot.signature_sha256
    snapshot_path = $snapshotFile
    added_classes = @($addedClasses)
    removed_classes = @($removedClasses)
    changed_classes = @($changedClasses)
}
$report | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $reportFile -Encoding UTF8

$result = [pscustomobject]@{
    ok = $ok
    api_version = $apiVersion
    class_count = $snapshot.class_count
    signature_sha256 = $snapshot.signature_sha256
    added_count = $addedClasses.Count
    removed_count = $removedClasses.Count
    changed_count = $changedClasses.Count
    baseline_updated = $baselineUpdated
    report = $reportFile
    snapshot = $snapshotFile
}
$result | ConvertTo-Json -Compress

if (-not $ok -and -not $NoFailOnMismatch) {
    throw "ABI ainpc-api diferit de baseline. added=$($addedClasses.Count), removed=$($removedClasses.Count), changed=$($changedClasses.Count). Raport: $reportFile"
}
