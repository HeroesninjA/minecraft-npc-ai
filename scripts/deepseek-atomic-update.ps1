param(
    [string]$ProjectRoot = ".",
    [string]$TaskId = "",
    [ValidateSet("DONE", "CANCELLED", "BLOCKED", "PARTIAL", "NEEDS_REVIEW")]
    [string]$Status = "DONE",
    [string]$Evidence = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($TaskId)) {
    Write-Error "Trebuie sa specifici -TaskId (ex: L455)"
    exit 1
}

$cursorPath = Join-Path $ProjectRoot "docs/deepseek/deepseek-execution-cursor.md"
$ledgerPath = Join-Path $ProjectRoot "docs/deepseek/deepseek-execution-ledger.json"
$backupCursor = Join-Path $ProjectRoot "docs/deepseek/.cursor-backup.md"
$backupLedger = Join-Path $ProjectRoot "docs/deepseek/.ledger-backup.json"

if (-not (Test-Path $cursorPath)) {
    Write-Error "Cursor file not found: $cursorPath"
    exit 1
}
if (-not (Test-Path $ledgerPath)) {
    Write-Error "Ledger file not found: $ledgerPath"
    exit 1
}

# Backup
Copy-Item $cursorPath $backupCursor -Force
Copy-Item $ledgerPath $backupLedger -Force

try {
    # Update cursor
    $cursor = Get-Content $cursorPath -Raw
    $cursor = $cursor -replace '- Current task: `L?\d+`', "- Current task: `$TaskId`"
    Set-Content $cursorPath $cursor -Encoding utf8 -NoNewline

    # Update ledger
    $ledger = Get-Content $ledgerPath -Raw | ConvertFrom-Json
    if ($ledger.PSObject.Properties.Name -contains $TaskId) {
        $ledger.$TaskId.status = $Status
        if ($Evidence) { $ledger.$TaskId.evidence = $Evidence }
    } else {
        $entry = @{ status = $Status; evidence = $Evidence; timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss") }
        $ledger | Add-Member -NotePropertyName $TaskId -NotePropertyValue $entry
    }
    $ledger | ConvertTo-Json -Depth 10 | Set-Content $ledgerPath -Encoding utf8

    Write-Host "[OK] Atomic update: $TaskId -> $Status"
    Write-Host "[INFO] Backup cursor: $backupCursor"
    Write-Host "[INFO] Backup ledger: $backupLedger"
}
catch {
    Write-Host "[EROARE] Update failed. Restoring from backup..."
    Copy-Item $backupCursor $cursorPath -Force
    Copy-Item $backupLedger $ledgerPath -Force
    Write-Error $_.Exception.Message
    exit 1
}
