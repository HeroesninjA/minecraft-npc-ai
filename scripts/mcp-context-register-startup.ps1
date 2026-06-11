[CmdletBinding()]
param(
    [string]$ProjectRoot = "",
    [string]$PowerShellExe = "powershell.exe",
    [string]$LauncherScriptPath = ".\scripts\mcp-context-nightly-matrix-launcher.ps1",
    [string]$StartupFileName = "AINPC-MCP-Context-Nightly.cmd",
    [string]$RegistrationStateFile = ".ai\mcp-startup-registration.json",
    [switch]$Apply,
    [switch]$Remove
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-AbsolutePath {
    param([string]$Path)
    if ([System.IO.Path]::IsPathRooted($Path)) { return $Path }
    return (Join-Path (Get-Location).Path $Path)
}

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Get-Location).Path
}

$absProjectRoot = Resolve-AbsolutePath -Path $ProjectRoot
if (-not (Test-Path -LiteralPath $absProjectRoot)) {
    throw "ProjectRoot nu exista: $absProjectRoot"
}

$absLauncherPath = if ([System.IO.Path]::IsPathRooted($LauncherScriptPath)) {
    $LauncherScriptPath
} else {
    Join-Path $absProjectRoot $LauncherScriptPath
}
if (-not (Test-Path -LiteralPath $absLauncherPath)) {
    throw "Launcher script nu exista: $absLauncherPath"
}

$startupDir = [Environment]::GetFolderPath("Startup")
if ([string]::IsNullOrWhiteSpace($startupDir)) {
    throw "Nu am putut rezolva folderul Startup."
}

if (-not (Test-Path -LiteralPath $startupDir)) {
    New-Item -ItemType Directory -Path $startupDir | Out-Null
}

$startupFilePath = Join-Path $startupDir $StartupFileName
$registrationStatePath = if ([System.IO.Path]::IsPathRooted($RegistrationStateFile)) {
    $RegistrationStateFile
} else {
    Join-Path $absProjectRoot $RegistrationStateFile
}
$cmdLine = "@echo off`r`ncd /d `"$absProjectRoot`"`r`n$PowerShellExe -NoProfile -ExecutionPolicy Bypass -File `"$absLauncherPath`"`r`n"

Write-Host "[mcp-context-register-startup] Preview:"
Write-Host ("  Startup dir: {0}" -f $startupDir)
Write-Host ("  Startup file: {0}" -f $startupFilePath)
Write-Host ("  Launcher: {0}" -f $absLauncherPath)
Write-Host ("  Registration state file: {0}" -f $registrationStatePath)

function Save-RegistrationState {
    param(
        [bool]$Applied,
        [bool]$Removed,
        [string]$Note = ""
    )

    $stateDir = Split-Path -Parent $registrationStatePath
    if (-not [string]::IsNullOrWhiteSpace($stateDir) -and -not (Test-Path -LiteralPath $stateDir)) {
        New-Item -ItemType Directory -Path $stateDir | Out-Null
    }

    $state = [ordered]@{
        updated_at = (Get-Date).ToString("o")
        project_root = $absProjectRoot
        startup_file = $startupFilePath
        launcher_script = $absLauncherPath
        applied = $Applied
        removed = $Removed
        note = $Note
    }
    Set-Content -LiteralPath $registrationStatePath -Value ($state | ConvertTo-Json -Depth 6) -Encoding UTF8
}

if ($Remove) {
    if (Test-Path -LiteralPath $startupFilePath) {
        if ($Apply) {
            Remove-Item -LiteralPath $startupFilePath -Force
            Save-RegistrationState -Applied $false -Removed $true -Note "startup_file_removed"
            Write-Host ("[mcp-context-register-startup] Removed: {0}" -f $startupFilePath)
        } else {
            Write-Host "[mcp-context-register-startup] Dry-run remove. Add -Apply to delete startup file."
        }
    } else {
        Write-Host "[mcp-context-register-startup] Startup file does not exist."
    }
    return
}

if (-not $Apply) {
    Write-Host "[mcp-context-register-startup] Dry-run mode. Add -Apply to create/update startup file."
    return
}

Set-Content -LiteralPath $startupFilePath -Value $cmdLine -Encoding ASCII
Save-RegistrationState -Applied $true -Removed $false -Note "startup_file_created_or_updated"
Write-Host ("[mcp-context-register-startup] Created/updated: {0}" -f $startupFilePath)
