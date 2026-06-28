package ro.ainpc.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AinpcFeatureStateToolsTest {
    @Test
    void featureStateReportsReadOnlyMcpSidecar() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("spring.application.name", "ainpc-mcp-service")
            .withProperty("spring.ai.mcp.server.protocol", "STREAMABLE")
            .withProperty("spring.ai.mcp.server.type", "SYNC");

        Map<String, Object> result = new AinpcFeatureStateTools(environment).featureState();

        assertEquals(1, result.get("schemaVersion"));
        assertEquals("ainpc-mcp-service", result.get("service"));
        assertTrue(result.containsKey("timestamp"));

        Map<?, ?> mcp = (Map<?, ?>) result.get("mcp");
        assertEquals(true, mcp.get("enabled"));
        assertEquals("STREAMABLE", mcp.get("protocol"));

        Map<?, ?> runtimeBridge = (Map<?, ?>) result.get("runtimeBridge");
        assertEquals(false, runtimeBridge.get("enabled"));

        Map<?, ?> tools = (Map<?, ?>) result.get("tools");
        assertEquals(true, tools.get("readOnly"));
        assertEquals(false, tools.get("writeToolsEnabled"));
        assertEquals(true, tools.get("semanticContextExport"));
        assertEquals(true, tools.get("semanticContextSummaryExport"));
        assertEquals(true, tools.get("questSemanticContextExport"));
        assertEquals(true, tools.get("questSemanticContextSummaryExport"));
        assertEquals(true, tools.get("questAuthoringContextSummaryExport"));
        assertEquals(true, tools.get("mappingSemanticContextExport"));
        assertEquals(true, tools.get("mappingSemanticContextSummaryExport"));
        assertEquals(true, tools.get("storySemanticContextExport"));
        assertEquals(true, tools.get("storySemanticContextSummaryExport"));
        assertEquals(true, tools.get("routingSemanticContextExport"));
        assertEquals(true, tools.get("routingSemanticContextSummaryExport"));
        assertEquals(true, tools.get("semanticRoutingSummaryExport"));
    }
}
