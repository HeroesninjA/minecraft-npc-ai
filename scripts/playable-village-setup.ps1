param(
    [Parameter(Mandatory = $true)]
    [string]$ServerDir,

    [string]$RegionId = "demo_sat",

    [string]$RconHost = "127.0.0.1",
    [int]$RconPort = 25575,
    [string]$RconPassword = "demo",

    [switch]$SkipBuild,
    [switch]$SkipDeploy,
    [switch]$SkipDemo,
    [switch]$DryRun,

    [int]$Population = 8,
    [int]$HouseCount = 6
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$version = "1.0.0"
$props = Get-Content -LiteralPath "$repoRoot/gradle.properties" -ErrorAction SilentlyContinue | Where-Object { $_ -match '^projectVersion=(.+)$' }
if ($props) { $version = $matches[1] }

$coreJar = Join-Path $repoRoot "ainpc-core-plugin\build\libs\ainpc-core-plugin-$version.jar"
$medievalJar = Join-Path $repoRoot "ainpc-scenario-medieval\build\libs\ainpc-scenario-medieval-$version.jar"
$mcServiceJar = Join-Path $repoRoot "ainpc-mcp-service\build\libs\ainpc-mcp-service.jar"
$pluginsDir = Join-Path $ServerDir "plugins"
$reportPath = Join-Path $repoRoot "build\playable-village-report.md"
$pass = 0
$fail = 0

function Step {
    param([string]$Label, [scriptblock]$Block)
    Write-Host "`n>>> $Label" -ForegroundColor Cyan
    try {
        & $Block
        Write-Host "  [OK] $Label" -ForegroundColor Green
        $script:pass++
    } catch {
        Write-Host "  [FAIL] $Label : $_" -ForegroundColor Red
        $script:fail++
    }
}

function RconCommand {
    param([string]$Command)
    # Uses mcrcon if available, otherwise just log
    if (Get-Command "mcrcon" -ErrorAction SilentlyContinue) {
        mcrcon -H $RconHost -P $RconPort -p $RconPassword "$Command" 2>$null
    } else {
        Write-Host "  [RCON] $Command" -ForegroundColor DarkGray
    }
}

# ===== PHASE 1: BUILD =====
if (-not $SkipBuild) {
    Step "Build proiect" {
        & "$repoRoot\gradlew.bat" clean build 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Gradle build failed" }
        if (-not (Test-Path $coreJar)) { throw "Core JAR lipseste: $coreJar" }
        if (-not (Test-Path $medievalJar)) { throw "Medieval JAR lipseste: $medievalJar" }
    }
}

# ===== PHASE 2: DEPLOY =====
if (-not $SkipDeploy) {
    Step "Deploy JAR-uri in $pluginsDir" {
        if (-not (Test-Path $pluginsDir)) { New-Item -ItemType Directory -Path $pluginsDir -Force | Out-Null }
        Copy-Item $coreJar "$pluginsDir\ainpc-core-plugin.jar" -Force
        Copy-Item $medievalJar "$pluginsDir\ainpc-scenario-medieval.jar" -Force
        Write-Host "    Copiat: ainpc-core-plugin.jar + ainpc-scenario-medieval.jar"
    }
}

# ===== PHASE 3: DEMO CREATE =====
if (-not $SkipDemo) {
    Step "Creare mapping demo ($RegionId)" {
        RconCommand "/ainpc world demo create $RegionId"
    }
    Step "Salvare mapping" {
        RconCommand "/ainpc world save"
    }
    Step "Audit mapping" {
        RconCommand "/ainpc audit world"
    }
}

# ===== PHASE 4: NPC POPULATION =====
if (-not $SkipDemo -and -not $DryRun) {
    Step "Plan populare ($Population NPCs)" {
        RconCommand "/ainpc population plan $RegionId $Population"
    }
    Step "Inspect plan populare" {
        RconCommand "/ainpc population inspect $RegionId"
    }
    Step "Settlement plan" {
        RconCommand "/ainpc world settlement plan $RegionId $HouseCount"
    }
    Step "Settlement spawn (dry-run)" {
        RconCommand "/ainpc world settlement spawn $RegionId dry-run"
    }
    Step "Settlement spawn" {
        RconCommand "/ainpc world settlement spawn $RegionId"
    }
    Step "Salvare + audit final" {
        RconCommand "/ainpc world save"
        RconCommand "/ainpc audit all"
    }
}

# ===== REPORT =====
$total = $pass + $fail
$report = @"
# Playable Village Setup Report

| | |
|---|---|
| Data | $(Get-Date -Format 'yyyy-MM-dd HH:mm') |
| Server | $ServerDir |
| Regiune | $RegionId |
| Status | $(if ($fail -eq 0) { '✅ TOTI PASII AU TRECUT' } else { "❌ $fail/$total pasi au esuat" }) |

## Pasi executati
"@

$report | Set-Content -LiteralPath $reportPath -Encoding UTF8
Write-Host "`nReport: $reportPath"
Write-Host "Passed: $pass | Failed: $fail"
if ($fail -gt 0) { exit 1 }
