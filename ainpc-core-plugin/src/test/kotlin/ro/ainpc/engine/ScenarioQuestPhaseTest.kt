package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScenarioQuestPhaseTest {

    @Test
    fun resolveQuestPhaseEmptyTemplateOrStatus() {
        assertEquals("", resolveQuestPhase(null, null as QuestStatus?, null as String?, mapOf<String, Int>()))
        assertEquals("", resolveQuestPhase(null, null as QuestStatus?, null as PlayerQuestProgress?))
    }

    @Test
    fun resolveQuestPhaseNotStartedReturnsEmpty() {
        val template = questTemplate("intro", "gather", "return")
        assertEquals("", resolveQuestPhase(template, QuestStatus.NOT_STARTED, null as String?, mapOf<String, Int>()))
    }

    @Test
    fun resolveQuestPhaseOfferedReturnsFirstPhase() {
        val template = questTemplate("intro", "gather", "return")
        assertEquals("intro", resolveQuestPhase(template, QuestStatus.OFFERED, null as String?, mapOf<String, Int>()))
    }

    @Test
    fun resolveQuestPhaseOfferedPreservesExistingPhase() {
        val template = questTemplate("intro", "gather", "return")
        assertEquals("custom_phase", resolveQuestPhase(template, QuestStatus.OFFERED, "custom_phase", mapOf<String, Int>()))
    }

    @Test
    fun resolveQuestPhaseActiveWithObjectivesNotYetSatisfiedReturnsWorkPhase() {
        val template = questTemplate("intro", "gather", "return")
        template.setObjectives(listOf(objective("item", "LOG", 3)))
        assertEquals("gather", resolveQuestPhase(template, QuestStatus.ACTIVE, "", mapOf<String, Int>()))
    }

    @Test
    fun resolveQuestPhaseActiveWithCompletedObjectivesReturnsReadyPhase() {
        val template = questTemplate("intro", "gather", "return")
        template.setObjectives(listOf(objective("item", "LOG", 1)))
        assertEquals("return", resolveQuestPhase(template, QuestStatus.ACTIVE, "", mapOf("item:log:0" to 1)))
    }

    @Test
    fun resolveQuestPhaseActiveWithStagesAdvancesCorrectly() {
        val template = questTemplate("intro", "stage1", "stage2", "return")
        template.setObjectives(listOf(
            objectiveWithStage("item", "WOOD", 1, "stage1"),
            objectiveWithStage("item", "IRON", 1, "stage2"),
        ))
        template.setQuestStages(listOf(
            FeaturePackLoader.QuestStageDefinition("stage1", "", "all_objectives", listOf("stage1_obj"), emptyMap()),
            FeaturePackLoader.QuestStageDefinition("stage2", "", "all_objectives", listOf("stage2_obj"), emptyMap()),
        ))

        assertEquals("stage2", resolveQuestPhase(template, QuestStatus.ACTIVE, "stage1", mapOf("stage1_obj" to 1)))
    }

    @Test
    fun resolveQuestPhaseCompletedReturnsLastPhase() {
        val template = questTemplate("intro", "gather", "completion")
        assertEquals("completion", resolveQuestPhase(template, QuestStatus.COMPLETED, "", mapOf<String, Int>()))
    }

    @Test
    fun resolveQuestPhaseFailedReturnsExistingOrDefault() {
        val template = questTemplate("intro", "gather")
        assertEquals("gather", resolveQuestPhase(template, QuestStatus.FAILED, "", mapOf<String, Int>()))
    }

    @Test
    fun resolveQuestPhaseFailedPreservesExisting() {
        val template = questTemplate("intro", "gather")
        assertEquals("failed_phase", resolveQuestPhase(template, QuestStatus.FAILED, "failed_phase", mapOf<String, Int>()))
    }

    @Test
    fun hasStagedObjectivesDetectsStageMetadata() {
        val template = questTemplate("intro", "stage1")
        template.setObjectives(listOf(objective("item", "LOG", 1)))
        assertFalse(hasStagedObjectives(template))
        assertFalse(hasStagedObjectives(null))

        template.setObjectives(listOf(objectiveWithStage("item", "LOG", 1, "stage1")))
        assertTrue(hasStagedObjectives(template))
    }

    @Test
    fun hasStagedObjectivesDetectsExplicitStageIds() {
        val template = questTemplate("intro", "stage1")
        template.setObjectives(listOf(objective("item", "LOG", 1)))
        template.setQuestStages(listOf(
            FeaturePackLoader.QuestStageDefinition("stage1", "", "all_objectives", listOf("obj1"), emptyMap()),
        ))
        assertTrue(hasStagedObjectives(template))
    }

    @Test
    fun getObjectiveStageExtractsFromMetadataAndVariables() {
        val objWithStage = objectiveWithStage("item", "LOG", 1, "gather_stage")
        assertEquals("gather_stage", getObjectiveStage(objWithStage))

        val objViaVariables = FeaturePackLoader.QuestEntryDefinition(
            "item", "IRON", 1, "",
            emptyMap(), mapOf("stage_id" to "smith_stage"), emptyMap(),
        )
        assertEquals("smith_stage", getObjectiveStage(objViaVariables))

        assertEquals("", getObjectiveStage(null))
    }

    @Test
    fun getOrderedObjectiveStagesReturnsUniqueStages() {
        val template = questTemplate("intro", "s1", "s2", "s3")
        template.setObjectives(listOf(
            objectiveWithStage("item", "WOOD", 1, "s1"),
            objectiveWithStage("item", "IRON", 1, "s2"),
            objectiveWithStage("item", "GOLD", 1, "s2"),
            objectiveWithStage("item", "DIAMOND", 1, "s3"),
        ))

        val stages = getOrderedObjectiveStages(template)
        assertEquals(listOf("s1", "s2", "s3"), stages)
        assertEquals(emptyList<String>(), getOrderedObjectiveStages(null))
    }

    @Test
    fun findMatchingObjectiveStageMatchesByPhasesMatch() {
        val template = questTemplate("intro", "stage_1", "stage_2")
        template.setObjectives(listOf(
            objectiveWithStage("item", "WOOD", 1, "stage_1"),
            objectiveWithStage("item", "IRON", 1, "stage_2"),
        ))

        assertEquals("stage_1", findMatchingObjectiveStage(template, "Stage 1"))
        assertEquals("", findMatchingObjectiveStage(template, ""))
        assertEquals("", findMatchingObjectiveStage(null, "stage_1"))
    }

    @Test
    fun findFirstIncompleteObjectiveStageReturnsFirstUnfinished() {
        val template = questTemplate("intro", "s1", "s2")
        template.setObjectives(listOf(
            objectiveWithStage("item", "WOOD", 1, "s1"),
            objectiveWithStage("item", "IRON", 1, "s2"),
        ))

        assertEquals("s1", findFirstIncompleteObjectiveStage(template, mapOf<String, Int>()))
        assertEquals("s2", findFirstIncompleteObjectiveStage(template, mapOf("s1_obj" to 1)))
        assertEquals("", findFirstIncompleteObjectiveStage(template, mapOf("s1_obj" to 1, "s2_obj" to 1)))
    }

    @Test
    fun areObjectivesSatisfiedForStageWithAllObjectivesMode() {
        val template = questTemplate("intro", "s1")
        template.setObjectives(listOf(objectiveWithStage("item", "WOOD", 2, "s1")))
        template.setQuestStages(listOf(
            FeaturePackLoader.QuestStageDefinition("s1", "", "all_objectives", listOf("obj1"), emptyMap()),
        ))

        assertFalse(areObjectivesSatisfiedForStage(template, "s1", mapOf("s1_obj" to 1)))
        assertTrue(areObjectivesSatisfiedForStage(template, "s1", mapOf("s1_obj" to 2)))
    }

    @Test
    fun areObjectivesSatisfiedForStageWithAnyObjectiveMode() {
        val template = questTemplate("intro", "s1")
        template.setObjectives(listOf(
            objectiveWithStage("item", "WOOD", 3, "s1"),
            objectiveWithStage("item", "STONE", 5, "s1"),
        ))
        template.setQuestStages(listOf(
            FeaturePackLoader.QuestStageDefinition("s1", "", "any_objective", listOf("obj1"), emptyMap()),
        ))

        assertFalse(areObjectivesSatisfiedForStage(template, "s1", mapOf<String, Int>()))
        assertTrue(areObjectivesSatisfiedForStage(template, "s1", mapOf("s1_obj" to 3)))
    }

    @Test
    fun isObjectiveActiveForPhaseWithStagedObjectives() {
        val template = questTemplate("intro", "s1", "s2")
        template.setObjectives(listOf(
            objectiveWithStage("item", "WOOD", 1, "s1"),
            objectiveWithStage("item", "IRON", 1, "s2"),
        ))

        val obj1 = FeaturePackLoader.QuestEntryDefinition("item", "WOOD", 1, "", mapOf("entry_id" to "s1_obj", "stage_id" to "s1"), emptyMap(), emptyMap())
        val obj2 = FeaturePackLoader.QuestEntryDefinition("item", "IRON", 1, "", mapOf("entry_id" to "s2_obj", "stage_id" to "s2"), emptyMap(), emptyMap())

        assertTrue(isObjectiveActiveForPhase(template, "s1", obj1))
        assertFalse(isObjectiveActiveForPhase(template, "s1", obj2))
    }

    @Test
    fun isObjectiveActiveForPhaseWithoutStagesAlwaysTrue() {
        val template = questTemplate("intro", "phase1")
        assertTrue(isObjectiveActiveForPhase(template, "phase1",
            FeaturePackLoader.QuestEntryDefinition("item", "LOG", 1, "")))
    }

    @Test
    fun canonicalQuestPhaseMatchesAgainstTemplatePhases() {
        val template = questTemplate("gather_resources", "return_to_village", "complete_quest")
        assertEquals("gather_resources", canonicalQuestPhase(template, "Gather Resources"))
        assertEquals("return_to_village", canonicalQuestPhase(template, "Return to Village"))
        assertEquals("unknown_phase", canonicalQuestPhase(template, "unknown_phase"))
    }

    @Test
    fun canonicalQuestPhaseWithNullTemplateReturnsTrimmedInput() {
        assertEquals("something", canonicalQuestPhase(null, "  something  "))
        assertEquals("", canonicalQuestPhase(null, null))
    }

    @Test
    fun getFirstQuestPhaseReturnsFirst() {
        assertEquals("", getFirstQuestPhase(null))
        assertEquals("a", getFirstQuestPhase(questTemplate("a", "b")))
    }

    @Test
    fun getDefaultActiveQuestPhaseReturnsSecondOrFirst() {
        assertEquals("", getDefaultActiveQuestPhase(null))
        assertEquals("only", getDefaultActiveQuestPhase(questTemplate("only")))
        assertEquals("b", getDefaultActiveQuestPhase(questTemplate("a", "b", "c")))
    }

    @Test
    fun getLastQuestPhaseReturnsLast() {
        assertEquals("", getLastQuestPhase(null))
        assertEquals("c", getLastQuestPhase(questTemplate("a", "b", "c")))
    }

    @Test
    fun getQuestWorkPhaseFindsSemanticPhase() {
        val template = questTemplate("intro", "gather_wood", "return")
        assertEquals("gather_wood", getQuestWorkPhase(template))
    }

    @Test
    fun getQuestWorkPhaseFallsBackToSecondPhase() {
        val template = questTemplate("intro", "middle", "completion")
        assertEquals("middle", getQuestWorkPhase(template))
    }

    @Test
    fun getReadyToTurnInQuestPhaseFindsReturnPhase() {
        val template = questTemplate("intro", "gather", "return_to_base", "completion")
        assertEquals("return_to_base", getReadyToTurnInQuestPhase(template))
    }

    @Test
    fun getReadyToTurnInQuestPhaseFallsBackToLastNonCompletionPhase() {
        val template = questTemplate("intro", "gather", "completion")
        assertEquals("gather", getReadyToTurnInQuestPhase(template))
    }

    @Test
    fun isQuestIntroOrAcceptancePhaseDetectsKeywords() {
        assertTrue(isQuestIntroOrAcceptancePhase("quest_intro"))
        assertTrue(isQuestIntroOrAcceptancePhase("OFFER_PHASE"))
        assertTrue(isQuestIntroOrAcceptancePhase("acceptance"))
        assertFalse(isQuestIntroOrAcceptancePhase("gather_wood"))
        assertFalse(isQuestIntroOrAcceptancePhase(null))
    }

    @Test
    fun isQuestReadyOrTerminalPhaseDetectsKeywords() {
        assertTrue(isQuestReadyOrTerminalPhase("return_to_village"))
        assertTrue(isQuestReadyOrTerminalPhase("TURN_IN"))
        assertTrue(isQuestReadyOrTerminalPhase("report_back"))
        assertTrue(isQuestReadyOrTerminalPhase("completion"))
        assertFalse(isQuestReadyOrTerminalPhase("gather_wood"))
    }

    @Test
    fun isQuestCompletionPhaseDetectsKeywords() {
        assertTrue(isQuestCompletionPhase("completion"))
        assertTrue(isQuestCompletionPhase("COMPLETE_PHASE"))
        assertTrue(isQuestCompletionPhase("final_stage"))
        assertTrue(isQuestCompletionPhase("resolution"))
        assertFalse(isQuestCompletionPhase("gather"))
        assertFalse(isQuestCompletionPhase(null))
    }

    @Test
    fun areObjectivesSatisfiedChecksAll() {
        val template = questTemplate("intro", "gather")
        template.setObjectives(listOf(
            FeaturePackLoader.QuestEntryDefinition("item", "LOG", 3, ""),
            FeaturePackLoader.QuestEntryDefinition("item", "STONE", 5, ""),
        ))

        assertFalse(areObjectivesSatisfied(template, mapOf("item:log:0" to 2, "item:stone:1" to 5)))
        assertTrue(areObjectivesSatisfied(template, mapOf("item:log:0" to 3, "item:stone:1" to 5)))
    }

    @Test
    fun areObjectivesSatisfiedNullTemplateReturnsTrue() {
        assertTrue(areObjectivesSatisfied(null, mapOf<String, Int>()))
    }

    @Test
    fun areObjectivesSatisfiedWithEmptyObjectivesAndEmptyProgressReturnsTrue() {
        val template = questTemplate("intro", "gather")
        assertTrue(areObjectivesSatisfied(template, mapOf<String, Int>()))
    }

    @Test
    fun resolveQuestPhaseWithPlayerQuestProgress() {
        val template = questTemplate("intro", "gather", "return")
        template.setObjectives(listOf(objective("item", "LOG", 1)))

        val progress = PlayerQuestProgress("test", "", QuestStatus.ACTIVE, 0L, 0L, 0L, "", mapOf("item:log:0" to 1), emptyMap())
        assertEquals("return", resolveQuestPhase(template, progress.status(), progress))
    }

    @Test
    fun getFirstObjectiveStageReturnsFirstStage() {
        val template = questTemplate("intro", "s1", "s2")
        template.setObjectives(listOf(
            objectiveWithStage("item", "WOOD", 1, "s1"),
            objectiveWithStage("item", "IRON", 1, "s2"),
        ))
        assertEquals("s1", getFirstObjectiveStage(template))
        assertEquals("", getFirstObjectiveStage(null))
    }

    @Test
    fun getLastObjectiveStageReturnsLast() {
        val template = questTemplate("intro", "s1", "s2")
        template.setObjectives(listOf(
            objectiveWithStage("item", "WOOD", 1, "s1"),
            objectiveWithStage("item", "IRON", 1, "s2"),
        ))
        assertEquals("s2", getLastObjectiveStage(template))
    }

    @Test
    fun findQuestStageMatchesById() {
        val stage = FeaturePackLoader.QuestStageDefinition("stage_1", "desc", "all_objectives", listOf("obj1"), emptyMap())
        val template = questTemplate("intro")
        template.setObjectives(listOf(objective("item", "LOG", 1)))
        template.setQuestStages(listOf(stage))

        assertEquals(stage, findQuestStage(template, "stage_1"))
        assertEquals(stage, findQuestStage(template, "Stage 1"))
        assertEquals(null, findQuestStage(template, "nonexistent"))
        assertEquals(null, findQuestStage(null, "stage_1"))
    }

    @Test
    fun stageCompletionModeReadsFromStage() {
        val template = questTemplate("intro", "s1")
        template.setQuestStages(listOf(
            FeaturePackLoader.QuestStageDefinition("s1", "", "any_objective", listOf("obj1"), emptyMap()),
        ))

        assertEquals("any_objective", stageCompletionMode(template, "s1"))
        assertEquals("all_objectives", stageCompletionMode(template, "unknown"))
        assertEquals("all_objectives", stageCompletionMode(null, "s1"))
    }

    @Test
    fun hasExplicitStageObjectiveIdsDetectsPopulatedStages() {
        assertFalse(hasExplicitStageObjectiveIds(null))
        val template = questTemplate("intro")
        template.setObjectives(listOf(objective("item", "LOG", 1)))
        assertFalse(hasExplicitStageObjectiveIds(template))
        template.setQuestStages(listOf(
            FeaturePackLoader.QuestStageDefinition("s1", "", "all_objectives", listOf("obj1"), emptyMap()),
        ))
        assertTrue(hasExplicitStageObjectiveIds(template))
    }

    @Test
    fun resolveQuestPhaseActiveChoosesWorkPhaseWhenObjectivesNotSatisfied() {
        val template = questTemplate("intro", "journey", "return")
        template.setObjectives(listOf(objective("item", "GOLD", 1)))
        assertEquals("journey", resolveQuestPhase(template, QuestStatus.ACTIVE, "", mapOf<String, Int>()))
    }

    @Test
    fun resolveQuestPhaseActiveSwitchesToReadyWhenAllSatisfied() {
        val template = questTemplate("intro", "hunt", "report")
        template.setObjectives(listOf(objective("item", "BONE", 1)))
        assertEquals("report", resolveQuestPhase(template, QuestStatus.ACTIVE, "hunt", mapOf("item:bone:0" to 1)))
    }

    private fun questTemplate(vararg phases: String): ScenarioEngine.ScenarioTemplate {
        val template = ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.QUEST)
        template.phases.addAll(phases)
        return template
    }

    private fun objective(type: String, itemId: String, amount: Int): FeaturePackLoader.QuestEntryDefinition =
        FeaturePackLoader.QuestEntryDefinition(type, itemId, amount, "", emptyMap(), emptyMap(), emptyMap())

    private fun objectiveWithStage(type: String, itemId: String, amount: Int, stage: String): FeaturePackLoader.QuestEntryDefinition =
        FeaturePackLoader.QuestEntryDefinition(type, itemId, amount, "", mapOf("entry_id" to "${stage}_obj"), mapOf("stage_id" to stage), emptyMap())
}
