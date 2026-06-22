package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class PlayerHubGuiDiagnosticsTest {
    @Test
    fun playerHubShowsAllButtons() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/PlayerHubGui.kt").readText()

        assertTrue(source.contains("Progresii"))
        assertTrue(source.contains("NPC"))
        assertTrue(source.contains("Statistici"))
        assertTrue(source.contains("Rutine"))
        assertTrue(source.contains("Shop"))
    }

    @Test
    fun playerHubHasRefreshAndClose() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/PlayerHubGui.kt").readText()

        assertTrue(source.contains("Refresh"))
        assertTrue(source.contains("Inchide"))
        assertTrue(source.contains("GuiKey.MAIN"))
    }

    @Test
    fun playerHubUsesGuiKeyNavigation() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/PlayerHubGui.kt").readText()

        assertTrue(source.contains("GuiKey.QUEST"))
        assertTrue(source.contains("GuiKey.INTERACT"))
        assertTrue(source.contains("GuiKey.STATS"))
        assertTrue(source.contains("GuiKey.ROUTINE"))
        assertTrue(source.contains("GuiKey.SHOP"))
    }
}
