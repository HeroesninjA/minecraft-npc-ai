[CmdletBinding()]
param(
    [string]$TaskName = "AINPC-MCP-Context-Nightly",
    [string]$StartTime = "02:30",
    [string]$ProjectRoot = "",
    [string]$SchtasksExe = "",
    [string]$PowerShellExe = "powershell.exe",
    [string]$ScriptPath = ".\scripts\mcp-context-nightly-matrix-launcher.ps1",
    [string]$Arguments = "",
    [switch]$Apply,
    [switch]$DiagnoseOnFailure,
    [string]$DiagnoseOutFile = ".ai\scheduler-diagnose.json"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-AbsolutePath {
    param([string]$Path)
    if ([System.IO.Path]::IsPathRooted($Path)) { return $Path }
    return (Join-Path (Get-Location).Path $Path)
}

function Resolve-SchtasksExePath {
    param([string]$Override)

    if (-not [string]::IsNullOrWhiteSpace($Override)) {
        $resolved = Resolve-AbsolutePath -Path $Override
        if (-not (Test-Path -LiteralPath $resolved)) {
            throw "SchtasksExe nu exista: $resolved"
        }
        return $resolved
    }

    $cmd = Get-Command "schtasks.exe" -ErrorAction SilentlyContinue
    if ($null -ne $cmd -and -not [string]::IsNullOrWhiteSpace($cmd.Source)) {
        return $cmd.Source
    }

    if (-not [string]::IsNullOrWhiteSpace($env:WINDIR)) {
        $fallback = Join-Path $env:WINDIR "System32\\schtasks.exe"
        if (Test-Path -LiteralPath $fallback) {
            return $fallback
        }
    }

    throw "Nu am gasit schtasks.exe. Seteaza explicit -SchtasksExe."
}

function Run-SchedulerDiagnose {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ProjectPath,
        [Parameter(Mandatory = $true)]
        [string]$DiagOutFile,
        [Parameter(Mandatory = $true)]
        [string]$SchtasksPath
    )

    try {
        $diagScript = Join-Path $ProjectPath "scripts\mcp-context-scheduler-diagnose.ps1"
        if (-not (Test-Path -LiteralPath $diagScript)) {
            Write-Warning "[mcp-context-register-schedule] Diagnose script not found."
            return
        }

        $diagOutAbs = if ([System.IO.Path]::IsPathRooted($DiagOutFile)) {
            $DiagOutFile
        } else {
            Join-Path $ProjectPath $DiagOutFile
        }

        & $diagScript -OutFile $diagOutAbs -SchtasksExe $SchtasksPath | Out-Null
        Write-Warning ("[mcp-context-register-schedule] Diagnose report: {0}" -f $diagOutAbs)
    } catch {
        Write-Warning ("[mcp-context-register-schedule] Diagnose failed: {0}" -f $_.Exception.Message)
    }
}

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Get-Location).Path
}

$absProjectRoot = Resolve-AbsolutePath -Path $ProjectRoot
$resolvedSchtasksExe = Resolve-SchtasksExePath -Override $SchtasksExe
if (-not (Test-Path -LiteralPath $absProjectRoot)) {
    throw "ProjectRoot nu exista: $absProjectRoot"
}

$absScriptPath = if ([System.IO.Path]::IsPathRooted($ScriptPath)) {
    $ScriptPath
} else {
    Join-Path $absProjectRoot $ScriptPath
}
if (-not (Test-Path -LiteralPath $absScriptPath)) {
    throw "ScriptPath nu exista: $absScriptPath"
}

$timeMatch = [regex]::Match($StartTime, "^(?:[01]\d|2[0-3]):[0-5]\d$")
if (-not $timeMatch.Success) {
    throw "StartTime invalid. Foloseste HH:mm, ex: 02:30"
}

$taskRunCommand = "{0} -NoProfile -ExecutionPolicy Bypass -File `"{1}`" {2}" -f $PowerShellExe, $absScriptPath, $Arguments
$taskRunCommand = $taskRunCommand.Trim()
$createArgs = @(
    "/Create",
    "/SC", "DAILY",
    "/TN", $TaskName,
    "/TR", $taskRunCommand,
    "/ST", $StartTime,
    "/F"
)

$queryArgs = @("/Query", "/TN", $TaskName)

Write-Host "[mcp-context-register-schedule] Task preview:"
Write-Host ("  Name: {0}" -f $TaskName)
Write-Host ("  Start: {0}" -f $StartTime)
Write-Host ("  ProjectRoot: {0}" -f $absProjectRoot)
Write-Host ("  Script: {0}" -f $absScriptPath)
Write-Host ("  Run command: {0}" -f $taskRunCommand)
Write-Host ("  Schtasks: {0}" -f $resolvedSchtasksExe)

if (-not $Apply) {
    Write-Host "[mcp-context-register-schedule] Dry-run mode. Add -Apply to register/update the task."
    return
}

Push-Location $absProjectRoot
try {
    & $resolvedSchtasksExe @createArgs | Out-Host
    $createExit = $LASTEXITCODE
    if ($createExit -ne 0) {
        throw "schtasks /Create a esuat cu exit code $createExit."
    }

    Write-Host ("[mcp-context-register-schedule] Task '{0}' created/updated." -f $TaskName)
    & $resolvedSchtasksExe @queryArgs | Out-Host
    $queryExit = $LASTEXITCODE
    if ($queryExit -ne 0) {
        throw "schtasks /Query a esuat cu exit code $queryExit."
    }
} catch {
    if ($DiagnoseOnFailure) {
        Run-SchedulerDiagnose -ProjectPath $absProjectRoot -DiagOutFile $DiagnoseOutFile -SchtasksPath $resolvedSchtasksExe
    }
    throw
} finally {
    Pop-Location
}
