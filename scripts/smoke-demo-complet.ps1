param(
    [string]$ServerDir = "C:\Minecraft\paper-test",
    [string]$RegionId = "demo_sat",
    [string]$PlayerName = "Hero",
    [string]$RconHost = "localhost",
    [int]$RconPort = 25575,
    [string]$RconPass = $env:RCON_PASSWORD,
    [switch]$Quick,        # skip build, use existing JARs
    [switch]$NoBackup,
    [switch]$Rcon          # executa comenzile automat prin RCON
)

# Read project version from gradle.properties
$version = "1.0.0"
$props = Get-Content -LiteralPath "gradle.properties" -ErrorAction SilentlyContinue | Where-Object { $_ -match '^projectVersion=(.+)$' }
if ($props) { $version = $matches[1] }

$ErrorActionPreference = "Stop"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$logFile = "demo-smoke-$timestamp.log"
$configFile = "$ServerDir/plugins/AINPC/config.yml"

function Log   { $msg = "[$(Get-Date -Format HH:mm:ss)] $args"; Write-Host $msg -ForegroundColor Gray; $msg | Out-File $logFile -Append }
function Info  { $msg = "[$(Get-Date -Format HH:mm:ss)] $args"; Write-Host $msg -ForegroundColor Cyan; $msg | Out-File $logFile -Append }
function Ok    { Write-Host "  OK" -ForegroundColor Green }
function Fail  { Write-Host "  FAIL" -ForegroundColor Red; $global:hasErrors = $true }

$global:hasErrors = $false

# =====================================================================
Log "=== Smoke Demo AINPC ==="
Log "Server: $ServerDir | Regiune: $RegionId | Player: $PlayerName"
Log "Log: $logFile"
Log ""

# =====================================================================
# 1. Build
if (-not $Quick) {
    Info "[1/10] BUILD"
    $result = ./gradlew.bat clean build 2>&1
    if ($LASTEXITCODE -eq 0) { Ok } else { Fail; exit 1 }
} else {
    Info "[1/10] BUILD (skip)"
}

# =====================================================================
# 2. Deploy JAR-uri
Info "[2/10] DEPLOY"
$jars = @(
    "ainpc-core-plugin/build/libs/ainpc-core-plugin-$version.jar",
    "ainpc-scenario-medieval/build/libs/ainpc-scenario-medieval-$version.jar"
)
if (-not (Test-Path $ServerDir/plugins)) { New-Item -ItemType Directory -Path "$ServerDir/plugins" -Force | Out-Null }
foreach ($jar in $jars) {
    if (Test-Path $jar) { Copy-Item $jar -Destination "$ServerDir/plugins/" -Force; Write-Host "  $jar" }
    else { Write-Host "  LIPS: $jar" -ForegroundColor Red; $global:hasErrors = $true }
}
if (-not $NoBackup) {
    $backupDir = "$ServerDir/backup-$timestamp"
    New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
    Copy-Item -Recurse "$ServerDir/plugins/AINPC" "$backupDir/" -ErrorAction SilentlyContinue
    Log "Backup: $backupDir"
}
if (-not $global:hasErrors) { Ok }

# =====================================================================
# 3. Config
Info "[3/10] CONFIG"
if (Test-Path $configFile) {
    (Get-Content $configFile) -replace 'routine: false', 'routine: true' -replace 'simulation: false', 'simulation: true' | Set-Content $configFile
    Log "  routine=true, simulation=true"
    Ok
} else {
    Log "  config.yml not found (will be created on first start)"
}

# =====================================================================
Info ""
Info "=== DEPLOY COMPLET ==="
Log "Acum porneste serverul Paper din: $ServerDir"
Log "Comenzi de rulat in consola serverului:"
Log ""

# =====================================================================
# 3b. RCON connection (optional)
$rconConnected = $false
if ($Rcon) {
    Info "[3b/10] RCON"
    if ([string]::IsNullOrWhiteSpace($RconPass)) {
        throw "RCON_PASSWORD nu este configurat. Furnizeaza secretul extern inainte de rulare."
    }
    . .\scripts\rcon-client.ps1
    $rconConnected = Connect-Rcon -Hostname $RconHost -Port $RconPort -Password $RconPass
    if ($rconConnected) { Ok } else { Fail }
}

