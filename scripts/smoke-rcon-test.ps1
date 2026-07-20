param(
    [string]$ServerDir = "C:\Users\HeroesninjA\IdeaProjects\test\paper-data\data",
    [string]$RconHost = "127.0.0.1",
    [int]$RconPort = 25575,
    [string]$RconPassword = "demo",
    [int]$StartTimeoutSec = 30,
    [int]$CmdDelayMs = 300
)

function Receive-RconPacketFixed {
    param($Stream)
    $lenBuf = New-Object byte[] 4
    $read = 0
    while ($read -lt 4) {
        $r = $Stream.Read($lenBuf, $read, 4 - $read)
        if ($r -le 0) { return $null }
        $read += $r
    }
    $len = [System.BitConverter]::ToInt32($lenBuf, 0)
    if ($len -le 0 -or $len -gt 16384) { return $null }
    $data = New-Object byte[] $len
    $read = 0
    while ($read -lt $len) {
        $r = $Stream.Read($data, $read, $len - $read)
        if ($r -le 0) { break }
        $read += $r
    }
    if ($data.Length -lt 8) { return $null }
    $reqId = [System.BitConverter]::ToInt32($data, 0)
    $type = [System.BitConverter]::ToInt32($data, 4)
    $bodyEnd = [Array]::IndexOf($data, 0, 8)
    if ($bodyEnd -eq -1) { $bodyEnd = $data.Length }
    $body = if ($bodyEnd -gt 8) { [System.Text.Encoding]::ASCII.GetString($data, 8, $bodyEnd - 8) } else { "" }
    return @{ RequestId = $reqId; Type = $type; Body = $body }
}

function Send-RconPacketFixed {
    param($Stream, [int]$Type, [string]$Body, [ref]$RequestId)
    $id = $RequestId.Value; $RequestId.Value++
    $bodyBytes = [System.Text.Encoding]::ASCII.GetBytes($Body + "`0")
    $payload = [System.BitConverter]::GetBytes($id) + [System.BitConverter]::GetBytes($Type) + $bodyBytes
    $len = [System.BitConverter]::GetBytes($payload.Length)
    $packet = $len + $payload
    $Stream.Write($packet, 0, $packet.Length)
    return $id
}

function Send-RconFixed {
    param($Stream, [string]$Command, [ref]$RequestId)
    $reqId = Send-RconPacketFixed -Stream $Stream -Type 2 -Body $Command -RequestId $RequestId
    Start-Sleep -Milliseconds 50
    $resp = Receive-RconPacketFixed -Stream $Stream
    if (-not $resp) { return "" }
    $fullBody = $resp.Body
    for ($i = 0; $i -lt 5; $i++) {
        Start-Sleep -Milliseconds 100
        $extra = Receive-RconPacketFixed -Stream $Stream
        if ($extra -and $extra.RequestId -eq $reqId) { $fullBody += $extra.Body }
        else { break }
    }
    return $fullBody
}

function Write-Result {
    param([string]$Label, [string]$Body, [bool]$FailOnEmpty = $true)
    $trimmed = $Body.Trim()
    if ($trimmed -eq "" -and $FailOnEmpty) {
        Write-Host "  FAIL: $Label -> (empty response)" -ForegroundColor Red
        return $false
    }
    $lines = $trimmed -split "`n" | Where-Object { $_.Trim() -ne "" }
    $count = $lines.Count
    $preview = ($lines | Select-Object -First 3) -join " || "
    if ($preview.Length -gt 120) { $preview = $preview.Substring(0, 120) + "..." }
    Write-Host "  OK [$Label] ($count lines): $preview" -ForegroundColor Green
    return $true
}

