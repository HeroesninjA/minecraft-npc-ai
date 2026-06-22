package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AuditGuiDiagnosticsTest {
    @Test
    fun auditGuiHasAllSixAuditModes() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/AuditGui.kt").readText()

        assertTrue(source.contains("ainpc audit "))
        assertTrue(source.contains("\"all\""))
        assertTrue(source.contains("\"npc\""))
        assertTrue(source.contains("\"world\""))
        assertTrue(source.contains("\"db\""))
        assertTrue(source.contains("\"spawn\""))
        assertTrue(source.contains("\"quest\""))
    }

    @Test
    fun auditGuiHasAdminNavigation() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/AuditGui.kt").readText()

        assertTrue(source.contains("Admin Mapping"))
        assertTrue(source.contains("Admin Quest"))
        assertTrue(source.contains("GuiKey.ADMIN_MAPPING"))
        assertTrue(source.contains("GuiKey.ADMIN_QUEST"))
    }

    @Test
    fun auditGuiHasStandardControls() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/AuditGui.kt").readText()

        assertTrue(source.contains("GuiNavigation.addStandardControls"))
        assertTrue(source.contains("GuiKey.AUDIT"))
        assertTrue(source.contains("fillEmpty"))
    }

    @Test
    fun auditGuiUsesRedstoneTorchIcon() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/AuditGui.kt").readText()

        assertTrue(source.contains("REDSTONE_TORCH"))
        assertTrue(source.contains("Audit operational"))
    }
}
