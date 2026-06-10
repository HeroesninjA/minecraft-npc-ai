[CmdletBinding()]
param(
    [string]$OutFile = "",
    [string]$SchtasksExe = ""
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

    return ""
}

function Invoke-SafeCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Exe,
        [Parameter(Mandatory = $true)]
        [string[]]$Args
    )

    try {
        $text = (& $Exe @Args 2>&1 | Out-String)
        $exitCode = $LASTEXITCODE
        return [pscustomobject]@{
            ok = ($exitCode -eq 0)
            exit_code = $exitCode
            output = $text.Trim()
        }
    } catch {
        return [pscustomobject]@{
            ok = $false
            exit_code = $null
            output = $_.Exception.Message
        }
    }
}

$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
$principal = New-Object Security.Principal.WindowsPrincipal($identity)
$isAdmin = $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

$scheduleService = $null
try {
    $svc = Get-Service -Name "Schedule" -ErrorAction Stop
    $scheduleService = [pscustomobject]@{
        exists = $true
        status = [string]$svc.Status
        start_type = [string]$svc.StartType
    }
} catch {
    $scheduleService = [pscustomobject]@{
        exists = $false
        status = ""
        start_type = ""
        error = $_.Exception.Message
    }
}

$schtasksPath = Resolve-SchtasksExePath -Override $SchtasksExe
$queryProbe = $null
$createProbe = $null

if (-not [string]::IsNullOrWhiteSpace($schtasksPath)) {
    $queryProbe = Invoke-SafeCommand -Exe $schtasksPath -Args @("/Query", "/FO", "LIST")
    $tempTask = "AINPC-MCP-DIAG-" + [Guid]::NewGuid().ToString("N").Substring(0, 8)
    $createProbe = Invoke-SafeCommand -Exe $schtasksPath -Args @("/Create", "/SC", "ONCE", "/TN", $tempTask, "/TR", "cmd /c echo ok", "/ST", "23:59", "/F")
    if ($createProbe.ok) {
        [void](Invoke-SafeCommand -Exe $schtasksPath -Args @("/Delete", "/TN", $tempTask, "/F"))
    }
} else {
    $queryProbe = [pscustomobject]@{
        ok = $false
        exit_code = $null
        output = "schtasks.exe not found"
    }
    $createProbe = [pscustomobject]@{
        ok = $false
        exit_code = $null
        output = "schtasks.exe not found"
    }
}

$report = [ordered]@{
    generated_at = (Get-Date).ToString("o")
    host = [Environment]::MachineName
    os_version = [Environment]::OSVersion.VersionString
    user = $identity.Name
    is_admin = $isAdmin
    schtasks_path = $schtasksPath
    schedule_service = $scheduleService
    probes = [ordered]@{
        query = $queryProbe
        create_delete = $createProbe
    }
}

$reportJson = $report | ConvertTo-Json -Depth 8

if (-not [string]::IsNullOrWhiteSpace($OutFile)) {
    $target = if ([System.IO.Path]::IsPathRooted($OutFile)) { $OutFile } else { Join-Path (Get-Location).Path $OutFile }
    $dir = Split-Path -Parent $target
    if (-not [string]::IsNullOrWhiteSpace($dir) -and -not (Test-Path -LiteralPath $dir)) {
        New-Item -ItemType Directory -Path $dir | Out-Null
    }
    Set-Content -LiteralPath $target -Value $reportJson -Encoding UTF8
    Write-Host ("[mcp-context-scheduler-diagnose] Report saved to: {0}" -f $target)
}

Write-Output $reportJson
