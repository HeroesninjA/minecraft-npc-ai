param(
    [Parameter(Mandatory = $true)]
    [string]$ReleaseId,

    [string]$OutputDir = ".ai\release-reports",

    [string]$ProjectRoot = "",

    [string]$ServerDir = "",

    [string]$CoreJar = "",

    [string]$MedievalJar = "",

    [string]$ApiJar = "",

    [string]$BackupReport = "",

    [string]$MappingSmokeReport = "",

    [string]$QuestSmokeReport = "",

    [string]$QuestRconReport = "",

    [string]$QuestPlayerReport = "",

    [string]$ApiAddonFreezeReport = "",

    [string]$OpenAiMode = "not-recorded",

    [string]$TestsGradle = "not-recorded",

    [string]$StartupSmoke = "not-recorded",

    [string]$MappingSmoke = "not-recorded",

    [string]$NpcSmoke = "not-recorded",

    [string]$QuestSmoke = "not-recorded",

    [string]$AuditFinal = "not-recorded",

    [string]$DebugDumpPath = "",

    [string[]]$KnownIssues = @(),

    [ValidateSet("release", "hold")]
    [string]$Decision = "hold",

    [switch]$FailOnMissingRequired
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

function Find-GitExe {
    $direct = Get-Command git -ErrorAction SilentlyContinue
    if ($direct) {
        return $direct.Source
    }
    $candidates = @(
        "C:\Program Files\Git\cmd\git.exe",
        "C:\Program Files\Git\bin\git.exe",
        (Join-Path $env:LOCALAPPDATA "Programs\Git\cmd\git.exe"),
        (Join-Path $env:LOCALAPPDATA "Programs\Git\bin\git.exe")
    )
    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            return $candidate
        }
    }
    return ""
}

