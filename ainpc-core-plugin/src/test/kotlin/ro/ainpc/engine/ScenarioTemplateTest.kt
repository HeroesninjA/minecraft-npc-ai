package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScenarioTemplateTest {

    @Test
    fun templateStoresBaseProperties() {
        val t = ScenarioTemplate(ScenarioType.QUEST)
        t.templateId = "medieval:Q01"
        t.displayName = "Iron Gathering"
        t.description = "Collect iron for the blacksmith"
        t.questCode = "Q01"
        t.questGiverProfession = "blacksmith"
        t.questRepeatable = false

        assertEquals(ScenarioType.QUEST, t.type)
        assertEquals("medieval:Q01", t.templateId)
        assertEquals("Iron Gathering", t.displayName)
        assertEquals("Q01", t.questCode)
        assertEquals("blacksmith", t.questGiverProfession)
        assertFalse(t.questRepeatable)
    }

    @Test
    fun templateManagesRoles() {
        val t = ScenarioTemplate(ScenarioType.QUEST)
        t.addRole("QUEST_GIVER", "NPC who gives the quest")
        t.addRole("HERO", "The player", true)

        assertEquals(2, t.roles.size)
        assertTrue(t.roles.containsKey("QUEST_GIVER"))
        assertTrue(t.roles.containsKey("HERO"))
    }

    @Test
    fun templateManagesPhases() {
        val t = ScenarioTemplate(ScenarioType.QUEST)
        t.addPhase("INTRODUCTION", "Quest intro")
        t.addPhase("ACCEPTANCE", "Quest accepted")
        t.addPhase("COMPLETION", "Quest done")

        assertEquals(3, t.phases.size)
        assertTrue(t.phases.contains("INTRODUCTION"))
    }

    @Test
    fun templateHasQuestBriefingThroughObjective() {
        val t = ScenarioTemplate(ScenarioType.QUEST)
        assertFalse(t.hasQuestBriefing())

        val def = FeaturePackLoader.QuestEntryDefinition(
            "collect_item", "IRON_INGOT", 3, "Collect iron"
        )
        t.objectives = listOf(def)
        assertTrue(t.hasQuestBriefing())
    }

    @Test
    fun templateHasQuestBriefingThroughQuestCode() {
        val t = ScenarioTemplate(ScenarioType.QUEST)
        assertFalse(t.hasQuestBriefing())

        t.questCode = "Q01"
        assertTrue(t.hasQuestBriefing())
    }

    @Test
    fun templateStoresPrerequisites() {
        val t = ScenarioTemplate(ScenarioType.QUEST)
        t.questPrerequisites = mutableListOf("Q01", "Q02")

        assertEquals(2, t.questPrerequisites.size)
        assertTrue(t.questPrerequisites.contains("Q01"))
    }

    @Test
    fun questStageStoresFields() {
        val stage = FeaturePackLoader.QuestStageDefinition(
            "GATHERING", "Collect materials", "all_objectives",
            listOf("collect_iron", "collect_wood"),
            mapOf("next_stage" to "RETURN", "source" to "test")
        )
        assertEquals("GATHERING", stage.id)
        assertEquals("Collect materials", stage.description)
        assertEquals("all_objectives", stage.completionMode)
        assertEquals(2, stage.objectiveIds.size)
        assertEquals("RETURN", stage.getNextStageId())
    }

    @Test
    fun questStageDefaultsCompletionMode() {
        val stage = FeaturePackLoader.QuestStageDefinition(
            "TEST", "Test stage", null, emptyList(), null
        )
        assertEquals("all_objectives", stage.completionMode)
    }
}
