[CmdletBinding()]
param(
    [string]$OutputRoot = ".ai\nightly",
    [string]$ReportRoot = ".ai",

    [ValidateSet("code", "debug", "planning", "full")]
    [string[]]$Profiles = @("code", "debug", "planning"),

    [ValidateSet("hybrid", "vector", "keyword")]
    [string]$Retrieval = "hybrid",

    [string]$Collection = "ainpc_code",
    [string]$McpRoot = "C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp",
    [int[]]$TokenBudgets = @(900, 1400, 2200),
    [int[]]$TopKValues = @(8, 12, 14),
    [int]$MinCharsForSuccess = 0,

    [switch]$NoTests,
    [switch]$DisableAutoScope,
    [switch]$UseAdaptiveThresholds,
    [string]$AdaptiveReportPath = ".ai\mcp-context-report.json",
    [int]$AdaptiveMinRuns = 5,

    [switch]$SaveContextOutputs,
    [switch]$EnsureQueryTemplates,
    [switch]$WriteCsvReports,
    [switch]$FreshRun,
    [switch]$FailOnQueryErrors
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-AbsolutePath {
    param([string]$Path)
    if ([System.IO.Path]::IsPathRooted($Path)) { return $Path }
    return (Join-Path (Get-Location).Path $Path)
}

function Ensure-QueryTemplate {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$Profile
    )

    if (Test-Path -LiteralPath $Path) {
        return $false
    }

    $templates = @{
        code = @(
            "# code profile queries",
            "NPCManager createNPC",
            "QuestTemplateSelector progression objective",
            "ScenarioEngine progression selector"
        )
        debug = @(
            "# debug profile queries",
            "quest debugdump anchors audit",
            "story context state events debug",
            "spawn audit duplicate npc investigate"
        )
        planning = @(
            "# planning profile queries",
            "roadmap faze prioritati",
            "mcp docker server mvp faze",
            "release checklist gate-uri"
        )
        full = @(
            "# full profile queries",
            "cross module quest mapping progression story context",
            "mcp codex integration audit backup watcher"
        )
    }

    $content = if ($templates.ContainsKey($Profile)) { $templates[$Profile] } else { @("# one query per line") }
    $dir = Split-Path -Parent $Path
    if (-not [string]::IsNullOrWhiteSpace($dir) -and -not (Test-Path -LiteralPath $dir)) {
        New-Item -ItemType Directory -Path $dir | Out-Null
    }
    Set-Content -LiteralPath $Path -Value $content -Encoding UTF8
    return $true
}

$nightlyScript = Join-Path $PSScriptRoot "mcp-context-nightly.ps1"
$reportScript = Join-Path $PSScriptRoot "mcp-context-summary-report.ps1"
if (-not (Test-Path -LiteralPath $nightlyScript)) {
    throw "Lipseste scriptul nightly: $nightlyScript"
}
if (-not (Test-Path -LiteralPath $reportScript)) {
    throw "Lipseste scriptul report: $reportScript"
}

$absOutputRoot = Resolve-AbsolutePath -Path $OutputRoot
$absReportRoot = Resolve-AbsolutePath -Path $ReportRoot
if (-not (Test-Path -LiteralPath $absOutputRoot)) {
    New-Item -ItemType Directory -Path $absOutputRoot | Out-Null
}
if (-not (Test-Path -LiteralPath $absReportRoot)) {
    New-Item -ItemType Directory -Path $absReportRoot | Out-Null
}

$globalReportPath = Join-Path $absReportRoot "mcp-context-report-global.json"
$matrixReportPath = Join-Path $absReportRoot "mcp-context-nightly-matrix-run.json"
if ($FreshRun) {
    if (Test-Path -LiteralPath $absOutputRoot -PathType Container) {
        Get-ChildItem -LiteralPath $absOutputRoot -Recurse -File -Filter "mcp-context-nightly-summary-*.jsonl" |
            Remove-Item -Force
        Get-ChildItem -LiteralPath $absOutputRoot -Recurse -File -Filter "mcp-context-report-*.json" |
            Remove-Item -Force
        Get-ChildItem -LiteralPath $absOutputRoot -Recurse -File -Filter "mcp-context-report-*.csv" |
            Remove-Item -Force
        Get-ChildItem -LiteralPath $absOutputRoot -Recurse -File -Filter "mcp-context-nightly-run.json" |
            Remove-Item -Force
        if ($SaveContextOutputs) {
            Get-ChildItem -LiteralPath $absOutputRoot -Recurse -File -Filter "mcp-context-*.txt" |
                Where-Object { $_.Name -match "^mcp-context-\d{3}-.+\.txt$" } |
                Remove-Item -Force
        }
    }
    $globalBaseName = [System.IO.Path]::GetFileNameWithoutExtension($globalReportPath)
    $freshGlobalTargets = @(
        $globalReportPath,
        (Join-Path $absReportRoot ($globalBaseName + "-rows.csv")),
        (Join-Path $absReportRoot ($globalBaseName + "-profiles.csv")),
        $matrixReportPath
    )
    foreach ($target in $freshGlobalTargets) {
        if (Test-Path -LiteralPath $target -PathType Leaf) {
            Remove-Item -LiteralPath $target -Force
        }
    }
}

$profileRuns = @()
$createdTemplates = @()

