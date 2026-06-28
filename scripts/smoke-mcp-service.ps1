param(
    [string]$McpUrl = "http://127.0.0.1:39841",
    [switch]$SkipBuild,
    [switch]$StartService,
    [switch]$StopService,
    [string]$ServiceJar = "",
    [int]$HealthTimeoutSeconds = 10
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$reportPath = Join-Path $repoRoot "build\mcp-smoke-report.json"
$results = @()

function Write-Step {
    param([string]$Label, [bool]$Ok, [string]$Detail)
    $status = if ($Ok) { "OK" } else { "FAIL" }
    Write-Host "[$status] $Label"
    if ($Detail) { Write-Host "       $Detail" }
    $global:results += @{
        step = $Label
        ok = $Ok
        detail = $Detail
        timestamp = (Get-Date -Format "o")
    }
}

function Invoke-McpTool {
    param([string]$ToolName, [string]$Method = "tools/call")
    $payload = @{
        jsonrpc = "2.0"
        id = "smoke-$(Get-Random)"
        method = $Method
        params = @{
            name = $ToolName
            arguments = @{}
        }
    } | ConvertTo-Json -Compress

    try {
        $response = Invoke-RestMethod -Uri "$McpUrl/mcp" -Method Post `
            -Body $payload -ContentType "application/json" `
            -TimeoutSec 5 -ErrorAction Stop
        return $response
    } catch {
        return $null
    }
}

# Step 1: Build service if needed
if (-not $SkipBuild) {
    Write-Step -Label "Build MCP service" -Ok $true -Detail "Running Gradle build..."
    & (Join-Path $repoRoot "gradlew.bat") :ainpc-mcp-service:build 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Step -Label "Build MCP service" -Ok $false -Detail "Gradle build failed"
        exit 1
    }
}

# Step 2: Start service if requested
if ($StartService) {
    Write-Step -Label "Start MCP service" -Ok $true -Detail "Starting sidecar..."
    $jar = if ($ServiceJar) { $ServiceJar } else { Join-Path $repoRoot "ainpc-mcp-service\build\libs\ainpc-mcp-service.jar" }
    $proc = Start-Process -FilePath "java" -ArgumentList "-jar `"$jar`"" -PassThru -NoNewWindow
    Start-Sleep -Seconds 3
}

# Step 3: Health check
try {
    $health = Invoke-RestMethod -Uri "$McpUrl/actuator/health" -TimeoutSec $HealthTimeoutSeconds -ErrorAction Stop
    Write-Step -Label "Health endpoint" -Ok ($health.status -eq "UP") -Detail "status=$($health.status)"
} catch {
    Write-Step -Label "Health endpoint" -Ok $false -Detail "Eroare: $_"
}

# Step 4: Ping tool
$ping = Invoke-McpTool -ToolName "ainpc.ping"
Write-Step -Label "Tool: ainpc.ping" -Ok ($ping -ne $null) -Detail "response=$(if ($ping) { $ping | ConvertTo-Json -Compress } else { 'null' })"

# Step 5: Feature state tool
$feature = Invoke-McpTool -ToolName "ainpc.feature.state"
Write-Step -Label "Tool: ainpc.feature.state" -Ok ($feature -ne $null) -Detail "ok=$($feature -ne $null)"

# Step 6: Server snapshot tool
$snapshot = Invoke-McpTool -ToolName "ainpc.server.snapshot"
Write-Step -Label "Tool: ainpc.server.snapshot" -Ok ($snapshot -ne $null) -Detail "ok=$($snapshot -ne $null)"

# Step 7: NPC list tool
$npcs = Invoke-McpTool -ToolName "ainpc.npc.list"
Write-Step -Label "Tool: ainpc.npc.list" -Ok ($npcs -ne $null) -Detail "ok=$($npcs -ne $null)"

# Step 8: World mapping summary tool
$mapping = Invoke-McpTool -ToolName "ainpc.world.mapping.summary"
Write-Step -Label "Tool: ainpc.world.mapping.summary" -Ok ($mapping -ne $null) -Detail "ok=$($mapping -ne $null)"

# Step 9: Quest summary tool
$quest = Invoke-McpTool -ToolName "ainpc.quest.summary"
Write-Step -Label "Tool: ainpc.quest.summary" -Ok ($quest -ne $null) -Detail "ok=$($quest -ne $null)"

# Step 10: Dialog context tool
$dialog = Invoke-McpTool -ToolName "ainpc.dialog.context"
Write-Step -Label "Tool: ainpc.dialog.context" -Ok ($dialog -ne $null) -Detail "ok=$($dialog -ne $null)"

# Step 11: Debug health tool
$debugHealth = Invoke-McpTool -ToolName "ainpc.debug.health"
Write-Step -Label "Tool: ainpc.debug.health" -Ok ($debugHealth -ne $null) -Detail "ok=$($debugHealth -ne $null)"

# Step 12: Semantic context unified tool
$semantic = Invoke-McpTool -ToolName "ainpc.semantic.context"
Write-Step -Label "Tool: ainpc.semantic.context (unified)" -Ok ($semantic -ne $null) -Detail "ok=$($semantic -ne $null)"

# Generate report
$report = @{
    timestamp = (Get-Date -Format "o")
    service = "ainpc-mcp-service"
    url = $McpUrl
    summary = @{
        total = $results.Count
        passed = ($results | Where-Object { $_.ok }).Count
        failed = ($results | Where-Object { -not $_.ok }).Count
    }
    steps = $results
}

$report | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $reportPath -Encoding UTF8
Write-Host "`nReport saved: $reportPath"
Write-Host "Passed: $($report.summary.passed)/$($report.summary.total) | Failed: $($report.summary.failed)"

if ($report.summary.failed -gt 0) {
    exit 1
}

# Stop service if we started it
if ($StopService -and $proc) {
    $proc.Kill()
    Write-Host "Service stopped."
}
