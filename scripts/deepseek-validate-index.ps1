param(
    [string]$ProjectRoot = "."
)

$ErrorActionPreference = "Stop"
$reportPath = Join-Path $ProjectRoot "docs/deepseek/deepseek-index-validation-report.txt"

function Write-Report {
    param([string]$Message)
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $line = "[$timestamp] $Message"
    Add-Content -Path $reportPath -Value $line
    Write-Host $line
}

Write-Report "=== DeepSeek Index Validation ==="

$batchGuide = Join-Path $ProjectRoot "docs/deepseek/deepseek-batch-guide.md"
$deepseekReadme = Join-Path $ProjectRoot "docs/deepseek/README.md"
$rootReadme = Join-Path $ProjectRoot "docs/README.md"
$activeSummary = Join-Path $ProjectRoot "docs/deepseek/deepseek-active-series-summary.md"
$archiveReadme = Join-Path $ProjectRoot "docs/deepseek/arhiva/README.md"

$files = @{
    "batch-guide" = $batchGuide
    "deepseek-README" = $deepseekReadme
    "root-README" = $rootReadme
    "active-summary" = $activeSummary
    "archive-README" = $archiveReadme
}

$presentFiles = @{}
foreach ($key in $files.Keys) {
    if (Test-Path $files[$key]) {
        $presentFiles[$key] = $true
        Write-Report "[OK] $key gasit: $($files[$key])"
    } else {
        $presentFiles[$key] = $false
        Write-Report "[LIPSESTE] $key: $($files[$key])"
    }
}

$activeBatches = @()
$knownPatterns = @("deepseek-taskuri-late-50-7.md","deepseek-taskuri-late-50-8.md","deepseek-taskuri-late-50-9.md",
    "deepseek-taskuri-late-50-10.md","deepseek-taskuri-late-50-11.md","deepseek-taskuri-late-50-12.md",
    "deepseek-taskuri-late-50-13.md","deepseek-taskuri-late-50-14.md","deepseek-taskuri-late-50-15.md",
    "deepseek-taskuri-late-50-24.md","deepseek-taskuri-late-50-25.md","deepseek-taskuri-late-50-26.md",
    "deepseek-taskuri-late-50-27.md","deepseek-taskuri-late-50-28.md","deepseek-taskuri-late-50-29.md",
    "deepseek-taskuri-late-50-30.md","deepseek-taskuri-late-50-31.md")

foreach ($pattern in $knownPatterns) {
    $path = Join-Path $ProjectRoot "docs/deepseek" $pattern
    if (Test-Path $path) {
        $activeBatches += $pattern
        Write-Report "[OK] Batch activ: $pattern"
    } else {
        Write-Report "[LIPSESTE] Batch asteptat dar negasit: $pattern"
    }
}

$draftBatches = @("deepseek-taskuri-late-50-16.md","deepseek-taskuri-late-50-17.md","deepseek-taskuri-late-50-18.md",
    "deepseek-taskuri-late-50-19.md","deepseek-taskuri-late-50-20.md","deepseek-taskuri-late-50-21.md")
foreach ($pattern in $draftBatches) {
    $path = Join-Path $ProjectRoot "docs/deepseek" $pattern
    if (Test-Path $path) {
        Write-Report "[DRAFT] Batch draft: $pattern"
    }
}

$missingInSummary = @()
foreach ($batch in $activeBatches) {
    if ($presentFiles["active-summary"]) {
        $content = Get-Content $activeSummary -Raw
        if ($content -notmatch [Regex]::Escape($batch)) {
            $missingInSummary += $batch
        }
    }
}
if ($missingInSummary.Count -gt 0) {
    Write-Report "[DIVERGENTA] Batch-uri active nementionate in active-series-summary:"
    foreach ($b in $missingInSummary) { Write-Report "  - $b" }
} else {
    Write-Report "[OK] Toate batch-urile active sunt mentionate in active-series-summary."
}

$totalBatches = $activeBatches.Count
Write-Report "=== Rezumat ==="
Write-Report "Batch-uri active: $totalBatches"
Write-Report "Draft-uri incomplete: $($draftBatches.Count)"
Write-Report "Indexuri prezente: $(($presentFiles.Values | Where-Object { $_ }).Count)/$($files.Count)"
Write-Report "Raport salvat in: $reportPath"
