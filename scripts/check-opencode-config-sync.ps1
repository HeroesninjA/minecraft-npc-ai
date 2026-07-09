param(
    [string]$ProjectRoot = "."
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$jsonPath = Join-Path $repoRoot "opencode.json"
$jsoncPath = Join-Path $repoRoot "opencode.jsonc"
$outputDir = Join-Path $repoRoot "build\opencode-config-sync"
if (-not (Test-Path -LiteralPath $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

function Remove-JsoncComments {
    param([string]$Text)

    $builder = [System.Text.StringBuilder]::new()
    $inString = $false
    $stringQuote = [char]0
    $inLineComment = $false
    $inBlockComment = $false
    $escape = $false

    for ($index = 0; $index -lt $Text.Length; $index += 1) {
        $current = $Text[$index]
        $next = if ($index + 1 -lt $Text.Length) { $Text[$index + 1] } else { [char]0 }

        if ($inLineComment) {
            if ($current -eq "`n") {
                $inLineComment = $false
                [void]$builder.Append($current)
            }
            continue
        }

        if ($inBlockComment) {
            if ($current -eq "*" -and $next -eq "/") {
                $inBlockComment = $false
                $index += 1
            }
            continue
        }

        if ($inString) {
            [void]$builder.Append($current)
            if ($escape) {
                $escape = $false
                continue
            }
            if ($current -eq "\") {
                $escape = $true
                continue
            }
            if ($current -eq $stringQuote) {
                $inString = $false
            }
            continue
        }

        if ($current -eq "/" -and $next -eq "/") {
            $inLineComment = $true
            $index += 1
            continue
        }

        if ($current -eq "/" -and $next -eq "*") {
            $inBlockComment = $true
            $index += 1
            continue
        }

        [void]$builder.Append($current)
        if ($current -eq '"' -or $current -eq "'") {
            $inString = $true
            $stringQuote = $current
        }
    }

    return $builder.ToString()
}

function Remove-TrailingCommas {
    param([string]$Text)

    return [regex]::Replace($Text, ',(?=\s*[}\]])', '')
}

function ConvertFrom-Jsonc {
    param([string]$Path)

    $text = Get-Content -LiteralPath $Path -Raw
    $clean = Remove-JsoncComments -Text $text
    $clean = Remove-TrailingCommas -Text $clean
    return $clean | ConvertFrom-Json
}

function Normalize-Value {
    param($Value)

    if ($null -eq $Value) { return $null }
    if ($Value -is [System.Collections.IDictionary]) {
        $ordered = [ordered]@{}
        foreach ($key in ($Value.Keys | Sort-Object)) {
            $ordered[$key] = Normalize-Value $Value[$key]
        }
        return $ordered
    }

    if ($Value -is [System.Collections.IEnumerable] -and $Value -isnot [string]) {
        $items = @()
        foreach ($item in $Value) {
            $items += ,(Normalize-Value $item)
        }
        return $items
    }

    if ($Value -is [pscustomobject]) {
        $ordered = [ordered]@{}
        foreach ($prop in ($Value.PSObject.Properties | Sort-Object Name)) {
            $ordered[$prop.Name] = Normalize-Value $prop.Value
        }
        return $ordered
    }

    return $Value
}

function Convert-ToCanonicalJson {
    param($Value)

    return (Normalize-Value $Value | ConvertTo-Json -Depth 64 -Compress)
}

$report = [ordered]@{
    generated_at = (Get-Date).ToString("o")
    project_root = $repoRoot
    json = @{
        path = $jsonPath
        exists = Test-Path -LiteralPath $jsonPath -PathType Leaf
    }
    jsonc = @{
        path = $jsoncPath
        exists = Test-Path -LiteralPath $jsoncPath -PathType Leaf
    }
    equal = $false
    mismatches = @()
}

if (-not $report.json.exists) { $report.mismatches += "Lipseste `opencode.json`." }
if (-not $report.jsonc.exists) { $report.mismatches += "Lipseste `opencode.jsonc`." }

if ($report.json.exists -and $report.jsonc.exists) {
    try {
        $jsonData = Get-Content -LiteralPath $jsonPath -Raw | ConvertFrom-Json
        $jsoncData = ConvertFrom-Jsonc -Path $jsoncPath
        $jsonCanonical = Convert-ToCanonicalJson $jsonData
        $jsoncCanonical = Convert-ToCanonicalJson $jsoncData

        if ($jsonCanonical -ne $jsoncCanonical) {
            $report.mismatches += "Configurile nu sunt identice dupa normalizare."
        } else {
            $report.equal = $true
        }
    } catch {
        $report.mismatches += "Eroare la parsare/normalizare: $($_.Exception.Message)"
    }
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$jsonReportPath = Join-Path $outputDir "opencode-config-sync-$timestamp.json"
$report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonReportPath -Encoding UTF8

if ($report.equal) {
    Write-Host "OpenCode config sync: OK"
} else {
    Write-Host "OpenCode config sync: DRIFT"
    foreach ($message in $report.mismatches) {
        Write-Host "[DRIFT] $message"
    }
}
Write-Host "Raport: $jsonReportPath"

return $report
