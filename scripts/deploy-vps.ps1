param(
    [string]$VpsHost = "141.147.48.170",
    [string]$VpsUser = "ubuntu",
    [string]$SshKey = "C:\Users\HeroesninjA\Downloads\iordacheemanuel0.key",
    [string]$ContainerName = "ainpc-demo",
    [string]$PluginsDir = "/home/ubuntu/testserver/paper-data/plugins",
    [int]$BootWait = 40,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path "$ScriptDir\.."

Write-Host "=== Deploy AINPC pe VPS ($VpsHost) ===" -ForegroundColor Cyan
Write-Host ""

# 1. Build
if (-not $SkipBuild) {
    Write-Host "[1/4] Build..." -ForegroundColor Yellow
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.2"
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    Push-Location $ProjectRoot
    try {
        $result = & .\gradlew.bat :ainpc-core-plugin:jar :ainpc-scenario-medieval:jar :ainpc-api:jar 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Host "BUILD FAILED!" -ForegroundColor Red
            exit 1
        }
        Write-Host "  Build OK" -ForegroundColor Green
    } finally {
        Pop-Location
    }
}

$jars = @(
    "$ProjectRoot\ainpc-core-plugin\build\libs\ainpc-core-plugin-1.0.0.jar",
    "$ProjectRoot\ainpc-scenario-medieval\build\libs\ainpc-scenario-medieval-1.0.0.jar",
    "$ProjectRoot\ainpc-api\build\libs\ainpc-api-1.0.0.jar"
)

# 2. Stop container + backup + SCP
Write-Host "[2/4] Oprire container + backup + transfer..." -ForegroundColor Yellow
$sshBase = "ssh -i `"$SshKey`" -o ConnectTimeout=10 $VpsUser@$VpsHost"

$stopCmd = "docker stop $ContainerName && cp -r $PluginsDir $PluginsDir-backup-temp && echo backup-ok"
$stopResult = & cmd /c "$sshBase `"$stopCmd`"" 2>&1
Write-Host "  Container oprit" -ForegroundColor Green

foreach ($jar in $jars) {
    $name = Split-Path -Leaf $jar
    Write-Host "  Transfer $name..." -ForegroundColor Gray
    & scp -i $SshKey $jar "$VpsUser@${VpsHost}:$PluginsDir/" 2>&1
}
Write-Host "  Transfer complet" -ForegroundColor Green

# 3. Start container
Write-Host "[3/4] Pornire container..." -ForegroundColor Yellow
$startResult = & cmd /c "$sshBase `"docker start $ContainerName`"" 2>&1
Write-Host "  Container pornit, astept $BootWait s..." -ForegroundColor Green
Start-Sleep -Seconds $BootWait

# 4. Verify
Write-Host "[4/4] Verificare..." -ForegroundColor Yellow
$plugins = & cmd /c "$sshBase `"docker exec $ContainerName rcon-cli 'plugins'`"" 2>&1
$audit = & cmd /c "$sshBase `"docker exec $ContainerName rcon-cli 'ainpc audit'`"" 2>&1

Write-Host ""
Write-Host "=== Rezultat ===" -ForegroundColor Cyan
if ($plugins) { Write-Host $plugins }
if ($audit) { Write-Host $audit }
Write-Host "=== Deploy complet ===" -ForegroundColor Cyan
