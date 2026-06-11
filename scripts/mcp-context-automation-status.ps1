[CmdletBinding()]
param(
    [string]$ProjectRoot = "",
    [string]$AiDir = ".ai",
    [string]$StartupFileName = "AINPC-MCP-Context-Nightly.cmd",
    [string]$StartupRegistrationStateFile = "mcp-startup-registration.json",
    [int]$FreshHours = 30,
    [string]$OutFile = "",
    [string]$TextSummaryOutFile = "",
    [switch]$FailOnUnhealthy
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-AbsolutePath {
    param([string]$Path)
    if ([System.IO.Path]::IsPathRooted($Path)) { return $Path }
    return (Join-Path (Get-Location).Path $Path)
}

function Read-JsonSafe {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }
    try {
        $raw = Get-Content -LiteralPath $Path -Raw -ErrorAction Stop
        if ([string]::IsNullOrWhiteSpace($raw)) { return $null }
        return ($raw | ConvertFrom-Json -ErrorAction Stop)
    } catch {
        return $null
    }
}

function Get-FileState {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [int]$FreshHoursValue
    )

    try {
        if (-not (Test-Path -LiteralPath $Path)) {
            return [pscustomobject]@{
                exists = $false
                path = $Path
                last_write_utc = ""
                age_hours = $null
                fresh = $false
                error = ""
            }
        }

        $item = Get-Item -LiteralPath $Path
        $ageHours = [Math]::Round(((Get-Date).ToUniversalTime() - $item.LastWriteTimeUtc).TotalHours, 2)
        return [pscustomobject]@{
            exists = $true
            path = $Path
            last_write_utc = $item.LastWriteTimeUtc.ToString("o")
            age_hours = $ageHours
            fresh = ($ageHours -le $FreshHoursValue)
            error = ""
        }
    } catch {
        return [pscustomobject]@{
            exists = $false
            path = $Path
            last_write_utc = ""
            age_hours = $null
            fresh = $false
            error = $_.Exception.Message
        }
    }
}

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Get-Location).Path
}

$absProjectRoot = Resolve-AbsolutePath -Path $ProjectRoot
$absAiDir = if ([System.IO.Path]::IsPathRooted($AiDir)) { $AiDir } else { Join-Path $absProjectRoot $AiDir }

$matrixRunPath = Join-Path $absAiDir "mcp-context-nightly-matrix-run.json"
$globalReportPath = Join-Path $absAiDir "mcp-context-report-global.json"
$globalRowsCsvPath = Join-Path $absAiDir "mcp-context-report-global-rows.csv"
$globalProfilesCsvPath = Join-Path $absAiDir "mcp-context-report-global-profiles.csv"

$startupDir = [Environment]::GetFolderPath("Startup")
$startupPath = Join-Path $startupDir $StartupFileName
$startupRegistrationStatePath = Join-Path $absAiDir $StartupRegistrationStateFile

$schedulerDiagnoseCandidates = @()
if (Test-Path -LiteralPath $absAiDir) {
    $schedulerDiagnoseCandidates = @(
        Get-ChildItem -LiteralPath $absAiDir -File -Filter "scheduler-diagnose*.json" |
            Sort-Object -Property LastWriteTimeUtc -Descending
    )
}
$schedulerDiagnosePath = if ($schedulerDiagnoseCandidates.Count -gt 0) { $schedulerDiagnoseCandidates[0].FullName } else { "" }

$matrixState = Get-FileState -Path $matrixRunPath -FreshHoursValue $FreshHours
$reportState = Get-FileState -Path $globalReportPath -FreshHoursValue $FreshHours
$rowsCsvState = Get-FileState -Path $globalRowsCsvPath -FreshHoursValue $FreshHours
$profilesCsvState = Get-FileState -Path $globalProfilesCsvPath -FreshHoursValue $FreshHours
$startupState = Get-FileState -Path $startupPath -FreshHoursValue (24 * 365 * 20)
$diagState = if ([string]::IsNullOrWhiteSpace($schedulerDiagnosePath)) {
    [pscustomobject]@{
        exists = $false
        path = ""
        last_write_utc = ""
        age_hours = $null
        fresh = $false
    }
} else {
    Get-FileState -Path $schedulerDiagnosePath -FreshHoursValue $FreshHours
}

$matrixRun = Read-JsonSafe -Path $matrixRunPath
$globalReport = Read-JsonSafe -Path $globalReportPath
$diagReport = if ($diagState.exists) { Read-JsonSafe -Path $diagState.path } else { $null }
$startupRegistrationState = Read-JsonSafe -Path $startupRegistrationStatePath

$matrixSummary = [ordered]@{
    profiles = @()
    failed_profiles = @()
    total_queries = $null
    failed_queries = $null
}
if ($null -ne $matrixRun -and $null -ne $matrixRun.profile_runs) {
    $profileRuns = @($matrixRun.profile_runs)
    $matrixSummary.profiles = @($profileRuns | ForEach-Object { $_.profile })
    $matrixSummary.failed_profiles = @(
        $profileRuns |
            Where-Object { (-not $_.success) -or (($null -ne $_.queries_failed) -and ([int]$_.queries_failed -gt 0)) } |
            ForEach-Object { $_.profile }
    )
    $matrixSummary.total_queries = (@($profileRuns | ForEach-Object { [int]$_.queries_total } | Measure-Object -Sum).Sum)
    $matrixSummary.failed_queries = (@($profileRuns | ForEach-Object { [int]$_.queries_failed } | Measure-Object -Sum).Sum)
}

