param(
  [string]$EvidenceFile = ".ai\codex-250-demo-paper-evidence-template.md",
  [switch]$AllowPending,
  [switch]$FailOnWarnings,
  [string]$JsonOutFile = ""
)

$ErrorActionPreference = "Stop"

function Add-Issue {
  param(
    [System.Collections.Generic.List[string]]$List,
    [string]$Message
  )
  if (-not [string]::IsNullOrWhiteSpace($Message)) {
    [void]$List.Add($Message)
  }
}

$errors = [System.Collections.Generic.List[string]]::new()
$warnings = [System.Collections.Generic.List[string]]::new()

if (-not (Test-Path -LiteralPath $EvidenceFile -PathType Leaf)) {
  Add-Issue $errors "Evidence file not found: $EvidenceFile"
} else {
  $text = Get-Content -LiteralPath $EvidenceFile -Raw

  $requiredTokens = @(
    "PAPER_EVIDENCE_TEMPLATE=true",
    "DO_NOT_STORE_SECRETS=true",
    ".\gradlew.bat test assemble",
    "/ainpc demo status demo_sat",
    "/ainpc audit all",
    "/ainpc debugdump all",
    "Debugdump before path",
    "Debugdump after path",
    "Release decision",
    "No secrets stored in this file",
    "LIVE gates have command output",
    "Restart used controlled stop"
  )

  foreach ($token in $requiredTokens) {
    if (-not $text.Contains($token)) {
      Add-Issue $errors "Missing required token: $token"
    }
  }

  if (-not $AllowPending -and $text.Contains("PENDING")) {
    Add-Issue $errors "Evidence still contains PENDING markers."
  } elseif ($AllowPending -and $text.Contains("PENDING")) {
    Add-Issue $warnings "Evidence contains PENDING markers, allowed by -AllowPending."
  }

  $secretPatterns = @(
    "sk-[A-Za-z0-9_-]{20,}",
    "(?i)authorization\s*:\s*bearer\s+\S+",
    "(?i)\b(api[_-]?key|token|password|secret)\s*[:=]\s*['""]?[^'""\s]+"
  )

  foreach ($pattern in $secretPatterns) {
    if ([regex]::IsMatch($text, $pattern)) {
      Add-Issue $errors "Potential secret found by pattern: $pattern"
    }
  }

  $decisionMatch = [regex]::Match($text, "(?m)^Decision:\s*(\S+)")
  if ($decisionMatch.Success) {
    $decision = $decisionMatch.Groups[1].Value.Trim()
    if ($decision -eq "PENDING") {
      if (-not $AllowPending) {
        Add-Issue $errors "Release decision is still PENDING."
      }
    } elseif ($decision -notin @("REMOVE", "KEEP_INTERNAL", "PROMOTE")) {
      Add-Issue $errors "Release decision must be REMOVE, KEEP_INTERNAL, or PROMOTE. Found: $decision"
    }
  } else {
    Add-Issue $errors "Missing release decision line."
  }
}

$status = if ($errors.Count -eq 0 -and (-not $FailOnWarnings -or $warnings.Count -eq 0)) { "passed" } else { "failed" }
$result = [ordered]@{
  status = $status
  evidence_file = $EvidenceFile
  allow_pending = [bool]$AllowPending
  fail_on_warnings = [bool]$FailOnWarnings
  errors = @($errors)
  warnings = @($warnings)
}

$json = $result | ConvertTo-Json -Depth 4
if (-not [string]::IsNullOrWhiteSpace($JsonOutFile)) {
  $parent = Split-Path -Parent $JsonOutFile
  if (-not [string]::IsNullOrWhiteSpace($parent)) {
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
  }
  Set-Content -LiteralPath $JsonOutFile -Value $json -Encoding UTF8
}

$json

if ($status -ne "passed") {
  exit 1
}
