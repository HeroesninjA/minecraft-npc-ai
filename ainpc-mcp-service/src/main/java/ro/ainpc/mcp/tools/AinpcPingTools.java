package ro.ainpc.mcp.tools;

import java.time.Instant;
import java.util.Map;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

@Component
public class AinpcPingTools {
    @McpTool(
        name = "ainpc.ping",
        description = "Read-only health probe for the AINPC MCP sidecar.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    public Map<String, Object> ping() {
        return Map.of(
            "status", "ok",
            "service", "ainpc-mcp-service",
            "readOnly", true,
            "timestamp", Instant.now().toString()
        );
    }
}
