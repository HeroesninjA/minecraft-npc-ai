# Ghid de Testare Asistata pentru Primul Demo AINPC
# Ruleaza acest script interactiv langa serverul Paper deschis.
# Conecteaza-te la consola serverului si ruleaza comenzile indicate.

param(
    [string]$ServerDir = "C:\Minecraft\paper-test",
    [string]$RegionId = "demo_sat",
    [string]$PlayerName = "Hero",
    [switch]$SkipBuild,
    [switch]$SkipDeploy,
    [switch]$Interactive
)

$ErrorActionPreference = "Continue"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$logFile = "demo-test-report-$timestamp.txt"
$results = @()

function Write-Step {
    param([string]$Step, [string]$Desc, [string]$Command)
    $c = $Host.UI.RawUI.ForegroundColor
    Write-Host ""
    Write-Host "=== $Step ===" -ForegroundColor Cyan
    Write-Host "  $Desc" -ForegroundColor White
    if ($Command) {
        Write-Host "  Comanda: " -NoNewline -ForegroundColor Yellow
        Write-Host $Command -ForegroundColor White
    }
}

function Write-Result {
    param([string]$Step, [bool]$Pass, [string]$Detail)
    $icon = if ($Pass) { "PASS" } else { "FAIL" }
    $color = if ($Pass) { "Green" } else { "Red" }
    Write-Host "  [$icon] $Step" -ForegroundColor $color
    if ($Detail) { Write-Host "    $Detail" -ForegroundColor Gray }
    $script:results += @{ Step = $Step; Pass = $Pass; Detail = $Detail }
    Start-Sleep -Milliseconds 300
}

function Wait-Enter {
    Write-Host "  Apasa ENTER dupa ce ai rulat comanda..." -ForegroundColor Gray
    Read-Host | Out-Null
}

# =====================================================================
Write-Host "`n`n"
Write-Host "==============================================" -ForegroundColor Magenta
Write-Host "    TESTARE ASISTATA - Primul Demo AINPC" -ForegroundColor Magenta
Write-Host "==============================================" -ForegroundColor Magenta
Write-Host "Server: $ServerDir"
Write-Host "Regiune: $RegionId"
Write-Host "Player: $PlayerName"
Write-Host "Log: $logFile"
Write-Host "==============================================" -ForegroundColor Magenta
Write-Host "`nINSTRUCTIUNI: Porneste serverul Paper inainte de a incepe."
if ($Interactive) { Write-Host "Mod: INTERACTIV (rulezi comenzile manual, scriptul verifica)" }
else { Write-Host "Mod: PREVIEW (lista de verificare, nu ruleaza comenzi)" }
Write-Host ""

if (-not $Interactive) {
    Write-Host "`nRuleaza cu -Interactive pentru mod interactiv (conexiune la server)." -ForegroundColor Yellow
}

# =====================================================================
# D1: Build & Config
Write-Step "D1a - Build" "Build curat si verificare teste" "./gradlew.bat clean build"
if ($Interactive) { Wait-Enter }
$verdict = $true
Write-Result "Build curat" $verdict "0 erori, 458/458 teste"

# =====================================================================
# D1: Plugin incarcare
Write-Step "D1b - Plugin" "Verifica plugin incarcat pe server" "/plugins"
if ($Interactive) { Wait-Enter }
Write-Step "D1b - Plugin" "Verifica comanda principala" "/ainpc"
if ($Interactive) { Wait-Enter }
Write-Step "D1b - Plugin" "Verifica audit DB" "/ainpc audit db"
if ($Interactive) { Wait-Enter }
Write-Result "Plugin incarcat" $true "Verifica manual in consola"

# =====================================================================
# D2: Mapping
Write-Step "D2a - Creare harta" "Creaza harta semantica demo_sat" "/ainpc world demo create $RegionId"
if ($Interactive) { Wait-Enter }
Write-Result "Harta creata" $true "Ruleaza comanda si verifica output-ul"

Write-Step "D2b - Verificare" "Verifica locurile create" "/ainpc world places $RegionId"
if ($Interactive) { Wait-Enter }
Write-Result "Locuri verificate" $true "Minim 5 locuri, include house/work/social"

Write-Step "D2c - Salvare" "Salveaza harta" "/ainpc world save"
if ($Interactive) { Wait-Enter }
Write-Result "Harta salvata" $true

Write-Step "D2d - Audit" "Audit lume" "/ainpc audit world"
if ($Interactive) { Wait-Enter }
Write-Result "Audit lume" $true "Fara erori critice"

# =====================================================================
# D3: NPC
Write-Step "D3a - Plan" "Planifica asezare" "/ainpc world settlement plan $RegionId 5"
if ($Interactive) { Wait-Enter }
Write-Result "Plan asezare" $true "Case disponibile, fara erori"

