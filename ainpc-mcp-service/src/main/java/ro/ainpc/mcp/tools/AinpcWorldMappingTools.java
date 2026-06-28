package ro.ainpc.mcp.tools;

import java.time.Instant;
import java.util.Map;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;
import ro.ainpc.mcp.bridge.McpSnapshotService;
import ro.ainpc.mcp.bridge.RuntimeSnapshot;
import ro.ainpc.mcp.bridge.SnapshotReader;

@Component
public class AinpcWorldMappingTools {
    private final McpSnapshotService mcpSnapshotService;

    public AinpcWorldMappingTools(McpSnapshotService mcpSnapshotService) {
        this.mcpSnapshotService = mcpSnapshotService;
    }

    @McpTool(
        name = "ainpc.world.mapping.summary",
        description = "Read-only world mapping summary: regions, places, nodes, and semantic index.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true, destructiveHint = false,
            idempotentHint = true, openWorldHint = false
        )
    )
    public Map<String, Object> worldMappingSummary() {
        SnapshotReader.SnapshotResult result = mcpSnapshotService.read("ainpc.world.mapping.summary");
        if (!result.isAvailable()) {
            return base("unavailable", result.getDetail());
        }
        RuntimeSnapshot.WorldSnapshot w = result.getSnapshot().getWorld();

        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "available", true,
            "regionCount", w.getRegionCount(),
            "placeCount", w.getPlaceCount(),
            "nodeCount", w.getNodeCount(),
            "regions", w.getRegionNames() != null ? w.getRegionNames() : java.util.List.of(),
            "places", w.getPlaceNames() != null ? w.getPlaceNames() : java.util.List.of(),
            "timestamp", Instant.now().toString()
        );
    }

    private Map<String, Object> base(String status, String detail) {
        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "available", false,
            "status", status,
            "detail", detail != null ? detail : "N/A",
            "timestamp", Instant.now().toString()
        );
    }
}
