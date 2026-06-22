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
        assertTrue(source.contains("Quest Mapping"))
        assertTrue(source.contains("Ancore"))
    }

    @Test
    fun creatorHubHasDemoAndSave() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/CreatorHubGui.kt").readText()

        assertTrue(source.contains("Demo mapping"))
        assertTrue(source.contains("Salveaza mapping"))
        assertTrue(source.contains("ainpc world demo create"))
        assertTrue(source.contains("ainpc world save"))
    }

    @Test
    fun creatorHubNavigatesToSubmenus() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/CreatorHubGui.kt").readText()

        assertTrue(source.contains("GuiKey.MAPPING_CREATOR"))
        assertTrue(source.contains("GuiKey.CREATOR_QUEST"))
        assertTrue(source.contains("GuiKey.QUEST_MAP"))
    }
}