# --- Start server ---
Write-Host "=== AINPC Smoke Test ===" -ForegroundColor Cyan
Write-Host "Starting Paper server from $ServerDir ..." -ForegroundColor Yellow

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = "java"
$psi.Arguments = "-jar paper-26.1.2-72.jar --nogui"
$psi.WorkingDirectory = $ServerDir
$psi.UseShellExecute = $false
$psi.CreateNoWindow = $true
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$proc = [System.Diagnostics.Process]::Start($psi)

$startTime = Get-Date
$serverReady = $false
$stdout = ""
while (-not $serverReady -and ((Get-Date) - $startTime).TotalSeconds -lt $StartTimeoutSec) {
    Start-Sleep -Seconds 1
    if ($proc.HasExited) {
        Write-Host "Server exited prematurely!" -ForegroundColor Red
        Write-Host $proc.StandardOutput.ReadToEnd()
        exit 1
    }
    $line = $proc.StandardOutput.ReadLine()
    while ($line -ne $null) {
        $stdout += $line + "`n"
        if ($line -match "Done \(.*\)! For help") { $serverReady = $true }
        if ($line -match "ERROR|eroare") { Write-Host "  [SERVER] $line" -ForegroundColor Red }
        $line = $proc.StandardOutput.ReadLine()
    }
}
if (-not $serverReady) {
    Write-Host "Server did not start within ${StartTimeoutSec}s!" -ForegroundColor Red
    $proc.Kill()
    exit 1
}
Write-Host "Server ready!" -ForegroundColor Green

# --- RCON ---
Write-Host "`nConnecting RCON..." -ForegroundColor Yellow
$tcp = New-Object System.Net.Sockets.TcpClient
$tcp.Connect($RconHost, $RconPort)
$tcp.NoDelay = $true
$stream = $tcp.GetStream()
$reqId = 1
$authReqId = Send-RconPacketFixed -Stream $stream -Type 3 -Body $RconPassword -RequestId ([ref]$reqId)
$authResp = Receive-RconPacketFixed -Stream $stream
if (-not $authResp -or $authResp.RequestId -eq -1) {
    Write-Host "RCON auth failed!" -ForegroundColor Red
    $stream.Close(); $tcp.Close(); $proc.Kill()
    exit 1
}
Write-Host "RCON connected!" -ForegroundColor Green

# --- Run commands ---
$tests = @(
    @{Cmd = "ainpc version"; Label = "Plugin Version"},
    @{Cmd = "ainpc status"; Label = "Plugin Status"},
    @{Cmd = "ainpc npc list"; Label = "NPC List"},
    @{Cmd = "ainpc world region list"; Label = "World Regions"},
    @{Cmd = "ainpc world place list"; Label = "World Places"},
    @{Cmd = "ainpc quest list"; Label = "Quest List"},
    @{Cmd = "ainpc economy balance"; Label = "Economy Balance"},
    @{Cmd = "ainpc guild list"; Label = "Guild List"},
    @{Cmd = "ainpc mapping audit"; Label = "Mapping Audit"},
    @{Cmd = "ainpc story events"; Label = "Story Events"},
    @{Cmd = "ainpc help"; Label = "Plugin Help"}
)

$passed = 0; $failed = 0
foreach ($t in $tests) {
    Start-Sleep -Milliseconds $CmdDelayMs
    $resp = Send-RconFixed -Stream $stream -Command $t.Cmd -RequestId ([ref]$reqId)
    if (Write-Result -Label $t.Label -Body $resp -FailOnEmpty ($t.Label -ne "Plugin Help")) {
        $passed++
    } else {
        Write-Host "    (raw: '$resp')" -ForegroundColor DarkGray
        $failed++
    }
}

# --- Cleanup ---
Write-Host "`n=== Results: $passed passed, $failed failed ===" -ForegroundColor Cyan
$stream.Close(); $tcp.Close()
Send-RconFixed -Stream $stream -Command "stop" -RequestId ([ref]$reqId) 2>$null
Start-Sleep -Seconds 3
if (-not $proc.HasExited) { $proc.Kill() }
exit ($failed -gt 0)
