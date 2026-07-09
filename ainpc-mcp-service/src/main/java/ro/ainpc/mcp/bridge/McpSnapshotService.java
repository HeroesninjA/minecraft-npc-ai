package ro.ainpc.mcp.bridge;

import com.google.gson.Gson;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class McpSnapshotService {
    private static final Gson GSON = new Gson();

    private final SnapshotReader snapshotReader;
    private final RedactingSnapshotFilter redactingFilter;
    private final McpAuditLogger auditLogger;

    public McpSnapshotService(
        SnapshotReader snapshotReader,
        RedactingSnapshotFilter redactingFilter,
        McpAuditLogger auditLogger
    ) {
        this.snapshotReader = snapshotReader;
        this.redactingFilter = redactingFilter;
        this.auditLogger = auditLogger;
    }

    public SnapshotReader.SnapshotResult read(String toolName) {
        long start = System.currentTimeMillis();
        SnapshotReader.SnapshotResult result = snapshotReader.read();
        long duration = System.currentTimeMillis() - start;
        int payloadSizeBytes = 0;

        if (result.isAvailable() && result.getSnapshot() != null) {
            RuntimeSnapshot redacted = redactingFilter.redact(result.getSnapshot());
            if (redacted != null) {
                result = SnapshotReader.SnapshotResult.valid(redacted, result.isFresh());
                payloadSizeBytes = GSON.toJson(redacted).getBytes(StandardCharsets.UTF_8).length;
            }
        }

        auditLogger.logToolCall(toolName, result.isAvailable(), duration,
            payloadSizeBytes);

        return result;
    }
}
