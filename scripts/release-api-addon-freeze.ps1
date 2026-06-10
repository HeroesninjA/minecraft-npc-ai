param(
    [string]$ReleaseId = "",

    [string]$OutputDir = ".ai\release-reports",

    [string]$ProjectRoot = "",

    [string]$ApiJar = "",

    [string]$MedievalJar = "",

    [switch]$FailOnWarnings
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function New-SafeId {
    param([string]$Value)

    $safe = ($Value.Trim() -replace "[^A-Za-z0-9._-]", "-").Trim("-")
    if ($safe.Length -eq 0) {
        return Get-Date -Format "yyyyMMdd-HHmmss"
    }
    return $safe
}

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

function Get-RelativePath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BasePath,

        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $baseFull = [System.IO.Path]::GetFullPath($BasePath).TrimEnd("\", "/") + [System.IO.Path]::DirectorySeparatorChar
    $pathFull = [System.IO.Path]::GetFullPath($Path)
    $baseUri = [Uri]$baseFull
    $pathUri = [Uri]$pathFull
    return [Uri]::UnescapeDataString($baseUri.MakeRelativeUri($pathUri).ToString()).Replace("/", "\")
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

function Read-GradleProperties {
    param([string]$Path)

    $values = @{}
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $values
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match "^\s*#" -or $line.Trim().Length -eq 0) {
            continue
        }
        if ($line -match "^\s*([^=]+?)\s*=\s*(.*)$") {
            $values[$Matches[1].Trim()] = $Matches[2].Trim()
        }
    }
    return $values
}

function New-FileSummary {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,

        [string]$Path,

        [bool]$Required = $false
    )

    $resolved = Resolve-OptionalPath -Path $Path
    if ($resolved -and (Test-Path -LiteralPath $resolved -PathType Leaf)) {
        $item = Get-Item -LiteralPath $resolved
        $hash = Get-FileHash -LiteralPath $resolved -Algorithm SHA256
        return [pscustomobject]@{
            name = $Name
            required = $Required
            exists = $true
            path = $resolved
            bytes = $item.Length
            sha256 = $hash.Hash
            last_write_time_utc = $item.LastWriteTimeUtc.ToString("o")
        }
    }

    return [pscustomobject]@{
        name = $Name
        required = $Required
        exists = $false
        path = $resolved
        bytes = 0
        sha256 = ""
        last_write_time_utc = ""
    }
}

function Get-SourceFreezeSummary {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Root,

        [Parameter(Mandatory = $true)]
        [string]$SourceDir
    )

    $sourceDirFull = Resolve-OptionalPath -Path $SourceDir
    if (-not (Test-Path -LiteralPath $sourceDirFull -PathType Container)) {
        return [pscustomobject]@{
            source_dir = $sourceDirFull
            file_count = 0
            normalized_sha256 = ""
            public_declaration_count = 0
            files = @()
            public_declarations = @()
        }
    }

    $files = @(Get-ChildItem -LiteralPath $sourceDirFull -Recurse -File |
        Where-Object { $_.Extension -in @(".kt", ".java") } |
        Sort-Object FullName)
    $normalizedParts = New-Object System.Collections.Generic.List[string]
    $fileSummaries = New-Object System.Collections.Generic.List[object]
    $declarations = New-Object System.Collections.Generic.List[object]

    foreach ($file in $files) {
        $relative = Get-RelativePath -BasePath $Root -Path $file.FullName
        $raw = Get-Content -LiteralPath $file.FullName -Raw
        $normalized = ($raw -replace "`r`n", "`n" -replace "`r", "`n")
        $normalized = (($normalized -split "`n") | ForEach-Object { $_.TrimEnd() }) -join "`n"
        $normalizedParts.Add("### $relative`n$normalized")
        $fileSummaries.Add((New-FileSummary -Name $relative -Path $file.FullName -Required $true))

        $lineNumber = 0
        foreach ($line in ($normalized -split "`n")) {
            $lineNumber++
            $trimmed = $line.Trim()
            if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("//") -or $trimmed.StartsWith("*")) {
                continue
            }
            if ($trimmed -match "^(private|internal)\b") {
                continue
            }
            if ($trimmed -match "^(public\s+)?((data\s+)?class|interface|enum\s+class|object|fun|val|var)\s+([A-Za-z_][A-Za-z0-9_]*)") {
                $declarations.Add([pscustomobject]@{
                    file = $relative
                    line = $lineNumber
                    declaration = ($trimmed -replace "\s+", " ")
                })
            }
        }
    }

    $combined = $normalizedParts.ToArray() -join "`n"
    return [pscustomobject]@{
        source_dir = $sourceDirFull
        file_count = $files.Count
        normalized_sha256 = Get-StringSha256 -Text $combined
        public_declaration_count = $declarations.Count
        files = @($fileSummaries.ToArray())
        public_declarations = @($declarations.ToArray())
    }
}

