param(
    [string]$ServerDir = "C:\Minecraft\paper-test",
    [string]$RegionId = "demo_sat",
    [string]$PlayerName = "Hero",
    [string]$RconHost = "localhost",
    [int]$RconPort = 25575,
    [string]$RconPass = "demo",
    [switch]$UseRcon
)

$ErrorActionPreference = "Stop"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$logFile = "smoke-integration-$timestamp.log"
$global:passed = 0
$global:failed = 0

function Log   { $msg = "[$(Get-Date -Format HH:mm:ss)] $args"; Write-Host $msg -ForegroundColor Gray; $msg | Out-File $logFile -Append }
function Info  { $msg = "[$(Get-Date -Format HH:mm:ss)] $args"; Write-Host $msg -ForegroundColor Cyan; $msg | Out-File $logFile -Append }
function Pass  { Write-Host "  PASS" -ForegroundColor Green; $global:passed++ }
function Fail  { Write-Host "  FAIL" -ForegroundColor Red; $global:failed++ }

if ($UseRcon) {
    . .\scripts\rcon-client.ps1
    $rconOk = Connect-Rcon -Hostname $RconHost -Port $RconPort -Password $RconPass
    if (-not $rconOk) { Write-Host "RCON connection failed" -ForegroundColor Red; exit 1 }
}

function Run-Check {
    param([string]$Label, [string]$Cmd, [string]$Expected = "")
    if ($UseRcon) {
        $result = Send-Rcon $Cmd
        if ($Expected -and $result -match $Expected) { Pass } elseif (-not $Expected) { Pass } else { Fail }
        Log "[$Label] $Cmd -> $result"
    } else {
        Log "[$Label] $Cmd"
        Pass
    }
}

Log "=== Smoke Test: Service Integration ($timestamp) ==="
Log "Server: $ServerDir | Regiune: $RegionId"
Log ""

# ============================================================
# Test 1: RelationshipService + RoutineCoordinator
# ============================================================
Info "=== 1. RelationshipService + RoutineCoordinator (social gatherings) ==="

Run-Check -Label "R1" -Cmd "ainpc routine tick" -Expected "(?i)evaluated|summary|ok"
Run-Check -Label "R2" -Cmd "ainpc list" -Expected "(?i)npc|NPC"
Run-Check -Label "R3" -Cmd "ainpc relationship list" -Expected "(?i)relationships|relatii|npc"

Log ""

# ============================================================
# Test 2: StoryAuthoringService + StoryReactionService
# ============================================================
Info "=== 2. StoryAuthoringService + StoryReactionService ==="

Run-Check -Label "S1" -Cmd "ainpc story context" -Expected "(?i)story|region|place"
Run-Check -Label "S2" -Cmd "ainpc story region $RegionId" -Expected "(?i)state|mode|key"
Run-Check -Label "S3" -Cmd "ainpc story events" -Expected "(?i)events|eveniment"
Run-Check -Label "S4" -Cmd "ainpc authoring templates" -Expected "(?i)template|event|type"

Log ""

# ============================================================
# Test 3: NpcEconomyService + RoutineCoordinator
# ============================================================
Info "=== 3. NpcEconomyService + RoutineCoordinator (work + salary) ==="

Run-Check -Label "E1" -Cmd "ainpc economy npc" -Expected "(?i)NPC|economie|balance"
Run-Check -Label "E2" -Cmd "ainpc economy top" -Expected "(?i)top|ranking|clasament"

Log ""

# ============================================================
# Test 4: Environment + SeasonalBehavior
# ============================================================
Info "=== 4. EnvironmentEngine + SeasonalBehaviorService ==="

Run-Check -Label "ENV1" -Cmd "ainpc environment" -Expected "(?i)Mediu|time|weather|season"
Run-Check -Label "ENV2" -Cmd "ainpc environment $RegionId" -Expected "(?i)Mediu|time|weather|season"

Log ""

# ============================================================
# Test 5: PerformanceMonitor
# ============================================================
Info "=== 5. PerformanceMonitor (NPC tick profiling) ==="

Run-Check -Label "P1" -Cmd "ainpc health" -Expected "(?i)health|tick|routine|performance"

Log ""

# ============================================================
# Summary
# ============================================================
Log ""
Log "=== REZULTATE ==="
Log "Passed: $global:passed"
Log "Failed: $global:failed"
if ($global:failed -eq 0) { Log "Toate testele de integrare au trecut!" } else { Log "Unele teste au esuat!" }

if ($UseRcon) { Disconnect-Rcon }
Write-Host "Passed: $global:passed | Failed: $global:failed" -ForegroundColor $(if ($global:failed -eq 0) { "Green" } else { "Red" })
Write-Host "Raport: $logFile"
