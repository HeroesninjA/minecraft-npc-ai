param(
    [switch]$Quick,
    [switch]$Smoke,
    [string]$PlayerName = "Hero"
)

$ErrorActionPreference = "Stop"

$version = "1.0.0"
$props = Get-Content -LiteralPath "gradle.properties" -ErrorAction SilentlyContinue |
    Where-Object { $_ -match '^projectVersion=(.+)$' }
if ($props) {
    $version = $matches[1]
}

$jars = @(
    "ainpc-core-plugin/build/libs/ainpc-core-plugin-$version.jar",
    "ainpc-scenario-medieval/build/libs/ainpc-scenario-medieval-$version.jar"
)
$mcpServiceJar = "ainpc-mcp-service/build/libs/ainpc-mcp-service.jar"

Write-Host "=== Setup Docker Demo AINPC ===" -ForegroundColor Cyan

Write-Host "[1/5] Verific Docker si configuratia..." -ForegroundColor Yellow
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "  Docker nu este disponibil." -ForegroundColor Red
    exit 1
}
if ([string]::IsNullOrWhiteSpace($env:RCON_PASSWORD)) {
    Write-Host "  Configureaza RCON_PASSWORD extern inainte de pornire." -ForegroundColor Red
    exit 1
}
Write-Host "  OK" -ForegroundColor Green

Write-Host "[2/5] Pregatesc JAR-urile Paper si MCP sidecar..." -ForegroundColor Yellow
$requiredBuildOutputs = @($jars + $mcpServiceJar)
$missingJars = @($requiredBuildOutputs | Where-Object { -not (Test-Path -LiteralPath $_ -PathType Leaf) })
if ($missingJars.Count -gt 0) {
    if ($Quick) {
        $missingJars | ForEach-Object { Write-Host "  LIPS: $_" -ForegroundColor Red }
        exit 1
    }
    & .\gradlew.bat clean build
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  BUILD FAIL" -ForegroundColor Red
        exit 1
    }
    $missingJars = @($requiredBuildOutputs | Where-Object { -not (Test-Path -LiteralPath $_ -PathType Leaf) })
    if ($missingJars.Count -gt 0) {
        $missingJars | ForEach-Object { Write-Host "  LIPS DUPA BUILD: $_" -ForegroundColor Red }
        exit 1
    }
}
Write-Host "  OK" -ForegroundColor Green

Write-Host "[3/5] Copiez core-ul si addonul in volumul demo..." -ForegroundColor Yellow
$pluginDir = "paper-data/plugins"
if (-not (Test-Path -LiteralPath $pluginDir -PathType Container)) {
    New-Item -ItemType Directory -Path $pluginDir -Force | Out-Null
}
Get-ChildItem -LiteralPath $pluginDir -Filter "ainpc-api-*.jar" -File -ErrorAction SilentlyContinue |
    ForEach-Object { Remove-Item -LiteralPath $_.FullName -Force }
foreach ($jar in $jars) {
    Copy-Item -LiteralPath $jar -Destination $pluginDir -Force
    Write-Host "  $jar" -ForegroundColor Gray
}
Write-Host "  OK" -ForegroundColor Green

Write-Host "[4/5] Pornesc containerul..." -ForegroundColor Yellow
& docker compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "  Docker compose FAIL" -ForegroundColor Red
    exit 1
}
Write-Host "  OK" -ForegroundColor Green

Write-Host "[5/5] Verific starea containerului..." -ForegroundColor Yellow
Start-Sleep -Seconds 5
$status = & docker ps --filter "name=ainpc-demo" --format "{{.Status}}"
Write-Host "  Container: $status" -ForegroundColor Gray
Write-Host "Server Paper demo pornit la localhost:25565." -ForegroundColor Green
Write-Host "RCON foloseste secretul furnizat extern." -ForegroundColor Green

if ($Smoke) {
    Write-Host "=== Executie RCON legacy; validarea manuala ramane obligatorie ===" -ForegroundColor Cyan
    Start-Sleep -Seconds 10
    & ".\scripts\smoke-demo-complet.ps1" `
        -Rcon `
        -RconHost localhost `
        -RconPort 25575 `
        -PlayerName $PlayerName `
        -Quick `
        -NoBackup
}

Write-Host "Oprire: docker compose down"
Write-Host "Loguri: docker compose logs -f"
