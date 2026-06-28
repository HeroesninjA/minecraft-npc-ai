package ro.ainpc.mcp.bridge;

import org.springframework.stereotype.Component;

@Component
public class McpSnapshotService {
    private final SnapshotReader snapshotReader;
    private final RedactingSnapshotFilter redactingFilter;
    private final McpAuditLogger auditLogger;
    private final McpRuntimeBridgeHealthIndicator healthIndicator;

    public McpSnapshotService(
        SnapshotReader snapshotReader,
        RedactingSnapshotFilter redactingFilter,
        McpAuditLogger auditLogger,
        McpRuntimeBridgeHealthIndicator healthIndicator
    ) {
        this.snapshotReader = snapshotReader;
        this.redactingFilter = redactingFilter;
        this.auditLogger = auditLogger;
        this.healthIndicator = healthIndicator;
    }

    public SnapshotReader.SnapshotResult read(String toolName) {
        long start = System.currentTimeMillis();
        SnapshotReader.SnapshotResult result = snapshotReader.read();
        long duration = System.currentTimeMillis() - start;

        if (result.isAvailable() && result.getSnapshot() != null) {
            RuntimeSnapshot redacted = redactingFilter.redact(result.getSnapshot());
            if (redacted != null) {
                result = SnapshotReader.SnapshotResult.valid(redacted, result.isFresh());
            }
        }

        auditLogger.logToolCall(toolName, result.isAvailable(), duration,
            result.getSnapshot() != null ? 512 : 64);

        return result;
    }

    public McpRuntimeBridgeHealthIndicator.BridgeHealthResult health() {
        return healthIndicator.check();
    }
}
