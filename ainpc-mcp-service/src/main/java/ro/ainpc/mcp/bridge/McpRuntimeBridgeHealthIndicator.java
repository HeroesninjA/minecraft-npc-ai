package ro.ainpc.mcp.bridge;

import org.springframework.stereotype.Component;

@Component
public class McpRuntimeBridgeHealthIndicator {
    private final SnapshotReader snapshotReader;

    public McpRuntimeBridgeHealthIndicator(SnapshotReader snapshotReader) {
        this.snapshotReader = snapshotReader;
    }

    public BridgeHealthResult check() {
        SnapshotReader.SnapshotStatus status = snapshotReader.status();

        if (status == SnapshotReader.SnapshotStatus.OFFLINE) {
            return new BridgeHealthResult("DOWN", "offline",
                "MCP ruleaza in mod offline. Snapshot dezactivat.");
        }

        SnapshotReader.SnapshotResult result = snapshotReader.read();

        if (!result.isAvailable()) {
            return new BridgeHealthResult("DOWN", "bridge_error",
                result.getDetail() != null ? result.getDetail()
                    : "Snapshot indisponibil: " + snapshotReader.getSnapshotPath());
        }
        if (result.getState() == SnapshotReader.SnapshotState.STALE) {
            return new BridgeHealthResult("UP", "stale",
                result.getDetail() != null ? result.getDetail()
                    : "Snapshot stale din " + snapshotReader.getSnapshotPath());
        }
        if (result.getState() == SnapshotReader.SnapshotState.CACHED) {
            return new BridgeHealthResult("UP", "cached",
                "Date din cache (TTL activ) din " + snapshotReader.getSnapshotPath());
        }
        return new BridgeHealthResult("UP", "connected",
            "Bridge runtime conectat si date proaspete din " + snapshotReader.getSnapshotPath());
    }

    public record BridgeHealthResult(String status, String bridge, String detail) {}
}
