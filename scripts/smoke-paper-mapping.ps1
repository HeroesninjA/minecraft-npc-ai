param(
    [Parameter(Mandatory = $true)]
    [string]$ServerDir,

    [string]$RegionId = "demo_sat",

    [switch]$SkipBuild,

    [switch]$RunTests,

    [switch]$NoCopy,

    [string]$WandRegionId = "wand_sat",

    [string]$QuestAnchorSelector = "tracked",

    [string]$QuestAnchorObjectiveId = "inspect_board",

    [string]$QuestAnchorObjectiveType = "inspect_node",

    [string]$QuestAnchorReference = "node:quest_board",

    [switch]$SkipWandFlow
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
$includeWandFlow = -not $SkipWandFlow

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

if ($includeWandFlow) {
    $commands += @(
        "",
        "# Flux manual wand complet - ruleaza in joc ca OP, nu in consola.",
        "# Foloseste o zona libera. Pentru pos1/pos2/point poti folosi click cu wand-ul",
        "# sau comenzile de mai jos, care iau pozitia curenta a jucatorului.",
        "# Ajusteaza prompturile daca vrei ID-uri/nume diferite.",
        "",
        "# Region din wand",
        "ainpc wand",
        "ainpc wand mode region",
        "# Mergi la primul colt si ruleaza:",
        "ainpc wand pos1",
        "# Mergi la al doilea colt si ruleaza:",
        "ainpc wand pos2",
        "ainpc map region sat $WandRegionId pentru smoke",
        "ainpc map preview",
        "ainpc map confirm",
        "ainpc audit world",
        "ainpc world save",
        "",
        "# Place din wand, in interiorul regiunii create",
        "ainpc wand mode place",
        "# Mergi la primul colt al cladirii/zonei si ruleaza:",
        "ainpc wand pos1",
        "# Mergi la al doilea colt al cladirii/zonei si ruleaza:",
        "ainpc wand pos2",
        "ainpc map place piata publica pentru smoke",
        "ainpc map preview",
        "ainpc map confirm",
        "ainpc audit world",
        "ainpc world save",
        "",
        "# Node din wand, in interiorul place-ului",
        "ainpc wand mode node",
        "# Mergi la punctul avizierului si ruleaza:",
        "ainpc wand point",
        "ainpc map node acesta este avizierul",
        "ainpc map preview",
        "ainpc map confirm",
        "ainpc audit world",
        "ainpc world save",
        "",
        "# NPC bind din wand. Trebuie sa existe un NPC incarcat aproape de jucator.",
        "ainpc wand mode npc_bind",
        "# Stai in place-ul tinta si ruleaza:",
        "ainpc wand point",
        "ainpc map npc_bind nearest social",
        "ainpc map preview",
        "ainpc map confirm",
        "ainpc world bindings list",
        "ainpc audit spawn",
        "ainpc world save",
        "",
        "# Quest anchor persistent din wand.",
        "# Inainte de acest pas, playerul trebuie sa aiba o progresie existenta in player_quests.",
        "# Selector implicit: $QuestAnchorSelector; objective_id implicit: $QuestAnchorObjectiveId.",
        "ainpc wand mode quest_anchor",
        "# Stai pe node/place/region tinta si ruleaza:",
        "ainpc wand point",
        "ainpc map quest_anchor $QuestAnchorSelector $QuestAnchorObjectiveId $QuestAnchorObjectiveType $QuestAnchorReference",
        "ainpc map preview",
        "ainpc map confirm",
        "ainpc quest anchors all",
        "ainpc audit quest",
        "ainpc debugdump quest"
    )
}

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
    "Wand flow included: $includeWandFlow",
    "Wand region prompt token: $WandRegionId",
    "Quest anchor selector: $QuestAnchorSelector",
    "Quest anchor objective id: $QuestAnchorObjectiveId",
    "Quest anchor objective type: $QuestAnchorObjectiveType",
    "Quest anchor reference: $QuestAnchorReference",
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
