package ro.ainpc.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import ro.ainpc.mcp.bridge.McpRuntimeBridgeHealthIndicator;
import ro.ainpc.mcp.bridge.SnapshotReader;

class AinpcFeatureStateToolsTest {
    @Test
    void featureStateReportsReadOnlyMcpSidecar() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("spring.application.name", "ainpc-mcp-service")
            .withProperty("spring.ai.mcp.server.protocol", "STREAMABLE")
            .withProperty("spring.ai.mcp.server.type", "SYNC");

        McpRuntimeBridgeHealthIndicator.BridgeHealthResult stubHealth =
            new McpRuntimeBridgeHealthIndicator.BridgeHealthResult("OK", "local-bridge", "Bridge connected");
        McpRuntimeBridgeHealthIndicator stubIndicator = new McpRuntimeBridgeHealthIndicator(null) {
            @Override
            public BridgeHealthResult check() {
                return stubHealth;
            }
        };

        Map<String, Object> result = new AinpcFeatureStateTools(environment, stubIndicator, null).featureState();

        assertEquals(2, result.get("schemaVersion"));
        assertEquals("ainpc-mcp-service", result.get("service"));
        assertTrue(result.containsKey("timestamp"));

        Map<?, ?> mcp = (Map<?, ?>) result.get("mcp");
        assertEquals(true, mcp.get("enabled"));
        assertEquals("STREAMABLE", mcp.get("protocol"));
        assertEquals(false, mcp.get("writeToolsEnabled"));

        Map<?, ?> runtimeBridge = (Map<?, ?>) result.get("runtimeBridge");
        assertEquals(true, runtimeBridge.get("enabled"));
        assertEquals("OK", runtimeBridge.get("status"));

        Map<?, ?> tools = (Map<?, ?>) result.get("tools");
        assertEquals(true, tools.get("readOnly"));
        assertEquals(false, tools.get("writeToolsEnabled"));
        assertEquals(true, tools.get("semanticContextExport"));
    }
}