function Get-ZipEntries {
    param([string]$Path)

    $resolved = Resolve-OptionalPath -Path $Path
    if (-not $resolved -or -not (Test-Path -LiteralPath $resolved -PathType Leaf)) {
        return @()
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($resolved)
    try {
        return @($zip.Entries | ForEach-Object { $_.FullName } | Sort-Object)
    } finally {
        $zip.Dispose()
    }
}

function Get-ZipEntryText {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $true)]
        [string]$EntryName
    )

    $resolved = Resolve-OptionalPath -Path $Path
    if (-not $resolved -or -not (Test-Path -LiteralPath $resolved -PathType Leaf)) {
        return ""
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($resolved)
    try {
        $entry = $zip.Entries | Where-Object { $_.FullName -eq $EntryName } | Select-Object -First 1
        if ($null -eq $entry) {
            return ""
        }
        $stream = $entry.Open()
        try {
            $reader = [System.IO.StreamReader]::new($stream, [System.Text.Encoding]::UTF8)
            try {
                return $reader.ReadToEnd()
            } finally {
                $reader.Dispose()
            }
        } finally {
            $stream.Dispose()
        }
    } finally {
        $zip.Dispose()
    }
}

function Test-ContainsAll {
    param(
        [string[]]$Values,

        [string[]]$Required
    )

    $missing = New-Object System.Collections.Generic.List[string]
    foreach ($item in $Required) {
        if ($Values -notcontains $item) {
            $missing.Add($item)
        }
    }
    return @($missing.ToArray())
}

function Add-MarkdownLine {
    param(
        [System.Collections.Generic.List[string]]$Lines,

        [string]$Text = ""
    )

    $Lines.Add($Text)
}

$repoRoot = if ($ProjectRoot -and $ProjectRoot.Trim().Length -gt 0) {
    Resolve-OptionalPath -Path $ProjectRoot
} else {
    Split-Path -Parent $PSScriptRoot
}
if (-not (Test-Path -LiteralPath $repoRoot -PathType Container)) {
    throw "ProjectRoot invalid: $repoRoot"
}

$safeReleaseId = New-SafeId -Value ($(if ($ReleaseId) { $ReleaseId } else { "api-addon-freeze" }))
$outputDirFull = Resolve-OptionalPath -Path $OutputDir
if (-not (Test-Path -LiteralPath $outputDirFull)) {
    New-Item -ItemType Directory -Path $outputDirFull -Force | Out-Null
}
$outputDirFull = (Resolve-Path -LiteralPath $outputDirFull).Path
$jsonPath = Join-Path $outputDirFull "$safeReleaseId-api-addon-freeze.json"
$markdownPath = Join-Path $outputDirFull "$safeReleaseId-api-addon-freeze.md"

$properties = Read-GradleProperties -Path (Join-Path $repoRoot "gradle.properties")
$projectVersion = if ($properties.ContainsKey("projectVersion")) { $properties["projectVersion"] } else { "" }
$paperVersion = if ($properties.ContainsKey("paperVersion")) { $properties["paperVersion"] } else { "" }

