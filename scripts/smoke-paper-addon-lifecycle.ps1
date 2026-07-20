[CmdletBinding()]
param(
    [string]$ProjectRoot = "",
    [string]$PaperJar = $env:PAPER_JAR,
    [string]$JavaExecutable = "",
    [string]$OutputFile = ".ai\release-reports\paper-addon-lifecycle-smoke.json",
    [ValidateRange(30, 600)]
    [int]$StartupTimeoutSeconds = 180,
    [ValidateRange(10, 180)]
    [int]$ShutdownTimeoutSeconds = 60,
    [ValidateRange(512, 4096)]
    [int]$MaxMemoryMb = 1024,
    [switch]$SkipBuild,
    [switch]$KeepServer
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-FileFromRoot {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PathValue,
        [Parameter(Mandatory = $true)]
        [string]$RootPath,
        [Parameter(Mandatory = $true)]
        [string]$Label
    )

    $candidate = if ([IO.Path]::IsPathRooted($PathValue)) {
        $PathValue
    } else {
        Join-Path $RootPath $PathValue
    }
    if (-not (Test-Path -LiteralPath $candidate -PathType Leaf)) {
        throw "$Label lipseste: $candidate"
    }
    return (Resolve-Path -LiteralPath $candidate).Path
}

function Resolve-JavaCommand {
    param(
        [string]$RequestedCommand
    )

    if (-not [string]::IsNullOrWhiteSpace($RequestedCommand)) {
        if (Test-Path -LiteralPath $RequestedCommand -PathType Leaf) {
            return (Resolve-Path -LiteralPath $RequestedCommand).Path
        }
        $requested = Get-Command $RequestedCommand -ErrorAction SilentlyContinue
        if ($null -ne $requested) {
            return $requested.Source
        }
        throw "JavaExecutable nu poate fi rezolvat: $RequestedCommand"
    }

    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $javaFromHome = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (-not (Test-Path -LiteralPath $javaFromHome -PathType Leaf)) {
            $javaFromHome = Join-Path $env:JAVA_HOME "bin\java"
        }
        if (Test-Path -LiteralPath $javaFromHome -PathType Leaf) {
            return (Resolve-Path -LiteralPath $javaFromHome).Path
        }
    }

    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($null -eq $javaCommand) {
        throw "Java nu este disponibil. Seteaza JAVA_HOME sau foloseste -JavaExecutable."
    }
    return $javaCommand.Source
}

function Resolve-PaperJar {
    param(
        [string]$RequestedJar,
        [string]$RootPath
    )

    if (-not [string]::IsNullOrWhiteSpace($RequestedJar)) {
        return Resolve-FileFromRoot -PathValue $RequestedJar -RootPath $RootPath -Label "Paper JAR"
    }

    $paperDataDirectory = Join-Path $RootPath "paper-data\data"
    if (Test-Path -LiteralPath $paperDataDirectory -PathType Container) {
        $candidate = Get-ChildItem -LiteralPath $paperDataDirectory -File -Filter "paper-*.jar" |
            Sort-Object LastWriteTimeUtc -Descending |
            Select-Object -First 1
        if ($null -ne $candidate) {
            return $candidate.FullName
        }
    }

    throw "Paper JAR lipseste. Seteaza PAPER_JAR sau foloseste -PaperJar."
}

function Get-ProjectVersion {
    param(
        [string]$RootPath
    )

    $propertiesPath = Join-Path $RootPath "gradle.properties"
    $match = Select-String -LiteralPath $propertiesPath -Pattern '^projectVersion=(.+)$' | Select-Object -First 1
    if ($null -eq $match) {
        throw "projectVersion lipseste din gradle.properties"
    }
    return $match.Matches[0].Groups[1].Value.Trim()
}

