package ro.ainpc.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AinpcSemanticContextToolsTest {
    @Test
    void semanticContextExportsStructuredWorldBlocks() {
        Map<String, Object> result = new AinpcSemanticContextTools().semanticContext();

        assertEquals(1, result.get("schemaVersion"));
        assertEquals("ainpc-mcp-service", result.get("service"));
        assertTrue(result.containsKey("timestamp"));

        List<?> blocks = (List<?>) result.get("blocks");
        assertEquals(4, blocks.size());
        assertTrue(blocks.toString().contains("WORLD_LORE"));
        assertTrue(blocks.toString().contains("WORLD_HISTORY"));
        assertTrue(blocks.toString().contains("NPC_LORE"));
        assertTrue(blocks.toString().contains("STORY_SIGNALS"));

        List<?> usage = (List<?>) result.get("usage");
        assertTrue(usage.toString().contains("WORLD_LORE"));
    }

    @Test
    void semanticContextSummaryExportsCompactRoutingHints() {
        Map<String, Object> result = new AinpcSemanticContextTools().semanticContextSummary();

        assertEquals(1, result.get("schemaVersion"));
        assertEquals("ainpc-mcp-service", result.get("service"));
        assertTrue(result.get("summary").toString().contains("WORLD_LORE"));
        assertEquals(4, result.get("blockCount"));

        List<?> blockNames = (List<?>) result.get("blockNames");
        assertEquals(List.of("WORLD_LORE", "WORLD_HISTORY", "NPC_LORE", "STORY_SIGNALS"), blockNames);

        List<?> recommendedOrder = (List<?>) result.get("recommendedOrder");
        assertEquals(List.of("WORLD_LORE", "WORLD_HISTORY", "NPC_LORE", "STORY_SIGNALS"), recommendedOrder);
    }

    @Test
    void questSemanticContextExportsStructuredQuestBlocks() {
        Map<String, Object> result = new AinpcSemanticContextTools().questSemanticContext();

        assertEquals(1, result.get("schemaVersion"));
        assertEquals("ainpc-mcp-service", result.get("service"));
        assertTrue(result.containsKey("timestamp"));

        List<?> blocks = (List<?>) result.get("blocks");
        assertEquals(3, blocks.size());
        assertTrue(blocks.toString().contains("QUEST_LORE"));
        assertTrue(blocks.toString().contains("QUEST_HISTORY"));
        assertTrue(blocks.toString().contains("QUEST_SIGNALS"));
    }

    @Test
    void questSemanticContextSummaryExportsCompactRoutingHints() {
        Map<String, Object> result = new AinpcSemanticContextTools().questSemanticContextSummary();

        assertEquals(1, result.get("schemaVersion"));
        assertEquals("ainpc-mcp-service", result.get("service"));
        assertTrue(result.get("summary").toString().contains("QUEST_LORE"));
        assertEquals(3, result.get("blockCount"));

        List<?> blockNames = (List<?>) result.get("blockNames");
        assertEquals(List.of("QUEST_LORE", "QUEST_HISTORY", "QUEST_SIGNALS"), blockNames);

        List<?> recommendedOrder = (List<?>) result.get("recommendedOrder");
        assertEquals(List.of("QUEST_LORE", "QUEST_HISTORY", "QUEST_SIGNALS"), recommendedOrder);
    }

    @Test
    void questAuthoringContextSummaryExportsCompactRoutingHints() {
        Map<String, Object> result = new AinpcSemanticContextTools().questAuthoringContextSummary();

        assertEquals(1, result.get("schemaVersion"));
        assertEquals("ainpc-mcp-service", result.get("service"));
        assertTrue(result.get("summary").toString().contains("QUEST_AUTHORING_LORE"));
        assertEquals(3, result.get("blockCount"));

        List<?> blockNames = (List<?>) result.get("blockNames");
        assertEquals(List.of("QUEST_AUTHORING_LORE", "QUEST_AUTHORING_HISTORY", "QUEST_AUTHORING_SIGNALS"), blockNames);

        List<?> recommendedOrder = (List<?>) result.get("recommendedOrder");
        assertEquals(List.of("QUEST_AUTHORING_LORE", "QUEST_AUTHORING_HISTORY", "QUEST_AUTHORING_SIGNALS"), recommendedOrder);
    }

    @Test
    void mappingSemanticContextExportsStructuredMappingBlocks() {
        Map<String, Object> result = new AinpcSemanticContextTools().mappingSemanticContext();

        assertEquals(1, result.get("schemaVersion"));
        assertEquals("ainpc-mcp-service", result.get("service"));
        assertTrue(result.containsKey("timestamp"));

        List<?> blocks = (List<?>) result.get("blocks");
        assertEquals(3, blocks.size());
        assertTrue(blocks.toString().contains("MAPPING_CONTEXT"));
        assertTrue(blocks.toString().contains("MAPPING_HISTORY"));
        assertTrue(blocks.toString().contains("MAPPING_STORY"));
    }

    @Test
    void mappingSemanticContextSummaryExportsCompactRoutingHints() {
        Map<String, Object> result = new AinpcSemanticContextTools().mappingSemanticContextSummary();

        assertEquals(1, result.get("schemaVersion"));
        assertEquals("ainpc-mcp-service", result.get("service"));
        assertTrue(result.get("summary").toString().contains("MAPPING_CONTEXT"));
        assertEquals(3, result.get("blockCount"));

        List<?> blockNames = (List<?>) result.get("blockNames");
        assertEquals(List.of("MAPPING_CONTEXT", "MAPPING_HISTORY", "MAPPING_STORY"), blockNames);

        List<?> recommendedOrder = (List<?>) result.get("recommendedOrder");
        assertEquals(List.of("MAPPING_CONTEXT", "MAPPING_HISTORY", "MAPPING_STORY"), recommendedOrder);
    }

    @Test
    void storySemanticContextExportsStructuredStoryBlocks() {
        Map<String, Object> result = new AinpcSemanticContextTools().storySemanticContext();

        assertEquals(1, result.get("schemaVersion"));
        assertEquals("ainpc-mcp-service", result.get("service"));
        assertTrue(result.containsKey("timestamp"));

        List<?> blocks = (List<?>) result.get("blocks");
        assertEquals(3, blocks.size());
        assertTrue(blocks.toString().contains("STORY_CONTEXT"));
        assertTrue(blocks.toString().contains("STORY_HISTORY"));
        assertTrue(blocks.toString().contains("STORY_SIGNALS"));
    }

    @Test
    void storySemanticContextSummaryExportsCompactRoutingHints() {
        Map<String, Object> result = new AinpcSemanticContextTools().storySemanticContextSummary();

        assertEquals(1, result.get("schemaVersion"));
        assertEquals("ainpc-mcp-service", result.get("service"));
        assertTrue(result.get("summary").toString().contains("STORY_CONTEXT"));
        assertEquals(3, result.get("blockCount"));

        List<?> blockNames = (List<?>) result.get("blockNames");
        assertEquals(List.of("STORY_CONTEXT", "STORY_HISTORY", "STORY_SIGNALS"), blockNames);

        List<?> recommendedOrder = (List<?>) result.get("recommendedOrder");
        assertEquals(List.of("STORY_CONTEXT", "STORY_HISTORY", "STORY_SIGNALS"), recommendedOrder);
    }

    @Test
    void routingSemanticContextExportsAllSemanticRouters() {
        Map<String, Object> result = new AinpcSemanticContextTools().routingSemanticContext();

        assertEquals(1, result.get("schemaVersion"));
        assertEquals("ainpc-mcp-service", result.get("service"));
        assertTrue(result.containsKey("timestamp"));

        List<?> blocks = (List<?>) result.get("blocks");
        assertEquals(5, blocks.size());
        assertTrue(blocks.toString().contains("ROUTING_WORLD"));
        assertTrue(blocks.toString().contains("ROUTING_STORY"));
        assertTrue(blocks.toString().contains("ROUTING_MAPPING"));
        assertTrue(blocks.toString().contains("ROUTING_QUEST"));
        assertTrue(blocks.toString().contains("ROUTING_QUEST_AUTHORING"));
    }

    @Test
    void routingSemanticContextSummaryExportsCompactRoutingHints() {
        Map<String, Object> result = new AinpcSemanticContextTools().routingSemanticContextSummary();

        assertEquals(1, result.get("schemaVersion"));
        assertEquals("ainpc-mcp-service", result.get("service"));
        assertTrue(result.get("summary").toString().contains("ROUTING_WORLD"));
        assertEquals(5, result.get("blockCount"));

        List<?> blockNames = (List<?>) result.get("blockNames");
        assertEquals(List.of("ROUTING_WORLD", "ROUTING_STORY", "ROUTING_MAPPING", "ROUTING_QUEST", "ROUTING_QUEST_AUTHORING"), blockNames);

        List<?> recommendedOrder = (List<?>) result.get("recommendedOrder");
        assertEquals(List.of("ROUTING_WORLD", "ROUTING_STORY", "ROUTING_MAPPING", "ROUTING_QUEST", "ROUTING_QUEST_AUTHORING"), recommendedOrder);
    }

    @Test
    void semanticRoutingSummaryExportsAllSemanticDomains() {
        Map<String, Object> result = new AinpcSemanticContextTools().semanticRoutingSummary();

        assertEquals(1, result.get("schemaVersion"));
        assertEquals("ainpc-mcp-service", result.get("service"));
        assertTrue(result.get("summary").toString().contains("WORLD ="));
        assertEquals(6, result.get("domainCount"));

        List<?> domains = (List<?>) result.get("domains");
        assertEquals(6, domains.size());
        assertTrue(domains.toString().contains("ainpc.semantic.context.summary"));
        assertTrue(domains.toString().contains("WORLD_LORE"));
        assertTrue(domains.toString().contains("ainpc.story.semantic.context.summary"));
        assertTrue(domains.toString().contains("STORY_CONTEXT"));
        assertTrue(domains.toString().contains("ainpc.mapping.semantic.context.summary"));
        assertTrue(domains.toString().contains("MAPPING_CONTEXT"));
        assertTrue(domains.toString().contains("ainpc.quest.semantic.context.summary"));
        assertTrue(domains.toString().contains("QUEST_LORE"));
        assertTrue(domains.toString().contains("ainpc.quest.authoring.context.summary"));
        assertTrue(domains.toString().contains("QUEST_AUTHORING_LORE"));
        assertTrue(domains.toString().contains("ainpc.routing.semantic.context.summary"));
        assertTrue(domains.toString().contains("ROUTING_WORLD"));

        List<?> recommendedOrder = (List<?>) result.get("recommendedOrder");
        assertEquals(List.of(
            "ainpc.semantic.context.summary",
            "ainpc.story.semantic.context.summary",
            "ainpc.mapping.semantic.context.summary",
            "ainpc.quest.semantic.context.summary",
            "ainpc.quest.authoring.context.summary",
            "ainpc.routing.semantic.context.summary"
        ), recommendedOrder);
    }
}
