package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ConfirmActionGuiDiagnosticsTest {
    @Test
    fun confirmActionGuiShowsConfirmation() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/ConfirmActionGui.kt").readText()

        assertTrue(source.contains("Confirmare expirata"))
        assertTrue(source.contains("REDSTONE_BLOCK"))
        assertTrue(source.contains("LIME_CONCRETE"))
        assertTrue(source.contains("RED_CONCRETE"))
    }

    @Test
    fun confirmActionGuiHasConfirmAndCancel() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/ConfirmActionGui.kt").readText()

        assertTrue(source.contains("Confirma"))
        assertTrue(source.contains("Anuleaza"))
        assertTrue(source.contains("runConfirmedCommand"))
        assertTrue(source.contains("returnFromConfirm"))
    }

    @Test
    fun confirmActionGuiHasBackNavigation() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/ConfirmActionGui.kt").readText()

        assertTrue(source.contains("Inapoi"))
        assertTrue(source.contains("GuiKey.MAIN"))
        assertTrue(source.contains("GuiKey.CONFIRM"))
    }
}