function Invoke-PluginBuild {
    param(
        [string]$RootPath,
        [string]$JavaPath
    )

    $wrapper = Join-Path $RootPath "gradlew.bat"
    if (-not (Test-Path -LiteralPath $wrapper -PathType Leaf)) {
        $wrapper = Join-Path $RootPath "gradlew"
    }
    if (-not (Test-Path -LiteralPath $wrapper -PathType Leaf)) {
        throw "Gradle wrapper lipseste in $RootPath"
    }

    $previousJavaHome = $env:JAVA_HOME
    $previousPath = $env:Path
    try {
        $javaHome = Split-Path (Split-Path $JavaPath -Parent) -Parent
        $env:JAVA_HOME = $javaHome
        $env:Path = "$(Join-Path $javaHome 'bin');$previousPath"
        & $wrapper :ainpc-core-plugin:jar :ainpc-scenario-medieval:jar --no-configuration-cache
        if ($LASTEXITCODE -ne 0) {
            throw "Build-ul JAR-urilor Paper a esuat cu exit code $LASTEXITCODE"
        }
    } finally {
        $env:JAVA_HOME = $previousJavaHome
        $env:Path = $previousPath
    }
}

function Read-PaperLog {
    param(
        [string]$LogPath
    )

    if (-not (Test-Path -LiteralPath $LogPath -PathType Leaf)) {
        return ""
    }
    try {
        return Get-Content -LiteralPath $LogPath -Raw -Encoding UTF8 -ErrorAction Stop
    } catch {
        return ""
    }
}

function Add-PatternFailures {
    param(
        [string]$Text,
        [string[]]$RequiredPatterns,
        [string[]]$ForbiddenPatterns,
        [System.Collections.Generic.List[string]]$Failures
    )

    foreach ($pattern in $RequiredPatterns) {
        if ($Text -notmatch $pattern) {
            $Failures.Add("lipseste semnalul: $pattern")
        }
    }
    foreach ($pattern in $ForbiddenPatterns) {
        if ($Text -match $pattern) {
            $Failures.Add("semnal interzis detectat: $pattern")
        }
    }
}

