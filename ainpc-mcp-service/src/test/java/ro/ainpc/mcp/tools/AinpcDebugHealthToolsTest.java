package ro.ainpc.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AinpcDebugHealthToolsTest {
    @Test
    void debugHealthReturnsReadOnlySidecarSnapshot() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("spring.application.name", "ainpc-mcp-service")
            .withProperty("server.address", "127.0.0.1")
            .withProperty("server.port", "39841")
            .withProperty("spring.ai.mcp.server.protocol", "STREAMABLE")
            .withProperty("spring.ai.mcp.server.type", "SYNC");

        Map<String, Object> result = new AinpcDebugHealthTools(environment).debugHealth();

        assertEquals(1, result.get("schemaVersion"));
        assertEquals("ainpc-mcp-service", result.get("service"));
        assertEquals("UP", result.get("status"));
        assertTrue(result.containsKey("timestamp"));

        Map<?, ?> server = (Map<?, ?>) result.get("server");
        assertEquals("127.0.0.1", server.get("address"));
        assertEquals("39841", server.get("port"));

        Map<?, ?> mcp = (Map<?, ?>) result.get("mcp");
        assertEquals("STREAMABLE", mcp.get("protocol"));
        assertEquals(false, mcp.get("writeToolsEnabled"));
    }
}
