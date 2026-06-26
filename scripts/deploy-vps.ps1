param(
    [string]$VpsHost = "141.147.48.170",
    [string]$VpsUser = "ubuntu",
    [string]$SshKey = "C:\Users\HeroesninjA\Downloads\iordacheemanuel0.key",
    [string]$RemotePluginDir = "/home/ubuntu/1.21/plugins",
    [string]$RconPassword = "demo",
    [int]$RconPort = 25575,
    [int]$BootWait = 40,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path "$ScriptDir\.."

Write-Host "=== Deploy AINPC pe VPS ($VpsHost) ===" -ForegroundColor Cyan

# 1. Build
if (-not $SkipBuild) {
    Write-Host "[1/4] Build..." -ForegroundColor Yellow
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

# 2. SCP jars to VPS
Write-Host "[2/4] Transfer JAR-uri..." -ForegroundColor Yellow
foreach ($jar in $jars) {
    $name = Split-Path -Leaf $jar
    Write-Host "  $name..." -ForegroundColor Gray
    & scp -i $SshKey $jar "$VpsUser@${VpsHost}:$RemotePluginDir/" 2>&1
}
Write-Host "  Transfer complet" -ForegroundColor Green

# 3. Restart server via RCON + notify
Write-Host "[3/4] Se trimite comanda de restart..." -ForegroundColor Yellow
$rcCmd = "python3 /tmp/rc.py 'stop'; sleep 5"
ssh -i "$SshKey" "$VpsUser@$VpsHost" $rcCmd
Write-Host "  Serverul se va reincepe automat (mc-server-runner)" -ForegroundColor Green
Write-Host "  Se asteapta $BootWait secunde..." -ForegroundColor Gray
Start-Sleep -Seconds $BootWait

# 4. Verify
Write-Host "[4/4] Verificare..." -ForegroundColor Yellow
$verifyCmd = "python3 /tmp/rc.py 'ainpc version'"
$version = ssh -i "$SshKey" "$VpsUser@$VpsHost" $verifyCmd
Write-Host "  $version" -ForegroundColor Gray

Write-Host ""
Write-Host "=== Deploy complet ===" -ForegroundColor Cyan
Write-Host "Ruleaza smoke test: python3 .ai/smoke_test.py" -ForegroundColor Gray