function Invoke-PaperPhase {
    param(
        [string]$Name,
        [string]$ServerDirectory,
        [string]$PaperJarPath,
        [string]$JavaPath,
        [int]$MemoryMb,
        [int]$StartupTimeout,
        [int]$ShutdownTimeout,
        [string[]]$RequiredReadyPatterns,
        [string[]]$RequiredStoppedPatterns,
        [string[]]$ForbiddenPatterns,
        [scriptblock]$ReadyAssertion,
        [scriptblock]$StoppedAssertion,
        [string]$EvidencePath
    )

    $startedAt = Get-Date
    $failures = [System.Collections.Generic.List[string]]::new()
    $latestLog = Join-Path $ServerDirectory "logs\latest.log"
    if (Test-Path -LiteralPath $latestLog -PathType Leaf) {
        Remove-Item -LiteralPath $latestLog -Force
    }

    $process = $null
    $processStarted = $false
    $stdoutTask = $null
    $stderrTask = $null
    $ready = $false
    $gracefulStop = $false
    $exitCode = $null
    $liveLog = ""
    $combinedLog = ""

    try {
        $arguments = "-Xms512M -Xmx${MemoryMb}M -jar `"$PaperJarPath`" --nogui"
        $startInfo = [Diagnostics.ProcessStartInfo]::new($JavaPath, $arguments)
        $startInfo.WorkingDirectory = $ServerDirectory
        $startInfo.UseShellExecute = $false
        $startInfo.RedirectStandardInput = $true
        $startInfo.RedirectStandardOutput = $true
        $startInfo.RedirectStandardError = $true

        $process = [Diagnostics.Process]::new()
        $process.StartInfo = $startInfo
        [void]$process.Start()
        $processStarted = $true
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()

        $startupDeadline = (Get-Date).AddSeconds($StartupTimeout)
        while ((Get-Date) -lt $startupDeadline -and -not $process.HasExited) {
            $liveLog = Read-PaperLog -LogPath $latestLog
            if ($liveLog -match 'Done \([0-9.,]+s\)!') {
                $ready = $true
                break
            }
            Start-Sleep -Milliseconds 500
        }

        if (-not $ready) {
            if ($process.HasExited) {
                $failures.Add("Paper s-a oprit inainte de readiness")
            } else {
                $failures.Add("timeout startup dupa $StartupTimeout secunde")
            }
        } else {
            $process.StandardInput.WriteLine("plugins")
            $process.StandardInput.Flush()
            $pluginsDeadline = (Get-Date).AddSeconds(5)
            while ((Get-Date) -lt $pluginsDeadline -and -not $process.HasExited) {
                $liveLog = Read-PaperLog -LogPath $latestLog
                if ($liveLog -match 'Server Plugins') {
                    break
                }
                Start-Sleep -Milliseconds 250
            }

            Add-PatternFailures `
                -Text $liveLog `
                -RequiredPatterns $RequiredReadyPatterns `
                -ForbiddenPatterns $ForbiddenPatterns `
                -Failures $failures
            if ($null -ne $ReadyAssertion) {
                foreach ($failure in @(& $ReadyAssertion $liveLog)) {
                    if (-not [string]::IsNullOrWhiteSpace([string]$failure)) {
                        $failures.Add([string]$failure)
                    }
                }
            }
        }
    } catch {
        $failures.Add("exceptie runner: $($_.Exception.Message)")
    } finally {
        if ($null -ne $process -and $processStarted) {
            if (-not $process.HasExited) {
                try {
                    $process.StandardInput.WriteLine("stop")
                    $process.StandardInput.Flush()
                    $gracefulStop = $process.WaitForExit($ShutdownTimeout * 1000)
                } catch {
                    $failures.Add("oprirea controlata a esuat: $($_.Exception.Message)")
                }
                if (-not $process.HasExited) {
                    $process.Kill()
                    $process.WaitForExit()
                    $failures.Add("Paper a fost oprit fortat dupa timeout")
                }
            }

            if ($process.HasExited) {
                $exitCode = $process.ExitCode
                if ($exitCode -ne 0) {
                    $failures.Add("Paper a iesit cu exit code $exitCode")
                }
            }

            $stdout = if ($null -ne $stdoutTask) { $stdoutTask.Result } else { "" }
            $stderr = if ($null -ne $stderrTask) { $stderrTask.Result } else { "" }
            $finalLog = Read-PaperLog -LogPath $latestLog
            $combinedLog = "$finalLog`n--- STDOUT ---`n$stdout`n--- STDERR ---`n$stderr"
            $process.Dispose()
        } elseif ($null -ne $process) {
            $process.Dispose()
        }
    }

    Add-PatternFailures `
        -Text $combinedLog `
        -RequiredPatterns $RequiredStoppedPatterns `
        -ForbiddenPatterns @() `
        -Failures $failures
    if ($null -ne $StoppedAssertion) {
        foreach ($failure in @(& $StoppedAssertion $combinedLog)) {
            if (-not [string]::IsNullOrWhiteSpace([string]$failure)) {
                $failures.Add([string]$failure)
            }
        }
    }

    [IO.File]::WriteAllText($EvidencePath, $combinedLog, [Text.UTF8Encoding]::new($false))
    return [pscustomobject]@{
        name = $Name
        ok = $failures.Count -eq 0
        ready = $ready
        graceful_stop = $gracefulStop
        exit_code = $exitCode
        duration_seconds = [Math]::Round(((Get-Date) - $startedAt).TotalSeconds, 3)
        evidence_log = $EvidencePath
        failures = @($failures)
    }
}

function Remove-SafeSmokeDirectory {
    param(
        [string]$DirectoryPath,
        [string]$TemporaryRoot
    )

    if (-not (Test-Path -LiteralPath $DirectoryPath -PathType Container)) {
        return
    }
    $resolvedDirectory = [IO.Path]::GetFullPath($DirectoryPath).TrimEnd('\', '/')
    $resolvedRoot = [IO.Path]::GetFullPath($TemporaryRoot).TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
    $directoryName = Split-Path $resolvedDirectory -Leaf
    if (-not $resolvedDirectory.StartsWith($resolvedRoot, [StringComparison]::OrdinalIgnoreCase) -or
        -not $directoryName.StartsWith("paper-addon-lifecycle-", [StringComparison]::OrdinalIgnoreCase)) {
        throw "Refuz stergerea directorului temporar neverificat: $resolvedDirectory"
    }
    Remove-Item -LiteralPath $resolvedDirectory -Recurse -Force
}

$root = if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    Split-Path $PSScriptRoot -Parent
} else {
    $ProjectRoot
}
$root = (Resolve-Path -LiteralPath $root).Path
$outputPath = if ([IO.Path]::IsPathRooted($OutputFile)) {
    [IO.Path]::GetFullPath($OutputFile)
} else {
    [IO.Path]::GetFullPath((Join-Path $root $OutputFile))
}
$outputDirectory = Split-Path $outputPath -Parent
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
$outputBase = Join-Path $outputDirectory ([IO.Path]::GetFileNameWithoutExtension($outputPath))
$temporaryRoot = Join-Path $root ".ai\tmp"
New-Item -ItemType Directory -Path $temporaryRoot -Force | Out-Null
$serverDirectory = Join-Path $temporaryRoot ("paper-addon-lifecycle-{0}-{1}" -f (Get-Date -Format "yyyyMMddHHmmssfff"), $PID)
$serverDirectory = [IO.Path]::GetFullPath($serverDirectory)
$phases = [System.Collections.Generic.List[object]]::new()
$errors = [System.Collections.Generic.List[string]]::new()
$startedAt = Get-Date
$javaPath = $null
$paperJarPath = $null
$coreJarPath = $null
$addonJarPath = $null
$projectVersion = $null

try {
    $javaPath = Resolve-JavaCommand -RequestedCommand $JavaExecutable
    $paperJarPath = Resolve-PaperJar -RequestedJar $PaperJar -RootPath $root
    $projectVersion = Get-ProjectVersion -RootPath $root

    if (-not $SkipBuild) {
        Invoke-PluginBuild -RootPath $root -JavaPath $javaPath
    }

    $coreJarPath = Resolve-FileFromRoot `
        -PathValue "ainpc-core-plugin\build\libs\ainpc-core-plugin-$projectVersion.jar" `
        -RootPath $root `
        -Label "Core JAR"
    $addonJarPath = Resolve-FileFromRoot `
        -PathValue "ainpc-scenario-medieval\build\libs\ainpc-scenario-medieval-$projectVersion.jar" `
        -RootPath $root `
        -Label "Addon JAR"

    $pluginsDirectory = Join-Path $serverDirectory "plugins"
    New-Item -ItemType Directory -Path $pluginsDirectory -Force | Out-Null
    Copy-Item -LiteralPath $coreJarPath -Destination $pluginsDirectory -Force
    Set-Content -LiteralPath (Join-Path $serverDirectory "eula.txt") -Value "eula=true" -Encoding Ascii
    @(
        "online-mode=false",
        "server-port=0",
        "enable-rcon=false",
        "level-type=minecraft:flat",
        'generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}',
        "generate-structures=false",
        "view-distance=2",
        "simulation-distance=2",
        "spawn-protection=0",
        "max-players=1"
    ) | Set-Content -LiteralPath (Join-Path $serverDirectory "server.properties") -Encoding Ascii

    $managedPackDirectory = Join-Path $serverDirectory "plugins\AINPCPlugin\packs\addons\ainpc-scenario-medieval"
    $addonConfigDirectory = Join-Path $serverDirectory "plugins\AINPCPlugin\addons\ainpc-scenario-medieval"
    $fatalPatterns = @(
        'NoClassDefFoundError',
        'ClassNotFoundException',
        'UnsupportedClassVersionError',
        'InvalidPluginException',
        'UnknownDependencyException',
        'Could not load.+ainpc',
        'Error occurred while enabling.+AINPC',
        'Addonul medieval a fost respins',
        '/ERROR\]:'
    )
    $coreReadyPatterns = @(
        '\[AINPCPlugin\] Loading server plugin AINPCPlugin v',
        '\[AINPCPlugin\] Enabling AINPCPlugin v',
        '\[AINPCPlugin\] AI NPC Plugin v.+ activat!',
        'Server Plugins',
        'AINPCPlugin'
    )

    $coreOnly = Invoke-PaperPhase `
        -Name "core-only" `
        -ServerDirectory $serverDirectory `
        -PaperJarPath $paperJarPath `
        -JavaPath $javaPath `
        -MemoryMb $MaxMemoryMb `
        -StartupTimeout $StartupTimeoutSeconds `
        -ShutdownTimeout $ShutdownTimeoutSeconds `
        -RequiredReadyPatterns $coreReadyPatterns `
        -RequiredStoppedPatterns @('\[AINPCPlugin\] Disabling AINPCPlugin v') `
        -ForbiddenPatterns ($fatalPatterns + @('AINPCScenarioMedieval')) `
        -ReadyAssertion {
            param($logText)
            if (Test-Path -LiteralPath $managedPackDirectory) {
                "folderul gestionat medieval exista in faza core-only"
            }
        } `
        -StoppedAssertion {
            param($logText)
            if (Test-Path -LiteralPath $managedPackDirectory) {
                "folderul gestionat medieval exista dupa oprirea core-only"
            }
        } `
        -EvidencePath "$outputBase-core-only.log"
    $phases.Add($coreOnly)
    if (-not $coreOnly.ok) {
        throw "Faza core-only a esuat: $($coreOnly.failures -join '; ')"
    }

    $addonDeployedPath = Join-Path $pluginsDirectory ([IO.Path]::GetFileName($addonJarPath))
    Copy-Item -LiteralPath $addonJarPath -Destination $addonDeployedPath -Force
    $fullLifecycle = Invoke-PaperPhase `
        -Name "core-with-addon" `
        -ServerDirectory $serverDirectory `
        -PaperJarPath $paperJarPath `
        -JavaPath $javaPath `
        -MemoryMb $MaxMemoryMb `
        -StartupTimeout $StartupTimeoutSeconds `
        -ShutdownTimeout $ShutdownTimeoutSeconds `
        -RequiredReadyPatterns ($coreReadyPatterns + @(
            '\[AINPCScenarioMedieval\] Loading server plugin AINPCScenarioMedieval v',
            '\[AINPCScenarioMedieval\] Enabling AINPCScenarioMedieval v',
            'Scenariul medieval este conectat la AINPC Core',
            'AINPCScenarioMedieval'
        )) `
        -RequiredStoppedPatterns @(
            '\[AINPCScenarioMedieval\] Disabling AINPCScenarioMedieval v',
            '\[AINPCPlugin\] Disabling AINPCPlugin v'
        ) `
        -ForbiddenPatterns $fatalPatterns `
        -ReadyAssertion {
            param($logText)
            $assertionFailures = [System.Collections.Generic.List[string]]::new()
            if (-not (Test-Path -LiteralPath $managedPackDirectory -PathType Container)) {
                $assertionFailures.Add("folderul pack-urilor gestionate nu a fost creat")
            }
            foreach ($managedPack in @("medieval.yml", "social.yml", "medieval_quest.yml")) {
                if (-not (Test-Path -LiteralPath (Join-Path $managedPackDirectory $managedPack) -PathType Leaf)) {
                    $assertionFailures.Add("pack gestionat lipsa: $managedPack")
                }
            }
            if (-not (Test-Path -LiteralPath (Join-Path $addonConfigDirectory "config.yml") -PathType Leaf)) {
                $assertionFailures.Add("config.yml per-addon nu a fost creat")
            }
            return @($assertionFailures)
        } `
        -StoppedAssertion {
            param($logText)
            $assertionFailures = [System.Collections.Generic.List[string]]::new()
            if (Test-Path -LiteralPath $managedPackDirectory) {
                $assertionFailures.Add("folderul pack-urilor gestionate nu a fost eliminat la shutdown")
            }
            if (-not (Test-Path -LiteralPath (Join-Path $addonConfigDirectory "config.yml") -PathType Leaf)) {
                $assertionFailures.Add("config.yml per-addon nu a ramas persistent dupa shutdown")
            }
            return @($assertionFailures)
        } `
        -EvidencePath "$outputBase-core-with-addon.log"
    $phases.Add($fullLifecycle)
    if (-not $fullLifecycle.ok) {
        throw "Faza core-with-addon a esuat: $($fullLifecycle.failures -join '; ')"
    }

    Remove-Item -LiteralPath $addonDeployedPath -Force
    $afterRemoval = Invoke-PaperPhase `
        -Name "core-after-addon-removal" `
        -ServerDirectory $serverDirectory `
        -PaperJarPath $paperJarPath `
        -JavaPath $javaPath `
        -MemoryMb $MaxMemoryMb `
        -StartupTimeout $StartupTimeoutSeconds `
        -ShutdownTimeout $ShutdownTimeoutSeconds `
        -RequiredReadyPatterns $coreReadyPatterns `
        -RequiredStoppedPatterns @('\[AINPCPlugin\] Disabling AINPCPlugin v') `
        -ForbiddenPatterns ($fatalPatterns + @('AINPCScenarioMedieval')) `
        -ReadyAssertion {
            param($logText)
            if (Test-Path -LiteralPath $managedPackDirectory) {
                "folderul gestionat medieval a reaparut dupa eliminarea addonului"
            }
        } `
        -StoppedAssertion {
            param($logText)
            if (Test-Path -LiteralPath $managedPackDirectory) {
                "folderul gestionat medieval exista dupa verificarea finala"
            }
        } `
        -EvidencePath "$outputBase-core-after-addon-removal.log"
    $phases.Add($afterRemoval)
    if (-not $afterRemoval.ok) {
        throw "Faza core-after-addon-removal a esuat: $($afterRemoval.failures -join '; ')"
    }
} catch {
    $errors.Add($_.Exception.Message)
}

$preserveServer = $KeepServer -or $errors.Count -gt 0
if (-not $preserveServer) {
    try {
        Remove-SafeSmokeDirectory -DirectoryPath $serverDirectory -TemporaryRoot $temporaryRoot
    } catch {
        $errors.Add("cleanup director temporar esuat: $($_.Exception.Message)")
        $preserveServer = $true
    }
}

$report = [ordered]@{
    schema_version = 1
    smoke = "paper-addon-lifecycle"
    started_at = $startedAt.ToUniversalTime().ToString("o")
    finished_at = (Get-Date).ToUniversalTime().ToString("o")
    ok = $errors.Count -eq 0
    project_version = $projectVersion
    java_executable = $javaPath
    paper_jar = $paperJarPath
    paper_jar_sha256 = if ($null -ne $paperJarPath) { (Get-FileHash -LiteralPath $paperJarPath -Algorithm SHA256).Hash } else { $null }
    core_jar = $coreJarPath
    core_jar_sha256 = if ($null -ne $coreJarPath) { (Get-FileHash -LiteralPath $coreJarPath -Algorithm SHA256).Hash } else { $null }
    addon_jar = $addonJarPath
    addon_jar_sha256 = if ($null -ne $addonJarPath) { (Get-FileHash -LiteralPath $addonJarPath -Algorithm SHA256).Hash } else { $null }
    server_directory = $serverDirectory
    server_directory_preserved = $preserveServer
    phases = @($phases)
    errors = @($errors)
}

[IO.File]::WriteAllText(
    $outputPath,
    ($report | ConvertTo-Json -Depth 8),
    [Text.UTF8Encoding]::new($false)
)

Write-Host "Paper addon lifecycle report: $outputPath"
Write-Host "Paper addon lifecycle ok: $($report.ok)"
if ($preserveServer) {
    Write-Host "Paper server evidence preserved: $serverDirectory"
}
if (-not $report.ok) {
    $errors | ForEach-Object { Write-Error $_ }
    exit 1
}
