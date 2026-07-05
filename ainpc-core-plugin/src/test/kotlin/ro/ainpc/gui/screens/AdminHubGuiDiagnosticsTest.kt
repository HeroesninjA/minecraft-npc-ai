package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AdminHubGuiDiagnosticsTest {
    @Test
    fun adminHubShowsAdminActions() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/AdminHubGui.kt").readText()

        assertTrue(source.contains("Admin Panel"))
        assertTrue(source.contains("World"))
        assertTrue(source.contains("Mapping Admin"))
        assertTrue(source.contains("Manager NPC"))
        assertTrue(source.contains("Audit"))
        assertTrue(source.contains("Debug"))
    }

    @Test
    fun adminHubHasDemoAndSave() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/AdminHubGui.kt").readText()

        assertTrue(source.contains("Demo mapping"))
        assertTrue(source.contains("Salveaza mapping"))
        assertTrue(source.contains("ainpc world demo create"))
        assertTrue(source.contains("ainpc world save"))
    }

    @Test
    fun adminHubHasRefreshAndClose() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/AdminHubGui.kt").readText()

        assertTrue(source.contains("Refresh"))
        assertTrue(source.contains("Inchide"))
        assertTrue(source.contains("GuiKey.ADMIN_HUB"))
    }

    @Test
    fun adminHubShowsReadOnlyBanner() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/AdminHubGui.kt").readText()

        assertTrue(source.contains("Read-only activ"))
        assertTrue(source.contains("MCP ruleaza in mod read-only."))
        assertTrue(source.contains("Scrierea in mapping este blocata."))
        assertTrue(source.contains("Inspectia ramane disponibila: status / history / export."))
    }
}
