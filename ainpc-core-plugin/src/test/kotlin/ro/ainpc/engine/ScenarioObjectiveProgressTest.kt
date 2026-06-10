package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScenarioObjectiveProgressTest {
    @Test
    fun objectiveTypeAliasesNormalizeToCanonicalTypes() {
        assertEquals("collect_item", normalizeObjectiveType(null))
        assertEquals("collect_item", normalizeObjectiveType("fetch"))
        assertEquals("deliver_to_npc", normalizeObjectiveType("turn-in"))
        assertEquals("talk_to_npc", normalizeObjectiveType("speak_to_npc"))
        assertEquals("visit_region", normalizeObjectiveType("go_to"))
        assertEquals("visit_place", normalizeObjectiveType("go-to-place"))
        assertEquals("inspect_node", normalizeObjectiveType("interact node"))
        assertEquals("kill_mob", normalizeObjectiveType("slay"))
        assertEquals("custom_type", normalizeObjectiveType("custom type"))

        assertTrue(matchesObjectiveType(objective(type = "FETCH"), "collect_item"))
        assertFalse(matchesObjectiveType(null, "collect_item"))
        assertTrue(usesInventoryProgress(objective(type = "deliver_item")))
        assertFalse(usesInventoryProgress(objective(type = "talk")))
        assertTrue(shouldConsumeObjectiveItem(objective(type = "collect")))
    }

    @Test
    fun objectiveKeysPreferStableEntryIdAndKeepLegacyLowercaseFormat() {
        val stableObjective = objective(
            type = "Collect",
            itemId = "OAK_LOG",
            metadata = mapOf("entry_id" to " collect.logs "),
        )

        assertEquals("collect.logs", buildObjectiveKey(stableObjective, 2))
        assertEquals("collect:oak_log:2", buildLegacyObjectiveKey(stableObjective, 2))
        assertEquals(
            listOf("collect.logs", "collect:oak_log:2"),
            objectiveKeyCandidates(stableObjective, 2),
        )

        val legacyObjective = objective(type = "Talk", itemId = "NPC_GUARD")
        assertEquals("talk:npc_guard:1", buildObjectiveKey(legacyObjective, 1))
        assertEquals(listOf("talk:npc_guard:1"), objectiveKeyCandidates(legacyObjective, 1))
        assertEquals("objective:entry:0", buildObjectiveKey(null, 0))
    }

    @Test
    fun readObjectiveProgressUsesMaxNonNegativeCandidateValue() {
        val objective = objective(
            type = "Collect",
            itemId = "OAK_LOG",
            metadata = mapOf("entry_id" to "collect.logs"),
        )
        val progress = mapOf(
            "collect.logs" to -2,
            "collect:oak_log:3" to 5,
        )

        assertEquals(0, readObjectiveProgress(null, objective, 3))
        assertEquals(5, readObjectiveProgress(progress, objective, 3))
    }

    @Test
    fun carryLegacyObjectiveProgressCopiesPositiveLegacyValueOnlyWhenStableMissing() {
        val objective = objective(
            type = "Deliver",
            itemId = "EMERALD",
            metadata = mapOf("entry_id" to "deliver.emerald"),
        )
        val progress = mutableMapOf("deliver:emerald:4" to 2)

        assertTrue(carryLegacyObjectiveProgress(progress, objective, 4))
        assertEquals(2, progress["deliver.emerald"])
        assertFalse(carryLegacyObjectiveProgress(progress, objective, 4))

        val negativeProgress = mutableMapOf("deliver:emerald:4" to -1)
        assertFalse(carryLegacyObjectiveProgress(negativeProgress, objective, 4))
        assertFalse(carryLegacyObjectiveProgress(null, objective, 4))
    }

    @Test
    fun hasObjectiveTypeReturnsTrueWhenMatchingTypeExists() {
        val template = questTemplate("phase1")
        template.setObjectives(listOf(objective(type = "FETCH"), objective(type = "talk")))
        assertTrue(hasObjectiveType(template, "collect_item"))
        assertFalse(hasObjectiveType(template, "kill_mob"))
        assertFalse(hasObjectiveType(null, "collect_item"))
    }

    @Test
    fun hasInventoryObjectiveDetectsInventoryUsingObjectives() {
        val template = questTemplate("phase1")
        template.setObjectives(listOf(objective(type = "FETCH"), objective(type = "talk")))
        assertTrue(hasInventoryObjective(template))
        val talkOnly = questTemplate("phase1")
        talkOnly.setObjectives(listOf(objective(type = "talk")))
        assertFalse(hasInventoryObjective(talkOnly))
        assertFalse(hasInventoryObjective(null))
    }

    @Test
    fun matchesObjectiveReferenceMatchesNormalizedReferences() {
        assertTrue(matchesObjectiveReference("npc_guard", "NPC_GUARD", "village_elder"))
        assertFalse(matchesObjectiveReference("npc_guard", "village_elder"))
        assertFalse(matchesObjectiveReference("", "anything"))
        assertFalse(matchesObjectiveReference(null, "anything"))
    }

    @Test
    fun matchesObjectiveReferenceHandlesPrefixedReferences() {
        assertTrue(matchesObjectiveReference("npc:guard", "npc:guard"))
        assertFalse(matchesObjectiveReference("npc:guard", "guard"))
    }

    @Test
    fun resolveQuestObjectiveStateWithProgressReturnsCompletedWhenProgressMet() {
        val progress = PlayerQuestProgress("t1", "q1", QuestStatus.ACTIVE, 0L, 0L, 0L, "", mapOf("k" to 5), mapOf())
        assertEquals(QuestObjectiveState.COMPLETED, resolveQuestObjectiveState(progress, 5, 5, true))
        assertEquals(QuestObjectiveState.COMPLETED, resolveQuestObjectiveState(progress, 5, 3, true))
    }

    @Test
    fun resolveQuestObjectiveStateWithProgressReturnsPendingWhenNotActiveForStage() {
        val progress = PlayerQuestProgress("t1", "q1", QuestStatus.ACTIVE, 0L, 0L, 0L, "", mapOf("k" to 0), mapOf())
        assertEquals(QuestObjectiveState.PENDING, resolveQuestObjectiveState(progress, 0, 5, false))
    }

    @Test
    fun resolveQuestObjectiveStateWithNullProgressDefaultsToNotStarted() {
        assertEquals(QuestObjectiveState.PENDING, resolveQuestObjectiveState(null as PlayerQuestProgress?, 0, 5, true))
    }

    @Test
    fun resolveQuestObjectiveStateWithStatusCompletedWhenProgressMet() {
        assertEquals(QuestObjectiveState.COMPLETED, resolveQuestObjectiveState(QuestStatus.ACTIVE, 5, 5, true))
        assertEquals(QuestObjectiveState.COMPLETED, resolveQuestObjectiveState(QuestStatus.COMPLETED, 0, 1, true))
    }

    @Test
    fun resolveQuestObjectiveStateWithStatusFailed() {
        assertEquals(QuestObjectiveState.FAILED, resolveQuestObjectiveState(QuestStatus.FAILED, 0, 1, true))
    }

    @Test
    fun resolveQuestObjectiveStateWithStatusPendingWhenNotActiveForStage() {
        assertEquals(QuestObjectiveState.PENDING, resolveQuestObjectiveState(QuestStatus.ACTIVE, 0, 5, false))
        assertEquals(QuestObjectiveState.PENDING, resolveQuestObjectiveState(QuestStatus.NOT_STARTED, 0, 5, true))
        assertEquals(QuestObjectiveState.PENDING, resolveQuestObjectiveState(QuestStatus.OFFERED, 0, 5, true))
        assertEquals(QuestObjectiveState.PENDING, resolveQuestObjectiveState(null as QuestStatus?, 0, 5, true))
    }

    @Test
    fun resolveQuestObjectiveStateShowsInProgressWhenPartialAndActive() {
        assertEquals(QuestObjectiveState.IN_PROGRESS, resolveQuestObjectiveState(QuestStatus.ACTIVE, 3, 10, true))
    }

    @Test
    fun resolveQuestObjectiveStateShowsStartedWhenZeroAndActive() {
        assertEquals(QuestObjectiveState.STARTED, resolveQuestObjectiveState(QuestStatus.ACTIVE, 0, 10, true))
    }

    @Test
    fun shouldShowObjectiveForCurrentStageReturnsTrueForNonStagedTemplate() {
        val template = questTemplate("phase1")
        template.setObjectives(listOf(objective()))
        assertTrue(shouldShowObjectiveForCurrentStage(template, null, objective()))
    }

    @Test
    fun shouldShowObjectiveForCurrentStageReturnsTrueWhenProgressNull() {
        val template = questTemplate("phase1", "phase2")
        template.setObjectives(listOf(objective()))
        assertTrue(shouldShowObjectiveForCurrentStage(template, null, objective()))
    }

    @Test
    fun incrementObjectiveProgressIncrementsFromZero() {
        val progress = mutableMapOf<String, Int>()
        assertTrue(incrementObjectiveProgress(progress, "obj1", 5))
        assertEquals(1, progress["obj1"])
    }

    @Test
    fun incrementObjectiveProgressReturnsFalseWhenAtMaxIncremented() {
        val progress = mutableMapOf("obj1" to 4)
        assertTrue(incrementObjectiveProgress(progress, "obj1", 5))
        assertEquals(5, progress["obj1"])
        assertFalse(incrementObjectiveProgress(progress, "obj1", 5))
        assertEquals(5, progress["obj1"])
    }

    @Test
    fun incrementObjectiveProgressCapsAtObjectiveAmountWhenCurrentExceeds() {
        val progress = mutableMapOf("obj1" to 10)
        assertTrue(incrementObjectiveProgress(progress, "obj1", 5))
        assertEquals(5, progress["obj1"])
    }

    @Test
    fun incrementObjectiveProgressIgnoresNegativeCurrentValue() {
        val progress = mutableMapOf("obj1" to -5)
        assertTrue(incrementObjectiveProgress(progress, "obj1", 5))
        assertEquals(1, progress["obj1"])
    }

    @Test
    fun countMaterialReturnsZeroForNullInputs() {
        assertEquals(0, countMaterial(null, null))
    }

    @Test
    fun removeMaterialHandlesNullInputsGracefully() {
        removeMaterial(null, null, 5)
        removeMaterial(null, null, 0)
        removeMaterial(null, null, -1)
    }

    @Test
    fun buildObjectiveProgressSnapshotReturnsEmptyMapForNullTemplate() {
        assertEquals(emptyMap<String, Int>(), buildObjectiveProgressSnapshot(null, null, null))
    }

    @Test
    fun buildObjectiveProgressSnapshotReturnsEmptyMapForTemplateWithNoObjectives() {
        val template = questTemplate()
        assertEquals(emptyMap<String, Int>(), buildObjectiveProgressSnapshot(null, template, null))
    }

    @Test
    fun buildObjectiveProgressSnapshotDelegatesToFourParamOverload() {
        val template = questTemplate()
        template.setObjectives(listOf(objective(type = "collect", itemId = "log", amount = 5)))
        val result = buildObjectiveProgressSnapshot(null, template, null)
        val expected = buildObjectiveProgressSnapshot(null, template, null, "")
        assertEquals(expected, result)
    }

    @Test
    fun buildCompletedObjectiveProgressReturnsEmptyForNullTemplate() {
        assertEquals(emptyMap<String, Int>(), buildCompletedObjectiveProgress(null, null))
    }

    @Test
    fun cloneStorageContentsReturnsEmptyArrayForNullInventory() {
        assertTrue(cloneStorageContents(null).isEmpty())
    }

    @Test
    fun simulateQuestObjectiveConsumptionHandlesNullInputs() {
        simulateQuestObjectiveConsumption(null, null)
        simulateQuestObjectiveConsumption(null, emptyList())
    }

    @Test
    fun simulateRemoveMaterialHandlesNullContents() {
        simulateRemoveMaterial(null, null, 5)
    }

    @Test
    fun simulateAddMaterialReturnsFalseForNullInputs() {
        assertFalse(simulateAddMaterial(null, null, 5))
    }

    @Test
    fun resolveObjectiveCurrentProgressReturnsZeroForNullObjective() {
        assertEquals(0, resolveObjectiveCurrentProgress(null, null, null, 0))
    }

    @Test
    fun resolveObjectiveCurrentProgressReturnsZeroForNullProgressWhenNotInventory() {
        val objective = objective(type = "talk", itemId = "guard", amount = 5)
        assertEquals(0, resolveObjectiveCurrentProgress(null, objective, null, 0))
    }

    @Test
    fun resolveObjectiveCurrentProgressUsesReadObjectiveProgressForNullPlayer() {
        val objective = objective(type = "collect", itemId = "log", amount = 5)
        val progress = PlayerQuestProgress("t1", "q1", QuestStatus.ACTIVE, 0L, 0L, 0L, "", mapOf("collect:log:0" to 3), mapOf())
        assertEquals(3, resolveObjectiveCurrentProgress(null, objective, progress, 0))
    }

    @Test
    fun inspectQuestInventoryReturnsSuccessForEmptyObjectives() {
        val result = inspectQuestInventory(null, emptyList())
        assertTrue(result.complete())
        assertTrue(result.missingItems().isEmpty())
    }

    @Test
    fun inspectQuestInventoryReportsMissingForNullMaterial() {
        val objective = objective(type = "unknown", itemId = "ghost_material", amount = 5)
        val result = inspectQuestInventory(null, listOf(objective))
        assertFalse(result.complete())
        assertTrue(result.missingItems().any { it.contains("ghost") })
    }

    @Test
    fun resolveObjectiveCurrentProgressCapsAtObjectiveAmount() {
        val objective = objective(type = "collect", itemId = "log", amount = 5)
        val progress = PlayerQuestProgress("t1", "q1", QuestStatus.ACTIVE, 0L, 0L, 0L, "", mapOf("collect:log:0" to 10), mapOf())
        assertEquals(5, resolveObjectiveCurrentProgress(null, objective, progress, 0))
    }

    private fun objective(
        type: String? = "collect_item",
        itemId: String? = "item",
        amount: Int = 1,
        metadata: Map<String, String> = emptyMap(),
    ): FeaturePackLoader.QuestEntryDefinition =
        FeaturePackLoader.QuestEntryDefinition(
            type,
            itemId,
            amount,
            "",
            metadata,
            emptyMap(),
            emptyMap(),
        )

    private fun questTemplate(vararg phases: String): ScenarioEngine.ScenarioTemplate {
        val template = ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.QUEST)
        template.phases.addAll(phases)
        return template
    }
}
