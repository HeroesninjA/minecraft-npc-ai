[CmdletBinding()]
param(
    [string]$Hostname = "127.0.0.1",
    [int]$Port = 25575,
    [string]$Password = $env:RCON_PASSWORD,
    [ValidateSet("all", "npc", "world", "db", "spawn", "quest", "wand")]
    [string]$Mode = "all",
    [ValidateSet("standard", "strict", "full", "offline")]
    [string]$Profile = "standard",
    [string]$ResponseFile
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$processExitCode = 3
$connected = $false

try {
    if ($Profile -ne "standard" -and $Mode -notin @("all", "quest")) {
        throw "Profilul $Profile este acceptat numai pentru modurile all si quest."
    }

    . (Join-Path $PSScriptRoot "ainpc-audit-contract.ps1")
    if (-not $PSBoundParameters.ContainsKey("ResponseFile")) {
        if ([string]::IsNullOrWhiteSpace($Password)) {
            throw "RCON_PASSWORD sau -Password este obligatoriu."
        }
        . (Join-Path $PSScriptRoot "rcon-client.ps1")
        $connectionResults = @(Connect-Rcon -Hostname $Hostname -Port $Port -Password $Password 6>$null)
        $connected = $connectionResults.Count -gt 0 -and [bool]$connectionResults[-1]
        if (-not $connected) {
            throw "Conectarea RCON a esuat."
        }

        $commandParts = @("ainpc", "audit", $Mode)
        if ($Profile -ne "standard") {
            $commandParts += $Profile
        }
        $commandParts += "json"
        $responseParts = @(Send-Rcon -Command ($commandParts -join " "))
        $response = if ($responseParts.Count -gt 0) { [string]$responseParts[-1] } else { "" }
    } else {
        if ([string]::IsNullOrWhiteSpace($ResponseFile)) {
            throw "-ResponseFile nu poate fi gol."
        }
        $response = Get-Content -LiteralPath $ResponseFile -Raw -ErrorAction Stop
    }

    $document = ConvertFrom-AinpcAuditResponse -Response $response -ExpectedMode $Mode -ExpectedProfile $Profile
    $auditExitCode = [int]$document.exit_code

    $document | ConvertTo-Json -Depth 20 -Compress
    $processExitCode = $auditExitCode
} catch {
    [Console]::Error.WriteLine("AINPC audit RCON: $($_.Exception.Message)")
    $processExitCode = 3
} finally {
    if ($connected) {
        Disconnect-Rcon 6>$null
    }
}

exit $processExitCode
