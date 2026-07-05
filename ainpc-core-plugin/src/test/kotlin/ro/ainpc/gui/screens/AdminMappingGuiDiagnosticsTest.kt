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
        assertTrue(source.contains("Where Am I"))
        assertTrue(source.contains("Mapping Dump"))
        assertTrue(source.contains("Demo Mapping"))
        assertTrue(source.contains("Salveaza Mapping"))
    }

    @Test
    fun adminMappingHasRegionList() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/AdminMappingGui.kt").readText()

        assertTrue(source.contains("openRegionDetail"))
        assertTrue(source.contains("Anterioara"))
        assertTrue(source.contains("Urmatoarea"))
        assertTrue(source.contains("Creaza Un Place"))
        assertTrue(source.contains("Creaza Un Nod"))
        assertTrue(source.contains("World Mode"))
        assertTrue(source.contains("World Admin"))
        assertTrue(source.contains("Foloseste Butonul Demo Mapping De Mai Sus."))
    }
}
