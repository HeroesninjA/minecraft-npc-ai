param(
    [string]$VpsHost = "141.147.48.170",
    [string]$VpsUser = "ubuntu",
    [string]$SshKey = "C:\Users\HeroesninjA\Downloads\iordacheemanuel0.key",
    [string]$RemotePluginDir = "/home/ubuntu/1.21/plugins",
    [string]$RconPassword = "demo",
    [int]$RconPort = 25575,
    [int]$BootWait = 40,
    [string]$PlayerName = "Hero",
    [string]$RegionId = "demo_sat",
    [switch]$SkipBuild,
    [switch]$SkipDeploy
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path "$ScriptDir\.."
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$logFile = Join-Path $ProjectRoot "smoke-vps-$timestamp.log"
$global:passed = 0
$global:failed = 0

function Log   { $msg = "[$(Get-Date -Format HH:mm:ss)] $args"; Write-Host $msg -ForegroundColor Gray; $msg | Out-File $logFile -Append }
function Info  { $msg = "[$(Get-Date -Format HH:mm:ss)] $args"; Write-Host $msg -ForegroundColor Cyan; $msg | Out-File $logFile -Append }
function Ok    { Write-Host "    OK" -ForegroundColor Green; $global:passed++ }
function Fail  { Write-Host "    FAIL" -ForegroundColor Red; $global:failed++; $script:failCount++ }
function Step  { param([string]$Label) Info "--- $Label ---"; $script:failCount = 0 }

function RconCmd($cmd) {
    $quoted = $cmd -replace "'", "'\\''"
    $result = ssh -i "$SshKey" "$VpsUser@$VpsHost" "python3 /tmp/rc.py '$quoted'" 2>&1
    return $result
}

function Run-Check($phase, $cmd) {
    try {
        $result = RconCmd $cmd
        $text = "$result"
        $text = $text -replace "`0", "" -replace "[^ -~§]", " " -replace "\s+", " "
        $trimmed = $text.Trim()
        if ($trimmed.Length -gt 0 -and $trimmed -notmatch "(error|failed|exception|Traceback|Unknown command|not found|Usage: /)") {
            if ($trimmed.Length -gt 400) {
                Write-Host "    ($($trimmed.Length) chars output)" -ForegroundColor Gray
                $trimmed.Substring(0, [Math]::Min(200, $trimmed.Length)) -split "`n" | Select-Object -First 3 | ForEach-Object {
                    Write-Host "    $_..." -ForegroundColor Gray
                }
            } else {
                $trimmed -split "`n" | Where-Object { $_.Trim() } | ForEach-Object {
                    Write-Host "    $_" -ForegroundColor Gray
                }
            }
            Ok
        } elseif ($trimmed -match "§[a-f0-9]" -or $trimmed.StartsWith("Plugins") -or $trimmed.Contains("§6")) {
            Write-Host "    ($($trimmed.Length) chars)" -ForegroundColor Gray
            Ok
        } else {
            Write-Host "    $trimmed" -ForegroundColor Yellow
            Fail
        }
        Log "[$phase] $cmd"
    } catch {
        Write-Host "    ERROR: $_" -ForegroundColor Red
        Fail
        Log "[$phase] $cmd -> ERROR: $_"
    }
    Start-Sleep -Milliseconds 500
}

Log "=== Smoke VPS AINPC ==="
Log "VPS: $VpsHost | Player: $PlayerName | Region: $RegionId"
Log "Log: $logFile`n"

# =====================================================================
# 1. BUILD
if (-not $SkipBuild) {
    Step "BUILD"
    Push-Location $ProjectRoot
    try {
        & .\gradlew.bat :ainpc-core-plugin:jar :ainpc-scenario-medieval:jar 2>&1
        if ($LASTEXITCODE -eq 0) { Ok } else { Fail; exit 1 }
    } finally { Pop-Location }
}

# =====================================================================
# 2. DEPLOY
if (-not $SkipDeploy) {
    Step "DEPLOY"
    $jars = @(
        "$ProjectRoot\ainpc-core-plugin\build\libs\ainpc-core-plugin-1.0.0.jar",
        "$ProjectRoot\ainpc-scenario-medieval\build\libs\ainpc-scenario-medieval-1.0.0.jar"
    )
    foreach ($jar in $jars) {
        $name = Split-Path -Leaf $jar
        Write-Host "  $name..." -ForegroundColor Gray
        & scp -i $SshKey $jar "$VpsUser@${VpsHost}:$RemotePluginDir/" 2>&1
    }
    Ok
}

# =====================================================================
# 3. RESTART + WAIT
Step "RESTART"
ssh -i "$SshKey" "$VpsUser@$VpsHost" "python3 /tmp/rc.py 'stop'; sleep 5" 2>&1
Write-Host "  Se asteapta $BootWait secunde..." -ForegroundColor Gray
Start-Sleep -Seconds $BootWait
Ok

# =====================================================================
$steps = @(
    @("PLUGIN", "plugins"),
    @("PLUGIN", "ainpc"),
    @("MAPPING", "ainpc world demo create $RegionId"),
    @("MAPPING", "ainpc world places $RegionId"),
    @("MAPPING", "ainpc world save"),
    @("MAPPING", "ainpc audit world"),
    @("NPC", "ainpc world settlement plan $RegionId"),
    @("NPC", "ainpc world settlement spawn $RegionId"),
    @("NPC", "ainpc list"),
    @("NPC", "ainpc audit spawn"),
    @("QUEST", "ainpc progression definitions"),
    @("QUEST", "ainpc quest nearest"),
    @("QUEST", "ainpc story context"),
    @("QUEST", "ainpc story events"),
    @("QUEST", "ainpc quest status nearest"),
    @("QUEST", "ainpc progression stored $PlayerName"),
    @("AUDIT", "ainpc audit quest"),
    @("AUDIT", "ainpc audit db"),
    @("DEMO", "ainpc demo status $RegionId")
)

$currentPhase = ""
foreach ($step in $steps) {
    $phase = $step[0]
    $cmd = $step[1]
    if ($phase -ne $currentPhase) { $currentPhase = $phase; Step $phase }
    Run-Check $phase $cmd
}

# =====================================================================
Log ""
Log "=== SUMMARY ==="
Log "Passed: $global:passed | Failed: $global:failed"
Log ""

if ($global:failed -gt 0) {
    Write-Host "REZULTAT: $global:passed passed, $global:failed FAILED" -ForegroundColor Red
    exit 1
} else {
    Write-Host "REZULTAT: $global:passed passed, 0 failed" -ForegroundColor Green
}
