package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcAgentSnapshot
import ro.ainpc.npc.NpcSimulationMode

class NpcCarePlanExecutorTest {
    private val now = 1_000_000L
    private val npc = AINPC(null).apply {
        worldName = "world"
        simulationMode = NpcSimulationMode.FULL
        hungerLevel = 20
        energyLevel = 100
        homeAnchor = AINPC.OwnedLocation("home", "acasa", "world", 1.0, 64.0, 3.0)
        context.isAtHome = true
    }
    private val input = NpcCarePlanningInput.capture(npc, NpcAgentSnapshot.capture(npc, npc.context, now))
    private var current = input
    private var allowed = true
    private var available = true
    private var applied = true
    private var homeChecks = 0
    private var afterHomeCheck: () -> Unit = {}
    private val actions = mutableListOf<NPCAction>()

    @Test
    fun `ready at home meal applies once through the adapter without claiming recovery`() {
        val outcome = execute()

        assertEquals(NpcCarePlanExecutor.Outcome.APPLIED, outcome)
        assertTrue(outcome.endsTick)
        assertEquals(listOf(NPCAction.EAT), actions)
        assertEquals(1, homeChecks)
        assertEquals(20, npc.hungerLevel)
        assertTrue(npc.selfCareGoal.snapshot(now) == null)
    }

    @Test
    fun `energy plan can apply sleep and an already started rest alternative`() {
        current = input.copy(agent = input.agent.copy(cognition = input.agent.cognition.copy(
            hungerLevel = 100, energyLevel = 10)))
        assertEquals(NpcCarePlanExecutor.Outcome.APPLIED, execute(NpcCarePlanner.plan(current)))
        assertEquals(listOf(NPCAction.SLEEP), actions)
        actions.clear()
        current = current.copy(agent = current.agent.copy(cognition = current.agent.cognition.copy(currentState = NPCState.RESTING)))
        assertEquals(NpcCarePlanExecutor.Outcome.APPLIED, execute(NpcCarePlanner.plan(current)))
        assertEquals(listOf(NPCAction.REST), actions)
    }

    @Test
    fun `travel stays with the routine and cannot call an execution adapter`() {
        npc.context.isAtHome = false
        current = NpcCarePlanningInput.capture(npc, NpcAgentSnapshot.capture(npc, npc.context, now))

        val outcome = execute(NpcCarePlanner.plan(current))

        assertEquals(NpcCarePlanExecutor.Outcome.TRAVEL_REQUIRED, outcome)
        assertFalse(outcome.endsTick)
        assertTrue(actions.isEmpty())
        assertEquals(0, homeChecks)
    }

    @Test
    fun `not ready status cannot be applied even with an injected alternative`() {
        val plan = NpcCarePlanPreview(input, NpcCarePlanPreview.Status.UNSAFE, NpcCareGoal.SATIETY,
            listOf(NpcCareAlternative(NPCAction.EAT, listOf(NpcCareStep.EAT_AT_HOME, NpcCareStep.OBSERVE_RECOVERY), 1)))
        assertEquals(NpcCarePlanExecutor.Outcome.NOT_READY, execute(plan))
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `malformed action goal and operator order cannot enter the adapter`() {
        val cases = listOf(
            NpcCarePlanPreview(input, NpcCarePlanPreview.Status.READY, NpcCareGoal.SATIETY),
            NpcCarePlanPreview(input, NpcCarePlanPreview.Status.READY, NpcCareGoal.ENERGY,
                listOf(NpcCareAlternative(NPCAction.EAT, listOf(NpcCareStep.EAT_AT_HOME, NpcCareStep.OBSERVE_RECOVERY), 1))),
            NpcCarePlanPreview(input, NpcCarePlanPreview.Status.READY, NpcCareGoal.SATIETY,
                listOf(NpcCareAlternative(NPCAction.EAT, listOf(NpcCareStep.OBSERVE_RECOVERY, NpcCareStep.EAT_AT_HOME), 1))),
        )
        cases.forEach { plan ->
            assertTrue(execute(plan) in setOf(NpcCarePlanExecutor.Outcome.NOT_READY, NpcCarePlanExecutor.Outcome.UNSUPPORTED))
        }
        assertTrue(actions.isEmpty())
        assertEquals(0, homeChecks)
    }

    @Test
    fun `expired proposal and changed needs never reach world checks or application`() {
        val plan = NpcCarePlanner.plan(input)
        current = input.copy(agent = input.agent.copy(capturedAtMillis = now + 5_000L))
        assertEquals(NpcCarePlanExecutor.Outcome.INVALIDATED, execute(plan))
        current = input.copy(agent = input.agent.copy(cognition = input.agent.cognition.copy(hungerLevel = 60)))
        assertEquals(NpcCarePlanExecutor.Outcome.INVALIDATED, execute(plan))
        assertTrue(actions.isEmpty())
        assertEquals(0, homeChecks)
    }

    @Test
    fun `policy and physical home rejection cannot apply care`() {
        allowed = false
        assertEquals(NpcCarePlanExecutor.Outcome.POLICY_BLOCKED, execute())
        assertEquals(0, homeChecks)
        allowed = true
        available = false
        val unavailable = execute()
        assertEquals(NpcCarePlanExecutor.Outcome.HOME_UNAVAILABLE, unavailable)
        assertTrue(unavailable.endsTick)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `direct changes during physical checks invalidate before any application`() {
        afterHomeCheck = {
            current = input.copy(agent = input.agent.copy(cognition = input.agent.cognition.copy(currentGoal = "vorbesc")))
        }

        assertEquals(NpcCarePlanExecutor.Outcome.INVALIDATED, execute())
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `policy is checked again after the physical world check`() {
        afterHomeCheck = { allowed = false }
        assertEquals(NpcCarePlanExecutor.Outcome.POLICY_BLOCKED, execute())
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `interrupted adapter ends this tick instead of reporting success or retrying`() {
        applied = false
        val outcome = execute()

        assertEquals(NpcCarePlanExecutor.Outcome.INTERRUPTED, outcome)
        assertTrue(outcome.endsTick)
        assertEquals(listOf(NPCAction.EAT), actions)
    }

    private fun execute(plan: NpcCarePlanPreview = NpcCarePlanner.plan(input)): NpcCarePlanExecutor.Outcome =
        NpcCarePlanExecutor.executeAtHome(plan, { current }, { allowed }, {
            homeChecks++
            afterHomeCheck()
            available
        }) { action ->
            actions += action
            applied
        }
}
