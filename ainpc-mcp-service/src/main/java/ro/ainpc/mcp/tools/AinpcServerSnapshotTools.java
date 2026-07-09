package ro.ainpc.mcp.tools;

import java.time.Instant;
import java.util.Map;
import java.util.Locale;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;
import ro.ainpc.mcp.bridge.McpSnapshotService;
import ro.ainpc.mcp.bridge.RuntimeSnapshot;
import ro.ainpc.mcp.bridge.SnapshotReader;

@Component
public class AinpcServerSnapshotTools {
    private final McpSnapshotService mcpSnapshotService;

    public AinpcServerSnapshotTools(McpSnapshotService mcpSnapshotService) {
        this.mcpSnapshotService = mcpSnapshotService;
    }

    @McpTool(
        name = "ainpc.server.snapshot",
        description = "Read-only server snapshot: version, NPC count, player count, world mapping counts.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true, destructiveHint = false,
            idempotentHint = true, openWorldHint = false
        )
    )
    public Map<String, Object> serverSnapshot() {
        SnapshotReader.SnapshotResult result = mcpSnapshotService.read("ainpc.server.snapshot");
        String snapshotState = result.getState().name().toLowerCase(Locale.ROOT);
        if (!result.isAvailable()) {
            return base(snapshotState, snapshotState, result.getDetail());
        }
        RuntimeSnapshot s = result.getSnapshot();
        RuntimeSnapshot.PluginSnapshot p = s.getPlugin();
        RuntimeSnapshot.NpcSnapshot n = s.getNpc();
        RuntimeSnapshot.WorldSnapshot w = s.getWorld();
        RuntimeSnapshot.FeatureSnapshot f = s.getFeatures();

        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "available", true,
            "snapshotState", snapshotState,
            "plugin", Map.of(
                "version", p.getVersion(),
                "serverType", p.getServerType(),
                "onlinePlayers", p.getOnlinePlayers(),
                "uptimeMinutes", p.getUptimeMinutes()
            ),
            "features", Map.of(
                "ai", f.isAi(),
                "mcp", f.isMcp(),
                "aiOrchestration", f.isAiOrchestration()
            ),
            "npc", Map.of(
                "totalCount", n.getTotalCount(),
                "byRegion", n.getByRegion()
            ),
            "world", Map.of(
                "regionCount", w.getRegionCount(),
                "placeCount", w.getPlaceCount(),
                "nodeCount", w.getNodeCount()
            ),
            "snapshotAge", s.getTimestamp(),
            "timestamp", Instant.now().toString()
        );
    }

    private Map<String, Object> base(String status, String snapshotState, String detail) {
        return Map.of(
            "schemaVersion", 1,
            "service", "ainpc-mcp-service",
            "available", false,
            "status", status,
            "snapshotState", snapshotState,
            "detail", detail != null ? detail : "N/A",
            "timestamp", Instant.now().toString()
        );
    }
}
