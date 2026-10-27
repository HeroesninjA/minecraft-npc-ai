package ro.ainpc.routine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcCognitiveState
import ro.ainpc.npc.NpcSelfCareGoal
import ro.ainpc.npc.NpcSimulationMode

class RoutineSelfCarePolicyTest {
    @Test
    fun `only a planned rest defers routine at home while full simulation can advance it`() {
        assertFalse(RoutineService.shouldDeferForSelfCare(NPCState.RESTING, 100, 40, true, true))
        assertTrue(RoutineService.shouldDeferForSelfCare(NPCState.RESTING, 100, 59, true, true, plannedRest = true))
        assertFalse(RoutineService.shouldDeferForSelfCare(NPCState.RESTING, 100, 60, true, true, plannedRest = true))
        assertFalse(RoutineService.shouldDeferForSelfCare(NPCState.RESTING, 100, -1, true, true, plannedRest = true))
        assertFalse(RoutineService.shouldDeferForSelfCare(NPCState.RESTING, 100, 40, false, true, plannedRest = true))
        assertFalse(RoutineService.shouldDeferForSelfCare(NPCState.RESTING, 100, 40, true, false, plannedRest = true))
    }

    @Test
    fun `rest priority requires the same safe recent home goal and a full NPC`() {
        val now = 1_000_000L
        val npc = AINPC(null).apply {
            worldName = "world"
            simulationMode = NpcSimulationMode.FULL
            energyLevel = 40
            homeAnchor = AINPC.OwnedLocation("home", "acasa", "world", 1.0, 64.0, 3.0)
            restorePersistedState(NPCState.RESTING)
        }
        assertFalse(RoutineService.hasActiveRestGoal(npc, now))
        npc.selfCareGoal.applied(NPCAction.REST, NpcCognitiveState.capture(npc, npc.context, now),
            NpcSelfCareGoal.homeKey(npc), true, now, allowRest = true)
        assertTrue(RoutineService.hasActiveRestGoal(npc, now))
        assertFalse(RoutineService.hasActiveRestGoal(npc, now + 300_000L))
        npc.context.isInDanger = true
        assertFalse(RoutineService.hasActiveRestGoal(npc, now))
        npc.context.isInDanger = false
        npc.homeAnchor = null
        assertFalse(RoutineService.hasActiveRestGoal(npc, now))
        npc.simulationMode = NpcSimulationMode.LIGHT
        assertFalse(RoutineService.hasActiveRestGoal(npc, now))
    }

    @Test
    fun `routine waits while simulated meal or sleep is still needed at home`() {
        assertTrue(RoutineService.shouldDeferForSelfCare(NPCState.EATING, 20, 80, true, true))
        assertTrue(RoutineService.shouldDeferForSelfCare(NPCState.SLEEPING, 80, 20, true, true))
    }

    @Test
    fun `routine resumes after recovery or when simulation cannot advance self care`() {
        assertTrue(RoutineService.shouldDeferForSelfCare(NPCState.EATING, 35, 80, true, true))
        assertTrue(RoutineService.shouldDeferForSelfCare(NPCState.SLEEPING, 80, 35, true, true))
        assertFalse(RoutineService.shouldDeferForSelfCare(NPCState.EATING, 60, 80, true, true))
        assertFalse(RoutineService.shouldDeferForSelfCare(NPCState.SLEEPING, 80, 60, true, true))
        assertFalse(RoutineService.shouldDeferForSelfCare(NPCState.EATING, 20, 80, false, true))
        assertFalse(RoutineService.shouldDeferForSelfCare(NPCState.SLEEPING, 80, 20, true, false))
        assertFalse(RoutineService.shouldDeferForSelfCare(NPCState.WORKING, 20, 20, true, true))
    }

    @Test
    fun `low needs route toward home with the more urgent need first`() {
        val home = AINPC.OwnedLocation("home", "casa", "world", 10.0, 64.0, 10.0)
        val meal = RoutineSelfCarePolicy.assignment(home, 20, 30, true)!!
        val sleep = RoutineSelfCarePolicy.assignment(home, 30, 10, true)!!

        assertEquals(RoutineSlot.HOME, meal.slot())
        assertSame(home, meal.targetAnchor())
        assertEquals(NPCState.EATING, meal.targetState())
        assertEquals(NPCState.SLEEPING, sleep.targetState())
        assertEquals(NPCState.EATING, RoutineSelfCarePolicy.assignment(home, 20, 20, true)?.targetState())
        assertEquals(NPCState.EATING, RoutineSelfCarePolicy.assignment(home, 45, 20, true,
            state = NPCState.EATING)?.targetState())
        assertEquals(NPCState.SLEEPING, RoutineSelfCarePolicy.assignment(home, 20, 45, true,
            state = NPCState.SLEEPING)?.targetState())
    }

    @Test
    fun `home routing requires an active full simulation and a known anchor`() {
        val home = AINPC.OwnedLocation("home", "casa", "world", 10.0, 64.0, 10.0)

        assertNull(RoutineSelfCarePolicy.assignment(null, 10, 10, true))
        assertNull(RoutineSelfCarePolicy.assignment(home, 10, 10, false))
        assertNull(RoutineSelfCarePolicy.assignment(home, 10, 10, true, inDanger = true))
        assertNull(RoutineSelfCarePolicy.assignment(home, 35, 35, true))
        assertNull(RoutineSelfCarePolicy.assignment(home, 60, 60, true, state = NPCState.EATING))
        assertEquals(5.5, RoutineSelfCarePolicy.arrivalRadius(12.0))
        assertEquals(2.0, RoutineSelfCarePolicy.arrivalRadius(2.0))
    }
}