$apiSourceDir = Join-Path $repoRoot "ainpc-api\src\main\kotlin"
$addonSourceDir = Join-Path $repoRoot "ainpc-scenario-medieval\src\main\kotlin"
$defaultApiJar = Join-Path $repoRoot "ainpc-api\build\libs\ainpc-api-$projectVersion.jar"
$defaultMedievalJar = Join-Path $repoRoot "ainpc-scenario-medieval\build\libs\ainpc-scenario-medieval-$projectVersion.jar"
$apiJarPath = if ($ApiJar) { $ApiJar } else { $defaultApiJar }
$medievalJarPath = if ($MedievalJar) { $MedievalJar } else { $defaultMedievalJar }
$apiJarSummary = New-FileSummary -Name "ainpc-api" -Path $apiJarPath -Required $true
$medievalJarSummary = New-FileSummary -Name "ainpc-scenario-medieval" -Path $medievalJarPath -Required $true

$apiSource = Get-SourceFreezeSummary -Root $repoRoot -SourceDir $apiSourceDir
$addonSource = Get-SourceFreezeSummary -Root $repoRoot -SourceDir $addonSourceDir
$apiJarEntries = Get-ZipEntries -Path $apiJarSummary.path
$medievalJarEntries = Get-ZipEntries -Path $medievalJarSummary.path
$sourcePluginYmlPath = Join-Path $repoRoot "ainpc-scenario-medieval\src\main\resources\plugin.yml"
$sourcePluginYml = if (Test-Path -LiteralPath $sourcePluginYmlPath -PathType Leaf) {
    Get-Content -LiteralPath $sourcePluginYmlPath -Raw
} else {
    ""
}
$jarPluginYml = Get-ZipEntryText -Path $medievalJarSummary.path -EntryName "plugin.yml"

$requiredApiClasses = @(
    "ro/ainpc/api/AINPCPlatformApi.class",
    "ro/ainpc/api/AddonRegistryApi.class",
    "ro/ainpc/addons/AINPCAddon.class",
    "ro/ainpc/addons/AddonDescriptor.class",
    "ro/ainpc/world/WorldRegionInfo.class",
    "ro/ainpc/world/WorldPlaceInfo.class",
    "ro/ainpc/world/WorldNodeInfo.class"
)
$requiredMedievalEntries = @(
    "plugin.yml",
    "config-template.yml",
    "packs/medieval_quest.yml",
    "ro/ainpc/addons/medieval/AINPCScenarioMedievalPlugin.class",
    "ro/ainpc/addons/medieval/MedievalScenarioAddon.class"
)
$missingApiClasses = Test-ContainsAll -Values $apiJarEntries -Required $requiredApiClasses
$missingMedievalEntries = Test-ContainsAll -Values $medievalJarEntries -Required $requiredMedievalEntries

$addonPluginChecks = [pscustomobject]@{
    source_plugin_yml = New-FileSummary -Name "addon-plugin-yml-source" -Path $sourcePluginYmlPath -Required $true
    source_has_core_dependency = $sourcePluginYml -match "depend:\s*\[\s*AINPCPlugin\s*\]"
    source_main_class_ok = $sourcePluginYml -match "main:\s*ro\.ainpc\.addons\.medieval\.AINPCScenarioMedievalPlugin"
    source_api_version_declared = $sourcePluginYml -match "api-version:\s*'1\.21'"
    jar_plugin_yml_present = $jarPluginYml.Trim().Length -gt 0
    jar_version = if ($jarPluginYml -match "(?m)^\s*version:\s*(.+?)\s*$") { $Matches[1].Trim() } else { "" }
    jar_version_matches_project = if ($projectVersion) { $jarPluginYml -match "(?m)^\s*version:\s*$([regex]::Escape($projectVersion))\s*$" } else { $false }
    jar_core_dependency_ok = $jarPluginYml -match "depend:\s*\[\s*AINPCPlugin\s*\]"
}

