package ro.ainpc.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class AinpcPingToolsTest {
    @Test
    void pingReturnsReadOnlyStatus() {
        Map<String, Object> result = new AinpcPingTools().ping();

        assertEquals("ok", result.get("status"));
        assertEquals("ainpc-mcp-service", result.get("service"));
        assertEquals(true, result.get("readOnly"));
        assertTrue(result.containsKey("timestamp"));
    }
}
