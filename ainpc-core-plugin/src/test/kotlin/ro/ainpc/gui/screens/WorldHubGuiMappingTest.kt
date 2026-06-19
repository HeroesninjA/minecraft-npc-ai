package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class WorldHubGuiMappingTest {
    @Test
    fun worldHubGuiExposesMappingDiagnosticsAndDebugDumpShortcut() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/WorldHubGui.kt").readText()

        assertTrue(source.contains("DebugDumpMappingText.buildMappingText(context.plugin())"))
        assertTrue(source.contains("ainpc debugdump mapping"))
        assertTrue(source.contains("Mapping diagnostics"))
    }
}
