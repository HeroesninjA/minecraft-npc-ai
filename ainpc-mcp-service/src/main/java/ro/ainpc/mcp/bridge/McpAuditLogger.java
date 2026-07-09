package ro.ainpc.mcp.bridge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class McpAuditLogger {
    private static final Logger LOG = LoggerFactory.getLogger(McpAuditLogger.class);

    private final Path auditPath;
    private final Gson gson;
    private final int maxEntries;
    private final AtomicInteger writeCount = new AtomicInteger(0);

    public McpAuditLogger(
        @Value("${mcp.audit.path:data/mcp-write-audit.json}") String auditPath,
        @Value("${mcp.audit.max-entries:1000}") int maxEntries
    ) {
        this.auditPath = Path.of(auditPath);
        this.gson = new GsonBuilder().create();
        this.maxEntries = maxEntries;
        this.writeCount.set(countExistingEntries());
    }

    public void logToolCall(String toolName, boolean available, long durationMs, int payloadSize) {
        JsonObject entry = new JsonObject();
        entry.addProperty("timestamp", Instant.now().toString());
        entry.addProperty("tool", toolName);
        entry.addProperty("available", available);
        entry.addProperty("durationMs", durationMs);
        entry.addProperty("payloadSizeBytes", payloadSize);
        entry.addProperty("source", "ainpc-mcp-service");

        synchronized (this) {
            try {
                Files.writeString(auditPath, gson.toJson(entry) + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                int count = writeCount.incrementAndGet();
                if (count > maxEntries) {
                    trim();
                }
            } catch (IOException e) {
                LOG.warn("Nu s-a putut scrie audit entry: {}", e.getMessage());
            }
        }
    }

    private void trim() {
        try {
            var lines = Files.readAllLines(auditPath);
            if (lines.size() > maxEntries) {
                var trimmed = lines.subList(lines.size() - maxEntries, lines.size());
                Files.write(auditPath, trimmed);
            }
            writeCount.set(0);
        } catch (IOException e) {
            LOG.warn("Nu s-a putut trimite audit log: {}", e.getMessage());
        }
    }

    private int countExistingEntries() {
        try {
            if (!Files.exists(auditPath)) {
                return 0;
            }
            return Files.readAllLines(auditPath).size();
        } catch (IOException e) {
            LOG.warn("Nu s-a putut citi audit log existent: {}", e.getMessage());
            return 0;
        }
    }
}
