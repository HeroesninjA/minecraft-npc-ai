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
        assertTrue(source.contains("Story diagnostics"))
        assertTrue(source.contains("Progression verification"))
        assertTrue(source.contains("getProgressionGuiSnapshot"))
        assertTrue(source.contains("storyContextService.buildForPlayer"))
    }
}
