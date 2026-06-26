param([string]$ProjectRoot = ".")

$ErrorActionPreference = "Stop"
$docsDir = Join-Path $ProjectRoot "docs/deepseek"
$reportPath = Join-Path $ProjectRoot "docs/deepseek/deepseek-stale-refs-report.txt"
$archiveDir = Join-Path $docsDir "arhiva"

$archivedFiles = @()
if (Test-Path $archiveDir) {
    $archivedFiles = Get-ChildItem $archiveDir -Filter "*.md" | ForEach-Object { $_.Name }
}

function Write-Report {
    param([string]$Message)
    Add-Content -Path $reportPath -Value $Message
    Write-Host $Message
}

Write-Report "=== DeepSeek Stale Reference Report ==="
Write-Report "Data: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Report ""

$staleCount = 0
$activeFiles = Get-ChildItem $docsDir -Filter "*.md" | Where-Object { $_.Directory.Name -ne "arhiva" }

foreach ($activeFile in $activeFiles) {
    $content = Get-Content $activeFile.FullName -Raw
    foreach ($archivedName in $archivedFiles) {
        $archivedPath = "arhiva/$archivedName"
        $oldPath = $archivedName
        if ($content -match [Regex]::Escape($oldPath) -and $content -notmatch [Regex]::Escape($archivedPath)) {
            Write-Report "[STALE] $($activeFile.Name): referinta '$oldPath' in loc de '$archivedPath'"
            $staleCount++
        }
    }
}

if ($staleCount -eq 0) {
    Write-Report "Nu s-au gasit referinte invechite."
} else {
    Write-Report "Total referinte invechite: $staleCount"
}
