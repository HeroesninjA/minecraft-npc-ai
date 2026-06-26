param(
    [string]$ProjectRoot = ".",
    [string]$BatchNumber = "",
    [string]$IntervalStart = "",
    [string]$IntervalEnd = "",
    [string]$Description = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($BatchNumber) -or [string]::IsNullOrWhiteSpace($IntervalStart)) {
    Write-Error "Trebuie sa specifici -BatchNumber si -IntervalStart."
    Write-Host "Ex: .\scripts\deepseek-batch-report.ps1 -BatchNumber 32 -IntervalStart L1601 -IntervalEnd L1650 -Description 'Prompt/Orchestration'"
    exit 1
}

$reportPath = Join-Path $ProjectRoot "docs/deepseek/deepseek-batch-${BatchNumber}-report-$(Get-Date -Format 'yyyy-MM-dd').md"
$changelogPath = Join-Path $ProjectRoot "CHANGELOG.md"

$reportContent = @"
# Batch ${BatchNumber} Report — $Description

Data: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

## Interval
- Start: $IntervalStart
- End: $IntervalEnd

## Fisiere afectate

| Fisier | Schimbare |
|--------|-----------|
| \`docs/deepseek/deepseek-taskuri-late-50-${BatchNumber}.md\` | Batch nou |
| \`docs/deepseek/deepseek-batch-guide.md\` | Actualizat |
| \`docs/deepseek/README.md\` | Actualizat |
| \`docs/README.md\` | Actualizat |
| \`docs/deepseek/deepseek-active-series-summary.md\` | Actualizat |
| \`CHANGELOG.md\` | Actualizat |

## Validare

- [ ] Intervalul continua secvential dupa ultimul batch activ
- [ ] Toate campurile obligatorii sunt prezente
- [ ] Linkurile sunt corecte
- [ ] Batch-ul este mentionat in toate indexurile
- [ ] Nu exista suprapuneri numerice

## Motiv

$Description
"@

Set-Content $reportPath $reportContent -Encoding utf8
Write-Host "[OK] Batch report creat: $reportPath"
