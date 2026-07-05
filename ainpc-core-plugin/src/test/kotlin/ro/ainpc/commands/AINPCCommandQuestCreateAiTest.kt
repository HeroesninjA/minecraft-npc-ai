package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AINPCCommandQuestCreateAiTest {
    @Test
    fun questCreateAiSupportsAdvancedPrefillAndSelectionFlow() {
        val source = File("src/main/kotlin/ro/ainpc/commands/AINPCCommandMisc.kt").readText()

        assertTrue(source.contains("AI Quick Actions: /ainpc quest create ai"))
        assertTrue(source.contains("Flow: create/refine prin quest_edit_query, from selection, sau hint-uri explicite."))
        assertTrue(source.contains("AI Quick Actions: /ainpc world create ai"))
        assertTrue(source.contains("Flow: create/refine prin presetul curent sau prin hint-uri explicite."))
        assertTrue(source.contains("shouldOpenAdvancedQuestCreate"))
        assertTrue(source.contains("from selection"))
        assertTrue(source.contains("buildQuestStagePrefill"))
        assertTrue(source.contains("parseQuestObjectivePreset"))
        assertTrue(source.contains("parseQuestRewardPreset"))
        assertTrue(source.contains("objective=collect_item:OAK_LOG:3"))
        assertTrue(source.contains("reward=item:EMERALD:5"))
        assertTrue(source.contains("record_story_event"))
        assertTrue(source.contains("GuiKey.QUEST_CREATE"))
        assertTrue(source.contains("Quest create AI a precompletat formularul avansat."))
        assertTrue(source.contains("quest_stage_"))
        assertTrue(source.contains("stage.index"))
        assertTrue(source.contains("stage_count"))
        assertTrue(source.contains("objective_target"))
        assertTrue(source.contains("quest_reward_type"))
    }
}
