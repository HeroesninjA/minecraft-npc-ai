package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class QuestLogGuiDiagnosticsTest {
    @Test
    fun questLogGuiExposesQuestDiagnosticsAndDebugDumpShortcut() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/QuestLogGui.kt").readText()

        assertTrue(source.contains("DebugDumpQuestText.buildQuestText(context.plugin())"))
        assertTrue(source.contains("ainpc debugdump quest"))
        assertTrue(source.contains("Quest diagnostics"))
    }
}
