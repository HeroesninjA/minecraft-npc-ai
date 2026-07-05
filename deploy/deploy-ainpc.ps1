param(
    [string]$VpsHost = "141.147.48.170",
    [string]$VpsUser = "ubuntu",
    [string]$SshKey = "C:\Users\HeroesninjA\Downloads\iordacheemanuel0.key",
    [string]$ServerUuid = "cd8c1a26-8639-40fd-9e52-aa95bfc11f3c",
    [switch]$SkipBuild,
    [switch]$SkipRestart,
    [switch]$SkipCleanup
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

# 3. Transfer JARs to VPS (via temp dir, then sudo mv)
Write-Host "[2/5] Transfer JAR-uri la VPS..." -ForegroundColor Yellow
$RemotePluginDir = "/var/lib/pterodactyl/$ServerUuid/plugins/"
$RemoteTmpDir = "/tmp/ainpc-deploy/"
ssh -i "$SshKey" "$VpsUser@$VpsHost" "mkdir -p $RemoteTmpDir" 2>&1
foreach ($jar in $jars) {
    $name = Split-Path -Leaf $jar
    Write-Host "  $name..." -ForegroundColor Gray
    & scp -i $SshKey $jar "$VpsUser@${VpsHost}:${RemoteTmpDir}" 2>&1
    if ($LASTEXITCODE -ne 0) { Write-Host "  SCP failed!" -ForegroundColor Red; exit 1 }
}
Write-Host "  Transfer OK" -ForegroundColor Green

# 4. Cleanup old AINPC jars (all, we copy fresh ones next)
if (-not $SkipCleanup) {
    Write-Host "[3/6] Cleanup JAR-uri vechi..." -ForegroundColor Yellow
    ssh -i "$SshKey" "$VpsUser@$VpsHost" "sudo rm -f ${RemotePluginDir}ainpc-core-plugin*.jar ${RemotePluginDir}ainpc-scenario-medieval*.jar ${RemotePluginDir}ainpc-api*.jar" 2>&1
    Write-Host "  Cleanup OK" -ForegroundColor Green
}

# 5. Move JARs to plugin dir with sudo and fix permissions
Write-Host "[4/6] Mut JAR-uri si setez permisiuni..." -ForegroundColor Yellow
foreach ($jar in $jars) {
    $name = Split-Path -Leaf $jar
    ssh -i "$SshKey" "$VpsUser@$VpsHost" "sudo mv ${RemoteTmpDir}$name $RemotePluginDir && sudo chown pterodactyl:pterodactyl $RemotePluginDir$name && sudo chmod 644 $RemotePluginDir$name" 2>&1
}
ssh -i "$SshKey" "$VpsUser@$VpsHost" "rm -rf $RemoteTmpDir" 2>&1
Write-Host "  Permisiuni OK" -ForegroundColor Green

# 6. Restart server via docker restart (fallback pana cand Wings API e functional)
if (-not $SkipRestart) {
    Write-Host "[5/6] Restart server..." -ForegroundColor Yellow
    Write-Host "  Se reporneste containerul..." -ForegroundColor Gray
    ssh -i "$SshKey" "$VpsUser@$VpsHost" "sudo docker restart $ServerUuid" 2>&1
    Write-Host "  Server restartat (asteapta ~30s pentru incarcare)" -ForegroundColor Green
}

# 7. Verify
Write-Host "[6/6] Verificare..." -ForegroundColor Yellow
Start-Sleep -Seconds 20
$verifyCmd = "sudo docker logs $ServerUuid 2>&1 | grep -E 'Done!|AINPC.*enabled|AINPC.*Feature Packs' | tail -3"
$result = ssh -i "$SshKey" "$VpsUser@$VpsHost" $verifyCmd 2>&1
Write-Host "  $result" -ForegroundColor Gray

Write-Host ""
Write-Host "=== Deploy complet ===" -ForegroundColor Cyan
Write-Host "Plugin-urile au fost actualizate pe server." -ForegroundColor White
