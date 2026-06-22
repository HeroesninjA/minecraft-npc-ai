package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class WorldRegionGuiDiagnosticsTest {
    @Test
    fun worldRegionGuiExposesRegionDetails() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/WorldRegionGui.kt").readText()

        assertTrue(source.contains("Detalii"))
        assertTrue(source.contains("Tags"))
        assertTrue(source.contains("Delimitare"))
        assertTrue(source.contains("Story"))
        assertTrue(source.contains("Places"))
    }

    @Test
    fun worldRegionGuiHasAdminActions() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/WorldRegionGui.kt").readText()

        assertTrue(source.contains("Teleport"))
        assertTrue(source.contains("Audit"))
        assertTrue(source.contains("Edit region"))
        assertTrue(source.contains("Delete region"))
    }

    @Test
    fun worldRegionGuiNavigatesToPlaces() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/WorldRegionGui.kt").readText()

        assertTrue(source.contains("openPlaceDetail"))
        assertTrue(source.contains("GuiKey.REGION"))
    }

    @Test
    fun worldRegionGuiUsesStandardControls() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/WorldRegionGui.kt").readText()

        assertTrue(source.contains("GuiNavigation.addStandardControls"))
        assertTrue(source.contains("fillEmpty"))
    }
}
