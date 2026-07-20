package ro.ainpc.mcp.tools;

import java.time.Instant;
import java.util.Map;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ro.ainpc.mcp.bridge.McpRuntimeBridgeHealthIndicator;
import ro.ainpc.mcp.bridge.SnapshotReader;

@Component
public class AinpcFeatureStateTools {
    private final Environment environment;
    private final McpRuntimeBridgeHealthIndicator bridgeHealth;
    private final SnapshotReader snapshotReader;

    public AinpcFeatureStateTools(Environment environment,
                                   McpRuntimeBridgeHealthIndicator bridgeHealth,
                                   SnapshotReader snapshotReader) {
        this.environment = environment;
        this.bridgeHealth = bridgeHealth;
        this.snapshotReader = snapshotReader;
    }

    @McpTool(
        name = "ainpc.feature.state",
        description = "Read-only feature state for the local AINPC MCP sidecar.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    public Map<String, Object> featureState() {
        boolean mcpEnabled = environment.getProperty("mcp.enabled", Boolean.class, true);
        boolean writeToolsEnabled = environment.getProperty("mcp.write_tools_enabled", Boolean.class, false);

        McpRuntimeBridgeHealthIndicator.BridgeHealthResult bridgeResult = bridgeHealth.check();

        Map<String, Object> snapshotInfo;
        if (snapshotReader != null) {
            snapshotInfo = Map.of(
                "status", snapshotReader.status().name(),
                "path", snapshotReader.getSnapshotPath().toString()
            );
        } else {
            snapshotInfo = Map.of(
                "status", "NO_READER",
                "path", "unavailable"
            );
        }

        return Map.of(
            "schemaVersion", 2,
            "service", environment.getProperty("spring.application.name", "ainpc-mcp-service"),
            "mcp", Map.of(
                "enabled", mcpEnabled,
                "protocol", environment.getProperty("spring.ai.mcp.server.protocol", "STREAMABLE"),
                "type", environment.getProperty("spring.ai.mcp.server.type", "SYNC"),
                "writeToolsEnabled", writeToolsEnabled
            ),
            "runtimeBridge", Map.of(
                "enabled", bridgeResult.bridge() != null && !"not_configured".equals(bridgeResult.bridge()),
                "status", bridgeResult.status(),
                "bridge", bridgeResult.bridge() != null ? bridgeResult.bridge() : "not_configured",
                "detail", bridgeResult.detail() != null ? bridgeResult.detail() : "OK"
            ),
            "snapshot", snapshotInfo,
            "tools", Map.ofEntries(
                Map.entry("readOnly", !writeToolsEnabled),
                Map.entry("writeToolsEnabled", writeToolsEnabled),
                Map.entry("semanticContextExport", true),
                Map.entry("semanticContextSummaryExport", true),
                Map.entry("questSemanticContextExport", true),
                Map.entry("questSemanticContextSummaryExport", true),
                Map.entry("questAuthoringContextSummaryExport", true),
                Map.entry("mappingSemanticContextExport", true),
                Map.entry("mappingSemanticContextSummaryExport", true),
                Map.entry("storySemanticContextExport", true),
                Map.entry("storySemanticContextSummaryExport", true),
                Map.entry("routingSemanticContextExport", true),
                Map.entry("routingSemanticContextSummaryExport", true),
                Map.entry("semanticRoutingSummaryExport", true)
            ),
            "timestamp", Instant.now().toString()
        );
    }
}