function Invoke-GitText {
    param(
        [Parameter(Mandatory = $true)]
        [string]$GitExe,

        [Parameter(Mandatory = $true)]
        [string]$WorkingDirectory,

        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    if (-not $GitExe -or -not (Test-Path -LiteralPath $GitExe -PathType Leaf)) {
        return ""
    }

    Push-Location $WorkingDirectory
    try {
        $output = & $GitExe @Arguments 2>$null
        if ($LASTEXITCODE -ne 0) {
            return ""
        }
        return ($output -join "`n").Trim()
    } finally {
        Pop-Location
    }
}

function New-ArtifactSummary {
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

function Read-JsonObject {
    param([string]$Path)

    $resolved = Resolve-OptionalPath -Path $Path
    if (-not $resolved -or -not (Test-Path -LiteralPath $resolved -PathType Leaf)) {
        return $null
    }
    return Get-Content -LiteralPath $resolved -Raw | ConvertFrom-Json
}

function Get-OptionalJsonValue {
    param(
        [object]$Object,

        [Parameter(Mandatory = $true)]
        [string]$Name,

        [object]$Default = ""
    )

    if ($null -eq $Object) {
        return $Default
    }
    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property -or $null -eq $property.Value) {
        return $Default
    }
    return $property.Value
}

function New-ReportFileSummary {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,

        [string]$Path
    )

    $resolved = Resolve-OptionalPath -Path $Path
    if (-not $resolved -or -not (Test-Path -LiteralPath $resolved -PathType Leaf)) {
        return [pscustomobject]@{
            name = $Name
            exists = $false
            path = $resolved
            bytes = 0
            sha256 = ""
        }
    }

    $item = Get-Item -LiteralPath $resolved
    $hash = Get-FileHash -LiteralPath $resolved -Algorithm SHA256
    return [pscustomobject]@{
        name = $Name
        exists = $true
        path = $resolved
        bytes = $item.Length
        sha256 = $hash.Hash
    }
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

$safeReleaseId = New-SafeId -Value $ReleaseId
$outputDirFull = Resolve-OptionalPath -Path $OutputDir
if (-not (Test-Path -LiteralPath $outputDirFull)) {
    New-Item -ItemType Directory -Path $outputDirFull -Force | Out-Null
}
$outputDirFull = (Resolve-Path -LiteralPath $outputDirFull).Path
$jsonPath = Join-Path $outputDirFull "$safeReleaseId-release-report.json"
$markdownPath = Join-Path $outputDirFull "$safeReleaseId-release-report.md"

$defaultCoreJar = Join-Path $repoRoot "ainpc-core-plugin\build\libs\ainpc-core-plugin-1.0.0.jar"
$defaultMedievalJar = Join-Path $repoRoot "ainpc-scenario-medieval\build\libs\ainpc-scenario-medieval-1.0.0.jar"
$defaultApiJar = Join-Path $repoRoot "ainpc-api\build\libs\ainpc-api-1.0.0.jar"

$artifactSummaries = @(
    New-ArtifactSummary -Name "core" -Path ($(if ($CoreJar) { $CoreJar } else { $defaultCoreJar })) -Required $true
    New-ArtifactSummary -Name "medieval-addon" -Path ($(if ($MedievalJar) { $MedievalJar } else { $defaultMedievalJar })) -Required $false
    New-ArtifactSummary -Name "api" -Path ($(if ($ApiJar) { $ApiJar } else { $defaultApiJar })) -Required $false
)

$backupReportObject = Read-JsonObject -Path $BackupReport
$backupSummary = if ($backupReportObject) {
    [pscustomobject]@{
        exists = $true
        report_path = (Resolve-OptionalPath -Path $BackupReport)
        backup_zip = $backupReportObject.backup_zip
        backup_zip_sha256 = $backupReportObject.backup_zip_sha256
        restore_check_ok = if ($null -ne $backupReportObject.restore_check) { $backupReportObject.restore_check.ok } else { $null }
        restore_report_path = $backupReportObject.restore_report_path
    }
} else {
    [pscustomobject]@{
        exists = $false
        report_path = (Resolve-OptionalPath -Path $BackupReport)
        backup_zip = ""
        backup_zip_sha256 = ""
        restore_check_ok = $null
        restore_report_path = ""
    }
}

$questRconObject = Read-JsonObject -Path $QuestRconReport
$questRconSummary = if ($questRconObject) {
    [pscustomobject]@{
        exists = $true
        report_path = (Resolve-OptionalPath -Path $QuestRconReport)
        ok = $questRconObject.ok
        mode = Get-OptionalJsonValue -Object $questRconObject -Name "mode" -Default "preflight-rcon"
        step_count = $questRconObject.step_count
        failed_steps = @($questRconObject.failed_steps)
    }
} else {
    [pscustomobject]@{
        exists = $false
        report_path = (Resolve-OptionalPath -Path $QuestRconReport)
        ok = $null
        mode = ""
        step_count = 0
        failed_steps = @()
    }
}

$questPlayerObject = Read-JsonObject -Path $QuestPlayerReport
$questPlayerSummary = if ($questPlayerObject) {
    [pscustomobject]@{
        exists = $true
        report_path = (Resolve-OptionalPath -Path $QuestPlayerReport)
        ok = $questPlayerObject.ok
        mode = Get-OptionalJsonValue -Object $questPlayerObject -Name "mode" -Default "player-assisted-rcon"
        player = Get-OptionalJsonValue -Object $questPlayerObject -Name "player" -Default ""
        step_count = $questPlayerObject.step_count
        failed_steps = @($questPlayerObject.failed_steps)
    }
} else {
    [pscustomobject]@{
        exists = $false
        report_path = (Resolve-OptionalPath -Path $QuestPlayerReport)
        ok = $null
        mode = ""
        player = ""
        step_count = 0
        failed_steps = @()
    }
}

$apiAddonFreezeObject = Read-JsonObject -Path $ApiAddonFreezeReport
$apiAddonFreezeSummary = if ($apiAddonFreezeObject) {
    $apiSignature = ""
    $addonSignature = ""
    $apiProperty = $apiAddonFreezeObject.PSObject.Properties["api"]
    if ($null -ne $apiProperty -and $null -ne $apiProperty.Value) {
        $sourceProperty = $apiProperty.Value.PSObject.Properties["source"]
        if ($null -ne $sourceProperty -and $null -ne $sourceProperty.Value) {
            $apiSignature = Get-OptionalJsonValue -Object $sourceProperty.Value -Name "normalized_sha256" -Default ""
        }
    }
    $addonProperty = $apiAddonFreezeObject.PSObject.Properties["addon"]
    if ($null -ne $addonProperty -and $null -ne $addonProperty.Value) {
        $sourceProperty = $addonProperty.Value.PSObject.Properties["source"]
        if ($null -ne $sourceProperty -and $null -ne $sourceProperty.Value) {
            $addonSignature = Get-OptionalJsonValue -Object $sourceProperty.Value -Name "normalized_sha256" -Default ""
        }
    }
    [pscustomobject]@{
        exists = $true
        report_path = (Resolve-OptionalPath -Path $ApiAddonFreezeReport)
        ok = $apiAddonFreezeObject.ok
        project_version = Get-OptionalJsonValue -Object $apiAddonFreezeObject -Name "project_version" -Default ""
        api_signature_sha256 = $apiSignature
        addon_signature_sha256 = $addonSignature
        warning_count = @($apiAddonFreezeObject.warnings).Count
    }
} else {
    [pscustomobject]@{
        exists = $false
        report_path = (Resolve-OptionalPath -Path $ApiAddonFreezeReport)
        ok = $null
        project_version = ""
        api_signature_sha256 = ""
        addon_signature_sha256 = ""
        warning_count = 0
    }
}

$reportFiles = @(
    New-ReportFileSummary -Name "backup-report" -Path $BackupReport
    New-ReportFileSummary -Name "mapping-smoke-report" -Path $MappingSmokeReport
    New-ReportFileSummary -Name "quest-smoke-report" -Path $QuestSmokeReport
    New-ReportFileSummary -Name "quest-rcon-report" -Path $QuestRconReport
    New-ReportFileSummary -Name "quest-player-rcon-report" -Path $QuestPlayerReport
    New-ReportFileSummary -Name "api-addon-freeze-report" -Path $ApiAddonFreezeReport
)

$gitExe = Find-GitExe
$gitCommit = Invoke-GitText -GitExe $gitExe -WorkingDirectory $repoRoot -Arguments @("rev-parse", "HEAD")
$gitBranch = Invoke-GitText -GitExe $gitExe -WorkingDirectory $repoRoot -Arguments @("branch", "--show-current")
$gitStatus = Invoke-GitText -GitExe $gitExe -WorkingDirectory $repoRoot -Arguments @("status", "--short")

$missingRequired = @($artifactSummaries | Where-Object { $_.required -and -not $_.exists })
$warnings = New-Object System.Collections.Generic.List[string]
foreach ($artifact in $artifactSummaries) {
    if ($artifact.required -and -not $artifact.exists) {
        $warnings.Add("Lipseste artefact obligatoriu: $($artifact.name) -> $($artifact.path)")
    }
}
if (-not $backupSummary.exists) {
    $warnings.Add("Nu a fost atasat raport backup/restore-check.")
} elseif ($backupSummary.restore_check_ok -ne $true) {
    $warnings.Add("Raportul de backup nu confirma restore_check.ok=true.")
}
if ($Decision -eq "release" -and (-not $questPlayerSummary.exists -or $questPlayerSummary.ok -ne $true)) {
    $warnings.Add("Decizia este release, dar quest player smoke nu este atasat sau nu confirma ok=true.")
}
if ($Decision -eq "release" -and (-not $apiAddonFreezeSummary.exists -or $apiAddonFreezeSummary.ok -ne $true)) {
    $warnings.Add("Decizia este release, dar API/addon freeze report nu este atasat sau nu confirma ok=true.")
}
if ($Decision -eq "release" -and $warnings.Count -gt 0) {
    $warnings.Add("Decizia este release, dar exista warning-uri de gate.")
}

$report = [pscustomobject]@{
    schema = "ainpc.release-report.v1"
    generated_at = (Get-Date).ToString("o")
    release_id = $safeReleaseId
    decision = $Decision
    project_root = $repoRoot
    server_dir = Resolve-OptionalPath -Path $ServerDir
    git = [pscustomobject]@{
        commit = $gitCommit
        branch = $gitBranch
        status_short = $gitStatus
    }
    artifacts = @($artifactSummaries)
    checks = [pscustomobject]@{
        tests_gradle = $TestsGradle
        startup_smoke = $StartupSmoke
        mapping_smoke = $MappingSmoke
        npc_smoke = $NpcSmoke
        quest_smoke = $QuestSmoke
        openai_mode = $OpenAiMode
        audit_final = $AuditFinal
        debugdump_path = Resolve-OptionalPath -Path $DebugDumpPath
    }
    backup = $backupSummary
    quest_rcon = $questRconSummary
    quest_player_rcon = $questPlayerSummary
    api_addon_freeze = $apiAddonFreezeSummary
    report_files = @($reportFiles)
    known_issues = @($KnownIssues)
    warnings = @($warnings.ToArray())
    gate = [pscustomobject]@{
        missing_required_artifacts = @($missingRequired | ForEach-Object { $_.name })
        backup_restore_check_ok = $backupSummary.restore_check_ok
        quest_player_smoke_ok = $questPlayerSummary.ok
        api_addon_freeze_ok = $apiAddonFreezeSummary.ok
        ready_for_release = ($Decision -eq "release" -and $warnings.Count -eq 0)
    }
}

$report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonPath -Encoding UTF8

$lines = New-Object System.Collections.Generic.List[string]
Add-MarkdownLine -Lines $lines -Text "# AINPC Release Report"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "- Release ID: $safeReleaseId"
Add-MarkdownLine -Lines $lines -Text "- Generated: $($report.generated_at)"
Add-MarkdownLine -Lines $lines -Text "- Decision: $Decision"
Add-MarkdownLine -Lines $lines -Text "- Commit: $gitCommit"
Add-MarkdownLine -Lines $lines -Text "- Branch: $gitBranch"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "## Artifacts"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "| Name | Exists | Bytes | SHA256 | Path |"
Add-MarkdownLine -Lines $lines -Text "|---|---:|---:|---|---|"
foreach ($artifact in $artifactSummaries) {
    Add-MarkdownLine -Lines $lines -Text "| $($artifact.name) | $($artifact.exists) | $($artifact.bytes) | $($artifact.sha256) | $($artifact.path) |"
}
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "## Checks"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "- Gradle tests: $TestsGradle"
Add-MarkdownLine -Lines $lines -Text "- Startup smoke: $StartupSmoke"
Add-MarkdownLine -Lines $lines -Text "- Mapping smoke: $MappingSmoke"
Add-MarkdownLine -Lines $lines -Text "- NPC smoke: $NpcSmoke"
Add-MarkdownLine -Lines $lines -Text "- Quest smoke: $QuestSmoke"
Add-MarkdownLine -Lines $lines -Text "- OpenAI mode: $OpenAiMode"
Add-MarkdownLine -Lines $lines -Text "- Final audit: $AuditFinal"
Add-MarkdownLine -Lines $lines -Text "- Debugdump path: $($report.checks.debugdump_path)"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "## Backup"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "- Backup report: $($backupSummary.report_path)"
Add-MarkdownLine -Lines $lines -Text "- Backup zip: $($backupSummary.backup_zip)"
Add-MarkdownLine -Lines $lines -Text "- Backup SHA256: $($backupSummary.backup_zip_sha256)"
Add-MarkdownLine -Lines $lines -Text "- Restore-check OK: $($backupSummary.restore_check_ok)"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "## Quest RCON"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "- Report: $($questRconSummary.report_path)"
Add-MarkdownLine -Lines $lines -Text "- OK: $($questRconSummary.ok)"
Add-MarkdownLine -Lines $lines -Text "- Mode: $($questRconSummary.mode)"
Add-MarkdownLine -Lines $lines -Text "- Step count: $($questRconSummary.step_count)"
Add-MarkdownLine -Lines $lines -Text "- Failed steps: $(@($questRconSummary.failed_steps) -join ', ')"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "## Quest Player RCON"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "- Report: $($questPlayerSummary.report_path)"
Add-MarkdownLine -Lines $lines -Text "- OK: $($questPlayerSummary.ok)"
Add-MarkdownLine -Lines $lines -Text "- Mode: $($questPlayerSummary.mode)"
Add-MarkdownLine -Lines $lines -Text "- Player: $($questPlayerSummary.player)"
Add-MarkdownLine -Lines $lines -Text "- Step count: $($questPlayerSummary.step_count)"
Add-MarkdownLine -Lines $lines -Text "- Failed steps: $(@($questPlayerSummary.failed_steps) -join ', ')"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "## API/Add-on Freeze"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "- Report: $($apiAddonFreezeSummary.report_path)"
Add-MarkdownLine -Lines $lines -Text "- OK: $($apiAddonFreezeSummary.ok)"
Add-MarkdownLine -Lines $lines -Text "- Project version: $($apiAddonFreezeSummary.project_version)"
Add-MarkdownLine -Lines $lines -Text "- API signature SHA256: $($apiAddonFreezeSummary.api_signature_sha256)"
Add-MarkdownLine -Lines $lines -Text "- Add-on signature SHA256: $($apiAddonFreezeSummary.addon_signature_sha256)"
Add-MarkdownLine -Lines $lines -Text "- Warning count: $($apiAddonFreezeSummary.warning_count)"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "## Known Issues"
Add-MarkdownLine -Lines $lines
if ($KnownIssues.Count -eq 0) {
    Add-MarkdownLine -Lines $lines -Text "- none recorded"
} else {
    foreach ($issue in $KnownIssues) {
        Add-MarkdownLine -Lines $lines -Text "- $issue"
    }
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
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "## Gate"
Add-MarkdownLine -Lines $lines
Add-MarkdownLine -Lines $lines -Text "- Ready for release: $($report.gate.ready_for_release)"
Add-MarkdownLine -Lines $lines -Text "- Missing required artifacts: $(@($report.gate.missing_required_artifacts) -join ', ')"

Set-Content -LiteralPath $markdownPath -Value $lines -Encoding UTF8

if ($FailOnMissingRequired -and $missingRequired.Count -gt 0) {
    throw "Lipsesc artefacte obligatorii: $(@($missingRequired | ForEach-Object { $_.name }) -join ', ')"
}

Write-Host "Release report JSON: $jsonPath"
Write-Host "Release report Markdown: $markdownPath"
[pscustomobject]@{
    ok = $true
    json = $jsonPath
    markdown = $markdownPath
    warning_count = $warnings.Count
    ready_for_release = $report.gate.ready_for_release
} | ConvertTo-Json -Compress
