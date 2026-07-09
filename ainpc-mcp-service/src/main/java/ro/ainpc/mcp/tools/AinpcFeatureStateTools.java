package ro.ainpc.mcp.tools;

import java.time.Instant;
import java.util.Map;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AinpcFeatureStateTools {
    private final Environment environment;

    public AinpcFeatureStateTools(Environment environment) {
        this.environment = environment;
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
        return Map.of(
            "schemaVersion", 1,
            "service", environment.getProperty("spring.application.name", "ainpc-mcp-service"),
            "mcp", Map.of(
                "enabled", true,
                "protocol", environment.getProperty("spring.ai.mcp.server.protocol", "STREAMABLE"),
                "type", environment.getProperty("spring.ai.mcp.server.type", "SYNC")
            ),
            "runtimeBridge", Map.of(
                "enabled", false,
                "status", "not_configured"
            ),
            "tools", Map.ofEntries(
                Map.entry("readOnly", true),
                Map.entry("writeToolsEnabled", true),
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
