[CmdletBinding()]
param(
    [string]$InputPath = ".ai",
    [string]$FilePattern = "*mcp-context*summary*.json*",
    [string]$OutFile = "",
    [string]$RowsCsvOutFile = "",
    [string]$ProfilesCsvOutFile = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Parse-JsonEntriesFromFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $raw = Get-Content -LiteralPath $Path -Raw -ErrorAction Stop
    if ([string]::IsNullOrWhiteSpace($raw)) {
        return @()
    }

    $entries = @()
    $trimmed = $raw.Trim()

    if ($trimmed.StartsWith("{") -and $trimmed.EndsWith("}")) {
        try {
            $single = $trimmed | ConvertFrom-Json -ErrorAction Stop
            if ($null -ne $single) {
                $entries += $single
            }
            return $entries
        } catch {
            # continue to line-by-line parse
        }
    }

    $lines = Get-Content -LiteralPath $Path -ErrorAction Stop
    foreach ($line in $lines) {
        if ([string]::IsNullOrWhiteSpace($line)) {
            continue
        }

        $lineTrim = $line.Trim()
        if (-not $lineTrim.StartsWith("{")) {
            continue
        }

        try {
            $item = $lineTrim | ConvertFrom-Json -ErrorAction Stop
            if ($null -ne $item) {
                $entries += $item
            }
        } catch {
            # skip invalid JSON line
        }
    }

    return $entries
}

function Safe-Int {
    param($Value)
    if ($null -eq $Value) { return $null }
    try { return [int]$Value } catch { return $null }
}

function Get-EntryProperty {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Object,
        [Parameter(Mandatory = $true)]
        [string]$Name,
        $Default = $null
    )

    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) {
        return $Default
    }
    return $property.Value
}

function Safe-Bool {
    param($Value, [bool]$Default = $false)
    if ($null -eq $Value) { return $Default }
    try { return [bool]$Value } catch { return $Default }
}

function Get-PercentileValue {
    param(
        [Parameter(Mandatory = $true)]
        [double[]]$Values,
        [Parameter(Mandatory = $true)]
        [double]$Percentile
    )

    if (($Values | Measure-Object).Count -eq 0) {
        return $null
    }

    $sorted = @($Values | Sort-Object)
    if ($sorted.Count -eq 1) {
        return [double]$sorted[0]
    }

    $rank = ($Percentile / 100.0) * ($sorted.Count - 1)
    $lowerIndex = [Math]::Floor($rank)
    $upperIndex = [Math]::Ceiling($rank)
    $weight = $rank - $lowerIndex

    $lower = [double]$sorted[$lowerIndex]
    $upper = [double]$sorted[$upperIndex]
    return $lower + (($upper - $lower) * $weight)
}

function Clamp-Int {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Value,
        [Parameter(Mandatory = $true)]
        [int]$Min,
        [Parameter(Mandatory = $true)]
        [int]$Max
    )

    if ($Value -lt $Min) { return $Min }
    if ($Value -gt $Max) { return $Max }
    return $Value
}

function Get-NullableAverage {
    param(
        [Parameter(Mandatory = $true)]
        [object[]]$Items,
        [Parameter(Mandatory = $true)]
        [string]$PropertyName
    )

    $values = @()
    foreach ($item in $Items) {
        if ($null -eq $item) { continue }
        $property = $item.PSObject.Properties[$PropertyName]
        if ($null -eq $property) { continue }
        $rawValue = $property.Value
        if ($null -eq $rawValue) { continue }
        try {
            $values += [double]$rawValue
        } catch {
            # ignore non-numeric values
        }
    }

    if ($values.Count -eq 0) {
        return $null
    }

    return (($values | Measure-Object -Average).Average)
}

function Round-Nullable {
    param(
        [Parameter(Mandatory = $false)]
        $Value,
        [int]$Digits = 2
    )

    if ($null -eq $Value) { return $null }
    return [Math]::Round([double]$Value, $Digits)
}

$basePath = if ([System.IO.Path]::IsPathRooted($InputPath)) {
    $InputPath
} else {
    Join-Path (Get-Location).Path $InputPath
}

if (-not (Test-Path -LiteralPath $basePath)) {
    throw "InputPath nu exista: $basePath"
}

