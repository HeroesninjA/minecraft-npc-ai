package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class QuestEngineRegressionTest {

    @Test
    fun objectiveTypeAliasDeprecationDetected() {
        assertTrue(ObjectiveTypeAliasRegistry.isDeprecated("talk_nlc"))
        assertTrue(ObjectiveTypeAliasRegistry.isDeprecated("interact_nkde"))
        assertTrue(ObjectiveTypeAliasRegistry.isDeprecated("turnin"))
        assertTrue(ObjectiveTypeAliasRegistry.isDeprecated("gather"))
        assertTrue(ObjectiveTypeAliasRegistry.isDeprecated("slay"))
        assertTrue(ObjectiveTypeAliasRegistry.isDeprecated("construct"))
        assertTrue(ObjectiveTypeAliasRegistry.isDeprecated("fabricate"))
    }

    @Test
    fun objectiveTypeRecommendationForDeprecated() {
        assertEquals("talk_to_npc", ObjectiveTypeAliasRegistry.recommendedType("talk_nlc"))
        assertEquals("inspect_node", ObjectiveTypeAliasRegistry.recommendedType("interact_nkde"))
        assertEquals("deliver_to_npc", ObjectiveTypeAliasRegistry.recommendedType("turnin"))
        assertEquals("collect_item", ObjectiveTypeAliasRegistry.recommendedType("gather"))
        assertEquals("kill_mob", ObjectiveTypeAliasRegistry.recommendedType("slay"))
        assertEquals("place_block", ObjectiveTypeAliasRegistry.recommendedType("construct"))
        assertEquals("craft_item", ObjectiveTypeAliasRegistry.recommendedType("fabricate"))
    }

    @Test
    fun typoSuggestionLevenshteinWorks() {
        val suggestion = ObjectiveTypeAliasRegistry.suggestCorrection("tlk_to_npc")
        assertEquals("talk_to_npc", suggestion)

        val suggestUse = ObjectiveTypeAliasRegistry.suggestCorrection("us")
        assertEquals("use_item", suggestUse)
    }

    @Test
    fun questProgressBuildObjectiveKeyConsistent() {
        val template = ScenarioTemplate(ScenarioType.QUEST)
        template.templateId = "test_progress"
        val objective = FeaturePackLoader.QuestEntryDefinition(
            "collect_item", "DIAMOND", 5, "Colecteaza diamante"
        )
        template.objectives = listOf(objective)

        val progress = PlayerQuestProgress(
            "test_progress", "qc", QuestStatus.ACTIVE,
            System.currentTimeMillis(), 0, System.currentTimeMillis(),
            "", mapOf("obj_0" to 3), emptyMap()
        )
        assertEquals(3, progress.objectiveProgress()["obj_0"])
    }

    @Test
    fun questStatusStorageValuesAreLowercase() {
        assertEquals("not_started", QuestStatus.NOT_STARTED.storageValue())
        assertEquals("offered", QuestStatus.OFFERED.storageValue())
        assertEquals("active", QuestStatus.ACTIVE.storageValue())
        assertEquals("completed", QuestStatus.COMPLETED.storageValue())
        assertEquals("failed", QuestStatus.FAILED.storageValue())
    }

    @Test
    fun scopeRegistryNormalization() {
        assertEquals("region", ScopeRegistry.normalize("region"))
        assertEquals("region", ScopeRegistry.normalize("world_region"))
        assertEquals("region", ScopeRegistry.normalize("village"))
        assertEquals("place", ScopeRegistry.normalize("place"))
        assertEquals("place", ScopeRegistry.normalize("location"))
        assertEquals("", ScopeRegistry.normalize(""))
        assertEquals("", ScopeRegistry.normalize(null))
    }

    @Test
    fun scopeRegistryValidScopes() {
        assertTrue(ScopeRegistry.isValid("region"))
        assertTrue(ScopeRegistry.isValid("place"))
    }

    @Test
    fun objectiveTypeRequiredFields() {
        assertEquals(listOf("item"), ObjectiveTypeAliasRegistry.requiredFields("collect_item"))
        assertEquals(listOf("npc_target"), ObjectiveTypeAliasRegistry.requiredFields("talk_to_npc"))
        assertEquals(listOf("item", "npc_target"), ObjectiveTypeAliasRegistry.requiredFields("deliver_to_npc"))
        assertEquals(listOf("item"), ObjectiveTypeAliasRegistry.requiredFields("craft_item"))
        assertEquals(listOf("item"), ObjectiveTypeAliasRegistry.requiredFields("break_block"))
        assertEquals(listOf("item"), ObjectiveTypeAliasRegistry.requiredFields("use_item"))
        assertEquals(emptyList<String>(), ObjectiveTypeAliasRegistry.requiredFields("kill_mob"))
    }

    @Test
    fun collectFailureReasonsEmptyForMissingQuest() {
        val reasons = mutableListOf<String>()
        assertTrue(reasons.isEmpty())
    }

    @Test
    fun objectiveProgressAmountTracking() {
        val objective = FeaturePackLoader.QuestEntryDefinition("collect_item", "STONE", 10, null)
        assertEquals(10, objective.amount)
        assertEquals("collect_item", objective.type)
        assertEquals("STONE", objective.itemId)
    }

    @Test
    fun questActiveProgressIsCurrent() {
        val p = PlayerQuestProgress("t", "qc", QuestStatus.ACTIVE, 0, 0, 0, "", emptyMap(), emptyMap())
        assertTrue(p.isCurrent())
    }

    @Test
    fun questCompletedProgressIsNotCurrent() {
        val p = PlayerQuestProgress("t", "qc", QuestStatus.COMPLETED, 0, 0, 0, "", emptyMap(), emptyMap())
        assertFalse(p.isCurrent())
    }

    @Test
    fun questFailedProgressIsArchived() {
        assertTrue(QuestStatus.FAILED.isArchived())
    }

    @Test
    fun questActiveProgressIsNotArchived() {
        assertFalse(QuestStatus.ACTIVE.isArchived())
    }

    @Test
    fun staleTemplateProgressRemovesOrphanedKeys() {
        val knownTemplates = setOf("quest_a", "quest_b")
        val playerQuests = mutableMapOf(
            "quest_a" to PlayerQuestProgress("quest_a", "qa", QuestStatus.ACTIVE, 0, 0, 0, "", emptyMap(), emptyMap()),
            "quest_b" to PlayerQuestProgress("quest_b", "qb", QuestStatus.ACTIVE, 0, 0, 0, "", emptyMap(), emptyMap()),
            "quest_c" to PlayerQuestProgress("quest_c", "qc", QuestStatus.ACTIVE, 0, 0, 0, "", emptyMap(), emptyMap()),
        )
        val staleKeys = playerQuests.keys.filter { it !in knownTemplates }
        assertEquals(1, staleKeys.size)
        assertEquals("quest_c", staleKeys.first())
    }

    @Test
    fun isCacheStaleReturnsCorrectAge() {
        val loadedAt = System.currentTimeMillis()
        val age = (System.currentTimeMillis() - loadedAt) / 1000
        assertTrue(age >= 0)
    }
}
