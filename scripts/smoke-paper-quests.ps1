param(
    [Parameter(Mandatory = $true)]
    [string]$ServerDir,

    [string]$PlayerName = "<player>",

    [switch]$SkipBuild,

    [switch]$RunTests,

    [switch]$NoCopy,

    [string]$RegionId = "demo_sat",

    [switch]$RunRconPreflight,

    [string]$RconHost = "127.0.0.1",

    [int]$RconPort = 25575,

    [string]$RconPassword = "",

    [int]$RconTimeoutSeconds = 5,

    [switch]$RunRconPlayerSmoke,

    [switch]$WaitForPlayerCheckpoints
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
$rconReportPath = Join-Path $serverDirFull "ainpc-quest-smoke-rcon-report.json"
$rconPlayerReportPath = Join-Path $serverDirFull "ainpc-quest-smoke-player-rcon-report.json"
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

function Read-ExactBytes {
    param(
        [Parameter(Mandatory = $true)]
        [System.IO.Stream]$Stream,

        [Parameter(Mandatory = $true)]
        [int]$Length
    )

    $buffer = New-Object byte[] $Length
    $offset = 0
    while ($offset -lt $Length) {
        $read = $Stream.Read($buffer, $offset, $Length - $offset)
        if ($read -le 0) {
            throw "Conexiunea RCON s-a inchis inainte de raspuns complet."
        }
        $offset += $read
    }
    return ,$buffer
}

function New-RconPacket {
    param(
        [Parameter(Mandatory = $true)]
        [int]$RequestId,

        [Parameter(Mandatory = $true)]
        [int]$Type,

        [string]$Body = ""
    )

    $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($Body)
    $length = 4 + 4 + $bodyBytes.Length + 2
    $packet = New-Object byte[] (4 + $length)
    [System.BitConverter]::GetBytes([int]$length).CopyTo($packet, 0)
    [System.BitConverter]::GetBytes([int]$RequestId).CopyTo($packet, 4)
    [System.BitConverter]::GetBytes([int]$Type).CopyTo($packet, 8)
    $bodyBytes.CopyTo($packet, 12)
    $packet[12 + $bodyBytes.Length] = 0
    $packet[13 + $bodyBytes.Length] = 0
    return ,$packet
}

function Read-RconPacket {
    param(
        [Parameter(Mandatory = $true)]
        [System.IO.Stream]$Stream
    )

    $lengthBytes = Read-ExactBytes -Stream $Stream -Length 4
    $length = [System.BitConverter]::ToInt32($lengthBytes, 0)
    if ($length -lt 10 -or $length -gt 1048576) {
        throw "Raspuns RCON invalid: length=$length."
    }

    $payload = Read-ExactBytes -Stream $Stream -Length $length
    $bodyLength = $length - 10
    $body = if ($bodyLength -gt 0) {
        [System.Text.Encoding]::UTF8.GetString($payload, 8, $bodyLength)
    } else {
        ""
    }

    [pscustomobject]@{
        Id = [System.BitConverter]::ToInt32($payload, 0)
        Type = [System.BitConverter]::ToInt32($payload, 4)
        Body = $body
    }
}

function Send-RconCommand {
    param(
        [Parameter(Mandatory = $true)]
        [System.IO.Stream]$Stream,

        [Parameter(Mandatory = $true)]
        [int]$RequestId,

        [Parameter(Mandatory = $true)]
        [int]$SentinelId,

        [Parameter(Mandatory = $true)]
        [string]$Command
    )

    $packet = New-RconPacket -RequestId $RequestId -Type 2 -Body $Command
    $Stream.Write($packet, 0, $packet.Length)
    $sentinelPacket = New-RconPacket -RequestId $SentinelId -Type 2 -Body ""
    $Stream.Write($sentinelPacket, 0, $sentinelPacket.Length)
    $Stream.Flush()

    $bodyParts = New-Object System.Collections.Generic.List[string]
    while ($true) {
        $response = Read-RconPacket -Stream $Stream
        if ($response.Id -eq -1) {
            throw "RCON a returnat request id invalid pentru comanda: $Command"
        }
        if ($response.Id -eq $RequestId) {
            $bodyParts.Add($response.Body)
            continue
        }
        if ($response.Id -eq $SentinelId) {
            break
        }
    }

    [pscustomobject]@{
        Id = $RequestId
        Type = 0
        Body = ($bodyParts.ToArray() -join "")
    }
}

function Remove-MinecraftFormatting {
    param([string]$Text)

    if ($null -eq $Text) {
        return ""
    }
    $section = [string][char]0x00A7
    return ($Text -replace ([regex]::Escape($section) + "."), "" -replace "&[0-9A-FK-ORa-fk-or]", "")
}

function Get-RconResponseIssues {
    param(
        [Parameter(Mandatory = $true)]
        [hashtable]$Step,

        [string]$CleanResponse
    )

    $issues = New-Object System.Collections.Generic.List[string]
    $hardFailurePatterns = @(
        "Unknown command",
        "Unknown or incomplete command",
        "An internal error occurred",
        "Exception",
        "Command exception"
    )
    foreach ($pattern in $hardFailurePatterns) {
        if ($CleanResponse -match [regex]::Escape($pattern)) {
            $issues.Add("raspunsul contine '$pattern'")
        }
    }

    $forbidden = if ($Step.ContainsKey("Forbidden")) { @($Step["Forbidden"]) } else { @() }
    foreach ($forbiddenText in $forbidden) {
        if ($CleanResponse -match [regex]::Escape($forbiddenText)) {
            $issues.Add("raspunsul contine text interzis '$forbiddenText'")
        }
    }

    $required = if ($Step.ContainsKey("Required")) { @($Step["Required"]) } else { @() }
    foreach ($requiredText in $required) {
        if ($CleanResponse -notmatch [regex]::Escape($requiredText)) {
            $issues.Add("lipseste textul asteptat '$requiredText'")
        }
    }

    $minTotal = if ($Step.ContainsKey("MinTotal")) { [int]$Step["MinTotal"] } else { 0 }
    if ($minTotal -gt 0) {
        if ($CleanResponse -match "Total:\s*(\d+)") {
            $total = [int]$Matches[1]
            if ($total -lt $minTotal) {
                $issues.Add("Total=$total, minim asteptat $minTotal")
            }
        } else {
            $issues.Add("nu pot citi linia Total din raspuns")
        }
    }

    $requireZeroAuditErrors = $Step.ContainsKey("RequireZeroAuditErrors") -and [bool]$Step["RequireZeroAuditErrors"]
    if ($requireZeroAuditErrors) {
        if ($CleanResponse -match "Rezultat:\s*(\d+)\s+erori") {
            $errorCount = [int]$Matches[1]
            if ($errorCount -ne 0) {
                $issues.Add("auditul raporteaza $errorCount erori")
            }
        } else {
            $issues.Add("nu pot citi linia Rezultat din audit")
        }
    }

    return @($issues)
}

function Invoke-QuestRconPlayerSmoke {
    param(
        [Parameter(Mandatory = $true)]
        [string]$HostName,

        [Parameter(Mandatory = $true)]
        [int]$Port,

        [Parameter(Mandatory = $true)]
        [string]$Password,

        [Parameter(Mandatory = $true)]
        [int]$TimeoutSeconds,

        [Parameter(Mandatory = $true)]
        [string]$TargetPlayer,

        [Parameter(Mandatory = $true)]
        [string]$SmokeRegionId,

        [Parameter(Mandatory = $true)]
        [string]$OutputPath,

        [bool]$WaitForCheckpoints = $false
    )

    if ($Password.Trim().Length -eq 0) {
        throw "Pentru -RunRconPlayerSmoke seteaza -RconPassword; parola nu este scrisa in raport."
    }
    if ($TargetPlayer.Trim().Length -eq 0 -or $TargetPlayer -eq "<player>") {
        throw "Pentru -RunRconPlayerSmoke seteaza -PlayerName cu numele playerului online."
    }

    $client = [System.Net.Sockets.TcpClient]::new()
    $client.ReceiveTimeout = $TimeoutSeconds * 1000
    $client.SendTimeout = $TimeoutSeconds * 1000
    $requestId = 1
    $startedAt = Get-Date
    $steps = @(
        @{ Name = "plugins"; Command = "plugins"; Required = @("AINPCPlugin", "AINPCScenarioMedieval") },
        @{ Name = "player-online"; Command = "list"; Required = @($TargetPlayer) },
        @{ Name = "world-demo"; Command = "ainpc world demo create $SmokeRegionId"; Required = @() },
        @{ Name = "audit-quest-before"; Command = "ainpc audit quest"; Required = @("AINPC Audit"); RequireZeroAuditErrors = $true },
        @{ Name = "quest-log-before"; Command = "ainpc quest log $TargetPlayer"; Forbidden = @("Jucatorul nu este online", "offline") },
        @{
            Name = "quest-nearest-offer"
            Command = "ainpc quest nearest $TargetPlayer"
            Checkpoint = "Pozitioneaza playerul langa NPC-ul care da Q01, de regula fierarul, apoi continua."
            Forbidden = @("Nu exista NPC-uri active", "nu are un quest disponibil", "Jucatorul nu este online")
        },
        @{
            Name = "quest-accept-nearest"
            Command = "ainpc quest accept nearest $TargetPlayer"
            Forbidden = @("Nu pot determina", "nu are un quest disponibil", "Jucatorul nu este online")
        },
        @{
            Name = "quest-track-start"
            Command = "ainpc quest track start $TargetPlayer"
            Forbidden = @("Nu am pornit quest tracking persistent", "Utilizare", "Jucatorul nu este online")
        },
        @{
            Name = "quest-status-before-items"
            Command = "ainpc quest status nearest $TargetPlayer"
            Forbidden = @("Nu exista NPC-uri active", "Nu pot determina")
        },
        @{ Name = "give-iron"; Command = "give $TargetPlayer iron_ingot 3"; Forbidden = @("No player was found", "That player cannot be found") },
        @{ Name = "give-planks"; Command = "give $TargetPlayer oak_planks 1"; Forbidden = @("No player was found", "That player cannot be found") },
        @{
            Name = "quest-status-after-items"
            Command = "ainpc quest status nearest $TargetPlayer"
            Forbidden = @("Nu exista NPC-uri active", "Nu pot determina")
        },
        @{
            Name = "quest-nearest-turn-in"
            Command = "ainpc quest nearest $TargetPlayer"
            Checkpoint = "Pastreaza playerul langa acelasi NPC pentru predare/confirmare, apoi continua."
            Forbidden = @("Nu exista NPC-uri active", "Jucatorul nu este online")
        },
        @{
            Name = "quest-log-completed"
            Command = "ainpc quest log $TargetPlayer completed"
            Required = @("Progresii arhivate")
            Forbidden = @("Jucatorul nu este online", "Nu exista progresii arhivate")
        },
        @{ Name = "quest-anchors"; Command = "ainpc quest anchors $TargetPlayer"; Forbidden = @("Jucatorul nu este online") },
        @{ Name = "story-events"; Command = "ainpc story events"; Required = @() },
        @{ Name = "audit-quest-after"; Command = "ainpc audit quest"; Required = @("AINPC Audit"); RequireZeroAuditErrors = $true }
    )

    $results = New-Object System.Collections.Generic.List[object]
    try {
        $connectTask = $client.ConnectAsync($HostName, $Port)
        if (-not $connectTask.Wait($TimeoutSeconds * 1000)) {
            throw "Timeout la conectarea RCON $HostName`:$Port."
        }
        $stream = $client.GetStream()

        $authPacket = New-RconPacket -RequestId $requestId -Type 3 -Body $Password
        $stream.Write($authPacket, 0, $authPacket.Length)
        $stream.Flush()
        $authResponse = Read-RconPacket -Stream $stream
        if ($authResponse.Id -eq -1) {
            throw "Autentificarea RCON a esuat."
        }
        $requestId++

        foreach ($step in $steps) {
            if ($WaitForCheckpoints -and $step.ContainsKey("Checkpoint")) {
                Write-Host ""
                Write-Host "[checkpoint] $($step.Checkpoint)"
                [void](Read-Host "Apasa Enter dupa ce playerul este pregatit")
            }

            $response = Send-RconCommand -Stream $stream -RequestId $requestId -SentinelId ($requestId + 1) -Command $step.Command
            $requestId += 2
            $cleanResponse = Remove-MinecraftFormatting -Text $response.Body
            $issues = @(Get-RconResponseIssues -Step $step -CleanResponse $cleanResponse)
            $results.Add([pscustomobject]@{
                name = $step.Name
                command = $step.Command
                ok = $issues.Count -eq 0
                issues = @($issues)
                checkpoint = if ($step.ContainsKey("Checkpoint")) { $step.Checkpoint } else { "" }
                response = $cleanResponse.Trim()
            })
        }
    } finally {
        $client.Close()
    }

    $resultItems = @($results.ToArray())
    $failed = @($resultItems | Where-Object { -not $_.ok })
    $failedStepNames = New-Object System.Collections.Generic.List[string]
    foreach ($failedStep in $failed) {
        $failedStepNames.Add($failedStep.name)
    }
    $reportObject = [pscustomobject]@{
        ok = $failed.Count -eq 0
        mode = "player-assisted-rcon"
        generated_at = (Get-Date).ToString("o")
        duration_seconds = [Math]::Round(((Get-Date) - $startedAt).TotalSeconds, 3)
        server = "$HostName`:$Port"
        player = $TargetPlayer
        region = $SmokeRegionId
        wait_for_checkpoints = $WaitForCheckpoints
        step_count = $resultItems.Count
        failed_steps = @($failedStepNames.ToArray())
        steps = @($resultItems)
    }

    $reportObject | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $OutputPath -Encoding UTF8
    if (-not $reportObject.ok) {
        throw "RCON player quest smoke a esuat: $($reportObject.failed_steps -join ', '). Raport: $OutputPath"
    }
    return $reportObject
}

function Invoke-QuestRconPreflight {
    param(
        [Parameter(Mandatory = $true)]
        [string]$HostName,

        [Parameter(Mandatory = $true)]
        [int]$Port,

        [Parameter(Mandatory = $true)]
        [string]$Password,

        [Parameter(Mandatory = $true)]
        [int]$TimeoutSeconds,

        [Parameter(Mandatory = $true)]
        [string]$PreflightRegionId,

        [Parameter(Mandatory = $true)]
        [string]$OutputPath
    )

    if ($Password.Trim().Length -eq 0) {
        throw "Pentru -RunRconPreflight seteaza -RconPassword; parola nu este scrisa in raport."
    }

    $client = [System.Net.Sockets.TcpClient]::new()
    $client.ReceiveTimeout = $TimeoutSeconds * 1000
    $client.SendTimeout = $TimeoutSeconds * 1000
    $requestId = 1
    $startedAt = Get-Date
    $steps = @(
        @{ Name = "plugins"; Command = "plugins"; Required = @("AINPCPlugin", "AINPCScenarioMedieval") },
        @{ Name = "ainpc-help"; Command = "ainpc"; Required = @("AINPC") },
        @{ Name = "world-demo"; Command = "ainpc world demo create $PreflightRegionId"; Required = @() },
        @{ Name = "progression-definitions"; Command = "ainpc progression definitions"; Required = @("Progression Definitions"); MinTotal = 1 },
        @{ Name = "quest-definitions"; Command = "ainpc quest definitions"; Required = @("Progression Definitions"); MinTotal = 1 },
        @{ Name = "contract-definitions"; Command = "ainpc contract definitions"; Required = @("Progression Definitions"); MinTotal = 1 },
        @{ Name = "duty-definitions"; Command = "ainpc duty definitions"; Required = @("Progression Definitions"); MinTotal = 1 },
        @{ Name = "bounty-definitions"; Command = "ainpc bounty definitions"; Required = @("Progression Definitions"); MinTotal = 1 },
        @{ Name = "event-definitions"; Command = "ainpc event definitions"; Required = @("Progression Definitions"); MinTotal = 1 },
        @{ Name = "tutorial-definitions"; Command = "ainpc tutorial definitions"; Required = @("Progression Definitions"); MinTotal = 1 },
        @{ Name = "ritual-definitions"; Command = "ainpc ritual definitions"; Required = @("Progression Definitions"); MinTotal = 1 },
        @{ Name = "audit-quest-offline"; Command = "ainpc audit quest offline"; Required = @("AINPC Audit: quest offline"); RequireZeroAuditErrors = $true }
    )

    $results = New-Object System.Collections.Generic.List[object]
    try {
        $connectTask = $client.ConnectAsync($HostName, $Port)
        if (-not $connectTask.Wait($TimeoutSeconds * 1000)) {
            throw "Timeout la conectarea RCON $HostName`:$Port."
        }
        $stream = $client.GetStream()

        $authPacket = New-RconPacket -RequestId $requestId -Type 3 -Body $Password
        $stream.Write($authPacket, 0, $authPacket.Length)
        $stream.Flush()
        $authResponse = Read-RconPacket -Stream $stream
        if ($authResponse.Id -eq -1) {
            throw "Autentificarea RCON a esuat."
        }
        $requestId++

        foreach ($step in $steps) {
            $response = Send-RconCommand -Stream $stream -RequestId $requestId -SentinelId ($requestId + 1) -Command $step.Command
            $requestId += 2
            $cleanResponse = Remove-MinecraftFormatting -Text $response.Body
            $issues = @(Get-RconResponseIssues -Step $step -CleanResponse $cleanResponse)
            $results.Add([pscustomobject]@{
                name = $step.Name
                command = $step.Command
                ok = $issues.Count -eq 0
                issues = @($issues)
                response = $cleanResponse.Trim()
            })
        }
    } finally {
        $client.Close()
    }

    $resultItems = @($results.ToArray())
    $failed = @($resultItems | Where-Object { -not $_.ok })
    $failedStepNames = New-Object System.Collections.Generic.List[string]
    foreach ($failedStep in $failed) {
        $failedStepNames.Add($failedStep.name)
    }
    $reportObject = [pscustomobject]@{
        ok = $failed.Count -eq 0
        generated_at = (Get-Date).ToString("o")
        duration_seconds = [Math]::Round(((Get-Date) - $startedAt).TotalSeconds, 3)
        server = "$HostName`:$Port"
        region = $PreflightRegionId
        step_count = $resultItems.Count
        failed_steps = @($failedStepNames.ToArray())
        steps = @($resultItems)
    }

    $reportObject | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $OutputPath -Encoding UTF8
    if (-not $reportObject.ok) {
        throw "RCON quest preflight a esuat: $($reportObject.failed_steps -join ', '). Raport: $OutputPath"
    }
    return $reportObject
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
    "ainpc world demo create $RegionId",
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
    "ainpc world demo create $RegionId",
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
    "ainpc world demo create $RegionId",
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
    "ainpc world demo create $RegionId",
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
    "ainpc world demo create $RegionId",
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
    "ainpc world demo create $RegionId",
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
    "ainpc world demo create $RegionId",
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
    "ainpc world demo create $RegionId",
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
    "Region: $RegionId",
    "Build skipped: $SkipBuild",
    "Tests requested: $RunTests",
    "Copy skipped: $NoCopy",
    "RCON preflight requested: $RunRconPreflight",
    "RCON player smoke requested: $RunRconPlayerSmoke",
    "RCON endpoint: $RconHost`:$RconPort",
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

if ($RunRconPreflight) {
    $rconResult = Invoke-QuestRconPreflight `
        -HostName $RconHost `
        -Port $RconPort `
        -Password $RconPassword `
        -TimeoutSeconds $RconTimeoutSeconds `
        -PreflightRegionId $RegionId `
        -OutputPath $rconReportPath

    Add-Content -LiteralPath $reportPath -Value @(
        "",
        "RCON preflight: $($rconResult.ok)",
        "RCON report: $rconReportPath"
    ) -Encoding UTF8
    Write-Host "RCON preflight: $($rconResult.ok)"
    Write-Host "Raport RCON: $rconReportPath"
}

if ($RunRconPlayerSmoke) {
    $playerRconResult = Invoke-QuestRconPlayerSmoke `
        -HostName $RconHost `
        -Port $RconPort `
        -Password $RconPassword `
        -TimeoutSeconds $RconTimeoutSeconds `
        -TargetPlayer $PlayerName `
        -SmokeRegionId $RegionId `
        -OutputPath $rconPlayerReportPath `
        -WaitForCheckpoints ([bool]$WaitForPlayerCheckpoints)

    Add-Content -LiteralPath $reportPath -Value @(
        "",
        "RCON player smoke: $($playerRconResult.ok)",
        "RCON player report: $rconPlayerReportPath"
    ) -Encoding UTF8
    Write-Host "RCON player smoke: $($playerRconResult.ok)"
    Write-Host "Raport RCON player: $rconPlayerReportPath"
}

if ($paperJars.Count -eq 0) {
    Write-Warning "Nu am gasit JAR de server in $serverDirFull. Scriptul a pregatit plugins/ si comenzile, dar serverul Paper trebuie pornit separat."
}

if (-not $hasEula) {
    Write-Warning "Nu exista eula.txt in server dir. Prima pornire Paper poate cere acceptarea EULA."
}

if (-not $hasServerProperties) {
    Write-Warning "Nu exista server.properties in server dir. Daca acesta este server nou, Paper il va genera la prima pornire."
}