$files = @(Get-ChildItem -LiteralPath $basePath -Recurse -File | Where-Object { $_.Name -like $FilePattern })
if ((@($files)).Count -eq 0) {
    throw "Nu am gasit fisiere cu pattern '$FilePattern' in $basePath"
}

$rows = @()
foreach ($file in $files) {
    $entries = Parse-JsonEntriesFromFile -Path $file.FullName
    foreach ($entry in $entries) {
        $selectedBudget = Safe-Int $entry.selected_token_budget
        $usedTokens = Safe-Int $entry.context_tokens_used
        $selectedAttempt = Safe-Int $entry.selected_attempt
        $selectedTopK = Safe-Int $entry.selected_top_k
        $outputLength = Safe-Int $entry.output_length
        $selectedUsefulRaw = Get-EntryProperty -Object $entry -Name "selected_useful" -Default $null
        $selectedUseful = if ($null -ne $selectedUsefulRaw) {
            Safe-Bool $selectedUsefulRaw
        } else {
            $null -ne $usedTokens -and $usedTokens -gt 0
        }
        $selectedFailedRaw = Get-EntryProperty -Object $entry -Name "selected_failed" -Default $null
        $selectedFailed = if ($null -ne $selectedFailedRaw) {
            Safe-Bool $selectedFailedRaw
        } else {
            -not $selectedUseful
        }
        $failureReason = [string](Get-EntryProperty -Object $entry -Name "selected_failure_reason" -Default "")
        if ($selectedFailed -and [string]::IsNullOrWhiteSpace($failureReason)) {
            $failureReason = if ($null -eq $usedTokens) { "legacy-missing-token-metadata" } else { "legacy-unusable" }
        }
        $contextTokensPresentRaw = Get-EntryProperty -Object $entry -Name "context_tokens_present" -Default $null
        $contextTokensPresent = if ($null -ne $contextTokensPresentRaw) {
            Safe-Bool $contextTokensPresentRaw
        } else {
            $null -ne $usedTokens
        }

        $rows += [pscustomobject]@{
            file = $file.FullName
            generated_at = $entry.generated_at
            query = $entry.query
            profile = if ([string]::IsNullOrWhiteSpace([string]$entry.profile)) { "unknown" } else { [string]$entry.profile }
            retrieval = if ([string]::IsNullOrWhiteSpace([string]$entry.retrieval)) { "unknown" } else { [string]$entry.retrieval }
            selected_attempt = $selectedAttempt
            selected_token_budget = $selectedBudget
            selected_top_k = $selectedTopK
            context_tokens_used = $usedTokens
            context_token_budget = Safe-Int $entry.context_token_budget
            output_length = $outputLength
            selected_useful = $selectedUseful
            selected_failed = $selectedFailed
            failure_reason = $failureReason
            context_tokens_present = $contextTokensPresent
            token_savings_vs_2200_budget = if ($selectedUseful -and $null -ne $selectedBudget) { 2200 - $selectedBudget } else { $null }
            token_savings_vs_2200_used = if ($selectedUseful -and $null -ne $usedTokens) { 2200 - $usedTokens } else { $null }
            auto_scope = [bool]$entry.auto_scope
        }
    }
}

if ((@($rows)).Count -eq 0) {
    throw "Nu exista intrari JSON valide in fisierele gasite."
}

$totalRuns = (@($rows)).Count
$usableRows = @($rows | Where-Object { $_.selected_useful -eq $true })
$failedRows = @($rows | Where-Object { $_.selected_failed -eq $true })
$usableRuns = (@($usableRows)).Count
$failedRuns = (@($failedRows)).Count
$attempt1 = (@($rows | Where-Object { $_.selected_attempt -eq 1 })).Count
$attempt2 = (@($rows | Where-Object { $_.selected_attempt -eq 2 })).Count
$attempt3 = (@($rows | Where-Object { $_.selected_attempt -eq 3 })).Count
$usableAttempt1 = (@($usableRows | Where-Object { $_.selected_attempt -eq 1 })).Count
$usableAttempt2 = (@($usableRows | Where-Object { $_.selected_attempt -eq 2 })).Count
$usableAttempt3 = (@($usableRows | Where-Object { $_.selected_attempt -eq 3 })).Count

