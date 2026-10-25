package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NPCState

class ActionEligibilityTest {
    @Test
    fun `idle npc without target or destinations cannot speak or start work`() {
        val situation = ActionSituation(false, false, false)
        val eligible = DecisionEngine.eligibleActions(NPCState.IDLE, false, situation)

        assertFalse(NPCAction.GREET in eligible)
        assertFalse(NPCAction.SOCIALIZE in eligible)
        assertFalse(NPCAction.START_WORK in eligible)
        assertTrue(NPCAction.OBSERVE in eligible)
        assertTrue(NPCAction.REST in eligible)
    }

    @Test
    fun `nearby interlocutor enables direct speech`() {
        val situation = ActionSituation(true, false, false)
        val eligible = DecisionEngine.eligibleActions(NPCState.TALKING, false, situation)

        assertTrue(NPCAction.TALK in eligible)
        assertTrue(NPCAction.TELL_STORY in eligible)
        assertTrue(NPCAction.COMPLIMENT in eligible)
        assertTrue(ActionEligibility.permits(NPCAction.GREET, situation))
    }

    @Test
    fun `conversation without interlocutor can end`() {
        val situation = ActionSituation(false, false, false)
        val eligible = DecisionEngine.eligibleActions(NPCState.TALKING, false, situation)

        assertTrue(eligible == listOf(NPCAction.DO_NOTHING))
    }

    @Test
    fun `known destinations enable socializing and work without a nearby target`() {
        val situation = ActionSituation(false, true, true)

        assertTrue(ActionEligibility.permits(NPCAction.SOCIALIZE, situation))
        assertTrue(ActionEligibility.permits(NPCAction.START_WORK, situation))
        assertTrue(ActionEligibility.permits(NPCAction.CONTINUE_WORK, situation))
        assertFalse(ActionEligibility.permits(NPCAction.GREET, situation))
    }

    @Test
    fun `emergency options remain available without social target or workplace`() {
        val situation = ActionSituation(false, false, false)
        val eligible = DecisionEngine.eligibleActions(NPCState.IDLE, true, situation)

        assertTrue(NPCAction.FLEE in eligible)
        assertTrue(NPCAction.CALL_HELP in eligible)
    }

    @Test
    fun `hungry and tired npc at home can eat and sleep`() {
        val situation = ActionSituation(false, false, false, isAtHome = true, hungerLevel = 20, energyLevel = 20)
        val eligible = DecisionEngine.eligibleActions(NPCState.IDLE, false, situation)

        assertTrue(NPCAction.EAT in eligible)
        assertTrue(NPCAction.SLEEP in eligible)
        assertTrue(NPCAction.EAT in DecisionEngine.eligibleActions(NPCState.WORKING, false, situation))
        assertFalse(NPCAction.EAT in DecisionEngine.eligibleActions(NPCState.IDLE, true, situation))
        assertFalse(NPCAction.SLEEP in DecisionEngine.eligibleActions(NPCState.TALKING, false, situation))
        assertFalse(NPCAction.EAT in DecisionEngine.eligibleActions(NPCState.PANICKING, false, situation))
    }

    @Test
    fun `basic care is unavailable away from home or above need thresholds`() {
        val away = ActionSituation(false, false, false, hungerLevel = 10, energyLevel = 10)
        val recovered = away.copy(isAtHome = true, hungerLevel = 35, energyLevel = 35)

        for (situation in listOf(away, recovered)) {
            val eligible = DecisionEngine.eligibleActions(NPCState.WORKING, false, situation)
            assertFalse(NPCAction.EAT in eligible)
            assertFalse(NPCAction.SLEEP in eligible)
        }
    }

