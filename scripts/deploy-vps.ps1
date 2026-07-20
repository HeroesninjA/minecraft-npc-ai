param(
    [string]$VpsHost = $env:AINPC_VPS_HOST,
    [string]$VpsUser = $env:AINPC_VPS_USER,
    [string]$SshKey = $env:AINPC_SSH_KEY,
    [string]$RemotePluginDir = $env:AINPC_REMOTE_PLUGIN_DIR,
    [string]$RconPassword = $env:RCON_PASSWORD,
    [int]$RconPort = 25575,
    [int]$BootWait = 40,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path "$ScriptDir\.."

if (-not $VpsHost) { throw "AINPC_VPS_HOST nu este setat. Furnizeaza host-ul VPS." }
if (-not $VpsUser) { throw "AINPC_VPS_USER nu este setat. Furnizeaza user-ul VPS." }
if (-not $SshKey) { throw "AINPC_SSH_KEY nu este setat. Furnizeaza calea catre cheia SSH." }
if (-not $RemotePluginDir) { throw "AINPC_REMOTE_PLUGIN_DIR nu este setat." }
if (-not $RconPassword) { throw "RCON_PASSWORD nu este setat. Furnizeaza secretul extern." }

Write-Host "=== Deploy AINPC pe VPS ($VpsHost) ===" -ForegroundColor Cyan

$version = "1.0.0"
$props = Get-Content -LiteralPath "$ProjectRoot\gradle.properties" -ErrorAction SilentlyContinue | Where-Object { $_ -match '^projectVersion=(.+)$' }
if ($props) { $version = $matches[1] }

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
    "$ProjectRoot\ainpc-core-plugin\build\libs\ainpc-core-plugin-$version.jar",
    "$ProjectRoot\ainpc-scenario-medieval\build\libs\ainpc-scenario-medieval-$version.jar"
)

# 2. SCP jars to VPS
Write-Host "[2/4] Transfer JAR-uri..." -ForegroundColor Yellow
foreach ($jar in $jars) {
    if (-not (Test-Path $jar)) {
        Write-Host "  LIPS: $jar" -ForegroundColor Red
        Write-Host "  Ruleaza fara -SkipBuild sau verifica versiunea in gradle.properties" -ForegroundColor Yellow
        exit 1
    }
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
Write-Host "Ruleaza smoke test: .\scripts\smoke-demo-complet.ps1 -Rcon" -ForegroundColor Gray
