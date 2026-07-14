package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class QuestMapGuiDiagnosticsTest {
    @Test
    fun questMapHasMechanicView() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/QuestMapGui.kt").readText()

        assertTrue(source.contains("Quest Mapping"))
        assertTrue(source.contains("renderMechanics"))
        assertTrue(source.contains("renderDefinitions"))
        assertTrue(source.contains("renderObjectives"))
        assertTrue(source.contains("renderDetail"))
    }

    @Test
    fun questMapHasBindingActions() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/QuestMapGui.kt").readText()

        assertTrue(source.contains("saveAnchorBinding"))
        assertTrue(source.contains("Delete binding"))
        assertTrue(source.contains("Edit binding"))
        assertTrue(source.contains("Listeaza places"))
    }

    @Test
    fun questMapHasModeToggle() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/QuestMapGui.kt").readText()

        assertTrue(source.contains("GLOBAL"))
        assertTrue(source.contains("PERSONAL"))
        assertTrue(source.contains("toggleQuestMapGlobalMode"))
    }
}
