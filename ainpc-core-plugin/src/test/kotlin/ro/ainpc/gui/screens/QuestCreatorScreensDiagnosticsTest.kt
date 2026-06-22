package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class QuestCreatorScreensDiagnosticsTest {
    private val root = "src/main/kotlin/ro/ainpc/gui/screens/"

    @Test
    fun questCreatorGui() {
        val src = File(root + "QuestCreatorGui.kt").readText()
        assertTrue(src.contains("Quest Creator"))
        assertTrue(src.contains("Definitii"))
        assertTrue(src.contains("Authoring"))
        assertTrue(src.contains("Test Quest"))
        assertTrue(src.contains("Quest Editor"))
        assertTrue(src.contains("Creeaza Quest Nou"))
    }

    @Test
    fun questCreatorDefinitionsGui() {
        val src = File(root + "QuestCreatorDefinitionsGui.kt").readText()
        assertTrue(src.contains("Definitii Progresie"))
        assertTrue(src.contains("progressionId"))
        assertTrue(src.contains("mechanicId"))
        assertTrue(src.contains("GuiNavigation.addStandardControls"))
    }

    @Test
    fun questCreatorTestGui() {
        val src = File(root + "QuestCreatorTestGui.kt").readText()
        assertTrue(src.contains("Test Quest") || src.contains("Quest Test"))
    }

    @Test
    fun questEditGui() {
        val src = File(root + "QuestEditGui.kt").readText()
        assertTrue(src.contains("Editeaza Quest") || src.contains("Quest Edit"))
    }

    @Test
    fun questCreateGui() {
        val src = File(root + "QuestCreateGui.kt").readText()
        assertTrue(src.contains("Creeaza Quest") || src.contains("Quest Create"))
    }
}
