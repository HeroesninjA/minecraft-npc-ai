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
        assertTrue(src.contains("creator_quest_map_target"))
        assertTrue(src.contains("creator_quest_log_filter"))
        assertTrue(src.contains("objective:<key>"))
        assertTrue(src.contains("Target curent"))
        assertTrue(src.contains("Filtru curent"))
        assertTrue(src.contains("Query curent"))
    }

    @Test
    fun questCreatorDefinitionsGui() {
        val src = File(root + "QuestCreatorDefinitionsGui.kt").readText()
        assertTrue(src.contains("Definitii Progresie"))
        assertTrue(src.contains("progressionId"))
        assertTrue(src.contains("mechanicId"))
        assertTrue(src.contains("creator_defs_filter"))
        assertTrue(src.contains("Filtru definitii"))
        assertTrue(src.contains("GuiNavigation.addStandardControls"))
    }

    @Test
    fun questCreatorTestGui() {
        val src = File(root + "QuestCreatorTestGui.kt").readText()
        assertTrue(src.contains("Test Quest") || src.contains("Quest Test"))
        assertTrue(src.contains("quest_test_target"))
        assertTrue(src.contains("Force target"))
        assertTrue(src.contains("Quest status"))
        assertTrue(src.contains("quest anchors \$target") || src.contains("ainpc quest anchors "))
    }

    @Test
    fun questEditGui() {
        val src = File(root + "QuestEditGui.kt").readText()
        assertTrue(src.contains("Editeaza Quest") || src.contains("Quest Edit"))
        assertTrue(src.contains("Cauta quest"))
        assertTrue(src.contains("quest_edit_query"))
        assertTrue(src.contains("Status: definitie gasita"))
        assertTrue(src.contains("quest negasit"))
    }

    @Test
    fun questCreateGui() {
        val src = File(root + "QuestCreateGui.kt").readText()
        assertTrue(src.contains("Creeaza Quest") || src.contains("Quest Create"))
        assertTrue(src.contains("Descriere"))
        assertTrue(src.contains("Locatie NPC"))
        assertTrue(src.contains("Status: completabil"))
        assertTrue(src.contains("Lipsesc"))
        assertTrue(src.contains("Next Stage:"))
        assertTrue(src.contains("openTextInput"))
        assertTrue(src.contains("quest_id"))
        assertTrue(src.contains("quest_mechanic"))
        assertTrue(src.contains("quest_base"))
        assertTrue(src.contains("quest_obj_type"))
        assertTrue(src.contains("quest_obj_count"))
        assertTrue(src.contains("quest_stage_"))
        assertTrue(src.contains("quest_reward_type"))
        assertTrue(src.contains("quest_dialog_type"))
        assertTrue(src.contains("quest_dialog_speaker"))
        assertTrue(src.contains("quest_dialog_text"))
        assertTrue(src.contains("quest_obj_dialog"))
        assertTrue(src.contains("quest_system_msg"))
        assertTrue(src.contains("quest_obj_target"))
        assertTrue(src.contains("quest_reward_value"))
        assertTrue(src.contains("quest_reward_count"))
        assertTrue(src.contains("Exporta Draft JSON"))
        assertTrue(src.contains("Sugestii"))
    }
}
