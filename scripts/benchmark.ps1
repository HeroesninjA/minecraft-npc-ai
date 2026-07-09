param(
    [string]$ServerDir = "",
    [string]$RconHost = "127.0.0.1",
    [int]$RconPort = 25575,
    [string]$RconPassword = "ainpc",
    [int]$Iterations = 3,
    [int]$IntervalSeconds = 30,
    [string]$OutputDir = "",
    [string]$PlayerName = "",
    [string]$RegionId = "demo_sat",
    [switch]$Help
)

if ($Help) {
    Write-Host @"
Usage: .\scripts\benchmark.ps1 [-ServerDir path] [-Iterations 3] [-IntervalSeconds 30] [-PlayerName name]

Measures:
  - NPC routine tick time (via /ainpc health)
  - Database query time (via cache stats)
  - Memory usage (Paper TPS)
  - NPC count and performance metrics

Results saved to build/benchmark/ with timestamp.
"@
    return
}

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $OutputDir) { $OutputDir = Join-Path $repoRoot "build\benchmark" }
if (-not (Test-Path -LiteralPath $OutputDir)) { New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null }

$rconScript = Join-Path $PSScriptRoot "rcon-client.ps1"
if (-not (Test-Path -LiteralPath $rconScript)) {
    throw "rcon-client.ps1 nu a fost gasit in $PSScriptRoot"
}

function Run-RconCommand {
    param([string]$Command)
    $result = & $rconScript -Host $RconHost -Port $RconPort -Password $RconPassword -Command $Command 2>&1
    return $result
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$results = @()

Write-Host "Benchmark AINPC - $timestamp"
Write-Host "Iteratii: $Iterations, interval: ${IntervalSeconds}s"
Write-Host ""

for ($i = 1; $i -le $Iterations; $i++) {
    Write-Host "--- Iteratia $i/$Iterations ---"

    $sample = @{
        iteration = $i
        timestamp = (Get-Date -Format "o")
    }

    $health = Run-RconCommand -Command "ainpc health"
    $sample.health_raw = $health

    if ($health -match 'avg[=:]\s*([\d.]+)') {
        $sample.routine_avg_ms = [double]$matches[1]
    }
    if ($health -match 'min[=:]\s*([\d.]+)') {
        $sample.routine_min_ms = [double]$matches[1]
    }
    if ($health -match 'max[=:]\s*([\d.]+)') {
        $sample.routine_max_ms = [double]$matches[1]
    }
    if ($health -match 'NPC[=:]\s*(\d+)') {
        $sample.npc_count = [int]$matches[1]
    }

    $tps = Run-RconCommand -Command "tps"
    $sample.tps_raw = $tps
    if ($tps -match '(\d+\.\d+)\s*/\s*(\d+\.\d+)') {
        $sample.tps_1m = [double]$matches[1]
        $sample.tps_5m = [double]$matches[2]
    }

    $mem = Run-RconCommand -Command "memory"
    $sample.memory_raw = $mem
    if ($mem -match 'Used:\s*([\d.]+)\s*(MB|GB)') {
        $sample.memory_used_mb = if ($matches[2] -eq "GB") { [double]$matches[1] * 1024 } else { [double]$matches[1] }
    }

    $results += $sample
    Write-Host "  Tick: avg=$($sample.routine_avg_ms)ms min=$($sample.routine_min_ms)ms max=$($sample.routine_max_ms)ms | NPC: $($sample.npc_count) | Mem: $($sample.memory_used_mb)MB | TPS: $($sample.tps_1m)"

    if ($i -lt $Iterations) {
        Write-Host "  Astept $IntervalSeconds secunde..."
        Start-Sleep -Seconds $IntervalSeconds
    }
}

Write-Host ""
Write-Host "=== Rezultate ==="
$avgTick = ($results | Where-Object { $_.routine_avg_ms -ne $null } | ForEach-Object { $_.routine_avg_ms } | Measure-Object -Average).Average
$avgMem = ($results | Where-Object { $_.memory_used_mb -ne $null } | ForEach-Object { $_.memory_used_mb } | Measure-Object -Average).Average
$avgTps = ($results | Where-Object { $_.tps_1m -ne $null } | ForEach-Object { $_.tps_1m } | Measure-Object -Average).Average

Write-Host "Routine tick mediu: ${avgTick}ms"
Write-Host "Memorie medie: ${avgMem}MB"
Write-Host "TPS mediu (1m): ${avgTps}"

$jsonPath = Join-Path $OutputDir "benchmark-$timestamp.json"
$results | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $jsonPath -Encoding UTF8
Write-Host "Rezultate salvate: $jsonPath"

$mdPath = Join-Path $OutputDir "benchmark-$timestamp.md"
@"
# Benchmark AINPC - $timestamp

| Metric | Value |
|---|---|
| Iteratii | $Iterations |
| Tick avg | ${avgTick}ms |
| Memorie medie | ${avgMem}MB |
| TPS mediu | ${avgTps} |

## Sample-uri individuale

$($results | ForEach-Object {
"| $($_.iteration) | $($_.routine_avg_ms)ms | $($_.routine_min_ms)ms | $($_.routine_max_ms)ms | $($_.npc_count) | $($_.memory_used_mb)MB | $($_.tps_1m) |"
})" | Sort-Object
"@ | Set-Content -LiteralPath $mdPath -Encoding UTF8

Write-Host "Raport salvat: $mdPath"

return @{
    ok = $true
    json = $jsonPath
    markdown = $mdPath
    avg_tick_ms = $avgTick
    avg_memory_mb = $avgMem
    avg_tps = $avgTps
}
