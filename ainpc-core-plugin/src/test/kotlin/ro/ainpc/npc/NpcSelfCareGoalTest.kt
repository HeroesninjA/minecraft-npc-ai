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
    fun `active snapshots anchor their explanation to the last observation without renewing it`() {
        for (action in listOf(NPCAction.EAT, NPCAction.SLEEP)) {
            val careState = if (action == NPCAction.EAT) NPCState.EATING else NPCState.SLEEPING
            val state = walking.copy(energyLevel = 20)
            val cases = listOf(Triple(state, "", false), Triple(state, home, false),
                Triple(state.copy(currentState = careState), home, true))
            for ((observedState, homeKey, isAtHome) in cases) {
                goal.applied(action, observedState, homeKey, isAtHome, now)
                val recorded = requireNotNull(goal.snapshot(now))

                val explanation = recorded.description()

                assertTrue(explanation.contains("La ultima observatie"), explanation)
                assertFalse(explanation.contains("Sunt acasa"))
                assertFalse(explanation.contains("imi lipseste o casa"))
                assertEquals(recorded, goal.snapshot(now + 120_000L))
                assertEquals(now, recorded.updatedAt)
                assertFalse(recorded.observedRecovery)
            }
        }
    }

    @Test
    fun `return diagnostics explain the same eligibility without mutating the objective`() {
        assertEquals(NpcSelfCareContinuity.NO_RECENT_GOAL, goal.returnStatus(walking, home, false, now))
        goal.applied(NPCAction.EAT, walking, home, false, now)
        val original = goal.snapshot(now)
        val cases = listOf(
            walking to NpcSelfCareContinuity.READY,
            walking.copy(isInDanger = true) to NpcSelfCareContinuity.UNSAFE,
            walking.copy(safetyLevel = 20) to NpcSelfCareContinuity.UNSAFE,
            walking.copy(hasInteractingPlayer = true) to NpcSelfCareContinuity.INTERLOCUTOR,
            walking.copy(currentState = NPCState.IDLE) to NpcSelfCareContinuity.STATE_MISMATCH,
            walking.copy(hungerLevel = -1) to NpcSelfCareContinuity.INVALID_NEED,
            walking.copy(hungerLevel = 101) to NpcSelfCareContinuity.INVALID_NEED,
            walking.copy(hungerLevel = 60) to NpcSelfCareContinuity.NEED_SATISFIED,
        )
        for ((state, status) in cases) {
            assertEquals(status, goal.returnStatus(state, home, false, now))
            assertEquals(status == NpcSelfCareContinuity.READY, goal.canContinueReturn(state, home, false, now))
            assertEquals(original, goal.snapshot(now))
        }
        assertEquals(NpcSelfCareContinuity.HOME_UNCONFIRMED, goal.returnStatus(walking, "", false, now))
        assertEquals(NpcSelfCareContinuity.HOME_CHANGED, goal.returnStatus(walking, "other", false, now))
        assertEquals(NpcSelfCareContinuity.AT_HOME, goal.returnStatus(walking, home, true, now))
        assertEquals(NpcSelfCareContinuity.NO_RECENT_GOAL, goal.returnStatus(walking, home, false, now - 1L))
        assertEquals(NpcSelfCareContinuity.NO_RECENT_GOAL, goal.returnStatus(walking, home, false, now + 300_000L))
        assertEquals(original, goal.snapshot(now))

        goal.interrupt(now + 1L)
        assertEquals(NpcSelfCareContinuity.NOT_RETURNING, goal.returnStatus(walking, home, false, now + 1L))
    }

    @Test
    fun `applied care tracks returning arrival and observed recovery without declaring early success`() {
        goal.applied(NPCAction.EAT, walking, home, false, now)
        assertEquals(NpcSelfCareGoal.Stage.RETURNING_HOME, goal.snapshot(now)!!.stage)
        assertTrue(goal.snapshot(now)!!.description().contains("sosirea nu era inca confirmata"))
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
        assertTrue(progress.description().contains("nu era inca incheiat"))
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
        assertTrue(goal.snapshot(now)!!.description().contains("lipsea o casa confirmata"))
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

    @Test
    fun `reapplying a return cannot hide three observed minutes without arrival or recovery`() {
        goal.applied(NPCAction.EAT, walking, home, false, now)
        repeat(6) { index ->
            val previousTick = now + index * 30_000L
            goal.observe(walking, walking, home, false, previousTick, previousTick + 30_000L)
            goal.applied(NPCAction.EAT, walking, home, false, previousTick + 30_000L)
        }

        val progress = goal.snapshot(now + 180_000L)!!
        assertEquals(180_000L, progress.withoutProgressMillis)
        assertTrue(progress.lacksProgress())
        assertFalse(goal.canContinueReturn(walking, home, false, now + 180_000L))
        assertEquals(NpcSelfCareContinuity.NO_PROGRESS, goal.returnStatus(walking, home, false, now + 180_000L))
        assertTrue(progress.description().contains("cauza nu este stabilita"))
        assertEquals(NpcSelfCareGoal.Stage.RETURNING_HOME, progress.stage)
        assertFalse(progress.observedRecovery)

        val eating = walking.copy(currentState = NPCState.EATING)
        goal.applied(NPCAction.EAT, eating, home, true, now + 180_001L)
        assertFalse(goal.snapshot(now + 180_001L)!!.lacksProgress())
        goal.observe(eating, eating.copy(hungerLevel = 28), home, true, now + 180_000L, now + 210_000L)
        assertTrue(goal.snapshot(now + 210_000L)!!.observedRecovery)
    }

    @Test
    fun `only valid new observation time counts and reading or selecting never creates progress`() {
        goal.applied(NPCAction.EAT, walking, home, false, now)
        goal.observe(walking, walking, home, false, now - 60_000L, now + 30_000L)
        val observed = goal.snapshot(now + 30_000L)!!
        assertEquals(30_000L, observed.withoutProgressMillis)
        goal.observe(walking, walking, home, false, now, now + 30_000L)
        assertEquals(observed, goal.snapshot(now + 30_000L))

        repeat(10) {
            assertTrue(goal.canContinueReturn(walking, home, false, now + 90_000L))
        }
        assertEquals(observed, goal.snapshot(now + 90_000L))
        goal.continueReturn(walking, home, now + 90_000L)
        assertEquals(30_000L, goal.snapshot(now + 90_000L)!!.withoutProgressMillis)
    }

    @Test
    fun `return continuity cannot bypass changed needs home safety interaction or freshness`() {
        goal.applied(NPCAction.EAT, walking, home, false, now)
        assertTrue(goal.canContinueReturn(walking, home, false, now))
        for (state in listOf(
            walking.copy(hungerLevel = 60), walking.copy(hungerLevel = -1),
            walking.copy(isInDanger = true), walking.copy(safetyLevel = 20),
            walking.copy(hasInteractingPlayer = true), walking.copy(currentState = NPCState.TALKING),
        )) {
            assertFalse(goal.canContinueReturn(state, home, false, now))
        }
        assertFalse(goal.canContinueReturn(walking, "other", false, now))
        assertFalse(goal.canContinueReturn(walking, "", false, now))
        assertFalse(goal.canContinueReturn(walking, home, true, now))
        assertFalse(goal.canContinueReturn(walking, home, false, now - 1L))
        assertFalse(goal.canContinueReturn(walking, home, false, now + 300_000L))

        goal.continueReturn(walking.copy(isInDanger = true), home, now + 1L)
        assertEquals(NpcSelfCareGoal.Stage.INTERRUPTED, goal.snapshot(now + 1L)!!.stage)
    }

    @Test
    fun `confirmed recovery resets missing progress time without promising movement`() {
        val eating = walking.copy(currentState = NPCState.EATING)
        goal.applied(NPCAction.EAT, eating, home, true, now)
        goal.observe(eating, eating, home, true, now, now + 120_000L)
        assertEquals(120_000L, goal.snapshot(now + 120_000L)!!.withoutProgressMillis)
        goal.observe(eating, eating.copy(hungerLevel = 28), home, true, now + 120_000L, now + 150_000L)
        assertEquals(0L, goal.snapshot(now + 150_000L)!!.withoutProgressMillis)
        assertFalse(goal.canContinueReturn(eating, home, true, now + 150_000L))
    }
}