$addonSourceText = if (Test-Path -LiteralPath $addonSourceDir -PathType Container) {
    @(Get-ChildItem -LiteralPath $addonSourceDir -Recurse -File -Include "*.kt", "*.java" |
        ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw }) -join "`n"
} else {
    ""
}
$addonRuntimeChecks = [pscustomobject]@{
    descriptor_id_ok = $addonSourceText -match 'ainpc-scenario-medieval'
    registers_addon = $addonSourceText -match 'registerAddon\(addon\)'
    unregisters_addon = $addonSourceText -match 'unregisterAddon'
    reloads_content = $addonSourceText -match 'reloadContent\(\)'
    installs_pack_resource = $addonSourceText -match 'packs/medieval_quest\.yml'
    exposes_config_template = $addonSourceText -match 'config-template\.yml'
}

$warnings = New-Object System.Collections.Generic.List[string]
if (-not $apiJarSummary.exists) {
    $warnings.Add("Lipseste JAR-ul API: $($apiJarSummary.path)")
}
if (-not $medievalJarSummary.exists) {
    $warnings.Add("Lipseste JAR-ul addon medieval: $($medievalJarSummary.path)")
}
if ($apiSource.file_count -eq 0) {
    $warnings.Add("Nu exista fisiere sursa in ainpc-api/src/main/kotlin.")
}
if ($apiSource.public_declaration_count -eq 0) {
    $warnings.Add("Nu s-au detectat declaratii publice in API.")
}
foreach ($missing in $missingApiClasses) {
    $warnings.Add("JAR-ul API nu contine clasa asteptata: $missing")
}
foreach ($missing in $missingMedievalEntries) {
    $warnings.Add("JAR-ul addon nu contine intrarea asteptata: $missing")
}
if (-not $addonPluginChecks.source_has_core_dependency) {
    $warnings.Add("plugin.yml addon nu declara depend: [AINPCPlugin].")
}
if (-not $addonPluginChecks.source_main_class_ok) {
    $warnings.Add("plugin.yml addon nu declara main class-ul asteptat.")
}
if (-not $addonPluginChecks.jar_version_matches_project) {
    $warnings.Add("plugin.yml din JAR nu are version=$projectVersion.")
}
if (-not $addonRuntimeChecks.descriptor_id_ok) {
    $warnings.Add("Sursa addonului nu contine descriptor id ainpc-scenario-medieval.")
}
if (-not $addonRuntimeChecks.registers_addon) {
    $warnings.Add("Sursa addonului nu inregistreaza addonul in AddonRegistry.")
}
if (-not $addonRuntimeChecks.unregisters_addon) {
    $warnings.Add("Sursa addonului nu dezregistreaza addonul la disable.")
}
if (-not $addonRuntimeChecks.reloads_content) {
    $warnings.Add("Sursa addonului nu cere reloadContent dupa schimbarea pack-ului.")
}
if (-not $addonRuntimeChecks.installs_pack_resource) {
    $warnings.Add("Sursa addonului nu referentiaza packs/medieval_quest.yml.")
}
if (-not $addonRuntimeChecks.exposes_config_template) {
    $warnings.Add("Sursa addonului nu referentiaza config-template.yml.")
}

$report = [pscustomobject]@{
    schema = "ainpc.api-addon-freeze.v1"
    generated_at = (Get-Date).ToString("o")
    release_id = $safeReleaseId
    project_root = $repoRoot
    project_version = $projectVersion
    paper_version = $paperVersion
    ok = $warnings.Count -eq 0
    api = [pscustomobject]@{
        source = $apiSource
        jar = $apiJarSummary
        required_classes = $requiredApiClasses
        missing_classes = @($missingApiClasses)
    }
    addon = [pscustomobject]@{
        source = $addonSource
        jar = $medievalJarSummary
        required_entries = $requiredMedievalEntries
        missing_entries = @($missingMedievalEntries)
        plugin_yml = $addonPluginChecks
        runtime = $addonRuntimeChecks
    }
    warnings = @($warnings.ToArray())
}

$report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonPath -Encoding UTF8

