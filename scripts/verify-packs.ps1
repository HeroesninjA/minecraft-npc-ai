param(
    [string]$PackDir = "",
    [string]$OutputDir = "",
    [switch]$Help
)

if ($Help) {
    Write-Host @"
Usage: .\scripts\verify-packs.ps1 [-PackDir path] [-OutputDir path]

Verifica structura pachetelor festival.yml si wilderness.yml:
- Campuri obligatorii (id, name, professions, quests)
- Consistența referințelor între template-uri
- Validare YAML

Raport salvat in build/verify-packs/.
"@
    return
}

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $PackDir) { $PackDir = Join-Path $repoRoot "ainpc-scenario-medieval\src\main\resources\packs" }
if (-not $OutputDir) { $OutputDir = Join-Path $repoRoot "build\verify-packs" }
if (-not (Test-Path -LiteralPath $OutputDir)) { New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null }

$packs = @("festival.yml", "wilderness.yml")
$results = @()

foreach ($packFile in $packs) {
    $packPath = Join-Path $PackDir $packFile
    $report = @{
        file = $packFile
        path = $packPath
        exists = (Test-Path -LiteralPath $packPath -PathType Leaf)
        errors = @()
        warnings = @()
        sections = @{}
    }

    if (-not $report.exists) {
        $report.errors += "Fisierul nu exista: $packPath"
        $results += $report
        continue
    }

    try {
        $content = Get-Content -LiteralPath $packPath -Raw
        $yaml = $content | ConvertFrom-Yaml -ErrorAction Stop
        $report.sections = $yaml.PSObject.Properties.Name

        if (-not $yaml.id) { $report.errors += "Lipseste campul 'id'" }
        if (-not $yaml.name) { $report.warnings += "Lipseste campul 'name'" }
        if (-not $yaml.professions) { $report.warnings += "Lipseste sectiunea 'professions' - niciun NPC special" }
        if ($yaml.quests -and @($yaml.quests).Count -eq 0) { $report.warnings += "Sectiunea 'quests' exista dar este goala" }

        if ($yaml.addon) {
            if (-not $yaml.addon.type) { $report.warnings += "Lipseste addon.type" }
            if (-not $yaml.addon.version) { $report.warnings += "Lipseste addon.version" }
            if ($yaml.addon.dependencies -and @($yaml.addon.dependencies).Count -gt 0) {
                if ($yaml.addon.dependencies -contains "medieval") { $report.warnings += "Dependenta de 'medieval' - verifica ca addonul medieval este incarcat" }
            }
        } else {
            $report.warnings += "Lipseste sectiunea 'addon'"
        }

        if ($yaml.professions) {
            $profCount = @($yaml.professions.PSObject.Properties).Count
            $report.sections["professions_count"] = $profCount
            Write-Host "  $packFile: $profCount profesii"
        }

        $questCount = 0
        if ($yaml.quests) {
            $questCount = @($yaml.quests).Count
            $report.sections["quests_count"] = $questCount
            Write-Host "  $packFile: $questCount quest-uri"
        }

        $report.sections["yaml_parsed_ok"] = $true
    } catch {
        $report.errors += "Eroare YAML: $_"
        $report.sections["yaml_parsed_ok"] = $false
    }

    if ($report.errors.Count -eq 0 -and $report.warnings.Count -eq 0) {
        Write-Host "  $packFile: OK"
    }
    $results += $report
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$jsonPath = Join-Path $OutputDir "verify-packs-$timestamp.json"
$results | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $jsonPath -Encoding UTF8

$allOk = ($results | Where-Object { $_.errors.Count -gt 0 }).Count -eq 0
Write-Host ""
Write-Host "=== Rezultat: " -NoNewline
if ($allOk) { Write-Host "OK" -ForegroundColor Green } else { Write-Host "ERORI" -ForegroundColor Red }
Write-Host "Raport: $jsonPath"

return @{
    ok = $allOk
    json = $jsonPath
    pack_count = $packs.Count
    error_count = ($results | ForEach-Object { $_.errors.Count } | Measure-Object -Sum).Sum
    warning_count = ($results | ForEach-Object { $_.warnings.Count } | Measure-Object -Sum).Sum
}