$reportSummary = [ordered]@{
    runs = $null
    attempt1_pct = $null
    profiles = @()
}
if ($null -ne $globalReport) {
    $reportSummary.runs = $globalReport.runs
    if ($null -ne $globalReport.attempt_distribution) {
        $reportSummary.attempt1_pct = $globalReport.attempt_distribution.attempt1_pct
    }
    if ($null -ne $globalReport.by_profile) {
        $reportSummary.profiles = @($globalReport.by_profile | ForEach-Object { $_.profile })
    }
}

$schedulerSummary = [ordered]@{
    diagnose_path = $diagState.path
    service_running = $null
    query_ok = $null
    create_ok = $null
    is_admin = $null
}
if ($null -ne $diagReport) {
    if ($null -ne $diagReport.schedule_service) {
        $schedulerSummary.service_running = ($diagReport.schedule_service.status -eq "Running")
    }
    if ($null -ne $diagReport.probes) {
        if ($null -ne $diagReport.probes.query) {
            $schedulerSummary.query_ok = [bool]$diagReport.probes.query.ok
        }
        if ($null -ne $diagReport.probes.create_delete) {
            $schedulerSummary.create_ok = [bool]$diagReport.probes.create_delete.ok
        }
    }
    $schedulerSummary.is_admin = $diagReport.is_admin
}

$schedulerHealthy = ($schedulerSummary.service_running -eq $true) -and ($schedulerSummary.query_ok -eq $true) -and ($schedulerSummary.create_ok -eq $true)
$startupHealthy = $startupState.exists
if (-not $startupHealthy -and -not [string]::IsNullOrWhiteSpace($startupState.error)) {
    if ($null -ne $startupRegistrationState -and $startupRegistrationState.applied -eq $true -and $startupRegistrationState.removed -ne $true) {
        $startupHealthy = $true
    }
}
$dataFresh = $matrixState.fresh -and $reportState.fresh

$overallStatus = "needs_attention"
if ($dataFresh -and ($schedulerHealthy -or $startupHealthy) -and (@($matrixSummary.failed_profiles)).Count -eq 0) {
    $overallStatus = "ok"
}

$result = [ordered]@{
    generated_at = (Get-Date).ToString("o")
    project_root = $absProjectRoot
    ai_dir = $absAiDir
    fresh_hours = $FreshHours
    overall_status = $overallStatus
    checks = [ordered]@{
        data_fresh = $dataFresh
        scheduler_healthy = $schedulerHealthy
        startup_fallback_present = $startupHealthy
        matrix_failed_profiles = @($matrixSummary.failed_profiles)
    }
    files = [ordered]@{
        matrix_run = $matrixState
        global_report = $reportState
        global_rows_csv = $rowsCsvState
        global_profiles_csv = $profilesCsvState
        startup_file = $startupState
        startup_registration_state = Get-FileState -Path $startupRegistrationStatePath -FreshHoursValue (24 * 365 * 20)
        scheduler_diagnose = $diagState
    }
    summaries = [ordered]@{
        matrix = $matrixSummary
        report = $reportSummary
        scheduler = $schedulerSummary
        startup_registration = if ($null -ne $startupRegistrationState) { $startupRegistrationState } else { $null }
    }
}

$json = $result | ConvertTo-Json -Depth 10

if (-not [string]::IsNullOrWhiteSpace($OutFile)) {
    $outPath = if ([System.IO.Path]::IsPathRooted($OutFile)) { $OutFile } else { Join-Path $absAiDir $OutFile }
    $outDir = Split-Path -Parent $outPath
    if (-not [string]::IsNullOrWhiteSpace($outDir) -and -not (Test-Path -LiteralPath $outDir)) {
        New-Item -ItemType Directory -Path $outDir | Out-Null
    }
    Set-Content -LiteralPath $outPath -Value $json -Encoding UTF8
    Write-Host ("[mcp-context-automation-status] Report saved to: {0}" -f $outPath)
}

$summaryLines = @(
    ("status={0}" -f $overallStatus),
    ("data_fresh={0}" -f $dataFresh),
    ("scheduler_healthy={0}" -f $schedulerHealthy),
    ("startup_fallback_present={0}" -f $startupHealthy),
    ("matrix_failed_profiles={0}" -f ((@($matrixSummary.failed_profiles)) -join ",")),
    ("matrix_profiles={0}" -f ((@($matrixSummary.profiles)) -join ",")),
    ("matrix_total_queries={0}" -f $matrixSummary.total_queries),
    ("matrix_failed_queries={0}" -f $matrixSummary.failed_queries),
    ("report_runs={0}" -f $reportSummary.runs),
    ("report_attempt1_pct={0}" -f $reportSummary.attempt1_pct)
)

if (-not [string]::IsNullOrWhiteSpace($TextSummaryOutFile)) {
    $textPath = if ([System.IO.Path]::IsPathRooted($TextSummaryOutFile)) { $TextSummaryOutFile } else { Join-Path $absAiDir $TextSummaryOutFile }
    $textDir = Split-Path -Parent $textPath
    if (-not [string]::IsNullOrWhiteSpace($textDir) -and -not (Test-Path -LiteralPath $textDir)) {
        New-Item -ItemType Directory -Path $textDir | Out-Null
    }
    Set-Content -LiteralPath $textPath -Value $summaryLines -Encoding UTF8
    Write-Host ("[mcp-context-automation-status] Text summary saved to: {0}" -f $textPath)
}

Write-Output $json

if ($FailOnUnhealthy -and $overallStatus -ne "ok") {
    throw ("[mcp-context-automation-status] overall_status={0}" -f $overallStatus)
}
