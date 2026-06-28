package ro.ainpc.mcp.tools;

import java.time.Instant;
import java.util.Map;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AinpcDebugHealthTools {
    private final Environment environment;

    public AinpcDebugHealthTools(Environment environment) {
        this.environment = environment;
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
        return Map.of(
            "schemaVersion", 1,
            "service", environment.getProperty("spring.application.name", "ainpc-mcp-service"),
            "status", "UP",
            "server", Map.of(
                "address", environment.getProperty("server.address", "127.0.0.1"),
                "port", environment.getProperty("server.port", "39841")
            ),
            "mcp", Map.of(
                "protocol", environment.getProperty("spring.ai.mcp.server.protocol", "STREAMABLE"),
                "type", environment.getProperty("spring.ai.mcp.server.type", "SYNC"),
                "writeToolsEnabled", false
            ),
            "runtimeBridge", Map.of(
                "enabled", false,
                "status", "not_configured"
            ),
            "timestamp", Instant.now().toString()
        );
    }
}
