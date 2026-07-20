param(
    [string]$McpUrl = "http://127.0.0.1:39841",
    [switch]$SkipBuild,
    [switch]$StartService,
    [switch]$StopService,
    [string]$ServiceJar = "",
    [int]$HealthTimeoutSeconds = 10,
    [switch]$AllowStaleSnapshot,
    [string]$ServiceProfile = "local-bridge",
    [string]$SnapshotPath = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$reportPath = Join-Path $repoRoot "build\mcp-smoke-report.json"
$serviceLogPath = Join-Path $repoRoot "build\mcp-smoke-service.log"
$serviceErrorLogPath = Join-Path $repoRoot "build\mcp-smoke-service.err.log"
$script:results = @()
$proc = $null
$script:mcpSessionId = $null
$previousSpringProfilesActive = $env:SPRING_PROFILES_ACTIVE
$previousMcpMode = $env:MCP_MODE
$previousMcpSnapshotPath = $env:MCP_SNAPSHOT_PATH

function Write-Step {
    param([string]$Label, [bool]$Ok, [string]$Detail)
    $status = if ($Ok) { "OK" } else { "FAIL" }
    Write-Host "[$status] $Label"
    if ($Detail) { Write-Host "       $Detail" }
    $script:results += @{
        step = $Label
        ok = $Ok
        detail = $Detail
        timestamp = (Get-Date -Format "o")
    }
}

function Invoke-McpTool {
    param([string]$ToolName, [string]$Method = "tools/call")
    if (-not $script:mcpSessionId) {
        $script:mcpSessionId = Initialize-McpSession
        if (-not $script:mcpSessionId) {
            return $null
        }
    }
    $payload = @{
        jsonrpc = "2.0"
        id = "smoke-$(Get-Random)"
        method = $Method
        params = @{
            name = $ToolName
            arguments = @{}
        }
    } | ConvertTo-Json -Compress

    $response = Invoke-McpRequest -Payload $payload -SessionId $script:mcpSessionId
    if ($null -eq $response -or $response.StatusCode -lt 200 -or $response.StatusCode -gt 299) {
        return $null
    }
    return $response
}

function Invoke-McpRequest {
    param([string]$Payload, [string]$SessionId = "")
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($Payload)
    $request = [System.Net.HttpWebRequest]::Create("$McpUrl/mcp")
    $request.Method = "POST"
    $request.ContentType = "application/json"
    $request.Accept = "application/json, text/event-stream"
    $request.Timeout = 5000
    $request.ContentLength = $bytes.Length
    try {
        if ($SessionId) {
            $request.Headers["Mcp-Session-Id"] = $SessionId
        }
        $stream = $request.GetRequestStream()
        $stream.Write($bytes, 0, $bytes.Length)
        $stream.Close()
        $response = $request.GetResponse()
        $reader = New-Object System.IO.StreamReader($response.GetResponseStream())
        $content = $reader.ReadToEnd()
        $reader.Close()
        $statusCode = [int]$response.StatusCode
        $headers = $response.Headers
        $response.Close()
        return [pscustomobject]@{
            StatusCode = $statusCode
            Headers = $headers
            Content = $content
        }
    } catch [System.Net.WebException] {
        if ($_.Exception.Response) {
            $response = $_.Exception.Response
            $reader = New-Object System.IO.StreamReader($response.GetResponseStream())
            $content = $reader.ReadToEnd()
            $reader.Close()
            $statusCode = [int]$response.StatusCode
            $headers = $response.Headers
            $response.Close()
            return [pscustomobject]@{
                StatusCode = $statusCode
                Headers = $headers
                Content = $content
            }
        }
        return $null
    }
}

function Initialize-McpSession {
    $payload = @{
        jsonrpc = "2.0"
        id = "smoke-init"
        method = "initialize"
        params = @{
            protocolVersion = "2025-06-18"
            capabilities = @{}
            clientInfo = @{
                name = "ainpc-smoke"
                version = "1.0.0"
            }
        }
    } | ConvertTo-Json -Compress -Depth 8

    $response = Invoke-McpRequest -Payload $payload
    if ($null -eq $response -or $response.StatusCode -lt 200 -or $response.StatusCode -gt 299) {
        $detail = if ($response) { "HTTP $($response.StatusCode): $($response.Content)" } else { "initialize request failed" }
        Write-Step -Label "MCP initialize" -Ok $false -Detail $detail
        return $null
    }
    $sessionId = $response.Headers["Mcp-Session-Id"]
    if ($sessionId -is [array]) {
        $sessionId = $sessionId[0]
    }
    $ok = -not [string]::IsNullOrWhiteSpace($sessionId)
    Write-Step -Label "MCP initialize" -Ok $ok -Detail "session=$(if ($ok) { 'created' } else { 'missing' })"
    if (-not $ok) {
        return $null
    }

    $initializedPayload = @{
        jsonrpc = "2.0"
        method = "notifications/initialized"
        params = @{}
    } | ConvertTo-Json -Compress
    $initializedResponse = Invoke-McpRequest -Payload $initializedPayload -SessionId $sessionId
    $initializedOk = $initializedResponse -ne $null -and $initializedResponse.StatusCode -ge 200 -and $initializedResponse.StatusCode -le 299
    Write-Step -Label "MCP initialized notification" -Ok $initializedOk -Detail "status=$(if ($initializedResponse) { $initializedResponse.StatusCode } else { 'null' })"
    if (-not $initializedOk) {
        return $null
    }
    return $sessionId
}

function Convert-McpToolPayload {
    param($Response)
    if ($null -eq $Response) {
        return $null
    }
    if ($Response.PSObject.Properties.Name -contains "Content") {
        $body = $Response.Content
        $dataLines = @($body -split "`r?`n" | Where-Object { $_ -like "data:*" } | ForEach-Object { $_.Substring(5).Trim() } | Where-Object { $_ })
        if ($dataLines.Count -gt 0) {
            $body = $dataLines[-1]
        }
        try {
            return Convert-McpToolPayload -Response ($body | ConvertFrom-Json)
        } catch {
            return $body
        }
    }
    if ($Response.PSObject.Properties.Name -contains "result") {
        $result = $Response.result
        if ($null -eq $result) {
            return $null
        }
        if ($result.PSObject.Properties.Name -contains "structuredContent") {
            return $result.structuredContent
        }
        if ($result.PSObject.Properties.Name -contains "content") {
            $first = @($result.content) | Select-Object -First 1
            if ($null -eq $first) {
                return $result
            }
            if ($first.PSObject.Properties.Name -contains "text") {
                try {
                    return $first.text | ConvertFrom-Json
                } catch {
                    return $first.text
                }
            }
        }
        return $result
    }
    return $Response
}

function Escape-JsonString {
    param([string]$Value)
    if ($null -eq $Value) {
        return ""
    }
    return $Value.Replace("\", "\\").
        Replace("""", "\""").
        Replace("`r", "\r").
        Replace("`n", "\n").
        Replace("`t", "\t")
}

function Write-SmokeReport {
    param(
        [string]$Path,
        [string]$Service,
        [string]$Url,
        [array]$Steps
    )
    $passed = @($Steps | Where-Object { $_.ok }).Count
    $failed = @($Steps | Where-Object { -not $_.ok }).Count
    $lines = @()
    $lines += "{"
    $lines += "  ""timestamp"": ""$(Escape-JsonString (Get-Date -Format "o"))"","
    $lines += "  ""service"": ""$(Escape-JsonString $Service)"","
    $lines += "  ""url"": ""$(Escape-JsonString $Url)"","
    $lines += "  ""summary"": { ""total"": $($Steps.Count), ""passed"": $passed, ""failed"": $failed },"
    $lines += "  ""steps"": ["
    for ($i = 0; $i -lt $Steps.Count; $i++) {
        $step = $Steps[$i]
        $comma = if ($i -lt ($Steps.Count - 1)) { "," } else { "" }
        $ok = if ($step.ok) { "true" } else { "false" }
        $lines += "    { ""step"": ""$(Escape-JsonString $step.step)"", ""ok"": $ok, ""detail"": ""$(Escape-JsonString $step.detail)"", ""timestamp"": ""$(Escape-JsonString $step.timestamp)"" }$comma"
    }
    $lines += "  ]"
    $lines += "}"
    $lines | Set-Content -LiteralPath $Path -Encoding UTF8
    return @{
        total = $Steps.Count
        passed = $passed
        failed = $failed
    }
}

function Stop-StartedMcpService {
    if ($proc) {
        $proc.Refresh()
        if (-not $proc.HasExited) {
            Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
        }
    }
    try {
        $port = ([System.Uri]$McpUrl).Port
        if ($port -gt 0) {
            $listeners = @(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
                Select-Object -ExpandProperty OwningProcess -Unique)
            foreach ($pidValue in $listeners) {
                if ($pidValue -gt 0) {
                    Stop-Process -Id $pidValue -Force -ErrorAction SilentlyContinue
                }
            }
        }
    } catch {
        Write-Warning "Nu am putut curata procesul MCP pornit de smoke: $_"
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
    if ($ServiceProfile) {
        $env:SPRING_PROFILES_ACTIVE = $ServiceProfile
    }
    $env:MCP_MODE = "bridge"
    if ($SnapshotPath) {
        $env:MCP_SNAPSHOT_PATH = $SnapshotPath
    }
    New-Item -ItemType Directory -Path (Split-Path -Parent $serviceLogPath) -Force | Out-Null
    $proc = Start-Process -FilePath "java" `
        -ArgumentList "-jar `"$jar`"" `
        -PassThru `
        -WindowStyle Hidden `
        -RedirectStandardOutput $serviceLogPath `
        -RedirectStandardError $serviceErrorLogPath
    Start-Sleep -Seconds 3
}

# Step 3: Health check
$healthOk = $false
$healthDetail = "not checked"
$healthDeadline = (Get-Date).AddSeconds($HealthTimeoutSeconds)
do {
    try {
        $health = Invoke-RestMethod -Uri "$McpUrl/actuator/health" -TimeoutSec 2 -ErrorAction Stop
        $healthOk = $health.status -eq "UP"
        $healthDetail = "status=$($health.status)"
        if ($healthOk) {
            break
        }
    } catch {
        $healthDetail = "Eroare: $_"
    }
    Start-Sleep -Milliseconds 500
} while ((Get-Date) -lt $healthDeadline)
Write-Step -Label "Process health endpoint" -Ok $healthOk -Detail $healthDetail
if (-not $healthOk -and $StartService -and $proc) {
    $proc.Refresh()
    if ($proc.HasExited) {
        Write-Step -Label "MCP process state" -Ok $false -Detail "process exited before health was UP; logs=$serviceLogPath;$serviceErrorLogPath"
    }
}

# Step 4: Ping tool
$ping = Invoke-McpTool -ToolName "ainpc.ping"
Write-Step -Label "Tool: ainpc.ping" -Ok ($ping -ne $null) -Detail "status=$(if ($ping) { $ping.StatusCode } else { 'null' })"

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
$debugPayload = Convert-McpToolPayload -Response $debugHealth
Write-Step -Label "Tool: ainpc.debug.health" -Ok ($debugPayload -ne $null) -Detail "ok=$($debugPayload -ne $null)"

# Step 11b: Runtime bridge health from debug payload
$bridgeStatus = $null
$snapshotStatus = $null
$snapshotPath = $null
if ($debugPayload -ne $null -and ($debugPayload.PSObject.Properties.Name -contains "runtimeBridge")) {
    $bridgeStatus = $debugPayload.runtimeBridge.status
}
if ($debugPayload -ne $null -and ($debugPayload.PSObject.Properties.Name -contains "snapshot")) {
    $snapshotStatus = $debugPayload.snapshot.status
    $snapshotPath = $debugPayload.snapshot.path
}
$acceptedBridgeStates = @("connected", "cached")
$acceptedSnapshotStates = @("AVAILABLE")
if ($AllowStaleSnapshot) {
    $acceptedBridgeStates += "stale"
    $acceptedSnapshotStates += "STALE"
}
$bridgeOk = $acceptedBridgeStates -contains $bridgeStatus
$snapshotOk = $acceptedSnapshotStates -contains $snapshotStatus
Write-Step -Label "Runtime bridge health" -Ok $bridgeOk -Detail "bridge=$bridgeStatus"
Write-Step -Label "Runtime snapshot state" -Ok $snapshotOk -Detail "snapshot=$snapshotStatus path=$snapshotPath"

# Step 12: Semantic context summary tool
$semantic = Invoke-McpTool -ToolName "ainpc.semantic.context.summary"
Write-Step -Label "Tool: ainpc.semantic.context.summary" -Ok ($semantic -ne $null) -Detail "ok=$($semantic -ne $null)"

# Generate report
$summary = Write-SmokeReport -Path $reportPath -Service "ainpc-mcp-service" -Url $McpUrl -Steps $script:results
Write-Host "`nReport saved: $reportPath"
Write-Host "Passed: $($summary.passed)/$($summary.total) | Failed: $($summary.failed)"

# Stop service if we started it
if ($StopService -and $StartService) {
    Stop-StartedMcpService
    Write-Host "Service stopped."
}

if ($StartService) {
    $env:SPRING_PROFILES_ACTIVE = $previousSpringProfilesActive
    $env:MCP_MODE = $previousMcpMode
    $env:MCP_SNAPSHOT_PATH = $previousMcpSnapshotPath
}

if ($summary.failed -gt 0) {
    exit 1
}
