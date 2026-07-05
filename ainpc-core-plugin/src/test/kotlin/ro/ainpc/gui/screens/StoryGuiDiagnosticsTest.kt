package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class StoryGuiDiagnosticsTest {
    @Test
    fun storyGuiExposesStoryDiagnosticsAndDebugDumpShortcut() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/StoryGui.kt").readText()

        assertTrue(source.contains("DebugDumpStoryText.buildStoryText(plugin)"))
        assertTrue(source.contains("ainpc debugdump story"))
        assertTrue(source.contains("Story Diagnostics"))
        assertTrue(source.contains("Progression Verification"))
        assertTrue(source.contains("Mapping Mode"))
        assertTrue(source.contains("Mapping State"))
        assertTrue(source.contains("Mapping Pool"))
        assertTrue(source.contains("Story Context read-only"))
        assertTrue(source.contains("Story Anchors"))
        assertTrue(source.contains("Story Warnings"))
        assertTrue(source.contains("Quest Events"))
        assertTrue(source.contains("Quest Code"))
        assertTrue(source.contains("Quest Status"))
        assertTrue(source.contains("getProgressionGuiSnapshot"))
        assertTrue(source.contains("storyContextService.buildForPlayer"))
    }
}