$avgUsed = Round-Nullable (Get-NullableAverage -Items $usableRows -PropertyName "context_tokens_used")
$avgBudget = Round-Nullable (Get-NullableAverage -Items $usableRows -PropertyName "selected_token_budget")
$avgSavingsBudget = Round-Nullable (Get-NullableAverage -Items $usableRows -PropertyName "token_savings_vs_2200_budget")
$avgSavingsUsed = Round-Nullable (Get-NullableAverage -Items $usableRows -PropertyName "token_savings_vs_2200_used")

$profileDefaults = @{
    code = 495
    debug = 352
    planning = 715
    full = 800
}

$byProfile = $rows |
    Group-Object -Property profile |
    Sort-Object -Property Name |
    ForEach-Object {
        $groupRows = $_.Group
        $usableGroupRows = @($groupRows | Where-Object { $_.selected_useful -eq $true })
        $failedGroupRows = @($groupRows | Where-Object { $_.selected_failed -eq $true })
        $outputLengths = @($usableGroupRows | Where-Object { $null -ne $_.output_length } | ForEach-Object { [double]$_.output_length })
        $p50OutputLength = Get-PercentileValue -Values $outputLengths -Percentile 50
        $p75OutputLength = Get-PercentileValue -Values $outputLengths -Percentile 75
        $usableGroupCount = (@($usableGroupRows)).Count
        $attempt1Pct = if ($usableGroupCount -gt 0) {
            [Math]::Round(((@($usableGroupRows | Where-Object { $_.selected_attempt -eq 1 })).Count * 100.0) / $usableGroupCount, 2)
        } else {
            0
        }
        $profileName = $_.Name
        $defaultMinChars = if ($profileDefaults.ContainsKey($profileName)) { [int]$profileDefaults[$profileName] } else { 495 }
        $recommendedMinChars = $defaultMinChars
        $recommendationSource = "profile_default"

        if ($usableGroupCount -ge 5) {
            $adjustmentFactor = if ($attempt1Pct -lt 60) {
                0.85
            } elseif ($attempt1Pct -lt 75) {
                0.95
            } elseif ($attempt1Pct -lt 90) {
                1.05
            } else {
                1.1
            }

            $candidate = [int][Math]::Round($defaultMinChars * $adjustmentFactor)
            if ($null -ne $p50OutputLength) {
                $p50Cap = [int][Math]::Round($p50OutputLength * 0.7)
                if ($candidate -gt $p50Cap) {
                    $candidate = $p50Cap
                }
            }

            $recommendedMinChars = Clamp-Int -Value $candidate -Min 220 -Max 1100
            $recommendationSource = "profile_default_adjusted_by_attempt1_pct"
        }

        [pscustomobject]@{
            profile = $profileName
            runs = (@($groupRows)).Count
            usable_runs = $usableGroupCount
            failed_runs = (@($failedGroupRows)).Count
            attempt1_pct = $attempt1Pct
            avg_budget = Round-Nullable (Get-NullableAverage -Items $usableGroupRows -PropertyName "selected_token_budget")
            avg_used = Round-Nullable (Get-NullableAverage -Items $usableGroupRows -PropertyName "context_tokens_used")
            avg_savings_vs_2200_budget = Round-Nullable (Get-NullableAverage -Items $usableGroupRows -PropertyName "token_savings_vs_2200_budget")
            avg_output_length = Round-Nullable (Get-NullableAverage -Items $usableGroupRows -PropertyName "output_length")
            p50_output_length = if ($null -ne $p50OutputLength) { [Math]::Round($p50OutputLength, 2) } else { $null }
            p75_output_length = if ($null -ne $p75OutputLength) { [Math]::Round($p75OutputLength, 2) } else { $null }
            recommended_min_chars_for_success = $recommendedMinChars
            recommendation_source = $recommendationSource
        }
    }

$recommendationsByProfile = @(
    $byProfile | ForEach-Object {
        [pscustomobject]@{
            profile = $_.profile
            runs = $_.runs
            usable_runs = $_.usable_runs
            failed_runs = $_.failed_runs
            min_chars_for_success_recommended = $_.recommended_min_chars_for_success
            source = $_.recommendation_source
            attempt1_pct = $_.attempt1_pct
        }
    }
)

