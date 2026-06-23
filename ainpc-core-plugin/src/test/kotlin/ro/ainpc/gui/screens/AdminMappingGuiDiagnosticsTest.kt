package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AdminMappingGuiDiagnosticsTest {
    @Test
    fun adminMappingShowsMappingOverview() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/AdminMappingGui.kt").readText()

        assertTrue(source.contains("Admin Mapping"))
        assertTrue(source.contains("Regiuni"))
        assertTrue(source.contains("Places"))
        assertTrue(source.contains("Noduri"))
        assertTrue(source.contains("Status: curat") || source.contains("Status: necesita save"))
    }

    @Test
    fun adminMappingHasAdminActions() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/AdminMappingGui.kt").readText()

        assertTrue(source.contains("Wand"))
        assertTrue(source.contains("Bindings"))
        assertTrue(source.contains("Where am I"))
        assertTrue(source.contains("Demo mapping"))
        assertTrue(source.contains("Salveaza mapping"))
    }

    @Test
    fun adminMappingHasRegionList() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/AdminMappingGui.kt").readText()

        assertTrue(source.contains("openRegionDetail"))
        assertTrue(source.contains("Anterioara"))
        assertTrue(source.contains("Urmatoarea"))
        assertTrue(source.contains("Creaza place"))
        assertTrue(source.contains("Creaza node"))
    }
}
