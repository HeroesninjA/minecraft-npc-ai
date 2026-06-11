[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Query,

    [ValidateSet("code", "debug", "planning", "full")]
    [string]$Profile = "code",

    [ValidateSet("hybrid", "vector", "keyword")]
    [string]$Retrieval = "hybrid",

    [string]$Collection = "ainpc_code",

    [string]$McpRoot = "C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp",

    [int[]]$TokenBudgets = @(900, 1400, 2200),

    [int[]]$TopKValues = @(8, 12, 14),

    [int]$MinCharsForSuccess = 0,

    [switch]$UseAdaptiveThresholds,

    [string]$AdaptiveReportPath = ".ai\mcp-context-report.json",

    [int]$AdaptiveMinRuns = 5,

    [switch]$NoTests,

    [string[]]$Include = @(),

    [string[]]$Exclude = @(),

    [switch]$DisableAutoScope,

    [string]$OutFile = "",

    [switch]$AppendOutFile,

    [string]$SummaryJson = "",

    [switch]$AppendSummaryJson,

    [switch]$VerboseAttempts,

    [switch]$FailOnUnusable
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($TokenBudgets.Count -ne $TopKValues.Count) {
    throw "TokenBudgets si TopKValues trebuie sa aiba acelasi numar de valori."
}

if (-not (Test-Path -LiteralPath $McpRoot)) {
    throw "MCP root nu exista: $McpRoot"
}

$effectiveInclude = @($Include | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
$effectiveExclude = @($Exclude | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })

if (-not $DisableAutoScope) {
    if ($effectiveInclude.Count -eq 0) {
        switch ($Profile) {
            "code" {
                $effectiveInclude = @(
                    "ainpc-core-plugin/src/main/",
                    "ainpc-api/src/main/",
                    "ainpc-scenario-medieval/src/main/",
                    "scripts/"
                )
            }
            "debug" {
                $effectiveInclude = @(
                    "ainpc-core-plugin/src/main/",
                    "ainpc-api/src/main/",
                    "ainpc-scenario-medieval/src/main/",
                    "scripts/",
                    "docs/"
                )
            }
            "planning" {
                $effectiveInclude = @("docs/")
            }
            "full" {
                $effectiveInclude = @(
                    "ainpc-core-plugin/src/main/",
                    "ainpc-api/src/main/",
                    "ainpc-scenario-medieval/src/main/",
                    "scripts/",
                    "docs/"
                )
            }
        }
    }

    if ($effectiveExclude.Count -eq 0) {
        $effectiveExclude = @(
            "ainpc-core-plugin/src/test/",
            "ainpc-api/src/test/",
            "ainpc-scenario-medieval/src/test/",
            "target/",
            "build/",
            ".gradle/",
            ".kotlin/",
            ".idea/",
            "node_modules/"
        )
    }
}

$profileMinChars = @{
    code = 495
    debug = 352
    planning = 715
    full = 800
}

$minCharsSource = "manual"

function Resolve-AdaptedMinChars {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ReportPath,
        [Parameter(Mandatory = $true)]
        [string]$ProfileName,
        [Parameter(Mandatory = $true)]
        [int]$RequiredRuns
    )

    if (-not (Test-Path -LiteralPath $ReportPath)) {
        return $null
    }

    try {
        $raw = Get-Content -LiteralPath $ReportPath -Raw -ErrorAction Stop
        if ([string]::IsNullOrWhiteSpace($raw)) {
            return $null
        }

        $report = $raw | ConvertFrom-Json -ErrorAction Stop
    } catch {
        return $null
    }

    $profileStats = @($report.by_profile | Where-Object { $_.profile -eq $ProfileName })
    if ($profileStats.Count -eq 0) {
        return $null
    }

    $runs = 0
    try { $runs = [int]$profileStats[0].runs } catch { $runs = 0 }
    if ($runs -lt $RequiredRuns) {
        return $null
    }

    $recommendation = @($report.recommendations.by_profile | Where-Object { $_.profile -eq $ProfileName })
    if ($recommendation.Count -gt 0) {
        try {
            $value = [int]$recommendation[0].min_chars_for_success_recommended
            if ($value -gt 0) {
                return $value
            }
        } catch {
            # fallback below
        }
    }

    try {
        $fallback = [int]$profileStats[0].recommended_min_chars_for_success
        if ($fallback -gt 0) {
            return $fallback
        }
    } catch {
        return $null
    }

    return $null
}

