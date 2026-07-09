package ro.ainpc.mcp.bridge;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;

class McpModeTest {

    @Test
    void parsesModeIndependentlyOfDefaultLocale() {
        Locale previous = Locale.getDefault();
        Locale.setDefault(new Locale("tr", "TR"));
        try {
            McpMode staticMode = new McpMode("STATIC");
            McpMode offlineMode = new McpMode("OFFLINE");

            assertTrue(staticMode.isStatic());
            assertTrue(offlineMode.isOffline());
        } finally {
            Locale.setDefault(previous);
        }
    }
}
