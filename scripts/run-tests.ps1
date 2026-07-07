param(
    [string]$Module = "all",
    [string]$Test = "",
    [switch]$List,
    [switch]$Count,
    [switch]$Failed,
    [switch]$Quiet,
    [switch]$NoBuild,
    [switch]$OnlyCore
)

# Helper script for running AINPC unit tests during development.
# Usage:
#   .\scripts\run-tests.ps1                       # all tests
#   .\scripts\run-tests.ps1 -Module gui           # GUI tests only
#   .\scripts\run-tests.ps1 -Module quest         # Quest engine tests
#   .\scripts\run-tests.ps1 -Module progression   # Progression tests
#   .\scripts\run-tests.ps1 -Module story         # Story tests
#   .\scripts\run-tests.ps1 -Module mapping       # World mapping tests
#   .\scripts\run-tests.ps1 -Module npc           # NPC tests
#   .\scripts\run-tests.ps1 -Module command       # Command tests
#   .\scripts\run-tests.ps1 -Test "QuestDirectorTest"  # single test class
#   .\scripts\run-tests.ps1 -List                 # list test categories
#   .\scripts\run-tests.ps1 -Count                # count tests per category
#   .\scripts\run-tests.ps1 -Failed               # rerun last failed tests
#   .\scripts\run-tests.ps1 -Quiet                # minimal output
#   .\scripts\run-tests.ps1 -OnlyCore             # skip API module (avoids pre-existing API test failure)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$coreModule = ":ainpc-core-plugin:test"

$testPackages = @{
    "all"         = ""
    "gui"         = "ro.ainpc.gui.*"
    "quest"       = "ro.ainpc.engine.*"
    "progression" = "ro.ainpc.progression.*"
    "story"       = "ro.ainpc.story.*"
    "mapping"     = "ro.ainpc.world.*"
    "npc"         = "ro.ainpc.npc.* ro.ainpc.managers.*"
    "command"     = "ro.ainpc.commands.*"
    "debug"       = "ro.ainpc.debug.*"
    "spawn"       = "ro.ainpc.spawn.*"
    "listener"    = "ro.ainpc.listeners.*"
    "economy"     = "ro.ainpc.economy.*"
    "ai"          = "ro.ainpc.ai.*"
    "routine"     = "ro.ainpc.routine.*"
    "topology"    = "ro.ainpc.topology.*"
    "utils"       = "ro.ainpc.utils.*"
    "database"    = "ro.ainpc.database.*"
}

if ($List) {
    Write-Host "Test categories:" -ForegroundColor Cyan
    foreach ($key in $testPackages.Keys | Sort-Object) {
        Write-Host "  $key" -ForegroundColor Green
    }
    exit 0
}

if ($Count) {
    Write-Host "Counting tests per category..." -ForegroundColor Cyan
    $testDir = Join-Path $root "ainpc-core-plugin\src\test\kotlin\ro\ainpc"
    foreach ($key in $testPackages.Keys | Sort-Object) {
        if ($key -eq "all") { continue }
        $pkgs = ($testPackages[$key] -split ' ').ForEach({ $_.Replace("ro.ainpc.", "").Replace(".*", "") })
        $fileCount = 0
        foreach ($pkg in $pkgs) {
            $pkgPath = Join-Path $testDir $pkg.Replace(".", "\")
            if (Test-Path $pkgPath) {
                $fileCount += (Get-ChildItem -Path $pkgPath -Filter "*Test*.kt" -Recurse -ErrorAction SilentlyContinue).Count
            }
        }
        $color = if ($fileCount -gt 0) { "Green" } else { "DarkGray" }
        Write-Host "  $key`: $fileCount test files" -ForegroundColor $color
    }
    exit 0
}

$testArgs = @($coreModule)
$filter = ""

if ($Test -ne "") {
    $testArgs += "--tests", $Test
} elseif ($Module -ne "all") {
    $pkgs = $testPackages[$Module]
    if ($pkgs -eq $null) {
        Write-Host "Module necunoscut: $Module" -ForegroundColor Red
        Write-Host "Module valide: $($testPackages.Keys -join ', ')" -ForegroundColor Yellow
        exit 1
    }
    Write-Host "Test module: $Module ($pkgs)" -ForegroundColor Cyan
    foreach ($pkg in ($pkgs -split ' ')) {
        $testArgs += "--tests", $pkg
    }
} else {
    Write-Host "Test module: all" -ForegroundColor Cyan
}

if ($Failed) {
    Write-Host "Rerunning last failed tests..." -ForegroundColor Yellow
    $testArgs += "-Dsurefire.rerunFailingTestsCount=1"
}

if (!$NoBuild) {
    Write-Host "Building core..." -ForegroundColor Cyan
    $buildArgs = @(":ainpc-core-plugin:classes", "-q")
    if ($OnlyCore) { $buildArgs += "-x", ":ainpc-api:compileTestJava" }
    & $root\gradlew.bat @buildArgs 2>&1 | ForEach-Object { if (!$Quiet) { Write-Host $_ } }
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Build failed!" -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

if ($Quiet) { $testArgs += "-q" }
if ($OnlyCore) { $testArgs += "-x", ":ainpc-api:compileTestJava" }
Write-Host "Running: gradlew $($testArgs -join ' ')" -ForegroundColor DarkGray
& $root\gradlew.bat @testArgs 2>&1 | ForEach-Object {
    $line = $_
    if ($line -match "FAILED|BUILD FAIL") {
        Write-Host $line -ForegroundColor Red
    } elseif ($line -match "PASSED|BUILD SUCCESS") {
        Write-Host $line -ForegroundColor Green
    } elseif ($line -match "test.*PASS|test.*FAIL") {
        if ($line -match "FAIL") { Write-Host $line -ForegroundColor Red }
        else { Write-Host $line -ForegroundColor DarkGray }
    } elseif (!$Quiet) {
        Write-Host $line
    }
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nAll tests passed!" -ForegroundColor Green
} else {
    Write-Host "`nSome tests FAILED!" -ForegroundColor Red
    exit $LASTEXITCODE
}
