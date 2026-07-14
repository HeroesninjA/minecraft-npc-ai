package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class WorldPlaceGuiDiagnosticsTest {
    @Test
    fun worldPlaceGuiExposesPlaceDetails() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/WorldPlaceGui.kt").readText()

        assertTrue(source.contains("Regiune parinte"))
        assertTrue(source.contains("Tip place"))
        assertTrue(source.contains("Tags"))
        assertTrue(source.contains("Metadata"))
        assertTrue(source.contains("Delimitare"))
    }

    @Test
    fun worldPlaceGuiHasResidentsAndBindings() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/WorldPlaceGui.kt").readText()

        assertTrue(source.contains("Locuitori"))
        assertTrue(source.contains("NPC Bindings"))
        assertTrue(source.contains("Quest Map"))
        assertTrue(source.contains("Story State"))
    }

    @Test
    fun worldPlaceGuiHasAdminActions() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/WorldPlaceGui.kt").readText()

        assertTrue(source.contains("Bind NPC"))
        assertTrue(source.contains("Delete place"))
        assertTrue(source.contains("Create node"))
        assertTrue(source.contains("Edit place"))
        assertTrue(source.contains("Teleport"))
        assertTrue(source.contains("Inspectie"))
        assertTrue(source.contains("Leaga Un NPC De Acest Place (Home/Work/Social)."))
    }
}
