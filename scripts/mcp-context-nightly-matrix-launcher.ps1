[CmdletBinding()]
param(
    [string]$OutputRoot = ".ai\nightly",
    [string]$ReportRoot = ".ai",
    [ValidateSet("code", "debug", "planning", "full")]
    [string[]]$Profiles = @("code", "debug", "planning"),
    [switch]$NoTests = $true,
    [switch]$UseAdaptiveThresholds = $true,
    [switch]$EnsureQueryTemplates = $true,
    [switch]$WriteCsvReports = $true,
    [switch]$FreshRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$matrixScript = Join-Path $scriptDir "mcp-context-nightly-matrix.ps1"

if (-not (Test-Path -LiteralPath $matrixScript)) {
    throw "Lipseste scriptul matrix: $matrixScript"
}

$invokeParams = @{
    OutputRoot = $OutputRoot
    ReportRoot = $ReportRoot
    Profiles = $Profiles
}

if ($NoTests) { $invokeParams["NoTests"] = $true }
if ($UseAdaptiveThresholds) { $invokeParams["UseAdaptiveThresholds"] = $true }
if ($EnsureQueryTemplates) { $invokeParams["EnsureQueryTemplates"] = $true }
if ($WriteCsvReports) { $invokeParams["WriteCsvReports"] = $true }
if ($FreshRun) { $invokeParams["FreshRun"] = $true }

& $matrixScript @invokeParams