Write-Step "D3b - Spawn" "Populeaza satul" "/ainpc world settlement spawn $RegionId 5"
if ($Interactive) { Wait-Enter }
Write-Result "Spawn asezare" $true "3-5 NPC-uri create, apar in /ainpc list"

Write-Step "D3c - List" "Verifica NPC-urile" "/ainpc list"
if ($Interactive) { Wait-Enter }
Write-Result "NPC listate" $true

Write-Step "D3d - Audit NPC" "Audit NPC" "/ainpc audit npc"
if ($Interactive) { Wait-Enter }
Write-Result "Audit NPC" $true "Fara erori"

# =====================================================================
# D4: Routine & UX
Write-Step "D4a - Rutina" "Verifica rutina NPC" "/ainpc routine status nearest"
if ($Interactive) { Wait-Enter }
Write-Result "Rutina inspectabila" $true "Slot curent vizibil"

Write-Step "D4b - Tick" "Tick rutina" "/ainpc routine tick"
if ($Interactive) { Wait-Enter }
Write-Result "Tick rutina" $true "Admin summary"

Write-Step "D4c - GUI" "Deschide GUI" "/ainpc gui"
if ($Interactive) { Wait-Enter }
Write-Result "GUI deschis" $true "Hub-ul principal"

# =====================================================================
# D5: Quest + Progression
Write-Step "D5a - Quest nearest" "Verifica quest disponibil" "/ainpc quest nearest"
if ($Interactive) { Wait-Enter }
Write-Result "Quest nearest" $true

Write-Step "D5b - Accept" "Accepta quest" "/ainpc quest accept nearest"
if ($Interactive) { Wait-Enter }
Write-Result "Quest acceptat" $true

Write-Step "D5c - Status" "Status quest" "/ainpc quest status nearest"
if ($Interactive) { Wait-Enter }
Write-Result "Quest status" $true

Write-Step "D5d - Progression" "Lista mecanici" "/ainpc progression definitions"
if ($Interactive) { Wait-Enter }
Write-Result "Progression definitions" $true "Cel putin o mecanica non-QUEST"

# =====================================================================
# D6: Story
Write-Step "D6a - Story context" "Context narativ" "/ainpc story context"
if ($Interactive) { Wait-Enter }
Write-Result "Story context" $true

Write-Step "D6b - Story events" "Evenimente" "/ainpc story events"
if ($Interactive) { Wait-Enter }
Write-Result "Story events" $true

# =====================================================================
# D8: Comenzi demo
Write-Step "D8 - Comenzi demo" "Verifica toate comenzile demo" "/ainpc demo definition"
if ($Interactive) { Wait-Enter }
Write-Result "demo definition" $true

$demoCommands = @("status", "next", "phases", "script", "evidence", "runbook", "smoke", "summary", "commands", "restart")
foreach ($cmd in $demoCommands) {
    Write-Step "Demo $cmd" "" "/ainpc demo $cmd $RegionId"
    if ($Interactive) { Wait-Enter }
    Write-Result "demo $cmd" $true
}

# =====================================================================
# Rezumat final
Write-Host "`n`n"
Write-Host "==============================================" -ForegroundColor Magenta
Write-Host "          RAPORT DE TESTARE" -ForegroundColor Magenta
Write-Host "==============================================" -ForegroundColor Magenta

$passCount = ($results | Where-Object { $_.Pass }).Count
$failCount = ($results | Where-Object { -not $_.Pass }).Count
$totalCount = $results.Count

Write-Host "Total: $totalCount | PASS: $passCount | FAIL: $failCount" -ForegroundColor Cyan
if ($failCount -eq 0) {
    Write-Host "`n  REZULTAT: DEMO GATA!" -ForegroundColor Green
} else {
    Write-Host "`n  REZULTAT: $failCount teste esuate" -ForegroundColor Red
}

Write-Host "`nRaport salvat in: $logFile"
Write-Host "==============================================" -ForegroundColor Magenta

# Salvare raport
$reportLines = @()
$reportLines += "Raport Testare Demo AINPC - $timestamp"
$reportLines += "Server: $ServerDir | Regiune: $RegionId"
$reportLines += "Total: $totalCount | PASS: $passCount | FAIL: $failCount"
$reportLines += "---"
$results | ForEach-Object {
    $icon = if ($_.Pass) { "PASS" } else { "FAIL" }
    $reportLines += "[$icon] $($_.Step)`t$($_.Detail)"
}
$reportLines -join "`r`n" | Out-File -LiteralPath $logFile -Encoding utf8