if ($MinCharsForSuccess -le 0) {
    $minCharsSource = "profile-default"
    $MinCharsForSuccess = $profileMinChars[$Profile]

    if ($UseAdaptiveThresholds) {
        $resolvedReportPath = if ([System.IO.Path]::IsPathRooted($AdaptiveReportPath)) {
            $AdaptiveReportPath
        } else {
            Join-Path (Get-Location).Path $AdaptiveReportPath
        }

        $adapted = Resolve-AdaptedMinChars -ReportPath $resolvedReportPath -ProfileName $Profile -RequiredRuns $AdaptiveMinRuns
        if ($null -ne $adapted -and $adapted -gt 0) {
            $MinCharsForSuccess = $adapted
            $minCharsSource = "adaptive-report"
        }
    }
} else {
    $minCharsSource = "manual"
}

$codexContextScript = Join-Path $McpRoot "scripts\codex-context.ps1"
$hasCodexContextScript = Test-Path -LiteralPath $codexContextScript

if (-not $hasCodexContextScript) {
    Write-Warning "Nu am gasit $codexContextScript. Folosesc fallback npm run mcp:context."
}

function Get-ContextOutputAssessment {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Text,
        [Parameter(Mandatory = $true)]
        [int]$MinChars
    )

    if ([string]::IsNullOrWhiteSpace($Text)) {
        return [pscustomobject]@{
            Useful = $false
            FailureReason = "empty-output"
            HasTokenMetadata = $false
            TokenUsed = $null
            TokenBudget = $null
            Signature = ""
        }
    }

    $normalized = $Text.Trim()
    $tokenMatch = [regex]::Match($normalized, "tokens=(\d+)/(\d+)")
    $hasTokenMetadata = $tokenMatch.Success
    $tokenUsed = if ($hasTokenMetadata) { [int]$tokenMatch.Groups[1].Value } else { $null }
    $tokenBudget = if ($hasTokenMetadata) { [int]$tokenMatch.Groups[2].Value } else { $null }
    $signature = [System.Convert]::ToBase64String(
        [System.Security.Cryptography.SHA256]::Create().ComputeHash(
            [System.Text.Encoding]::UTF8.GetBytes(($normalized -replace "\s+", " ").Trim())
        )
    )

    if ($normalized.Length -lt $MinChars) {
        return [pscustomobject]@{
            Useful = $false
            FailureReason = "below-min-chars"
            HasTokenMetadata = $hasTokenMetadata
            TokenUsed = $tokenUsed
            TokenBudget = $tokenBudget
            Signature = $signature
        }
    }

    if (-not $hasTokenMetadata) {
        return [pscustomobject]@{
            Useful = $false
            FailureReason = "missing-token-metadata"
            HasTokenMetadata = $false
            TokenUsed = $null
            TokenBudget = $null
            Signature = $signature
        }
    }

    $failurePatterns = @(
        "no hits",
        "0 hits",
        "hits: []",
        "`"hits`": []",
        "no relevant",
        "token_used_estimate`": 0",
        "tokens=0/"
    )

    $lower = $normalized.ToLowerInvariant()
    foreach ($pattern in $failurePatterns) {
        if ($lower.Contains($pattern.ToLowerInvariant())) {
            return [pscustomobject]@{
                Useful = $false
                FailureReason = "failure-pattern:$pattern"
                HasTokenMetadata = $hasTokenMetadata
                TokenUsed = $tokenUsed
                TokenBudget = $tokenBudget
                Signature = $signature
            }
        }
    }

    if ($lower.Contains("code:`r`n`r`nrules:`r`n`r`nchangelog:`r`n`r`nmemory:") -or
        $lower.Contains("code:`n`nrules:`n`nchangelog:`n`nmemory:")) {
        return [pscustomobject]@{
            Useful = $false
            FailureReason = "empty-context-pack"
            HasTokenMetadata = $hasTokenMetadata
            TokenUsed = $tokenUsed
            TokenBudget = $tokenBudget
            Signature = $signature
        }
    }

    return [pscustomobject]@{
        Useful = $true
        FailureReason = ""
        HasTokenMetadata = $hasTokenMetadata
        TokenUsed = $tokenUsed
        TokenBudget = $tokenBudget
        Signature = $signature
    }
}

