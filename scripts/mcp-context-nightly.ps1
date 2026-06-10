[CmdletBinding()]
param(
    [string]$QueryFile = ".ai\mcp-nightly-queries.txt",
    [string]$OutputDir = ".ai",
    [string]$SummaryJsonlName = "mcp-context-nightly-summary.jsonl",
    [string]$ReportOut = ".ai\mcp-context-report.json",

    [ValidateSet("code", "debug", "planning", "full")]
    [string]$Profile = "code",

    [ValidateSet("hybrid", "vector", "keyword")]
    [string]$Retrieval = "hybrid",

    [string]$Collection = "ainpc_code",
    [string]$McpRoot = "C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp",
    [int[]]$TokenBudgets = @(900, 1400, 2200),
    [int[]]$TopKValues = @(8, 12, 14),
    [int]$MinCharsForSuccess = 0,

    [switch]$NoTests,
    [string[]]$Include = @(),
    [string[]]$Exclude = @(),
    [switch]$DisableAutoScope,

    [switch]$UseAdaptiveThresholds,
    [string]$AdaptiveReportPath = ".ai\mcp-context-report.json",
    [int]$AdaptiveMinRuns = 5,

    [switch]$SaveContextOutputs,
    [switch]$WriteCsvReports,
    [switch]$FreshRun,
    [switch]$FailOnQueryErrors
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-AbsolutePath {
    param([string]$Path)
    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }
    return (Join-Path (Get-Location).Path $Path)
}

function Sanitize-FileName {
    param([string]$Name)
    $invalid = [System.IO.Path]::GetInvalidFileNameChars()
    $result = $Name
    foreach ($ch in $invalid) {
        $result = $result.Replace([string]$ch, "_")
    }
    $result = $result -replace "\s+", "_"
    if ($result.Length -gt 80) {
        $result = $result.Substring(0, 80)
    }
    return $result
}

$queryPath = Resolve-AbsolutePath -Path $QueryFile
$outputPath = Resolve-AbsolutePath -Path $OutputDir
$summaryJsonlPath = Join-Path $outputPath $SummaryJsonlName
$reportOutPath = Resolve-AbsolutePath -Path $ReportOut

if (-not (Test-Path -LiteralPath $outputPath)) {
    New-Item -ItemType Directory -Path $outputPath | Out-Null
}

if (-not (Test-Path -LiteralPath $queryPath)) {
    $template = @(
        "# One query per line",
        "# Lines starting with # are ignored",
        "NPCManager createNPC",
        "QuestTemplateSelector progression objective"
    )
    $queryDir = Split-Path -Parent $queryPath
    if (-not [string]::IsNullOrWhiteSpace($queryDir) -and -not (Test-Path -LiteralPath $queryDir)) {
        New-Item -ItemType Directory -Path $queryDir | Out-Null
    }
    Set-Content -LiteralPath $queryPath -Value $template -Encoding UTF8
    throw "QueryFile nu exista. Am creat template la: $queryPath"
}

$rawLines = Get-Content -LiteralPath $queryPath -ErrorAction Stop
$queries = @(
    $rawLines |
        ForEach-Object { $_.Trim() } |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and -not $_.StartsWith("#") }
)

if ($queries.Count -eq 0) {
    throw "Nu exista query-uri valide in $queryPath"
}

$stagedScript = Join-Path $PSScriptRoot "mcp-context-staged.ps1"
$codeScript = Join-Path $PSScriptRoot "mcp-context-code.ps1"
$reportScript = Join-Path $PSScriptRoot "mcp-context-summary-report.ps1"

if (-not (Test-Path -LiteralPath $stagedScript)) {
    throw "Lipseste scriptul staged: $stagedScript"
}
if (-not (Test-Path -LiteralPath $codeScript)) {
    throw "Lipseste scriptul code shortcut: $codeScript"
}
if (-not (Test-Path -LiteralPath $reportScript)) {
    throw "Lipseste scriptul report: $reportScript"
}

