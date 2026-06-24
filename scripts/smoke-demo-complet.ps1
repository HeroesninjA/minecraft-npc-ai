param(
    [string]$ServerDir = "C:\Minecraft\paper-test",
    [string]$RegionId = "demo_sat",
    [string]$PlayerName = "Hero",
    [string]$RconHost = "localhost",
    [int]$RconPort = 25575,
    [string]$RconPass = "demo",
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
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.2"
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
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
    "ainpc-scenario-medieval/build/libs/ainpc-scenario-medieval-$version.jar",
    "ainpc-api/build/libs/ainpc-api-$version.jar"
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
    . .\scripts\rcon-client.ps1
    $rconConnected = Connect-Rcon -Hostname $RconHost -Port $RconPort -Password $RconPass
    if ($rconConnected) { Ok } else { Fail }
}

# =====================================================================
# 4-10. Comenzi de verificat pe server (sau prin RCON)
function Run-Check {
    param([string]$Phase, [string]$Cmd)
    if ($rconConnected) {
        Write-Host "  [$Phase] > $cmd" -ForegroundColor Yellow
        $result = Send-Rcon $Cmd
        if ($result) { $result.Trim() -split "`n" | Where-Object { $_.Trim() } | ForEach-Object { Write-Host "    $_" -ForegroundColor Gray } }
        else { Write-Host "    (no output)" -ForegroundColor Gray }
        Log "[$Phase] $Cmd -> OK"
    } else {
        Log "  $cmd"
    }
}
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
    @("FINAL",   "/ainpc audit all"),
    @("FINAL",   "/ainpc debugdump all")
)

$currentPhase = ""
foreach ($step in $steps) {
    $phase = $step[0]
    $cmd = $step[1]
    if ($phase -ne $currentPhase) {
        $currentPhase = $phase
        Log ""
        Log "--- $phase ---"
    }
    if ($rconConnected) { Run-Check -Phase $phase -Cmd $cmd } else { Log "  $cmd" }
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
} else {
    Log "REZULTAT: Smoke gata de verificare pe server"
    Write-Host "`nREZULTAT: Smoke gata de verificare pe server" -ForegroundColor Green
}

Log "Raport: $logFile"
