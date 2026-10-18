package ro.ainpc.debug

import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DebugDumpStoryTextTest {
    @Test
    fun statePreviewKeepsTheVisibleLinesFromTheFullStoryDump() {
        val states = JsonObject().apply {
            addProperty("available", true)
            addProperty("region_state_count", 2)
            addProperty("place_state_count", 3)
            addProperty("invalid_json_count", 1)
        }
        val preview = DebugDumpStoryText.buildStoryStatePreviewText(4, states)
        val full = DebugDumpStoryText.buildCapturedStoryText(emptyList(), states, JsonObject(), JsonObject())
        val previewLines = preview.lineSequence().filter { it.isNotBlank() }.take(6).toList()
        val fullLines = full.lineSequence().filter { it.isNotBlank() }.take(6).toList()

        assertEquals(listOf(
            "AINPC Story Dump",
            "Active scenarios: 4",
            "Story states available: true",
            "Region story states: 2",
            "Place story states: 3",
            "Invalid story JSON rows: 1",
        ), previewLines)
        assertEquals(6, preview.lineSequence().count { it.isNotBlank() })
        assertEquals(previewLines.drop(2), fullLines.drop(2))
    }

    @Test
    fun buildsStorySummaryTextFromStateAndEventDumps() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpStoryText.kt").readText()

        assertTrue(source.contains("DebugDumpStoryStateJson.buildStoryStatesJson(plugin)"))
        assertTrue(source.contains("DebugDumpStoryEventJson.buildStoryEventsJson(plugin, gson)"))
        assertTrue(source.contains("buildSummaryText(plugin: AINPCPlugin): String"))
        assertTrue(source.contains("Active scenarios"))
        assertTrue(source.contains("AINPC Story Summary"))
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
