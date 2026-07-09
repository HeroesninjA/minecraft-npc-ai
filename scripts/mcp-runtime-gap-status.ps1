param(
    [string]$ProjectRoot = ".",
    [string]$ChecklistPath = ""
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
if ([string]::IsNullOrWhiteSpace($ChecklistPath)) {
    $ChecklistPath = Join-Path $repoRoot "docs\mcp-runtime-gap-checklist.md"
}

$outputDir = Join-Path $repoRoot ".ai"
if (-not (Test-Path -LiteralPath $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

if (-not (Test-Path -LiteralPath $ChecklistPath -PathType Leaf)) {
    throw "Nu am gasit checklist-ul: $ChecklistPath"
}

$section = "root"
$items = New-Object System.Collections.Generic.List[object]
$counts = [ordered]@{}

foreach ($line in Get-Content -LiteralPath $ChecklistPath) {
    if ($line -match '^(#+)\s+(?<title>.+?)\s*$') {
        $section = ($Matches.title.Trim() -replace '\s+', ' ')
        if (-not $counts.Contains($section)) {
            $counts[$section] = [ordered]@{ total = 0; done = 0; pending = 0 }
        }
        continue
    }

    if ($line -match '^\s*-\s+\[(?<mark>[ xX])\]\s+(?<text>.+?)\s*$') {
        $done = $Matches.mark -match '[xX]'
        if (-not $counts.Contains($section)) {
            $counts[$section] = [ordered]@{ total = 0; done = 0; pending = 0 }
        }

        $counts[$section].total += 1
        if ($done) {
            $counts[$section].done += 1
        } else {
            $counts[$section].pending += 1
        }

        $items.Add([ordered]@{
            section = $section
            done = $done
            text = $Matches.text.Trim()
        }) | Out-Null
    }
}

$totalItems = $items.Count
$doneItems = ($items | Where-Object { $_.done } | Measure-Object).Count
$pendingItems = $totalItems - $doneItems
$completionPct = if ($totalItems -gt 0) { [math]::Round(($doneItems * 100.0) / $totalItems, 2) } else { 100 }

$report = [ordered]@{
    generated_at = (Get-Date).ToString("o")
    project_root = $repoRoot
    checklist = $ChecklistPath
    total_items = $totalItems
    done_items = $doneItems
    pending_items = $pendingItems
    completion_pct = $completionPct
    sections = $counts
    items = $items
}

$jsonPath = Join-Path $outputDir "mcp-runtime-gap-status.json"
$txtPath = Join-Path $outputDir "mcp-runtime-gap-status.txt"

$report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonPath -Encoding UTF8

$lines = @()
$lines += "MCP runtime gap status"
$lines += "Generated: $($report.generated_at)"
$lines += "Checklist: $ChecklistPath"
$lines += "Progress: $doneItems/$totalItems ($completionPct%)"
$lines += ""
foreach ($entry in $counts.GetEnumerator()) {
    $value = $entry.Value
    $lines += ("{0}: {1}/{2} done, {3} pending" -f $entry.Key, $value.done, $value.total, $value.pending)
}
$lines | Set-Content -LiteralPath $txtPath -Encoding UTF8

Write-Host "MCP runtime gap status: OK"
Write-Host "JSON: $jsonPath"
Write-Host "TXT:  $txtPath"

return $report
