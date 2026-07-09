package ro.ainpc.mcp.bridge;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class McpMode {
    private final String mode;

    public McpMode(@Value("${mcp.mode:static}") String mode) {
        this.mode = mode != null ? mode.trim().toLowerCase(Locale.ROOT) : "static";
    }

    public boolean isOffline() {
        return "offline".equals(mode);
    }

    public boolean isStatic() {
        return "static".equals(mode);
    }
}
