package ro.ainpc.npc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NpcSelfCareGoalTest {
    private val goal = NpcSelfCareGoal()
    private val now = 1_000_000L
    private val home = "world:0,64,0"
    private val walking = NpcCognitiveState(hungerLevel = 20, currentState = NPCState.WALKING)

    @Test
    fun `applied care tracks returning arrival and observed recovery without declaring early success`() {
        goal.applied(NPCAction.EAT, walking, home, false, now)
        assertEquals(NpcSelfCareGoal.Stage.RETURNING_HOME, goal.snapshot(now)!!.stage)
        assertTrue(goal.snapshot(now)!!.description().contains("sosirea nu este inca confirmata"))
        val eating = walking.copy(currentState = NPCState.EATING)
        goal.onStateChanged(NPCState.EATING, now + 1L)
        goal.applied(NPCAction.EAT, eating, home, true, now + 1L)
        assertEquals(NpcSelfCareGoal.Stage.RECOVERING, goal.snapshot(now + 1L)!!.stage)
        assertFalse(goal.snapshot(now + 1L)!!.observedRecovery)

        goal.observe(eating, eating.copy(hungerLevel = 28), home, true, now, now + 30_000L)

        val progress = goal.snapshot(now + 30_000L)!!
        assertEquals(20, progress.initialNeed)
        assertEquals(28, progress.observedNeed)
        assertTrue(progress.observedRecovery)
        assertEquals(NpcSelfCareGoal.Stage.RECOVERING, progress.stage)
        assertTrue(progress.description().contains("nu este inca incheiat"))
    }

    @Test
    fun `completion requires an observed recovered need and survives a later noncare choice`() {
        val sleeping = NpcCognitiveState(energyLevel = 55, currentState = NPCState.SLEEPING)
        goal.applied(NPCAction.SLEEP, sleeping, home, true, now)
        assertEquals(NpcSelfCareGoal.Stage.RECOVERING, goal.snapshot(now)!!.stage)
        goal.observe(sleeping, sleeping.copy(energyLevel = 61), home, true, now, now + 30_000L)
        val completed = goal.snapshot(now + 30_000L)!!
        assertEquals(NpcSelfCareGoal.Stage.SATISFIED, completed.stage)
        assertTrue(completed.observedRecovery)
        assertTrue(completed.description().contains("obiectiv de odihna este incheiat"))

        goal.onStateChanged(NPCState.WORKING, now + 30_001L)
        goal.applied(NPCAction.START_WORK, walking.copy(currentState = NPCState.WORKING), home, false, now + 30_001L)

        assertEquals(completed, goal.snapshot(now + 30_001L))
    }

    @Test
    fun `satisfied need from an unknown change does not prove the care plan caused recovery`() {
        goal.applied(NPCAction.EAT, walking, home, false, now)
        goal.observe(walking.copy(hungerLevel = 70), walking.copy(hungerLevel = 68), home, false, now, now + 30_000L)

        val completed = goal.snapshot(now + 30_000L)!!
        assertEquals(NpcSelfCareGoal.Stage.SATISFIED, completed.stage)
        assertFalse(completed.observedRecovery)
        assertTrue(completed.description().contains("nu confirma cauza"))
        assertFalse(completed.description().contains("de acasa"))
    }

    @Test
    fun `missing home is blocked and a changed home cannot inherit old progress`() {
        goal.applied(NPCAction.EAT, walking, "", false, now)
        assertEquals(NpcSelfCareGoal.Stage.BLOCKED, goal.snapshot(now)!!.stage)
        assertTrue(goal.snapshot(now)!!.description().contains("lipseste o casa confirmata"))
        goal.applied(NPCAction.EAT, walking, home, false, now + 1L)
        assertEquals(NpcSelfCareGoal.Stage.RETURNING_HOME, goal.snapshot(now + 1L)!!.stage)
        goal.observe(walking, walking.copy(hungerLevel = 70), "other:home", false, now, now + 30_000L)
        assertEquals(NpcSelfCareGoal.Stage.INTERRUPTED, goal.snapshot(now + 30_000L)!!.stage)
    }

    @Test
    fun `danger conversations and incompatible choices interrupt rather than complete care`() {
        for (state in listOf(NPCState.TALKING, NPCState.COMBAT, NPCState.RESTING, NPCState.SLEEPING)) {
            goal.applied(NPCAction.EAT, walking, home, false, now)
            goal.onStateChanged(state, now + 1L)
            assertEquals(NpcSelfCareGoal.Stage.INTERRUPTED, goal.snapshot(now + 1L)!!.stage)
        }
        goal.applied(NPCAction.EAT, walking, home, false, now + 2L)
        goal.observe(walking.copy(isInDanger = true), walking.copy(hungerLevel = 70), home, false, now, now + 30_000L)
        assertEquals(NpcSelfCareGoal.Stage.INTERRUPTED, goal.snapshot(now + 30_000L)!!.stage)
    }

    @Test
    fun `invalid gaps and timestamps cannot create an observed completion`() {
        for (previousTickAt in listOf(0L, now + 30_000L, now - 120_001L)) {
            goal.applied(NPCAction.EAT, walking, home, false, now)
            goal.observe(walking, walking.copy(hungerLevel = 70), home, false, previousTickAt, now + 30_000L)
            assertNull(goal.snapshot(now + 30_000L))
        }
        goal.applied(NPCAction.EAT, walking, home, false, now)
        assertNull(goal.snapshot(now - 1L))
        assertTrue(goal.snapshot(now + 299_999L) != null)
        assertNull(goal.snapshot(now + 300_000L))
        goal.clear()
        assertNull(goal.snapshot(now))
    }

    @Test
    fun `uncared states or already healthy needs cannot create a successful objective`() {
        goal.applied(NPCAction.EAT, walking.copy(hungerLevel = 90), home, false, now)
        assertNull(goal.snapshot(now))
        goal.applied(NPCAction.SLEEP, walking.copy(energyLevel = 10), home, true, now)
        assertNull(goal.snapshot(now))
        goal.applied(NPCAction.REST, walking, home, false, now)
        assertNull(goal.snapshot(now))
    }
}
