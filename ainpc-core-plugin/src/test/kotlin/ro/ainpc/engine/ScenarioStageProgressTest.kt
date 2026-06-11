package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScenarioStageProgressTest {
    @Test
    fun stageCompletionModeAliasesNormalizeToCanonicalValues() {
        assertEquals("all_objectives", normalizeStageCompletionMode(null))
        assertEquals("all_objectives", normalizeStageCompletionMode("all objectives"))
        assertEquals("any_objective", normalizeStageCompletionMode("any-objectives"))
        assertEquals("manual_turn_in", normalizeStageCompletionMode("turn-in"))
        assertEquals("custom_mode", normalizeStageCompletionMode("custom mode"))
    }

    @Test
    fun phasesMatchUsesReferenceNormalizationAndRejectsBlankInput() {
        assertTrue(phasesMatch("READY_TO_RETURN", "ready-to-return"))
        assertTrue(phasesMatch(" Stage 2 ", "stage_2"))
        assertFalse(phasesMatch("", "stage_2"))
        assertFalse(phasesMatch(null, "stage_2"))
        assertFalse(phasesMatch("stage_1", "stage_2"))
    }

    @Test
    fun stageReferencesObjectiveMatchesEntryIdOrItemId() {
        val stage = stage("collect.logs", "minecraft:emerald")
        val stableObjective = objective(itemId = "OAK_LOG", entryId = "collect.logs")
        val itemObjective = objective(itemId = "EMERALD", entryId = "")

        assertTrue(stageReferencesObjective(stage, stableObjective))
        assertTrue(stageReferencesObjective(stage, itemObjective))
        assertFalse(stageReferencesObjective(stage, objective(itemId = "DIAMOND", entryId = "collect.diamond")))
        assertFalse(stageReferencesObjective(null, stableObjective))
        assertFalse(stageReferencesObjective(stage, null))
    }

    @Test
    fun objectiveListedInAnyStageScansTemplateStages() {
        val template = ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.QUEST)
        template.setQuestStages(listOf(stage("talk.guard"), stage("collect.logs")))

        assertTrue(objectiveListedInAnyStage(template, objective(itemId = "OAK_LOG", entryId = "collect.logs")))
        assertFalse(objectiveListedInAnyStage(template, objective(itemId = "DIAMOND", entryId = "collect.diamond")))
        assertFalse(objectiveListedInAnyStage(null, objective(itemId = "OAK_LOG", entryId = "collect.logs")))
        assertFalse(objectiveListedInAnyStage(template, null))
    }

    private fun stage(vararg objectiveIds: String): FeaturePackLoader.QuestStageDefinition =
        FeaturePackLoader.QuestStageDefinition(
            "stage",
            "",
            "all_objectives",
            objectiveIds.toList(),
            emptyMap(),
        )

    private fun objective(itemId: String, entryId: String): FeaturePackLoader.QuestEntryDefinition =
        FeaturePackLoader.QuestEntryDefinition(
            "collect_item",
            itemId,
            1,
            "",
            if (entryId.isBlank()) emptyMap() else mapOf("entry_id" to entryId),
            emptyMap(),
            emptyMap(),
        )
}
