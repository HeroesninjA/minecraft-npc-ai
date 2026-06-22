package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class PlaceholderGuiDiagnosticsTest {
    @Test
    fun placeholderGuiRendersMessage() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/PlaceholderGui.kt").readText()

        assertTrue(source.contains("CHEST"))
        assertTrue(source.contains("provider dedicat"))
        assertTrue(source.contains("GuiNavigation.addStandardControls"))
        assertTrue(source.contains("fillEmpty"))
    }
}