$failuresByReason = @(
    $failedRows |
        Group-Object -Property failure_reason |
        Sort-Object -Property Count -Descending |
        ForEach-Object {
            [pscustomobject]@{
                reason = if ([string]::IsNullOrWhiteSpace($_.Name)) { "unknown" } else { $_.Name }
                count = $_.Count
            }
        }
)

$summary = [ordered]@{
    generated_at = (Get-Date).ToString("o")
    input_path = $basePath
    file_pattern = $FilePattern
    files_scanned = $files.Count
    runs = $totalRuns
    usable_runs = $usableRuns
    failed_runs = $failedRuns
    usable_run_pct = [Math]::Round(($usableRuns * 100.0) / $totalRuns, 2)
    attempt_distribution = [ordered]@{
        attempt1 = $attempt1
        attempt2 = $attempt2
        attempt3 = $attempt3
        attempt1_pct = [Math]::Round(($attempt1 * 100.0) / $totalRuns, 2)
        usable_attempt1 = $usableAttempt1
        usable_attempt2 = $usableAttempt2
        usable_attempt3 = $usableAttempt3
        usable_attempt1_pct = if ($usableRuns -gt 0) { [Math]::Round(($usableAttempt1 * 100.0) / $usableRuns, 2) } else { 0 }
    }
    averages = [ordered]@{
        selected_budget = $avgBudget
        tokens_used = $avgUsed
        savings_vs_2200_budget = $avgSavingsBudget
        savings_vs_2200_used = $avgSavingsUsed
    }
    by_profile = @($byProfile)
    failures_by_reason = @($failuresByReason)
    recommendations = [ordered]@{
        by_profile = @($recommendationsByProfile)
    }
}

$summaryJson = $summary | ConvertTo-Json -Depth 8

if (-not [string]::IsNullOrWhiteSpace($OutFile)) {
    $targetPath = if ([System.IO.Path]::IsPathRooted($OutFile)) { $OutFile } else { Join-Path (Get-Location).Path $OutFile }
    $targetDir = Split-Path -Parent $targetPath
    if (-not [string]::IsNullOrWhiteSpace($targetDir) -and -not (Test-Path -LiteralPath $targetDir)) {
        New-Item -ItemType Directory -Path $targetDir | Out-Null
    }
    Set-Content -LiteralPath $targetPath -Value $summaryJson -Encoding UTF8
    Write-Host ("[mcp-context-summary-report] Report saved to: {0}" -f $targetPath)
}

if (-not [string]::IsNullOrWhiteSpace($RowsCsvOutFile)) {
    $rowsCsvPath = if ([System.IO.Path]::IsPathRooted($RowsCsvOutFile)) { $RowsCsvOutFile } else { Join-Path (Get-Location).Path $RowsCsvOutFile }
    $rowsCsvDir = Split-Path -Parent $rowsCsvPath
    if (-not [string]::IsNullOrWhiteSpace($rowsCsvDir) -and -not (Test-Path -LiteralPath $rowsCsvDir)) {
        New-Item -ItemType Directory -Path $rowsCsvDir | Out-Null
    }
    $rows | Export-Csv -LiteralPath $rowsCsvPath -NoTypeInformation -Encoding UTF8
    Write-Host ("[mcp-context-summary-report] Rows CSV saved to: {0}" -f $rowsCsvPath)
}

if (-not [string]::IsNullOrWhiteSpace($ProfilesCsvOutFile)) {
    $profilesCsvPath = if ([System.IO.Path]::IsPathRooted($ProfilesCsvOutFile)) { $ProfilesCsvOutFile } else { Join-Path (Get-Location).Path $ProfilesCsvOutFile }
    $profilesCsvDir = Split-Path -Parent $profilesCsvPath
    if (-not [string]::IsNullOrWhiteSpace($profilesCsvDir) -and -not (Test-Path -LiteralPath $profilesCsvDir)) {
        New-Item -ItemType Directory -Path $profilesCsvDir | Out-Null
    }
    $byProfile | Export-Csv -LiteralPath $profilesCsvPath -NoTypeInformation -Encoding UTF8
    Write-Host ("[mcp-context-summary-report] Profiles CSV saved to: {0}" -f $profilesCsvPath)
}

Write-Output $summaryJson
