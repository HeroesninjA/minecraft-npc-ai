package ro.ainpc.mcp.bridge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class McpMode {
    private final String mode;

    public McpMode(@Value("${mcp.mode:static}") String mode) {
        this.mode = mode != null ? mode.trim().toLowerCase() : "static";
    }

    public boolean isOnline() {
        return "bridge".equals(mode);
    }

    public boolean isOffline() {
        return "offline".equals(mode);
    }

    public boolean isStatic() {
        return "static".equals(mode);
    }

    public String getMode() {
        return mode;
    }
}
