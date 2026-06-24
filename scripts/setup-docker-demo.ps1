param(
    [switch]$Quick,
    [switch]$Smoke,      # ruleaza si smoke testul automat dupa pornire
    [string]$PlayerName = "Hero"
)

# Read project version from gradle.properties
$version = "1.0.0"
$props = Get-Content -LiteralPath "gradle.properties" -ErrorAction SilentlyContinue | Where-Object { $_ -match '^projectVersion=(.+)$' }
if ($props) { $version = $matches[1] }

$ErrorActionPreference = "Stop"

Write-Host "=== Setup Docker Demo AINPC ===" -ForegroundColor Cyan
Write-Host ""

# 1. Check Docker
Write-Host "[1/5] Verific Docker..." -ForegroundColor Yellow
$dockerOk = Get-Command docker -ErrorAction SilentlyContinue
if (-not $dockerOk) {
    Write-Host "  Docker nu e instalat. Instaleaza Docker Desktop si reincearca." -ForegroundColor Red
    exit 1
}
Write-Host "  OK" -ForegroundColor Green

# 2. JARs
Write-Host "[2/5] Pregatesc JAR-uri..." -ForegroundColor Yellow
$jars = @(
    "ainpc-core-plugin/build/libs/ainpc-core-plugin-$version.jar",
    "ainpc-scenario-medieval/build/libs/ainpc-scenario-medieval-$version.jar",
    "ainpc-api/build/libs/ainpc-api-$version.jar"
)
$missing = $false
foreach ($jar in $jars) {
    if (-not (Test-Path $jar)) { Write-Host "  LIPS: $jar" -ForegroundColor Red; $missing = $true }
}
if ($missing) {
    if (-not $Quick) {
        Write-Host "  Rulez build..." -ForegroundColor Yellow
        $env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.2"
        $env:Path = "$env:JAVA_HOME\bin;$env:Path"
        ./gradlew.bat clean build 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) { Write-Host "  BUILD FAIL" -ForegroundColor Red; exit 1 }
    } else {
        Write-Host "  Ruleaza cu -Quick fara build? JAR-uri lipsesc." -ForegroundColor Red
        exit 1
    }
}
Write-Host "  OK" -ForegroundColor Green

# 3. Copy JARs to Docker volume
Write-Host "[3/5] Copiez JAR-urile in Docker..." -ForegroundColor Yellow
if (-not (Test-Path "paper-data/plugins")) { New-Item -ItemType Directory -Path "paper-data/plugins" -Force | Out-Null }
foreach ($jar in $jars) {
    Copy-Item $jar -Destination "paper-data/plugins/" -Force
    Write-Host "  $jar" -ForegroundColor Gray
}
Write-Host "  OK" -ForegroundColor Green

# 4. Docker compose up
Write-Host "[4/5] Pornesc containerul..." -ForegroundColor Yellow
Write-Host "  docker compose up -d" -ForegroundColor Gray
docker compose up -d 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "  Docker compose FAIL" -ForegroundColor Red
    exit 1
}
Write-Host "  OK" -ForegroundColor Green

# 5. Status
Write-Host "[5/5] Status..." -ForegroundColor Yellow
Start-Sleep -Seconds 5
$status = docker ps --filter "name=ainpc-demo" --format "{{.Status}}" 2>&1
Write-Host "  Container: $status" -ForegroundColor Gray
Write-Host ""

Write-Host "╔══════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║  Server Paper DEMO pornit!          ║" -ForegroundColor Green
Write-Host "║  IP: localhost:25565                 ║" -ForegroundColor Green
Write-Host "║  RCON: localhost:25575 (parola: demo)║" -ForegroundColor Green
Write-Host "║                                      ║" -ForegroundColor Green
Write-Host "║  Conecteaza-te in Minecraft si:       ║" -ForegroundColor Green
Write-Host "║  /ainpc world demo create demo_sat   ║" -ForegroundColor Green
Write-Host "║  /ainpc world settlement spawn ...   ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""

# Smoke test optional
if ($Smoke) {
    Write-Host "=== Smoke test automat..." -ForegroundColor Cyan
    Start-Sleep -Seconds 10
    & ".\scripts\smoke-demo-complet.ps1" -Rcon -RconHost localhost -RconPort 25575 -RconPass demo -PlayerName $PlayerName -Quick -NoBackup
}

Write-Host "Pentru a opri: docker compose down"
Write-Host "Pentru a vedea log-ul: docker compose logs -f"
