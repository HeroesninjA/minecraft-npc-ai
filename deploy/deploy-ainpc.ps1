param(
    [string]$VpsHost = "141.147.48.170",
    [string]$VpsUser = "ubuntu",
    [string]$SshKey = "C:\Users\HeroesninjA\Downloads\iordacheemanuel0.key",
    [string]$ServerUuid = "cd8c1a26-8639-40fd-9e52-aa95bfc11f3c",
    [string]$PanelUrl = "http://141.147.48.170:8081",
    [string]$PanelUser = "admin",
    [string]$PanelPass = "f9ddcb53eaae889b09cfba0a",
    [switch]$SkipBuild,
    [switch]$SkipRestart
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path "$ScriptDir\.."

Write-Host "=== Deploy AINPC plugin to Pterodactyl ($VpsHost) ===" -ForegroundColor Cyan

# 1. Build
if (-not $SkipBuild) {
    Write-Host "[1/5] Build..." -ForegroundColor Yellow
    Push-Location $ProjectRoot
    try {
        & .\gradlew.bat :ainpc-core-plugin:jar :ainpc-scenario-medieval:jar 2>&1
        if ($LASTEXITCODE -ne 0) { Write-Host "BUILD FAILED!" -ForegroundColor Red; exit 1 }
        Write-Host "  Build OK" -ForegroundColor Green
    } finally { Pop-Location }
}

$jars = @(
    "$ProjectRoot\ainpc-core-plugin\build\libs\ainpc-core-plugin-1.0.0.jar",
    "$ProjectRoot\ainpc-scenario-medieval\build\libs\ainpc-scenario-medieval-1.0.0.jar"
)

# 2. Verify JARs exist
foreach ($jar in $jars) {
    if (-not (Test-Path $jar)) { Write-Host "JAR not found: $jar" -ForegroundColor Red; exit 1 }
}

# 3. Transfer JARs to VPS
Write-Host "[2/5] Transfer JAR-uri la VPS..." -ForegroundColor Yellow
$RemotePluginDir = "/var/lib/pterodactyl/$ServerUuid/plugins/"
foreach ($jar in $jars) {
    $name = Split-Path -Leaf $jar
    Write-Host "  $name..." -ForegroundColor Gray
    & scp -i $SshKey $jar "$VpsUser@${VpsHost}:$RemotePluginDir" 2>&1
    if ($LASTEXITCODE -ne 0) { Write-Host "  SCP failed!" -ForegroundColor Red; exit 1 }
}
Write-Host "  Transfer OK" -ForegroundColor Green

# 4. Fix permissions on VPS
Write-Host "[3/5] Set permisiuni..." -ForegroundColor Yellow
ssh -i "$SshKey" "$VpsUser@$VpsHost" "sudo chown pterodactyl:pterodactyl $RemotePluginDir*.jar 2>/dev/null; sudo chmod 644 $RemotePluginDir*.jar 2>/dev/null"
Write-Host "  Permisiuni OK" -ForegroundColor Green

# 5. Restart server via Panel API
if (-not $SkipRestart) {
    Write-Host "[4/5] Restart server..." -ForegroundColor Yellow
    Write-Host "  Serverul va fi repornit pentru a incarca noile plugin-uri." -ForegroundColor Gray
    
    # Restart via SSH command to Wings API
    $restartCmd = "curl -s -X POST 'http://localhost:8080/api/servers/$ServerUuid/power' -H 'Authorization: Bearer 0c0oUWMqhpqTzujB0HC29es3mEhYoJpHAdzf2ZrW7WfSerOAg3VyhxjAlLYdeNvn' -H 'Content-Type: application/json' -d '{\"action\":\"restart\"}'"
    $result = ssh -i "$SshKey" "$VpsUser@$VpsHost" $restartCmd 2>&1
    Write-Host "  $result" -ForegroundColor Gray
    Write-Host "  Server restartat (asteapta ~30s pentru incarcare)" -ForegroundColor Green
}

# 6. Verify
Write-Host "[5/5] Verificare..." -ForegroundColor Yellow
Start-Sleep -Seconds 5
$verifyCmd = "docker logs $ServerUuid 2>&1 | grep -E 'Done!|AINPC.*enabled' | tail -2"
$result = ssh -i "$SshKey" "$VpsUser@$VpsHost" $verifyCmd 2>&1
Write-Host "  $result" -ForegroundColor Gray

Write-Host ""
Write-Host "=== Deploy complet ===" -ForegroundColor Cyan
Write-Host "Plugin-urile au fost actualizate pe server." -ForegroundColor White
