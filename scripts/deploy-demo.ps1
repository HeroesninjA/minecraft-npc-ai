param(
    [Parameter(Mandatory=$true)]
    [string]$ServerDir,
    [switch]$SkipBuild,
    [switch]$SkipBackup
)

# Read project version from gradle.properties
$version = "1.0.0"
$props = Get-Content -LiteralPath "gradle.properties" -ErrorAction SilentlyContinue | Where-Object { $_ -match '^projectVersion=(.+)$' }
if ($props) { $version = $matches[1] }

$ErrorActionPreference = "Stop"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"

Write-Host "=== Deploy AINPC pe serverul Paper ===" -ForegroundColor Cyan
Write-Host "Server: $ServerDir"
Write-Host ""

# 1. Build
if (-not $SkipBuild) {
    Write-Host "[1/4] Build..." -ForegroundColor Yellow
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.2"
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    $result = ./gradlew.bat clean build 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "BUILD ESICAT!" -ForegroundColor Red
        exit 1
    }
    Write-Host "  OK" -ForegroundColor Green
}

# 2. Backup
if (-not $SkipBackup) {
    Write-Host "[2/4] Backup..." -ForegroundColor Yellow
    $backupDir = "$ServerDir/backup-$timestamp"
    New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
    Copy-Item -Recurse "$ServerDir/plugins/AINPC" "$backupDir/plugins-AINPC" -ErrorAction SilentlyContinue
    Copy-Item "$ServerDir/plugins/ainpc-*.jar" "$backupDir/" -ErrorAction SilentlyContinue
    Write-Host "  Backup in: $backupDir" -ForegroundColor Green
}

# 3. Copiere JAR-uri
Write-Host "[3/4] Copiere JAR-uri..." -ForegroundColor Yellow
$jars = @(
    "ainpc-core-plugin/build/libs/ainpc-core-plugin-$version.jar",
    "ainpc-scenario-medieval/build/libs/ainpc-scenario-medieval-$version.jar",
    "ainpc-api/build/libs/ainpc-api-$version.jar"
)
foreach ($jar in $jars) {
    if (Test-Path -LiteralPath $jar) {
        Copy-Item -LiteralPath $jar -Destination "$ServerDir/plugins/" -Force
        Write-Host "  Copiat: $jar" -ForegroundColor Green
    } else {
        Write-Host "  LIPS: $jar" -ForegroundColor Red
    }
}

# 4. Nota config
Write-Host "[4/4] NOTA: Dupa primul start, editati manual plugins/AINPC/config.yml:" -ForegroundColor Yellow
Write-Host "  features.routine = true"
Write-Host "  features.simulation = true"
Write-Host "  Apoi /ainpc reload"
Write-Host ""

# 5. Verificare finala
Write-Host "[5/5] Verificare..." -ForegroundColor Yellow
Get-ChildItem "$ServerDir/plugins/ainpc-*.jar" | ForEach-Object {
    Write-Host "  $($_.Name): $([math]::Round($_.Length/1KB)) KB" -ForegroundColor Green
}

Write-Host ""
Write-Host "=== Deploy complet ===" -ForegroundColor Cyan
Write-Host "Porniti serverul, apoi verificati cu /plugins si /ainpc"
