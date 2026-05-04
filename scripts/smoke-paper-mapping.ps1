param(
    [Parameter(Mandatory = $true)]
    [string]$ServerDir,

    [string]$RegionId = "demo_sat",

    [switch]$SkipBuild,

    [switch]$RunTests,

    [switch]$NoCopy
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$serverPath = Resolve-Path -LiteralPath $ServerDir -ErrorAction Stop
$serverDirFull = $serverPath.Path
$pluginsDir = Join-Path $serverDirFull "plugins"
$coreJar = Join-Path $repoRoot "ainpc-core-plugin\target\ainpc-core-plugin-1.0.0.jar"
$medievalJar = Join-Path $repoRoot "ainpc-scenario-medieval\target\ainpc-scenario-medieval-1.0.0.jar"
$commandsPath = Join-Path $serverDirFull "ainpc-mapping-smoke-commands.txt"
$reportPath = Join-Path $serverDirFull "ainpc-mapping-smoke-report.txt"

function Invoke-RepoCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    Push-Location $repoRoot
    try {
        & mvn @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Comanda Maven a esuat: mvn $($Arguments -join ' ')"
        }
    } finally {
        Pop-Location
    }
}

if (-not $SkipBuild) {
    if ($RunTests) {
        Invoke-RepoCommand -Arguments @("test")
    }
    Invoke-RepoCommand -Arguments @("package", "-DskipTests")
}

if (-not (Test-Path -LiteralPath $coreJar)) {
    throw "Lipseste JAR-ul core: $coreJar. Ruleaza mvn package -DskipTests."
}

if (-not (Test-Path -LiteralPath $medievalJar)) {
    throw "Lipseste JAR-ul medieval: $medievalJar. Ruleaza mvn package -DskipTests."
}

if (-not (Test-Path -LiteralPath $pluginsDir)) {
    New-Item -ItemType Directory -Path $pluginsDir | Out-Null
}

if (-not $NoCopy) {
    Copy-Item -LiteralPath $coreJar -Destination $pluginsDir -Force
    Copy-Item -LiteralPath $medievalJar -Destination $pluginsDir -Force
}

$paperJars = @(Get-ChildItem -LiteralPath $serverDirFull -Filter "*.jar" -File |
    Where-Object { $_.Name -match "paper|server|purpur|folia|spigot" })
$hasEula = Test-Path -LiteralPath (Join-Path $serverDirFull "eula.txt")
$hasServerProperties = Test-Path -LiteralPath (Join-Path $serverDirFull "server.properties")
$coreHash = Get-FileHash -LiteralPath $coreJar -Algorithm SHA256
$medievalHash = Get-FileHash -LiteralPath $medievalJar -Algorithm SHA256

$commands = @(
    "# AINPC mapping smoke test",
    "# Ruleaza in consola Paper fara slash sau in joc ca OP cu slash.",
    "# Regiune test: $RegionId",
    "",
    "plugins",
    "ainpc",
    "ainpc world demo create $RegionId",
    "ainpc world settlement plan $RegionId",
    "ainpc world settlement spawn $RegionId",
    "ainpc audit world",
    "ainpc audit spawn",
    "ainpc world save",
    "",
    "# Apoi da restart/reload serverului si ruleaza:",
    "ainpc audit world",
    "ainpc audit spawn",
    "ainpc world places $RegionId"
)

Set-Content -LiteralPath $commandsPath -Value $commands -Encoding UTF8

$report = @(
    "AINPC Paper mapping smoke preparation",
    "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')",
    "Repo: $repoRoot",
    "Server: $serverDirFull",
    "Plugins dir: $pluginsDir",
    "Region: $RegionId",
    "Build skipped: $SkipBuild",
    "Tests requested: $RunTests",
    "Copy skipped: $NoCopy",
    "",
    "Core jar: $coreJar",
    "Core SHA256: $($coreHash.Hash)",
    "Medieval jar: $medievalJar",
    "Medieval SHA256: $($medievalHash.Hash)",
    "",
    "Server jar found: $($paperJars.Count -gt 0)",
    "eula.txt found: $hasEula",
    "server.properties found: $hasServerProperties",
    "",
    "Next file with commands:",
    $commandsPath
)

Set-Content -LiteralPath $reportPath -Value $report -Encoding UTF8

Write-Host "Pregatit smoke test mapping pentru serverul Paper."
Write-Host "Comenzi: $commandsPath"
Write-Host "Raport: $reportPath"

if ($paperJars.Count -eq 0) {
    Write-Warning "Nu am gasit JAR de server in $serverDirFull. Scriptul a pregatit plugins/ si comenzile, dar serverul Paper trebuie pornit separat."
}

if (-not $hasEula) {
    Write-Warning "Nu exista eula.txt in server dir. Prima pornire Paper poate cere acceptarea EULA."
}

if (-not $hasServerProperties) {
    Write-Warning "Nu exista server.properties in server dir. Daca acesta este server nou, Paper il va genera la prima pornire."
}
