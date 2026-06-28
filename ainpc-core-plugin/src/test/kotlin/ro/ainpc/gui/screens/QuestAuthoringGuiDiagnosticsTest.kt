package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class QuestAuthoringGuiDiagnosticsTest {
    @Test
    fun questAuthoringGuiExposesAuthoringDiagnosticsAndDumpShortcut() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/QuestAuthoringGui.kt").readText()

        assertTrue(source.contains("DebugDumpAuthoringText.buildAuthoringText(plugin, player, preferredQuestSelector, preferredMechanicId)"))
        assertTrue(source.contains("ainpc authoring dump"))
        assertTrue(source.contains("Authoring diagnostics"))
        assertTrue(source.contains("QUEST_AUTHORING_SUMMARY"))
        assertTrue(source.contains("Quest authoring summary"))
    }
}
