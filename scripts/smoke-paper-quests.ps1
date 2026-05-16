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
$coreJar = Join-Path $repoRoot "ainpc-core-plugin\build\libs\ainpc-core-plugin-1.0.0.jar"
$medievalJar = Join-Path $repoRoot "ainpc-scenario-medieval\build\libs\ainpc-scenario-medieval-1.0.0.jar"
$commandsPath = Join-Path $serverDirFull "ainpc-quest-smoke-commands.txt"
$reportPath = Join-Path $serverDirFull "ainpc-quest-smoke-report.txt"
$gradleWrapper = Join-Path $repoRoot "gradlew.bat"

function Invoke-RepoCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    Push-Location $repoRoot
    try {
        & $gradleWrapper @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Comanda Gradle a esuat: $gradleWrapper $($Arguments -join ' ')"
        }
    } finally {
        Pop-Location
    }
}

if (-not $SkipBuild) {
    if ($RunTests) {
        Invoke-RepoCommand -Arguments @("test")
    }
    Invoke-RepoCommand -Arguments @("assemble")
}

if (-not (Test-Path -LiteralPath $coreJar)) {
    throw "Lipseste JAR-ul core: $coreJar. Ruleaza .\gradlew.bat assemble."
}

if (-not (Test-Path -LiteralPath $medievalJar)) {
    throw "Lipseste JAR-ul medieval: $medievalJar. Ruleaza .\gradlew.bat assemble."
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
    "# Pentru C02, contract non-quest peste mapping-ul demo:",
    "# Stai langa negustor in zona pietei, accepta contractul, apoi verifica avizierul pietei.",
    "ainpc world demo create demo_sat",
    "ainpc contract definitions village_contracts",
    "ainpc progression definitions investigation",
    "ainpc progression definitions duty",
    "ainpc contract log $PlayerName",
    "# Daca primul nearest ofera C01, accepta-l; al doilea nearest ar trebui sa poata oferi C02 cat timp limita mecanicii permite.",
    "ainpc contract nearest $PlayerName",
    "ainpc contract accept nearest $PlayerName",
    "ainpc contract nearest $PlayerName",
    "ainpc contract accept nearest $PlayerName",
    "ainpc contract status C02 $PlayerName",
    "ainpc contract track start C02 $PlayerName",
    "ainpc quest anchors $PlayerName",
    "# Mergi in piata la node-ul quest_board, apoi revino la negustor cu confirmarea.",
    "give $PlayerName paper 1",
    "ainpc contract progress C02 $PlayerName",
    "ainpc contract status C02 $PlayerName",
    "ainpc contract stored $PlayerName village_contracts 20",
    "ainpc progression stored $PlayerName investigation 20",
    "ainpc story events",
    "ainpc audit quest",
    "",
    "# Pentru D01, sarcina NPC non-quest peste aceeasi infrastructura de progres:",
    "# Stai langa garda, accepta sarcina, verifica regiunea si avizierul, apoi raporteaza.",
    "ainpc world demo create demo_sat",
    "ainpc duty definitions npc_duties",
    "ainpc progression definitions duty",
    "ainpc duty log $PlayerName",
    "ainpc duty nearest $PlayerName",
    "ainpc duty accept nearest $PlayerName",
    "ainpc duty status D01 $PlayerName",
    "ainpc duty track start D01 $PlayerName",
    "ainpc quest anchors $PlayerName",
    "# Mergi in regiunea satului si inspecteaza node-ul quest_board, apoi revino la garda.",
    "ainpc duty progress D01 $PlayerName",
    "ainpc duty status D01 $PlayerName",
    "ainpc duty stored $PlayerName npc_duties 20",
    "ainpc progression stored $PlayerName duty 20",
    "ainpc story events",
    "ainpc audit quest",
    "",
    "# Pentru B01, bounty local peste aceeasi infrastructura de progres:",
    "# Stai langa garda, accepta bounty-ul, confirma regiunea si elimina scheletii, apoi raporteaza.",
    "ainpc world demo create demo_sat",
    "ainpc bounty definitions local_bounties",
    "ainpc progression definitions bounty",
    "ainpc bounty log $PlayerName",
    "ainpc bounty nearest $PlayerName",
    "ainpc bounty accept nearest $PlayerName",
    "ainpc bounty status B01 $PlayerName",
    "ainpc bounty track start B01 $PlayerName",
    "ainpc quest anchors $PlayerName",
    "# Mergi in regiunea satului, elimina 2 scheleti, apoi revino la garda.",
    "ainpc bounty progress B01 $PlayerName",
    "ainpc bounty status B01 $PlayerName",
    "ainpc bounty stored $PlayerName local_bounties 20",
    "ainpc progression stored $PlayerName bounty 20",
    "ainpc story events",
    "ainpc audit quest",
    "",
    "# Pentru B02, al doilea bounty local pe aceeasi mecanica local_bounties:",
    "# Dupa ce B01 este completat sau abandonat, stai langa fermier, accepta bounty-ul, verifica ferma si elimina paianjenii.",
    "ainpc world demo create demo_sat",
    "ainpc bounty definitions local_bounties",
    "ainpc progression definitions bounty",
    "ainpc bounty log $PlayerName",
    "ainpc bounty nearest $PlayerName",
    "ainpc bounty accept nearest $PlayerName",
    "ainpc bounty status B02 $PlayerName",
    "ainpc bounty track start B02 $PlayerName",
    "ainpc quest anchors $PlayerName",
    "# Mergi la ferma mapata, elimina 2 paianjeni, apoi revino la fermier.",
    "ainpc bounty progress B02 $PlayerName",
    "ainpc bounty status B02 $PlayerName",
    "ainpc bounty stored $PlayerName local_bounties 20",
    "ainpc progression stored $PlayerName bounty 20",
    "ainpc story events",
    "ainpc audit quest",
    "",
    "# Pentru E01, eveniment local temporar peste aceeasi infrastructura de progres:",
    "# Stai langa garda, accepta evenimentul, verifica piata/avizierul si adu piatra pentru reparatie.",
    "ainpc world demo create demo_sat",
    "ainpc event definitions village_events",
    "ainpc progression definitions event",
    "ainpc event log $PlayerName",
    "ainpc event nearest $PlayerName",
    "ainpc event accept nearest $PlayerName",
    "ainpc event status E01 $PlayerName",
    "ainpc event track start E01 $PlayerName",
    "ainpc quest anchors $PlayerName",
    "# Mergi in piata la node-ul quest_board, apoi revino la garda cu 2 cobblestone.",
    "give $PlayerName cobblestone 2",
    "ainpc event progress E01 $PlayerName",
    "ainpc event status E01 $PlayerName",
    "ainpc event stored $PlayerName village_events 20",
    "ainpc progression stored $PlayerName event 20",
    "ainpc story events",
    "ainpc audit quest",
    "",
    "# Pentru T01, tutorial onboarding peste aceeasi infrastructura de progres:",
    "# Stai langa preot, accepta tutorialul, verifica piata/avizierul si revino la preot.",
    "ainpc world demo create demo_sat",
    "ainpc tutorial definitions onboarding",
    "ainpc progression definitions tutorial",
    "ainpc tutorial log $PlayerName",
    "ainpc tutorial nearest $PlayerName",
    "ainpc tutorial accept nearest $PlayerName",
    "ainpc tutorial status T01 $PlayerName",
    "ainpc tutorial track start T01 $PlayerName",
    "ainpc quest anchors $PlayerName",
    "# Mergi in piata la node-ul quest_board, apoi revino la preot.",
    "ainpc tutorial progress T01 $PlayerName",
    "ainpc tutorial status T01 $PlayerName",
    "ainpc tutorial stored $PlayerName onboarding 20",
    "ainpc progression stored $PlayerName tutorial 20",
    "ainpc story events",
    "ainpc audit quest",
    "",
    "# Pentru R01, ritual local cu altar semantic in demo mapping:",
    "# Stai langa preot, accepta ritualul, pregateste lumanarile, verifica altarul si revino.",
    "ainpc world demo create demo_sat",
    "ainpc ritual definitions village_rituals",
    "ainpc progression definitions ritual",
    "ainpc ritual log $PlayerName",
    "ainpc ritual nearest $PlayerName",
    "ainpc ritual accept nearest $PlayerName",
    "ainpc ritual status R01 $PlayerName",
    "ainpc ritual track start R01 $PlayerName",
    "ainpc quest anchors $PlayerName",
    "give $PlayerName candle 2",
    "# Mergi la altarul semantic si inspecteaza node-ul ritual_circle, apoi revino la preot.",
    "ainpc ritual progress R01 $PlayerName",
    "ainpc ritual status R01 $PlayerName",
    "ainpc ritual stored $PlayerName village_rituals 20",
    "ainpc progression stored $PlayerName ritual 20",
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
