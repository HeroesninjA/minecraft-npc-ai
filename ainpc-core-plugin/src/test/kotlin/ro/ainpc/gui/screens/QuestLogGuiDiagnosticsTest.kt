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
        assertTrue(source.contains("Quest diagnostics"))
        assertTrue(source.contains("buildQuestLogStatusLines"))
        assertTrue(source.contains("Filtru curent"))
        assertTrue(source.contains("Tracked"))
    }
}