$lines = New-Object System.Collections.Generic.List[string]
Add-MarkdownLine -Lines $lines -Text "# AINPC API/Add-on Freeze Report"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "- Release ID: $safeReleaseId"
Add-MarkdownLine -Lines $lines -Text "- Generated: $($report.generated_at)"
Add-MarkdownLine -Lines $lines -Text "- Project version: $projectVersion"
Add-MarkdownLine -Lines $lines -Text "- Paper version: $paperVersion"
Add-MarkdownLine -Lines $lines -Text "- OK: $($report.ok)"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "## API"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "- Source files: $($apiSource.file_count)"
Add-MarkdownLine -Lines $lines -Text "- Public declarations: $($apiSource.public_declaration_count)"
Add-MarkdownLine -Lines $lines -Text "- Normalized source SHA256: $($apiSource.normalized_sha256)"
Add-MarkdownLine -Lines $lines -Text "- API JAR: $($apiJarSummary.path)"
Add-MarkdownLine -Lines $lines -Text "- API JAR SHA256: $($apiJarSummary.sha256)"
Add-MarkdownLine -Lines $lines -Text "- Missing required classes: $(@($missingApiClasses) -join ', ')"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "## Add-on"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "- Source files: $($addonSource.file_count)"
Add-MarkdownLine -Lines $lines -Text "- Source SHA256: $($addonSource.normalized_sha256)"
Add-MarkdownLine -Lines $lines -Text "- Add-on JAR: $($medievalJarSummary.path)"
Add-MarkdownLine -Lines $lines -Text "- Add-on JAR SHA256: $($medievalJarSummary.sha256)"
Add-MarkdownLine -Lines $lines -Text "- JAR plugin version: $($addonPluginChecks.jar_version)"
Add-MarkdownLine -Lines $lines -Text "- Missing required entries: $(@($missingMedievalEntries) -join ', ')"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "## Runtime Checks"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "- `depend: [AINPCPlugin]`: $($addonPluginChecks.source_has_core_dependency)"
Add-MarkdownLine -Lines $lines -Text "- Main class OK: $($addonPluginChecks.source_main_class_ok)"
Add-MarkdownLine -Lines $lines -Text "- JAR version matches project: $($addonPluginChecks.jar_version_matches_project)"
Add-MarkdownLine -Lines $lines -Text "- Descriptor ID OK: $($addonRuntimeChecks.descriptor_id_ok)"
Add-MarkdownLine -Lines $lines -Text "- Registers add-on: $($addonRuntimeChecks.registers_addon)"
Add-MarkdownLine -Lines $lines -Text "- Unregisters add-on: $($addonRuntimeChecks.unregisters_addon)"
Add-MarkdownLine -Lines $lines -Text "- Reloads content: $($addonRuntimeChecks.reloads_content)"
Add-MarkdownLine -Lines $lines -Text "- Installs pack resource: $($addonRuntimeChecks.installs_pack_resource)"
Add-MarkdownLine -Lines $lines -Text "- Exposes config template: $($addonRuntimeChecks.exposes_config_template)"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "## Public API Declarations"
Add-MarkdownLine -Lines $lines
foreach ($declaration in $apiSource.public_declarations) {
    $declarationLine = '- {0}:{1} `{2}`' -f $declaration.file, $declaration.line, $declaration.declaration
    Add-MarkdownLine -Lines $lines -Text $declarationLine
}
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "## Warnings"
Add-MarkdownLine -Lines $lines
if ($warnings.Count -eq 0) {
    Add-MarkdownLine -Lines $lines -Text "- none"
} else {
    foreach ($warning in $warnings) {
        Add-MarkdownLine -Lines $lines -Text "- $warning"
    }
}

Set-Content -LiteralPath $markdownPath -Value $lines -Encoding UTF8

if ($FailOnWarnings -and $warnings.Count -gt 0) {
    throw "API/addon freeze report are warning-uri: $($warnings.Count). Raport: $jsonPath"
}

Write-Host "API/addon freeze JSON: $jsonPath"
Write-Host "API/addon freeze Markdown: $markdownPath"
[pscustomobject]@{
    ok = $warnings.Count -eq 0
    json = $jsonPath
    markdown = $markdownPath
    warning_count = $warnings.Count
    api_signature_sha256 = $apiSource.normalized_sha256
    addon_signature_sha256 = $addonSource.normalized_sha256
} | ConvertTo-Json -Compress
