package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DebugDumpRoutingTextTest {
    @Test
    fun buildsRoutingSummaryTextFromAllSemanticDumpEntryPoints() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpRoutingText.kt").readText()

        assertTrue(source.contains("AINPC Routing Dump"))
        assertTrue(source.contains("AINPC Routing Summary"))
        assertTrue(source.contains("ROUTING_WORLD -> ainpc.semantic.context / ainpc.semantic.context.summary"))
        assertTrue(source.contains("ROUTING_STORY -> ainpc.story.semantic.context / ainpc.story.semantic.context.summary"))
        assertTrue(source.contains("ROUTING_MAPPING -> ainpc.mapping.semantic.context / ainpc.mapping.semantic.context.summary"))
        assertTrue(source.contains("ROUTING_QUEST -> ainpc.quest.semantic.context / ainpc.quest.semantic.context.summary"))
        assertTrue(source.contains("ROUTING_QUEST_AUTHORING -> ainpc.quest.authoring.context.summary"))
        assertTrue(source.contains("WORLD = lore/history/npc"))
        assertTrue(source.contains("STORY = context/history/signals"))
        assertTrue(source.contains("MAPPING = context/history/story links"))
        assertTrue(source.contains("QUEST = lore/history/signals"))
        assertTrue(source.contains("QUEST_AUTHORING = selector/seed/history/signals"))
        assertTrue(source.contains("Recommended order: ainpc.semantic.context.summary -> ainpc.story.semantic.context.summary -> ainpc.mapping.semantic.context.summary -> ainpc.quest.semantic.context.summary -> ainpc.quest.authoring.context.summary -> ainpc.routing.semantic.context.summary"))
        assertTrue(source.contains("MCP runtime:"))
        assertTrue(source.contains("ainpc.feature.state"))
        assertTrue(source.contains("Semantic exports:"))
    }
}