    @Test
    fun `care continues until recovery threshold without competing actions`() {
        val eating = ActionSituation(false, false, false, isAtHome = true, hungerLevel = 45,
            currentState = NPCState.EATING)
        val sleeping = eating.copy(energyLevel = 45, currentState = NPCState.SLEEPING)

        assertEquals(listOf(NPCAction.EAT), DecisionEngine.eligibleActions(NPCState.EATING, false, eating))
        assertEquals(listOf(NPCAction.SLEEP), DecisionEngine.eligibleActions(NPCState.SLEEPING, false, sleeping))
        assertFalse(NPCAction.EAT in DecisionEngine.eligibleActions(NPCState.EATING, false,
            eating.copy(hungerLevel = 60)))
        assertFalse(NPCAction.SLEEP in DecisionEngine.eligibleActions(NPCState.SLEEPING, false,
            sleeping.copy(energyLevel = 60)))
        assertFalse(NPCAction.EAT in DecisionEngine.eligibleActions(NPCState.EATING, true, eating))
    }

    @Test
    fun `diagnostic reports blocked actions without changing eligibility`() {
        val situation = ActionSituation(false, false, false, isAtHome = true)
        val blocked = DecisionEngine.blockedActions(NPCState.IDLE, false, situation)
        val eligible = DecisionEngine.eligibleActions(NPCState.IDLE, false, situation)

        assertEquals(ActionBlockReason.NO_INTERLOCUTOR, blocked[NPCAction.GREET])
        assertEquals(ActionBlockReason.NO_WORKPLACE, blocked[NPCAction.START_WORK])
        assertEquals(ActionBlockReason.NO_SOCIAL_DESTINATION, blocked[NPCAction.SOCIALIZE])
        assertEquals(ActionBlockReason.HUNGER_SUFFICIENT, blocked[NPCAction.EAT])
        assertEquals(ActionBlockReason.ENERGY_SUFFICIENT, blocked[NPCAction.SLEEP])
        assertTrue(blocked.keys.none { it in eligible })
    }

    @Test
    fun `diagnostic skips alternatives while self care is locked`() {
        val situation = ActionSituation(false, false, false, isAtHome = true, hungerLevel = 45,
            currentState = NPCState.EATING)

        assertEquals(listOf(NPCAction.EAT), DecisionEngine.eligibleActions(NPCState.EATING, false, situation))
        assertTrue(DecisionEngine.blockedActions(NPCState.EATING, false, situation).isEmpty())
        assertEquals(ActionBlockReason.NOT_AT_HOME,
            ActionEligibility.blockReason(NPCAction.EAT, situation.copy(isAtHome = false)))
    }

    @Test
    fun `simulated meal recovers hunger only while eating at home and stays bounded`() {
        assertEquals(38, DecisionEngine.nextHungerLevel(30, NPCState.EATING, true, 1.5, 1.0))
        assertEquals(28, DecisionEngine.nextHungerLevel(30, NPCState.EATING, false, 1.5, 1.0))
        assertEquals(28, DecisionEngine.nextHungerLevel(30, NPCState.WORKING, true, 1.5, 1.0))
        assertEquals(100, DecisionEngine.nextHungerLevel(99, NPCState.EATING, true, 1.5, 1.0))
        assertEquals(0, DecisionEngine.nextHungerLevel(0, NPCState.IDLE, false, 1.5, 1.0))
    }

    @Test
    fun `return priority cannot override danger conversation or actual care at home`() {
        val returning = ActionSituation(false, false, false, returningForCare = true,
            currentState = NPCState.WALKING, hungerLevel = 20)
        assertEquals(listOf(NPCAction.WALK_TO_TARGET),
            DecisionEngine.eligibleActions(NPCState.WALKING, false, returning))
        assertTrue(DecisionEngine.blockedActions(NPCState.WALKING, false, returning).isEmpty())
        assertFalse(NPCAction.WALK_TO_TARGET in DecisionEngine.eligibleActions(NPCState.WALKING, true, returning))
        assertFalse(NPCAction.WALK_TO_TARGET in DecisionEngine.eligibleActions(NPCState.TALKING, false, returning))
        assertFalse(NPCAction.WALK_TO_TARGET in DecisionEngine.eligibleActions(NPCState.WALKING, false,
            returning.copy(hasInteractingPlayer = true)))
        assertEquals(listOf(NPCAction.EAT), DecisionEngine.eligibleActions(NPCState.EATING, false,
            returning.copy(isAtHome = true, currentState = NPCState.EATING)))
    }
}
