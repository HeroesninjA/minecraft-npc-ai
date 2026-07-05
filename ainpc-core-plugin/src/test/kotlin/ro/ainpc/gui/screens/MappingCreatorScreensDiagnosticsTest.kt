package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class MappingCreatorScreensDiagnosticsTest {
    private val root = "src/main/kotlin/ro/ainpc/gui/screens/"

    @Test
    fun mappingCreatorGui() {
        val src = File(root + "MappingCreatorGui.kt").readText()
        assertTrue(src.contains("Mapping Creator"))
        assertTrue(src.contains("Creaza Regiune"))
        assertTrue(src.contains("Creaza Place"))
        assertTrue(src.contains("Creaza Node"))
        assertTrue(src.contains("Admin Mapping"))
        assertTrue(src.contains("AI Quick Actions"))
        assertTrue(src.contains("Mapping AI create"))
        assertTrue(src.contains("Mapping AI help"))
        assertTrue(src.contains("Mapping AI refine"))
        assertTrue(src.contains("Mapping AI preview"))
        assertTrue(src.contains("World Create/Refine."))
        assertTrue(src.contains("/ainpc world create ai"))
        assertTrue(src.contains("world create ai preview"))
        assertTrue(src.contains("world create ai help"))
        assertTrue(src.contains("Salveaza"))
    }

    @Test
    fun mappingCreateRegionGui() {
        val src = File(root + "MappingCreateRegionGui.kt").readText()
        assertTrue(src.contains("Creaza Regiune") || src.contains("Create Region"))
        assertTrue(src.contains("AI refine region"))
        assertTrue(src.contains("GuiNavigation.addStandardControls") || src.contains("fillEmpty"))
        assertFalse(src.contains("private var selectedType"))
        assertTrue(src.contains("val regionType ="))
    }

    @Test
    fun mappingCreatePlaceGui() {
        val src = File(root + "MappingCreatePlaceGui.kt").readText()
        assertTrue(src.contains("Creaza Place") || src.contains("Create Place"))
        assertTrue(src.contains("AI refine place"))
        assertTrue(src.contains("GuiNavigation.addStandardControls") || src.contains("fillEmpty"))
    }

    @Test
    fun mappingCreateNodeGui() {
        val src = File(root + "MappingCreateNodeGui.kt").readText()
        assertTrue(src.contains("Creaza Node") || src.contains("Create Node"))
        assertTrue(src.contains("AI refine node"))
        assertTrue(src.contains("GuiNavigation.addStandardControls") || src.contains("fillEmpty"))
    }
}
