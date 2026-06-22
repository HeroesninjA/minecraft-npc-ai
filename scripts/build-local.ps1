param(
    [string]$JavaHome,
    [string[]]$GradleArgs = @("clean", "build")
)

$ErrorActionPreference = "Stop"

function Resolve-JavaHomeCandidate {
    param(
        [string]$Candidate
    )

    if ([string]::IsNullOrWhiteSpace($Candidate)) {
        return $null
    }

    $resolved = $Candidate
    if (Test-Path -LiteralPath $Candidate) {
        $resolved = (Resolve-Path -LiteralPath $Candidate).Path
    }

    if (Test-Path -LiteralPath (Join-Path $resolved 'bin/java.exe')) {
        return $resolved
    }

    return $null
}

function Find-LocalJavaHome {
    $standardCandidates = @(
        $env:JAVA_HOME,
        'C:\Program Files\Java\jdk-25.0.2',
        'C:\Program Files\Java\jdk-25',
        'C:\Program Files\Java\jdk-21.0.2',
        'C:\Program Files\Java\jdk-21',
        'C:\Program Files\Java\jdk-17',
        'C:\Program Files\Eclipse Adoptium',
        'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\jbr',
        'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1',
        'C:\Program Files\JetBrains\IntelliJ IDEA 2025.3\jbr',
        'C:\Program Files\JetBrains\IntelliJ IDEA 2025.3'
    )

    foreach ($candidate in $standardCandidates) {
        $javaHome = Resolve-JavaHomeCandidate -Candidate $candidate
        if ($javaHome) {
            return $javaHome
        }
    }

    $jetBrainsRoots = @(
        'C:\Program Files\JetBrains',
        'C:\Program Files (x86)\JetBrains'
    )

    foreach ($root in $jetBrainsRoots) {
        if (-not (Test-Path -LiteralPath $root)) {
            continue
        }

        Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -like 'IntelliJ IDEA*' } |
            ForEach-Object {
                $candidateRoots = @(
                    (Join-Path $_.FullName 'jbr'),
                    $_.FullName
                )

                foreach ($candidateRoot in $candidateRoots) {
                    $javaHome = Resolve-JavaHomeCandidate -Candidate $candidateRoot
                    if ($javaHome) {
                        return $javaHome
                    }
                }
            }
    }

    return $null
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$resolvedJavaHome = Resolve-JavaHomeCandidate -Candidate $JavaHome
if (-not $resolvedJavaHome) {
    $resolvedJavaHome = Find-LocalJavaHome
}

if (-not $resolvedJavaHome) {
    throw "Nu am gasit un JDK local. Seteaza -JavaHome sau instaleaza un JDK 21+."
}

$env:JAVA_HOME = $resolvedJavaHome
$env:Path = "$(Join-Path $env:JAVA_HOME 'bin');$env:Path"

Write-Host "JAVA_HOME = $env:JAVA_HOME" -ForegroundColor Cyan
Write-Host "Gradle args = $($GradleArgs -join ' ')" -ForegroundColor Cyan

$gradleWrapper = Join-Path $repoRoot 'gradlew.bat'
& $gradleWrapper @GradleArgs
exit $LASTEXITCODE
