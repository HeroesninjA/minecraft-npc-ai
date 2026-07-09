param(
    [string]$ChangelogPath = "",
    [string]$OutputDir = "",
    [string]$NewVersion = "",
    [string]$ReleaseTitle = "",
    [switch]$UpdateGradleProps,
    [switch]$Help
)

if ($Help) {
    Write-Host @"
Usage: .\scripts\release-notes.ps1 [-NewVersion X.Y.Z] [-ReleaseTitle "Title"] [-UpdateGradleProps] [-OutputDir path]

Examples:
  .\scripts\release-notes.ps1 -NewVersion 1.1.0 -ReleaseTitle "NPC Routines + Economy" -UpdateGradleProps
  .\scripts\release-notes.ps1 -OutputDir .\build\release-notes
"@
    return
}

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $ChangelogPath) { $ChangelogPath = Join-Path $repoRoot "CHANGELOG.md" }
if (-not $OutputDir) { $OutputDir = Join-Path $repoRoot "build\release-notes" }

if (-not (Test-Path -LiteralPath $ChangelogPath -PathType Leaf)) {
    throw "CHANGELOG.md nu a fost gasit la: $ChangelogPath"
}

if (-not (Test-Path -LiteralPath $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$content = Get-Content -LiteralPath $ChangelogPath -Raw
$lines = Get-Content -LiteralPath $ChangelogPath

$unreleasedStart = -1
$unreleasedEnd = -1

for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match "^## \[Unreleased\]") {
        $unreleasedStart = $i
    }
    if ($unreleasedStart -ge 0 -and $lines[$i] -match "^## \[") {
        $unreleasedEnd = $i
        break
    }
}

if ($unreleasedStart -lt 0) {
    throw "Nu s-a gasit sectiunea ## [Unreleased] in CHANGELOG.md"
}
if ($unreleasedEnd -lt 0) { $unreleasedEnd = $lines.Count }

$unreleasedLines = $lines[($unreleasedStart + 1)..($unreleasedEnd - 1)] | Where-Object { $_.Trim() -ne "" }

$date = Get-Date -Format "yyyy-MM-dd"
$version = if ($NewVersion) { $NewVersion } else { "NEXT" }
$title = if ($ReleaseTitle) { " - $ReleaseTitle" } else { "" }

$releaseNotes = @"
# v$version$title

**Data:** $date

$($unreleasedLines -join "`n")
"@

$safeVersion = $version -replace '\.', '-'
$mdPath = Join-Path $OutputDir "release-notes-v$safeVersion.md"
Set-Content -LiteralPath $mdPath -Value $releaseNotes -Encoding UTF8

if ($UpdateGradleProps -and $NewVersion) {
    $propsPath = Join-Path $repoRoot "gradle.properties"
    $props = Get-Content -LiteralPath $propsPath
    $updated = $props -replace '^projectVersion=.*', "projectVersion=$NewVersion"
    Set-Content -LiteralPath $propsPath -Value $updated -Encoding UTF8
    Write-Host "Updated gradle.properties to version $NewVersion"
}

Write-Host "Release notes generated: $mdPath"
return @{
    ok = $true
    path = $mdPath
    version = $version
    date = $date
}
