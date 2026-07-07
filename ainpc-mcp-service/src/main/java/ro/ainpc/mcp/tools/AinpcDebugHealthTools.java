package ro.ainpc.mcp.tools;

import java.time.Instant;
import java.util.Map;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import ro.ainpc.mcp.bridge.McpRuntimeBridgeHealthIndicator;

@Component
public class AinpcDebugHealthTools {
    private final Environment environment;
    private final McpRuntimeBridgeHealthIndicator bridgeHealth;

    public AinpcDebugHealthTools(Environment environment, McpRuntimeBridgeHealthIndicator bridgeHealth) {
        this.environment = environment;
        this.bridgeHealth = bridgeHealth;
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

        return Map.of(
            "schemaVersion", 1,
            "service", environment.getProperty("spring.application.name", "ainpc-mcp-service"),
            "status", bridge.status(),
            "server", Map.of(
                "address", environment.getProperty("server.address", "127.0.0.1"),
                "port", environment.getProperty("server.port", "39841")
            ),
            "mcp", Map.of(
                "protocol", environment.getProperty("spring.ai.mcp.server.protocol", "STREAMABLE"),
                "type", environment.getProperty("spring.ai.mcp.server.type", "SYNC"),
                "writeToolsEnabled", true
            ),
            "runtimeBridge", Map.of(
                "enabled", true,
                "status", bridge.bridge(),
                "detail", bridge.detail() != null ? bridge.detail() : "OK"
            ),
            "timestamp", Instant.now().toString()
        );
    }
}