$runReportPath = Join-Path $outputPath "mcp-context-nightly-run.json"
if ($FreshRun) {
    $freshTargets = @($summaryJsonlPath, $reportOutPath, $runReportPath)
    if ($WriteCsvReports) {
        $reportBaseName = [System.IO.Path]::GetFileNameWithoutExtension($reportOutPath)
        $reportDir = Split-Path -Parent $reportOutPath
        $freshTargets += (Join-Path $reportDir ($reportBaseName + "-rows.csv"))
        $freshTargets += (Join-Path $reportDir ($reportBaseName + "-profiles.csv"))
    }
    foreach ($target in $freshTargets) {
        if (-not [string]::IsNullOrWhiteSpace($target) -and (Test-Path -LiteralPath $target -PathType Leaf)) {
            Remove-Item -LiteralPath $target -Force
        }
    }
    if ($SaveContextOutputs -and (Test-Path -LiteralPath $outputPath -PathType Container)) {
        Get-ChildItem -LiteralPath $outputPath -File -Filter "mcp-context-*.txt" |
            Where-Object { $_.Name -match "^mcp-context-\d{3}-.+\.txt$" } |
            Remove-Item -Force
    }
}

$successCount = 0
$failedCount = 0
$failures = @()

for ($i = 0; $i -lt $queries.Count; $i++) {
    $query = $queries[$i]
    Write-Host ("[mcp-context-nightly] Query {0}/{1}: {2}" -f ($i + 1), $queries.Count, $query)

    try {
        $commonParams = @{
            Query = $query
            Retrieval = $Retrieval
            Collection = $Collection
            McpRoot = $McpRoot
            TokenBudgets = $TokenBudgets
            TopKValues = $TopKValues
            MinCharsForSuccess = $MinCharsForSuccess
            Include = $Include
            Exclude = $Exclude
            SummaryJson = $summaryJsonlPath
            AppendSummaryJson = $true
            FailOnUnusable = $true
        }

        if ($NoTests) { $commonParams["NoTests"] = $true }
        if ($DisableAutoScope) { $commonParams["DisableAutoScope"] = $true }
        if ($UseAdaptiveThresholds) { $commonParams["UseAdaptiveThresholds"] = $true }
        if (-not [string]::IsNullOrWhiteSpace($AdaptiveReportPath)) {
            $commonParams["AdaptiveReportPath"] = $AdaptiveReportPath
            $commonParams["AdaptiveMinRuns"] = $AdaptiveMinRuns
        }
        if ($SaveContextOutputs) {
            $safeName = Sanitize-FileName -Name $query
            $contextOut = Join-Path $outputPath ("mcp-context-{0:000}-{1}.txt" -f ($i + 1), $safeName)
            $commonParams["OutFile"] = $contextOut
        }

        if ($Profile -eq "code") {
            & $codeScript @commonParams | Out-Null
        } else {
            $commonParams["Profile"] = $Profile
            & $stagedScript @commonParams | Out-Null
        }

        $successCount++
    } catch {
        $failedCount++
        $failures += [pscustomobject]@{
            query = $query
            error = $_.Exception.Message
        }
        Write-Warning ("[mcp-context-nightly] Failed query: {0}" -f $query)
    }
}

$reportParams = @{
    InputPath = $outputPath
    FilePattern = "*mcp-context*summary*.json*"
    OutFile = $reportOutPath
}
if ($WriteCsvReports) {
    $reportBaseName = [System.IO.Path]::GetFileNameWithoutExtension($reportOutPath)
    $reportDir = Split-Path -Parent $reportOutPath
    $reportParams["RowsCsvOutFile"] = Join-Path $reportDir ($reportBaseName + "-rows.csv")
    $reportParams["ProfilesCsvOutFile"] = Join-Path $reportDir ($reportBaseName + "-profiles.csv")
}

& $reportScript @reportParams | Out-Null

$runReport = [ordered]@{
    generated_at = (Get-Date).ToString("o")
    query_file = $queryPath
    output_dir = $outputPath
    summary_jsonl = $summaryJsonlPath
    aggregated_report = $reportOutPath
    profile = $Profile
    retrieval = $Retrieval
    queries_total = $queries.Count
    queries_succeeded = $successCount
    queries_failed = $failedCount
    failures = @($failures)
}

Set-Content -LiteralPath $runReportPath -Value ($runReport | ConvertTo-Json -Depth 8) -Encoding UTF8
Write-Host ("[mcp-context-nightly] Run report saved to: {0}" -f $runReportPath)

if ($FailOnQueryErrors -and $failedCount -gt 0) {
    throw ("[mcp-context-nightly] {0} query-uri au esuat din {1}. Vezi {2}" -f $failedCount, $queries.Count, $runReportPath)
}