foreach ($profile in $Profiles) {
    $profileOutputDir = Join-Path $absOutputRoot $profile
    if (-not (Test-Path -LiteralPath $profileOutputDir)) {
        New-Item -ItemType Directory -Path $profileOutputDir | Out-Null
    }

    $queryFile = Join-Path $absReportRoot ("mcp-nightly-queries-{0}.txt" -f $profile)
    if ($EnsureQueryTemplates) {
        $created = Ensure-QueryTemplate -Path $queryFile -Profile $profile
        if ($created) { $createdTemplates += $queryFile }
    }

    Write-Host ("[mcp-context-nightly-matrix] Running profile '{0}' with query file {1}" -f $profile, $queryFile)

    $profileReportOut = Join-Path $profileOutputDir ("mcp-context-report-{0}.json" -f $profile)
    $summaryName = "mcp-context-nightly-summary-{0}.jsonl" -f $profile
    $adaptivePathForProfile = Join-Path $profileOutputDir ("mcp-context-report-{0}.json" -f $profile)

    try {
        $invokeParams = @{
            QueryFile = $queryFile
            OutputDir = $profileOutputDir
            SummaryJsonlName = $summaryName
            ReportOut = $profileReportOut
            Profile = $profile
            Retrieval = $Retrieval
            Collection = $Collection
            McpRoot = $McpRoot
            TokenBudgets = $TokenBudgets
            TopKValues = $TopKValues
            MinCharsForSuccess = $MinCharsForSuccess
            AdaptiveReportPath = $adaptivePathForProfile
            AdaptiveMinRuns = $AdaptiveMinRuns
        }

        if ($NoTests) { $invokeParams["NoTests"] = $true }
        if ($DisableAutoScope) { $invokeParams["DisableAutoScope"] = $true }
        if ($UseAdaptiveThresholds) { $invokeParams["UseAdaptiveThresholds"] = $true }
        if ($SaveContextOutputs) { $invokeParams["SaveContextOutputs"] = $true }
        if ($WriteCsvReports) { $invokeParams["WriteCsvReports"] = $true }
        if ($FreshRun) { $invokeParams["FreshRun"] = $true }
        if ($FailOnQueryErrors) { $invokeParams["FailOnQueryErrors"] = $true }

        & $nightlyScript @invokeParams

        $runPath = Join-Path $profileOutputDir "mcp-context-nightly-run.json"
        $status = if (Test-Path -LiteralPath $runPath) {
            try {
                $obj = (Get-Content -LiteralPath $runPath -Raw | ConvertFrom-Json)
                [pscustomobject]@{
                    profile = $profile
                    success = $true
                    queries_total = [int]$obj.queries_total
                    queries_succeeded = [int]$obj.queries_succeeded
                    queries_failed = [int]$obj.queries_failed
                    run_report = $runPath
                    report = $profileReportOut
                }
            } catch {
                [pscustomobject]@{
                    profile = $profile
                    success = $true
                    queries_total = $null
                    queries_succeeded = $null
                    queries_failed = $null
                    run_report = $runPath
                    report = $profileReportOut
                }
            }
        } else {
            [pscustomobject]@{
                profile = $profile
                success = $true
                queries_total = $null
                queries_succeeded = $null
                queries_failed = $null
                run_report = ""
                report = $profileReportOut
            }
        }
        $profileRuns += $status
    } catch {
        $profileRuns += [pscustomobject]@{
            profile = $profile
            success = $false
            queries_total = $null
            queries_succeeded = 0
            queries_failed = $null
            run_report = ""
            report = $profileReportOut
            error = $_.Exception.Message
        }
        Write-Warning ("[mcp-context-nightly-matrix] Profile '{0}' failed: {1}" -f $profile, $_.Exception.Message)
    }
}

$globalReportParams = @{
    InputPath = $absOutputRoot
    FilePattern = "*mcp-context-nightly-summary-*.jsonl"
    OutFile = $globalReportPath
}
if ($WriteCsvReports) {
    $globalBaseName = [System.IO.Path]::GetFileNameWithoutExtension($globalReportPath)
    $globalReportDir = Split-Path -Parent $globalReportPath
    $globalReportParams["RowsCsvOutFile"] = Join-Path $globalReportDir ($globalBaseName + "-rows.csv")
    $globalReportParams["ProfilesCsvOutFile"] = Join-Path $globalReportDir ($globalBaseName + "-profiles.csv")
}

& $reportScript @globalReportParams | Out-Null

$matrixReport = [ordered]@{
    generated_at = (Get-Date).ToString("o")
    output_root = $absOutputRoot
    report_root = $absReportRoot
    profiles = @($Profiles)
    created_query_templates = @($createdTemplates)
    profile_runs = @($profileRuns)
    global_report = $globalReportPath
}

Set-Content -LiteralPath $matrixReportPath -Value ($matrixReport | ConvertTo-Json -Depth 8) -Encoding UTF8
Write-Host ("[mcp-context-nightly-matrix] Matrix run report saved to: {0}" -f $matrixReportPath)

if ($FailOnQueryErrors) {
    $failedProfiles = @($profileRuns | Where-Object { -not $_.success -or (($null -ne $_.queries_failed) -and $_.queries_failed -gt 0) })
    if ($failedProfiles.Count -gt 0) {
        throw ("[mcp-context-nightly-matrix] Exista profile cu erori ({0}). Vezi {1}" -f $failedProfiles.Count, $matrixReportPath)
    }
}
