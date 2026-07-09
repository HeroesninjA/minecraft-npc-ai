param(
    [string]$ProjectRoot = "."
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = Resolve-Path -LiteralPath $ProjectRoot
$repoRoot = $repoRoot.Path

$opencodePath = Join-Path $repoRoot "opencode.json"
$codexPath = Join-Path $repoRoot ".codex\config.toml"
$outputDir = Join-Path $repoRoot "build\opencode-config-drift"
if (-not (Test-Path -LiteralPath $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

function Read-CodexMcpUrls {
    param([string]$Path)

    $urls = @{}
    $currentSection = ""
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ($trimmed -match '^\[mcp_servers\.(?<name>[^\]]+)\]$') {
            $currentSection = $Matches.name
            continue
        }

        if ($trimmed -match '^url\s*=\s*"(?<url>[^"]+)"$' -and $currentSection) {
            $urls[$currentSection] = $Matches.url
        }
    }

    return $urls
}

function Read-OpenCodeMcpUrls {
    param([string]$Path)

    $json = Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
    $urls = @{}
    foreach ($property in $json.mcp.PSObject.Properties) {
        $urls[$property.Name] = $property.Value.url
    }
    return $urls
}

$report = [ordered]@{
    generated_at = (Get-Date).ToString("o")
    project_root = $repoRoot
    opencode = @{
        path = $opencodePath
        exists = Test-Path -LiteralPath $opencodePath -PathType Leaf
    }
    codex = @{
        path = $codexPath
        exists = Test-Path -LiteralPath $codexPath -PathType Leaf
    }
    expected_servers = @("ainpc-project-memory", "serena", "context7")
    mismatches = @()
    missing = @()
}

if (-not $report.opencode.exists) {
    $report.mismatches += "Lipseste config-ul OpenCode: $opencodePath"
}
if (-not $report.codex.exists) {
    $report.mismatches += "Lipseste config-ul Codex: $codexPath"
}

if ($report.opencode.exists -and $report.codex.exists) {
    $openCodeUrls = Read-OpenCodeMcpUrls -Path $opencodePath
    $codexUrls = Read-CodexMcpUrls -Path $codexPath

    foreach ($server in $report.expected_servers) {
        if (-not $openCodeUrls.ContainsKey($server)) {
            $report.missing += "OpenCode lipseste serverul '$server'"
            continue
        }
        if (-not $codexUrls.ContainsKey($server)) {
            $report.missing += "Codex lipseste serverul '$server'"
            continue
        }

        if ($openCodeUrls[$server] -ne $codexUrls[$server]) {
            $report.mismatches += "URL diferit pentru '$server': OpenCode='$($openCodeUrls[$server])' vs Codex='$($codexUrls[$server])'"
        }
    }
}

$report.ok = ($report.mismatches.Count -eq 0 -and $report.missing.Count -eq 0)
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$jsonPath = Join-Path $outputDir "opencode-config-drift-$timestamp.json"
$report | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $jsonPath -Encoding UTF8

if ($report.ok) {
    Write-Host "OpenCode/Codex config drift: OK"
} else {
    Write-Host "OpenCode/Codex config drift: DRIFT"
    foreach ($item in $report.missing) { Write-Host "[MISSING] $item" }
    foreach ($item in $report.mismatches) { Write-Host "[MISMATCH] $item" }
}
Write-Host "Raport: $jsonPath"

return $report
