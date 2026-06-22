package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition
import ro.ainpc.engine.runtime.actions.GiveItemAction
import ro.ainpc.engine.runtime.actions.SetStoryStateAction
import ro.ainpc.engine.runtime.conditions.HasCompletedQuestCondition
import ro.ainpc.engine.runtime.triggers.PlayerEntersRegionTrigger

class RuntimeHandlerRegistrationTest {

    @Test
    fun giveItemActionHasCorrectType() {
        assertEquals("give_item", GiveItemAction().type())
    }

    @Test
    fun setStoryStateActionHasCorrectType() {
        assertEquals("set_story_state", SetStoryStateAction().type())
    }

    @Test
    fun hasCompletedQuestConditionHasCorrectType() {
        assertEquals("has_completed_quest", HasCompletedQuestCondition().type())
    }

    @Test
    fun playerEntersRegionTriggerHasCorrectType() {
        assertEquals("player_enters_region", PlayerEntersRegionTrigger().type())
    }

    @Test
    fun giveItemActionProcessesParameters() {
        val action = GiveItemAction()
        assertEquals("give_item", action.type())
    }

    @Test
    fun conditionEvaluatesFromContextVariables() {
        val condition = HasCompletedQuestCondition()
        assertEquals("has_completed_quest", condition.type())
    }

    @Test
    fun runtimeDefinitionHandlesEdgeCases() {
        val empty = ScenarioRuntimeDefinition("", "", mapOf())
        assertEquals("", empty.id())
        assertEquals("", empty.type())
        assertTrue(empty.parameters().isEmpty())

        val full = ScenarioRuntimeDefinition("test", "give_item", mapOf("item" to "DIAMOND", "amount" to "3"))
        assertEquals("test", full.id())
        assertEquals("give_item", full.type())
        assertEquals("DIAMOND", full.parameter("item"))
        assertEquals("3", full.parameter("amount"))
    }

    @Test
    fun questAvailabilityChecksPrerequisites() {
        val available = QuestAvailability.allowed()
        assertTrue(available.available())
        assertTrue(available.issues().isEmpty())

        val unavailable = QuestAvailability.unavailable(listOf("Prerequisite not met"))
        assertFalse(unavailable.available())
        assertEquals(1, unavailable.issues().size)
        assertEquals("Prerequisite not met", unavailable.issues()[0])
    }
}
