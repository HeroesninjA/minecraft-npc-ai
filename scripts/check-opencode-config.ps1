param(
    [string]$ProjectRoot = "."
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$driftScript = Join-Path $repoRoot "scripts\check-opencode-config-drift.ps1"
$syncScript = Join-Path $repoRoot "scripts\check-opencode-config-sync.ps1"

if (-not (Test-Path -LiteralPath $driftScript -PathType Leaf)) {
    throw "Nu am gasit scriptul de drift: $driftScript"
}
if (-not (Test-Path -LiteralPath $syncScript -PathType Leaf)) {
    throw "Nu am gasit scriptul de sync: $syncScript"
}

$driftReport = & $driftScript -ProjectRoot $repoRoot
$syncReport = & $syncScript -ProjectRoot $repoRoot

$report = [ordered]@{
    generated_at = (Get-Date).ToString("o")
    project_root = $repoRoot
    drift = $driftReport
    sync = $syncReport
    ok = [bool]($driftReport.ok -and $syncReport.equal)
}

if ($report.ok) {
    Write-Host "OpenCode config check: OK"
} else {
    Write-Host "OpenCode config check: DRIFT"
}

return $report