# =====================================================================
# 4-10. Comenzi de verificat pe server (sau prin RCON)
function Run-Check {
    param([string]$Phase, [string]$Cmd, [string]$ExpectContains)
    if ($rconConnected) {
        Write-Host "  [$Phase] > $cmd" -ForegroundColor Yellow
        $result = Send-Rcon $Cmd
        if ($result) {
            $lines = $result -split "`r`n" | Where-Object { $_.Trim() }
            foreach ($line in $lines) { Write-Host "    $line" -ForegroundColor Gray }
            if ($ExpectContains -and $result -notmatch [regex]::Escape($ExpectContains)) {
                Write-Host "    EXPECTED: $ExpectContains" -ForegroundColor Red
                Log "[$Phase] $Cmd -> FAIL (expected '$ExpectContains' not found)"
                $global:hasVerificationErrors = $true
            } elseif ($ExpectContains) {
                Log "[$Phase] $Cmd -> PASS"
            } else {
                Log "[$Phase] $Cmd -> EXECUTED_UNVERIFIED"
            }
        } else {
            Write-Host "    (no output)" -ForegroundColor Gray
            Log "[$Phase] $Cmd -> NO_OUTPUT"
        }
    } else {
        Log "  $cmd"
    }
}
$global:hasVerificationErrors = $false
$steps = @(
    @("PLUGIN",  "/plugins"),
    @("PLUGIN",  "/ainpc"),
    @("CONFIG",  "/ainpc reload"),
    @("MAPPING", "/ainpc world demo create $RegionId"),
    @("MAPPING", "/ainpc world places $RegionId"),
    @("MAPPING", "/ainpc world save"),
    @("MAPPING", "/ainpc audit world"),
    @("NPC",     "/ainpc world settlement plan $RegionId 5"),
    @("NPC",     "/ainpc world settlement spawn $RegionId 5"),
    @("NPC",     "/ainpc list"),
    @("NPC",     "/ainpc audit npc"),
    @("UX",      "/ainpc routine status nearest"),
    @("UX",      "/ainpc routine tick"),
    @("UX",      "/ainpc gui"),
    @("QUEST",   "/ainpc quest nearest"),
    @("QUEST",   "/ainpc quest accept nearest"),
    @("QUEST",   "/ainpc quest status nearest"),
    @("QUEST",   "/ainpc progression definitions"),
    @("QUEST",   "/ainpc progression stored $PlayerName"),
    @("STORY",   "/ainpc story context"),
    @("STORY",   "/ainpc story events"),
    @("STORY",   "/ainpc debugdump story"),
    @("DEMO",    "/ainpc demo definition"),
    @("DEMO",    "/ainpc demo status $RegionId"),
    @("DEMO",    "/ainpc demo phases $RegionId $PlayerName"),
    @("DEMO",    "/ainpc demo summary $RegionId $PlayerName"),
    @("FINAL",   "/ainpc audit all", "Errors: 0"),
    @("FINAL",   "/ainpc debugdump all")
)

$currentPhase = ""
foreach ($step in $steps) {
    $phase = $step[0]
    $cmd = $step[1]
    $expect = if ($step.Count -ge 3) { $step[2] } else { $null }
    if ($phase -ne $currentPhase) {
        $currentPhase = $phase
        Log ""
        Log "--- $phase ---"
    }
    if ($rconConnected) { Run-Check -Phase $phase -Cmd $cmd -ExpectContains $expect } else { Log "  $cmd" }
}

Log ""
Log "=== SFARSIT SMOKE ==="
Log "Dupa ce rulezi toate comenzile, verifica:"
Log "  - Toate raspund fara stacktrace"
Log "  - NPC-urile apar in /ainpc list"
Log "  - Quest-urile pot fi acceptate si inspectate"
Log "  - Auditul final nu raporteaza erori critice"
Log ""

if ($rconConnected) {
    Disconnect-Rcon
    Log "RCON deconectat"
}

if ($global:hasErrors) {
    Log "REZULTAT: Smoke INCOMPLET (unele fisiere lipsesc)"
    Write-Host "`nREZULTAT: Smoke INCOMPLET" -ForegroundColor Yellow
} elseif ($global:hasVerificationErrors) {
    Log "REZULTAT: Smoke cu verificari esuate"
    Write-Host "`nREZULTAT: Verificari esuate" -ForegroundColor Red
} elseif ($rconConnected) {
    Log "REZULTAT: Smoke complet, verificari de baza trecute"
    Write-Host "`nREZULTAT: Smoke OK" -ForegroundColor Green
} else {
    Log "REZULTAT: Pregatire completa; raspunsurile necesita validare semantica manuala"
    Write-Host "`nREZULTAT: Comenzi executate, validare manuala obligatorie" -ForegroundColor Yellow
}

Log "Raport: $logFile"
