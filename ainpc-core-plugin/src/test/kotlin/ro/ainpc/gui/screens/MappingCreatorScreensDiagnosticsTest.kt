package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class MappingCreatorScreensDiagnosticsTest {
    private val root = "src/main/kotlin/ro/ainpc/gui/screens/"

    @Test
    fun mappingCreatorGui() {
        val src = File(root + "MappingCreatorGui.kt").readText()
        assertTrue(src.contains("Creator Mapping"))
        assertTrue(src.contains("Creaza Regiune"))
        assertTrue(src.contains("Creaza Place"))
        assertTrue(src.contains("Creaza Node"))
        assertTrue(src.contains("Admin Mapping"))
        assertTrue(src.contains("Salveaza"))
    }

    @Test
    fun mappingCreateRegionGui() {
        val src = File(root + "MappingCreateRegionGui.kt").readText()
        assertTrue(src.contains("Creaza Regiune") || src.contains("Create Region"))
        assertTrue(src.contains("GuiNavigation.addStandardControls") || src.contains("fillEmpty"))
    }

    @Test
    fun mappingCreatePlaceGui() {
        val src = File(root + "MappingCreatePlaceGui.kt").readText()
        assertTrue(src.contains("Creaza Place") || src.contains("Create Place"))
        assertTrue(src.contains("GuiNavigation.addStandardControls") || src.contains("fillEmpty"))
    }

    @Test
    fun mappingCreateNodeGui() {
        val src = File(root + "MappingCreateNodeGui.kt").readText()
        assertTrue(src.contains("Creaza Node") || src.contains("Create Node"))
        assertTrue(src.contains("GuiNavigation.addStandardControls") || src.contains("fillEmpty"))
    }
}
