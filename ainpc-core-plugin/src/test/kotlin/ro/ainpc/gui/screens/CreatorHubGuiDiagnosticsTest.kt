package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class CreatorHubGuiDiagnosticsTest {
    @Test
    fun creatorHubShowsCreatorActions() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/CreatorHubGui.kt").readText()

        assertTrue(source.contains("Creator Tools"))
        assertTrue(source.contains("Creator Mapping"))
        assertTrue(source.contains("Quest Creator"))
        assertTrue(source.contains("Quest Overview"))
        assertTrue(source.contains("Quest Authoring"))
        assertTrue(source.contains("Quest AI"))
        assertTrue(source.contains("Quest Mapping"))
        assertTrue(source.contains("Ancore"))
    }

    @Test
    fun creatorHubHasDemoAndSave() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/CreatorHubGui.kt").readText()

        assertTrue(source.contains("Demo mapping"))
        assertTrue(source.contains("Salveaza mapping"))
        assertTrue(source.contains("AI Quick Actions"))
        assertTrue(source.contains("Mapping AI create"))
        assertTrue(source.contains("Mapping AI help"))
        assertTrue(source.contains("World AI refine"))
        assertTrue(source.contains("Quest AI refine selection"))
        assertTrue(source.contains("/ainpc world create ai region"))
        assertTrue(source.contains("/ainpc quest create ai from selection"))
        assertTrue(source.contains("Quest Draft Preview"))
        assertTrue(source.contains("Mapping AI preview"))
        assertTrue(source.contains("Quest Editor"))
        assertTrue(source.contains("Deschide editorul cu lista de definitii."))
        assertTrue(source.contains("deschide quest editor"))
        assertTrue(source.contains("/ainpc quest preview"))
        assertTrue(source.contains("/ainpc world create ai preview region"))
        assertTrue(source.contains("GuiKey.QUEST_EDIT"))
        assertTrue(source.contains("/ainpc world create ai"))
        assertTrue(source.contains("ainpc world demo create"))
        assertTrue(source.contains("ainpc world save"))
    }

    @Test
    fun creatorHubNavigatesToSubmenus() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/CreatorHubGui.kt").readText()

        assertTrue(source.contains("GuiKey.MAPPING_CREATOR"))
        assertTrue(source.contains("ainpc quest create ai"))
        assertTrue(source.contains("GuiKey.CREATOR_QUEST"))
        assertTrue(source.contains("GuiKey.QUEST_MAP"))
    }
}
