package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class QuestLogGuiDiagnosticsTest {
    @Test
    fun questLogGuiExposesQuestDiagnosticsAndDebugDumpShortcut() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/QuestLogGui.kt").readText()

        assertTrue(source.contains("DebugDumpQuestText.buildQuestText(plugin)"))
        assertTrue(source.contains("ainpc debugdump quest"))
        assertTrue(source.contains("Quest Diagnostics"))
        assertTrue(source.contains("Quest Generation"))
        assertTrue(source.contains("Quest Entries Active"))
        assertTrue(source.contains("Quest Events Recente"))
        assertTrue(source.contains("Quest Signals"))
        assertTrue(source.contains("Story State"))
        assertTrue(source.contains("buildQuestLogStatusLines"))
        assertTrue(source.contains("Filtru curent"))
        assertTrue(source.contains("Tracked"))
    }
}
