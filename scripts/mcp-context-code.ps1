[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Query,

    [ValidateSet("hybrid", "vector", "keyword")]
    [string]$Retrieval = "hybrid",

    [string]$Collection = "ainpc_code",

    [string]$McpRoot = "C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp",

    [int[]]$TokenBudgets = @(900, 1400, 2200),

    [int[]]$TopKValues = @(8, 12, 14),

    [int]$MinCharsForSuccess = 0,

    [switch]$UseAdaptiveThresholds,

    [string]$AdaptiveReportPath = ".ai\mcp-context-report.json",

    [int]$AdaptiveMinRuns = 5,

    [switch]$NoTests,

    [string[]]$Include = @(),

    [string[]]$Exclude = @(),

    [switch]$DisableAutoScope,

    [string]$OutFile = "",

    [switch]$AppendOutFile,

    [string]$SummaryJson = "",

    [switch]$AppendSummaryJson,

    [switch]$VerboseAttempts,

    [switch]$FailOnUnusable
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$stagedScript = Join-Path $PSScriptRoot "mcp-context-staged.ps1"
if (-not (Test-Path -LiteralPath $stagedScript)) {
    throw "Nu am gasit scriptul staged: $stagedScript"
}

$invokeParams = @{
    Query = $Query
    Profile = "code"
    Retrieval = $Retrieval
    Collection = $Collection
    McpRoot = $McpRoot
    TokenBudgets = $TokenBudgets
    TopKValues = $TopKValues
    MinCharsForSuccess = $MinCharsForSuccess
    AdaptiveReportPath = $AdaptiveReportPath
    AdaptiveMinRuns = $AdaptiveMinRuns
    Include = $Include
    Exclude = $Exclude
}

if ($NoTests) {
    $invokeParams["NoTests"] = $true
}
if ($DisableAutoScope) {
    $invokeParams["DisableAutoScope"] = $true
}
if ($UseAdaptiveThresholds) {
    $invokeParams["UseAdaptiveThresholds"] = $true
}
if (-not [string]::IsNullOrWhiteSpace($OutFile)) {
    $invokeParams["OutFile"] = $OutFile
}
if ($AppendOutFile) {
    $invokeParams["AppendOutFile"] = $true
}
if (-not [string]::IsNullOrWhiteSpace($SummaryJson)) {
    $invokeParams["SummaryJson"] = $SummaryJson
}
if ($AppendSummaryJson) {
    $invokeParams["AppendSummaryJson"] = $true
}
if ($VerboseAttempts) {
    $invokeParams["VerboseAttempts"] = $true
}
if ($FailOnUnusable) {
    $invokeParams["FailOnUnusable"] = $true
}

& $stagedScript @invokeParams
