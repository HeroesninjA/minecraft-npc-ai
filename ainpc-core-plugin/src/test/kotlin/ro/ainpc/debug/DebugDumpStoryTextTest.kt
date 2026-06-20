package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DebugDumpStoryTextTest {
    @Test
    fun buildsStorySummaryTextFromStateAndEventDumps() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpStoryText.kt").readText()

        assertTrue(source.contains("DebugDumpStoryStateJson.buildStoryStatesJson(plugin)"))
        assertTrue(source.contains("DebugDumpStoryEventJson.buildStoryEventsJson(plugin, gson)"))
        assertTrue(source.contains("Active scenarios"))
        assertTrue(source.contains("scenarios.values.toList()"))
        assertTrue(source.contains("Progression cross-link available:"))
        assertTrue(source.contains("Story event progression links:"))
        assertTrue(source.contains("Story progression gaps:"))
        assertTrue(source.contains("Regions by mode"))
        assertTrue(source.contains("Places by source"))
        assertTrue(source.contains("Events by type"))
        assertTrue(source.contains("Events by quest code"))
    }
}
