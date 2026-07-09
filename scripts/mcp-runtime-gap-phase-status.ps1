param(
    [string]$ProjectRoot = ".",
    [int[]]$BlockedPhaseIds = @()
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$outputDir = Join-Path $repoRoot ".ai"
if (-not (Test-Path -LiteralPath $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

$phases = @(
    @{
        id = 1
        name = "Bridge runtime read-only"
        files = @(
            "ainpc-core-plugin/src/main/kotlin/ro/ainpc/mcp/bridge/RuntimeSnapshot.kt",
            "ainpc-core-plugin/src/main/kotlin/ro/ainpc/mcp/bridge/RuntimeSnapshotProducer.kt",
            "ainpc-mcp-service/src/main/java/ro/ainpc/mcp/bridge/RuntimeSnapshot.java",
            "ainpc-mcp-service/src/main/java/ro/ainpc/mcp/bridge/SnapshotReader.java"
        )
    },
    @{
        id = 2
        name = "Tool-uri reale read-only"
        files = @(
            "ainpc-mcp-service/src/main/java/ro/ainpc/mcp/tools/AinpcServerSnapshotTools.java",
            "ainpc-mcp-service/src/main/java/ro/ainpc/mcp/tools/AinpcNpcTools.java",
            "ainpc-mcp-service/src/main/java/ro/ainpc/mcp/tools/AinpcWorldMappingTools.java",
            "ainpc-mcp-service/src/main/java/ro/ainpc/mcp/tools/AinpcQuestSnapshotTools.java"
        )
    },
    @{
        id = 3
        name = "Redactare si audit"
        files = @(
            "ainpc-mcp-service/src/main/java/ro/ainpc/mcp/bridge/RedactingSnapshotFilter.java",
            "ainpc-mcp-service/src/main/java/ro/ainpc/mcp/bridge/McpAuditLogger.java",
            "ainpc-mcp-service/src/main/java/ro/ainpc/mcp/bridge/McpSnapshotService.java"
        )
    },
    @{
        id = 4
        name = "Health real"
        files = @(
            "ainpc-mcp-service/src/main/java/ro/ainpc/mcp/bridge/McpRuntimeBridgeHealthIndicator.java"
        )
    },
    @{
        id = 5
        name = "Consolidare si integrare"
        files = @(
            "ainpc-mcp-service/src/main/java/ro/ainpc/mcp/tools/AinpcSemanticContextTools.java",
            "ainpc-core-plugin/src/main/kotlin/ro/ainpc/mcp/bridge/McpDialogContextProvider.kt"
        )
    },
    @{
        id = 6
        name = "Profiles si smoke"
        files = @(
            "ainpc-mcp-service/src/main/resources/application-local-bridge.yml",
            "ainpc-mcp-service/src/main/resources/application-offline.yml",
            "ainpc-mcp-service/src/main/resources/application-static.yml",
            "scripts/smoke-mcp-service.ps1"
        )
    },
    @{
        id = 7
        name = "Teste"
        files = @(
            "ainpc-mcp-service/src/test/java/ro/ainpc/mcp/bridge/SnapshotReaderTest.java",
            "ainpc-mcp-service/src/test/java/ro/ainpc/mcp/bridge/RedactingSnapshotFilterTest.java",
            "ainpc-mcp-service/src/test/java/ro/ainpc/mcp/bridge/McpRuntimeBridgeHealthIndicatorTest.java",
            "ainpc-mcp-service/src/test/java/ro/ainpc/mcp/tools/AinpcServerSnapshotToolsTest.java"
        )
    }
)

$reportPhases = @()
foreach ($phase in $phases) {
    $existingFiles = @()
    $missingFiles = @()
    foreach ($file in $phase.files) {
        $absolutePath = Join-Path $repoRoot $file
        if (Test-Path -LiteralPath $absolutePath -PathType Leaf) {
            $existingFiles += $file
        } else {
            $missingFiles += $file
        }
    }

    $status =
        if ($BlockedPhaseIds -contains [int]$phase.id) { "blocked" }
        elseif ($missingFiles.Count -eq 0) { "done" }
        else { "doing" }

    $reportPhases += [ordered]@{
        id = $phase.id
        name = $phase.name
        status = $status
        files_total = $phase.files.Count
        files_existing = $existingFiles.Count
        files_missing = $missingFiles.Count
        missing_files = $missingFiles
        files = $phase.files
    }
}

$doneCount = ($reportPhases | Where-Object { $_.status -eq "done" } | Measure-Object).Count
$doingCount = ($reportPhases | Where-Object { $_.status -eq "doing" } | Measure-Object).Count
$blockedCount = ($reportPhases | Where-Object { $_.status -eq "blocked" } | Measure-Object).Count
$overallStatus =
    if ($blockedCount -gt 0) { "blocked" }
    elseif ($doingCount -gt 0) { "doing" }
    else { "done" }

$report = [ordered]@{
    generated_at = (Get-Date).ToString("o")
    project_root = $repoRoot
    overall_status = $overallStatus
    done_count = $doneCount
    doing_count = $doingCount
    blocked_count = $blockedCount
    phases = $reportPhases
}

$jsonPath = Join-Path $outputDir "mcp-runtime-gap-phase-status.json"
$txtPath = Join-Path $outputDir "mcp-runtime-gap-phase-status.txt"

$report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonPath -Encoding UTF8

$lines = @()
$lines += "MCP runtime gap phase status"
$lines += "Generated: $($report.generated_at)"
$lines += "Overall: $overallStatus"
$lines += ""
foreach ($phase in $reportPhases) {
    $lines += ("[{0}] {1}: {2} ({3}/{4} files)" -f $phase.id, $phase.name, $phase.status, $phase.files_existing, $phase.files_total)
}
$lines | Set-Content -LiteralPath $txtPath -Encoding UTF8

Write-Host "MCP runtime gap phase status: OK"
Write-Host "JSON: $jsonPath"
Write-Host "TXT:  $txtPath"

return $report
