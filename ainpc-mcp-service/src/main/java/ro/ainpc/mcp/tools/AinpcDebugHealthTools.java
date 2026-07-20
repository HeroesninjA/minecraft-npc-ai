package ro.ainpc.mcp.tools;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import ro.ainpc.mcp.bridge.McpRuntimeBridgeHealthIndicator;
import ro.ainpc.mcp.bridge.SnapshotReader;

@Component
public class AinpcDebugHealthTools {
    private final Environment environment;
    private final McpRuntimeBridgeHealthIndicator bridgeHealth;
    private final SnapshotReader snapshotReader;

    public AinpcDebugHealthTools(Environment environment,
                                  McpRuntimeBridgeHealthIndicator bridgeHealth,
                                  SnapshotReader snapshotReader) {
        this.environment = environment;
        this.bridgeHealth = bridgeHealth;
        this.snapshotReader = snapshotReader;
    }

    @McpTool(
        name = "ainpc.debug.health",
        description = "Read-only diagnostic health snapshot for the local AINPC MCP sidecar.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    public Map<String, Object> debugHealth() {
        McpRuntimeBridgeHealthIndicator.BridgeHealthResult bridge = bridgeHealth.check();
        boolean writeToolsEnabled = environment.getProperty("mcp.write_tools_enabled", Boolean.class, false);

        Map<String, Object> snapshotInfo;
        if (snapshotReader != null) {
            SnapshotReader.SnapshotResult snapshotResult = snapshotReader.read();
            snapshotInfo = new LinkedHashMap<>();
            snapshotInfo.put("status", snapshotResult.getState().name());
            snapshotInfo.put("path", snapshotReader.getSnapshotPath().toString());
            snapshotInfo.put("available", snapshotResult.isAvailable());
            if (snapshotResult.isAvailable() && snapshotResult.getSnapshot().getHealth() != null) {
                snapshotInfo.put("runtimeHealth", snapshotResult.getSnapshot().getHealth());
            } else if (snapshotResult.getDetail() != null) {
                snapshotInfo.put("detail", snapshotResult.getDetail());
            }
        } else {
            snapshotInfo = Map.of(
                "status", "NO_READER",
                "path", "unavailable"
            );
        }

        return Map.of(
            "schemaVersion", 3,
            "service", environment.getProperty("spring.application.name", "ainpc-mcp-service"),
            "status", bridge.status(),
            "server", Map.of(
                "address", environment.getProperty("server.address", "127.0.0.1"),
                "port", environment.getProperty("server.port", "39841")
            ),
            "mcp", Map.of(
                "protocol", environment.getProperty("spring.ai.mcp.server.protocol", "STREAMABLE"),
                "type", environment.getProperty("spring.ai.mcp.server.type", "SYNC"),
                "writeToolsEnabled", writeToolsEnabled
            ),
            "runtimeBridge", Map.of(
                "enabled", bridge.bridge() != null && !"not_configured".equals(bridge.bridge()),
                "status", bridge.bridge(),
                "detail", bridge.detail() != null ? bridge.detail() : "OK"
            ),
            "snapshot", snapshotInfo,
            "timestamp", Instant.now().toString()
        );
    }
}
