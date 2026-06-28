package ro.ainpc.mcp.bridge;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class McpRuntimeBridgeHealthIndicator {
    private final SnapshotReader snapshotReader;

    public McpRuntimeBridgeHealthIndicator(SnapshotReader snapshotReader) {
        this.snapshotReader = snapshotReader;
    }

    public BridgeHealthResult check() {
        SnapshotReader.SnapshotResult result = snapshotReader.read();

        if (!result.isAvailable()) {
            return new BridgeHealthResult("DOWN", "bridge_error",
                result.getDetail() != null ? result.getDetail() : "Snapshot indisponibil.");
        }
        if (!result.isFresh()) {
            return new BridgeHealthResult("UP", "cached",
                "Date din cache (TTL activ).");
        }
        return new BridgeHealthResult("UP", "connected",
            "Bridge runtime conectat si date proaspete.");
    }

    public record BridgeHealthResult(String status, String bridge, String detail) {}
}