$attempts = @()
$selected = $null
$retryStoppedReason = ""

for ($i = 0; $i -lt $TokenBudgets.Count; $i++) {
    $budget = $TokenBudgets[$i]
    $topK = $TopKValues[$i]

    Write-Host ("[mcp-context-staged] Attempt {0}/{1}: TokenBudget={2}, TopK={3}, Profile={4}, Retrieval={5}" -f ($i + 1), $TokenBudgets.Count, $budget, $topK, $Profile, $Retrieval)

    $outputText = ""
    try {
        if ($hasCodexContextScript) {
            $invokeParams = @{
                Query = $Query
                Profile = $Profile
                TokenBudget = $budget
                TopK = $topK
                Retrieval = $Retrieval
                Collection = $Collection
            }

            if ($NoTests) {
                $invokeParams["NoTests"] = $true
            }

            if ($effectiveInclude.Count -gt 0) {
                $invokeParams["Include"] = ($effectiveInclude -join ",")
            }

            if ($effectiveExclude.Count -gt 0) {
                $invokeParams["Exclude"] = ($effectiveExclude -join ",")
            }

            $outputText = (& $codexContextScript @invokeParams 2>&1 | Out-String)
        } else {
            Push-Location $McpRoot
            try {
                $includeArg = if ($effectiveInclude.Count -gt 0) { $effectiveInclude -join "," } else { "" }
                $excludeArg = if ($effectiveExclude.Count -gt 0) { $effectiveExclude -join "," } else { "" }
                $includeTestsArg = if ($NoTests) { "false" } else { "true" }
                $outputText = (& npm run mcp:context -- $Query $budget $topK $Collection prompt $Profile $includeArg $excludeArg $includeTestsArg $Retrieval 2>&1 | Out-String)
            } finally {
                Pop-Location
            }
        }
    } catch {
        $outputText = ($_ | Out-String)
    }

    $assessment = Get-ContextOutputAssessment -Text $outputText -MinChars $MinCharsForSuccess
    $candidate = [pscustomobject]@{
        Attempt = $i + 1
        TokenBudget = $budget
        TopK = $topK
        Output = $outputText
        Length = if ($null -eq $outputText) { 0 } else { $outputText.Length }
        Useful = [bool]$assessment.Useful
        FailureReason = [string]$assessment.FailureReason
        HasTokenMetadata = [bool]$assessment.HasTokenMetadata
        TokenUsed = $assessment.TokenUsed
        ContextTokenBudget = $assessment.TokenBudget
        Signature = [string]$assessment.Signature
    }

    $attempts += $candidate

    if ($VerboseAttempts) {
        Write-Host ("[mcp-context-staged] Attempt {0} output length: {1}" -f $candidate.Attempt, $candidate.Length)
    }

    if ($candidate.Useful) {
        $selected = $candidate
        break
    }

    if ($attempts.Count -ge 2) {
        $previous = $attempts[-2]
        if (-not $previous.Useful -and $previous.Signature -eq $candidate.Signature) {
            $retryStoppedReason = "repeated-unusable-output:$($candidate.FailureReason)"
            Write-Warning "[mcp-context-staged] Oprire retry: output inutil identic intre attempturi."
            break
        }
    }
}

if ($null -eq $selected -and $attempts.Count -gt 0) {
    $selected = $attempts[-1]
}

if ($null -eq $selected) {
    throw "Nu am putut obtine context MCP."
}

Write-Host ("[mcp-context-staged] Selected attempt: {0} (TokenBudget={1}, TopK={2}, OutputLength={3})" -f $selected.Attempt, $selected.TokenBudget, $selected.TopK, $selected.Length)
if ($VerboseAttempts) {
    Write-Host ("[mcp-context-staged] Effective include: {0}" -f ($(if ($effectiveInclude.Count -gt 0) { $effectiveInclude -join ", " } else { "<none>" })))
    Write-Host ("[mcp-context-staged] Effective exclude: {0}" -f ($(if ($effectiveExclude.Count -gt 0) { $effectiveExclude -join ", " } else { "<none>" })))
    Write-Host ("[mcp-context-staged] MinCharsForSuccess: {0} (source: {1})" -f $MinCharsForSuccess, $minCharsSource)
}

