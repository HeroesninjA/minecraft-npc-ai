param(
    [Parameter(Mandatory = $true)]
    [string]$ServerDir,

    [string]$PlayerName = "<player>",

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
$commandsPath = Join-Path $serverDirFull "ainpc-quest-smoke-commands.txt"
$reportPath = Join-Path $serverDirFull "ainpc-quest-smoke-report.txt"

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
    "# AINPC quest smoke test",
    "# Ruleaza in consola Paper fara slash sau in joc ca OP cu slash.",
    "# Jucator tinta: $PlayerName",
    "# Pentru testul cu nearest, jucatorul trebuie sa stea langa NPC-ul care da questul.",
    "",
    "plugins",
    "ainpc",
    "ainpc audit quest",
    "ainpc quest log $PlayerName",
    "ainpc quest log $PlayerName active",
    "ainpc quest log $PlayerName main",
    "ainpc quest log $PlayerName side",
    "ainpc quest log tracked $PlayerName",
    "ainpc quest log $PlayerName all",
    "ainpc quest log $PlayerName repeatable",
    "ainpc quest nearest $PlayerName",
    "ainpc quest accept nearest $PlayerName",
    "ainpc quest status nearest $PlayerName",
    "ainpc quest track start $PlayerName",
    "# Selector explicit util cand ai mai multe questuri curente:",
    "# ainpc quest status Q01 $PlayerName",
    "# ainpc quest track start Q01 $PlayerName",
    "# ainpc quest debug Q01 $PlayerName",
    "# ainpc quest abandon tracked $PlayerName",
    "",
    "# Pentru Q01, stai langa fierar si foloseste itemele de test:",
    "give $PlayerName iron_ingot 3",
    "give $PlayerName oak_planks 1",
    "ainpc quest status nearest $PlayerName",
    "ainpc quest nearest $PlayerName",
    "ainpc quest log $PlayerName",
    "ainpc quest log $PlayerName completed",
    "ainpc quest anchors $PlayerName",
    "ainpc audit quest",
    "",
    "# Pentru Q06, dupa ce Q01 este completat si exista mapping demo cu fierarie:",
    "# Stai langa fierar, apoi accepta Q06.",
    "ainpc world demo create demo_sat",
    "ainpc audit world",
    "ainpc quest nearest $PlayerName",
    "ainpc quest accept nearest $PlayerName",
    "ainpc quest track start Q06 $PlayerName",
    "ainpc quest debug Q06 $PlayerName",
    "ainpc quest anchors $PlayerName",
    "# Mergi la place-ul fierariei si la node-ul inspect_1, apoi revino la fierar.",
    "ainpc quest status nearest $PlayerName",
    "ainpc story events",
    "ainpc audit quest",
    "",
    "# Pentru Q07, dupa ce Q03 si Q04 sunt completate:",
    "# Stai langa hangiu, accepta questul, apoi mergi langa garda pentru obiectivul social.",
    "give $PlayerName paper 1",
    "ainpc quest nearest $PlayerName",
    "ainpc quest accept nearest $PlayerName",
    "ainpc quest track start Q07 $PlayerName",
    "ainpc quest debug Q07 $PlayerName",
    "# Stai langa garda si interactioneaza pentru confirmarea patrulei.",
    "ainpc quest nearest $PlayerName",
    "ainpc quest status nearest $PlayerName",
    "# Intoarce-te langa hangiu si preda painea pentru pachet.",
    "give $PlayerName bread 2",
    "ainpc quest status nearest $PlayerName",
    "ainpc quest nearest $PlayerName",
    "ainpc story events",
    "ainpc audit quest",
    "",
    "# Pentru Q08, dupa ce Q03 este completat si exista mapping demo:",
    "# Stai langa garda, accepta patrula, apoi misca-te in regiunea demo.",
    "ainpc quest nearest $PlayerName",
    "ainpc quest accept nearest $PlayerName",
    "ainpc quest track start Q08 $PlayerName",
    "ainpc quest debug Q08 $PlayerName",
    "ainpc quest anchors $PlayerName",
    "# In joc ca OP: omoara 2 zombie langa hotar. Din consola poti pregati mobii asa:",
    "execute at $PlayerName run summon zombie ~ ~ ~",
    "execute at $PlayerName run summon zombie ~ ~ ~",
    "ainpc quest status nearest $PlayerName",
    "# Revino langa garda si inchide patrula.",
    "ainpc quest nearest $PlayerName",
    "ainpc story events",
    "ainpc audit quest",
    "",
    "# Daca vrei sa retestezi acelasi quest:",
    "# ainpc quest abandon Q08 $PlayerName",
    "# ainpc quest reset nearest $PlayerName"
)

Set-Content -LiteralPath $commandsPath -Value $commands -Encoding UTF8

$report = @(
    "AINPC Paper quest smoke preparation",
    "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')",
    "Repo: $repoRoot",
    "Server: $serverDirFull",
    "Plugins dir: $pluginsDir",
    "Player: $PlayerName",
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

Write-Host "Pregatit smoke test questuri pentru serverul Paper."
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
