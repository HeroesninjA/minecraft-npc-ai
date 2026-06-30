# Instaleaza git hooks pentru AINPC
# Ruleaza o data dupa clonare sau cand hook-urile se modifica

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$hookDir = Join-Path $repoRoot ".githooks"
$gitDir = Join-Path $repoRoot ".git"

if (-not (Test-Path $gitDir)) {
    Write-Host "Nu exista .git directory. Ruleaza din radacina repo-ului." -ForegroundColor Red
    exit 1
}

Write-Host "Configurare git hooks path: $hookDir" -ForegroundColor Cyan
git config core.hooksPath ".githooks"
Write-Host "Hook-uri instalate:" -ForegroundColor Green

Get-ChildItem -Path $hookDir -Name | ForEach-Object {
    Write-Host "  - $_" -ForegroundColor Green
}

Write-Host "`nHook-urile rula automat la commit." -ForegroundColor Cyan