$tokenUsed = $selected.TokenUsed
$contextTokenBudget = $selected.ContextTokenBudget

$summaryObject = [ordered]@{
    generated_at = (Get-Date).ToString("o")
    query = $Query
    profile = $Profile
    retrieval = $Retrieval
    collection = $Collection
    auto_scope = (-not $DisableAutoScope)
    min_chars_for_success = $MinCharsForSuccess
    min_chars_source = $minCharsSource
    adaptive_thresholds_enabled = [bool]$UseAdaptiveThresholds
    adaptive_report_path = if ($UseAdaptiveThresholds) { $AdaptiveReportPath } else { "" }
    selected_attempt = $selected.Attempt
    selected_token_budget = $selected.TokenBudget
    selected_top_k = $selected.TopK
    output_length = $selected.Length
    selected_useful = [bool]$selected.Useful
    selected_failed = -not [bool]$selected.Useful
    selected_failure_reason = [string]$selected.FailureReason
    context_tokens_present = [bool]$selected.HasTokenMetadata
    context_tokens_used = $tokenUsed
    context_token_budget = $contextTokenBudget
    retry_stopped_reason = $retryStoppedReason
    include = $effectiveInclude
    exclude = $effectiveExclude
    attempts = @(
        $attempts | ForEach-Object {
            [ordered]@{
                attempt = $_.Attempt
                token_budget = $_.TokenBudget
                top_k = $_.TopK
                output_length = $_.Length
                useful = $_.Useful
                failure_reason = $_.FailureReason
                context_tokens_present = $_.HasTokenMetadata
                context_tokens_used = $_.TokenUsed
                context_token_budget = $_.ContextTokenBudget
            }
        }
    )
}

if (-not [string]::IsNullOrWhiteSpace($OutFile)) {
    $targetPath = if ([System.IO.Path]::IsPathRooted($OutFile)) { $OutFile } else { Join-Path (Get-Location).Path $OutFile }
    $targetDir = Split-Path -Parent $targetPath
    if (-not [string]::IsNullOrWhiteSpace($targetDir) -and -not (Test-Path -LiteralPath $targetDir)) {
        New-Item -ItemType Directory -Path $targetDir | Out-Null
    }

    if ($AppendOutFile) {
        Add-Content -LiteralPath $targetPath -Value $selected.Output -Encoding UTF8
    } else {
        Set-Content -LiteralPath $targetPath -Value $selected.Output -Encoding UTF8
    }

    Write-Host ("[mcp-context-staged] Output saved to: {0}" -f $targetPath)
}

if (-not [string]::IsNullOrWhiteSpace($SummaryJson)) {
    $summaryPath = if ([System.IO.Path]::IsPathRooted($SummaryJson)) { $SummaryJson } else { Join-Path (Get-Location).Path $SummaryJson }
    $summaryDir = Split-Path -Parent $summaryPath
    if (-not [string]::IsNullOrWhiteSpace($summaryDir) -and -not (Test-Path -LiteralPath $summaryDir)) {
        New-Item -ItemType Directory -Path $summaryDir | Out-Null
    }

    if ($AppendSummaryJson) {
        $summaryLine = ($summaryObject | ConvertTo-Json -Depth 8 -Compress)
        Add-Content -LiteralPath $summaryPath -Value $summaryLine -Encoding UTF8
    } else {
        $summaryPretty = ($summaryObject | ConvertTo-Json -Depth 8)
        Set-Content -LiteralPath $summaryPath -Value $summaryPretty -Encoding UTF8
    }

    Write-Host ("[mcp-context-staged] Summary saved to: {0}" -f $summaryPath)
}

Write-Output $selected.Output

if ($FailOnUnusable -and -not $selected.Useful) {
    throw ("Context MCP inutil pentru query '{0}': {1}. Attempt={2}, OutputLength={3}" -f $Query, $selected.FailureReason, $selected.Attempt, $selected.Length)
}
