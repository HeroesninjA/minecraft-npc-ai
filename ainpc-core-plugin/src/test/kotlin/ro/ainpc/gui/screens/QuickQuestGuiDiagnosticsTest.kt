package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class QuickQuestGuiDiagnosticsTest {
    @Test
    fun quickQuestGuiUsesTitleCaseDescriptions() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/QuickQuestGui.kt").readText()

        assertTrue(source.contains("Quest Creat Rapid: \$name"))
    }
}
