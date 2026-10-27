package ro.ainpc.engine

import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NPCContext
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcAgentSnapshot
import ro.ainpc.npc.NpcSelfCareGoal
import ro.ainpc.npc.NpcSimulationMode
import java.lang.reflect.Proxy

class NpcCarePlannerTest {
    private val now = 1_000_000L
    private val npc = AINPC(null).apply {
        worldName = "world"
        simulationMode = NpcSimulationMode.FULL
        hungerLevel = 20
        energyLevel = 100
        safetyLevel = 100
        homeAnchor = AINPC.OwnedLocation("home", "acasa", "world", 10.0, 64.0, -30.0)
    }

    @Test
    fun `hungry NPC proposes return care and observation without applying anything`() {
        val before = input()
        val plan = NpcCarePlanner.plan(before)

        assertEquals(NpcCarePlanPreview.Status.READY, plan.status)
        assertEquals(NpcCareGoal.SATIETY, plan.goal)
        assertEquals(60, plan.targetNeed)
        assertEquals(listOf(NPCAction.EAT), plan.alternatives.map { it.action })
        assertEquals(listOf(NpcCareStep.RETURN_HOME, NpcCareStep.EAT_AT_HOME, NpcCareStep.OBSERVE_RECOVERY),
            plan.selected?.steps)
        assertEquals(before.agent.cognition, input().agent.cognition)
        assertEquals(before.agent.decisionRevision, npc.decisionRevision)
        assertEquals(before.agent.stateRevision, npc.stateRevision)
        assertNull(npc.lastDecision)
        assertNull(npc.selfCareGoal.snapshot(now))
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        assertEquals(0L, npc.lastSimulationTickAt)
    }

    @Test
    fun `at home plan omits travel and does not invent food stock or observed recovery`() {
        npc.context.isAtHome = true
        val plan = NpcCarePlanner.plan(input())

        assertEquals(listOf(NpcCareStep.EAT_AT_HOME, NpcCareStep.OBSERVE_RECOVERY), plan.selected?.steps)
        assertEquals(20, npc.hungerLevel)
        assertNull(npc.selfCareGoal.snapshot(now))
        assertTrue(plan.status.label.contains("neexecutata"))
        assertTrue(plan.status.label.contains("nu sunt confirmate"))
    }

    @Test
    fun `when both needs are low the more urgent cognitive focus supplies one goal`() {
        npc.hungerLevel = 30
        npc.energyLevel = 10
        assertEquals(NpcCareGoal.ENERGY, NpcCarePlanner.plan(input()).goal)
        npc.hungerLevel = 5
        assertEquals(NpcCareGoal.SATIETY, NpcCarePlanner.plan(input()).goal)
        npc.hungerLevel = 10
        assertEquals(NpcCareGoal.SATIETY, NpcCarePlanner.plan(input()).goal)
    }

    @Test
    fun `repeated planning remains deterministic and bounded across care states and need levels`() {
        val captured = input()
        val levels = listOf(0, 20, 34, 35, 59, 60, 100)
        listOf(NPCState.IDLE, NPCState.WALKING, NPCState.EATING, NPCState.SLEEPING, NPCState.RESTING).forEach { state ->
            levels.forEach { hunger ->
                levels.forEach { energy ->
                    val current = captured.copy(agent = captured.agent.copy(cognition = captured.agent.cognition.copy(
                        currentState = state, hungerLevel = hunger, energyLevel = energy)))
                    val first = NpcCarePlanner.plan(current)
                    val second = NpcCarePlanner.plan(current)
                    assertEquals(first.status, second.status)
                    assertEquals(first.goal, second.goal)
                    assertEquals(first.alternatives.map { it.action to it.heuristicCost },
                        second.alternatives.map { it.action to it.heuristicCost })
                    assertTrue(first.alternatives.size <= 2)
                    first.alternatives.forEach { alternative ->
                        assertTrue(alternative.steps.size in 2..3)
                        assertEquals(NpcCareStep.OBSERVE_RECOVERY, alternative.steps.last())
                    }
                }
            }
        }
    }

    @Test
    fun `energy goal compares two bounded alternatives and prefers sleep from idle`() {
        npc.hungerLevel = 100
        npc.energyLevel = 10
        val plan = NpcCarePlanner.plan(input())

        assertEquals(NpcCareGoal.ENERGY, plan.goal)
        assertEquals(listOf(NPCAction.SLEEP, NPCAction.REST), plan.alternatives.map { it.action })
        assertTrue(plan.alternatives.all { it.steps.size == 3 && it.steps.last() == NpcCareStep.OBSERVE_RECOVERY })
        assertTrue(plan.alternatives.first().heuristicCost < plan.alternatives.last().heuristicCost)
        assertEquals(NpcCareStep.SLEEP_AT_HOME, plan.alternatives.first().steps[1])
        assertEquals(NpcCareStep.REST_AT_HOME, plan.alternatives.last().steps[1])
    }

    @Test
    fun `existing rest is preferred and continued up to the recovery threshold`() {
        npc.hungerLevel = 100
        npc.energyLevel = 50
        npc.restorePersistedState(NPCState.RESTING)
        assertEquals(NPCAction.REST, NpcCarePlanner.plan(input()).selected?.action)
        npc.energyLevel = 60
        assertEquals(NpcCarePlanPreview.Status.NO_GOAL, NpcCarePlanner.plan(input()).status)
    }

    @Test
    fun `start threshold does not create a goal but active eating continues until recovered`() {
        npc.hungerLevel = 35
        assertEquals(NpcCarePlanPreview.Status.NO_GOAL, NpcCarePlanner.plan(input()).status)
        npc.restorePersistedState(NPCState.EATING)
        assertEquals(NPCAction.EAT, NpcCarePlanner.plan(input()).selected?.action)
        npc.hungerLevel = 60
        assertEquals(NpcCarePlanPreview.Status.NO_GOAL, NpcCarePlanner.plan(input()).status)
    }

    @Test
    fun `existing return continues above start threshold using the same captured home`() {
        val captured = input()
        val progress = NpcSelfCareGoal.Snapshot(NPCAction.EAT, NpcSelfCareGoal.Stage.RETURNING_HOME,
            20, 40, requireNotNull(captured.home).key(), false, now)
        val state = captured.agent.cognition.copy(hungerLevel = 40, currentState = NPCState.WALKING,
            selfCareProgress = progress)
        val plan = NpcCarePlanner.plan(captured.copy(agent = captured.agent.copy(cognition = state)))
        assertEquals(NPCAction.EAT, plan.selected?.action)
    }

    @Test
    fun `stalled goal does not become another ready return proposal`() {
        val captured = input()
        val progress = NpcSelfCareGoal.Snapshot(NPCAction.EAT, NpcSelfCareGoal.Stage.RETURNING_HOME,
            20, 20, requireNotNull(captured.home).key(), false, now, withoutProgressMillis = 180_000L)
        val plan = NpcCarePlanner.plan(captured.copy(agent = captured.agent.copy(
            cognition = captured.agent.cognition.copy(selfCareProgress = progress))))
        assertEquals(NpcCarePlanPreview.Status.NO_PROGRESS, plan.status)
        assertTrue(plan.alternatives.isEmpty())
    }

    @Test
    fun `danger unsafe needs interlocutors and unsupported states defer care`() {
        val captured = input()
        val cases = listOf(
            captured.agent.cognition.copy(isInDanger = true) to NpcCarePlanPreview.Status.UNSAFE,
            captured.agent.cognition.copy(safetyLevel = 34) to NpcCarePlanPreview.Status.UNSAFE,
            captured.agent.cognition.copy(currentState = NPCState.COMBAT) to NpcCarePlanPreview.Status.UNSAFE,
            captured.agent.cognition.copy(hungerLevel = -1) to NpcCarePlanPreview.Status.INVALID_NEEDS,
            captured.agent.cognition.copy(energyLevel = 101) to NpcCarePlanPreview.Status.INVALID_NEEDS,
            captured.agent.cognition.copy(safetyLevel = 101) to NpcCarePlanPreview.Status.INVALID_NEEDS,
            captured.agent.cognition.copy(hasInteractingPlayer = true) to NpcCarePlanPreview.Status.INTERLOCUTOR,
            captured.agent.cognition.copy(currentState = NPCState.TRADING) to NpcCarePlanPreview.Status.BUSY,
        )
        cases.forEach { (state, expected) ->
            val plan = NpcCarePlanner.plan(captured.copy(agent = captured.agent.copy(cognition = state)))
            assertEquals(expected, plan.status)
            assertTrue(plan.alternatives.isEmpty())
            assertNull(plan.selected)
        }
    }

    @Test
    fun `missing invalid or foreign home cannot produce a ready plan even when cached at home`() {
        npc.context.isAtHome = true
        val captured = input()
        listOf(null, captured.home?.copy(worldName = "other"), captured.home?.copy(worldName = ""),
            captured.home?.copy(x = Double.NaN), captured.home?.copy(y = Double.POSITIVE_INFINITY),
            captured.home?.copy(z = Double.NEGATIVE_INFINITY)).forEach { home ->
            assertEquals(NpcCarePlanPreview.Status.HOME_UNCONFIRMED,
                NpcCarePlanner.plan(captured.copy(home = home)).status)
        }
    }

    @Test
    fun `non full modes do not get ready care plans`() {
        val captured = input()
        NpcSimulationMode.entries.filter { it != NpcSimulationMode.FULL }.forEach { mode ->
            val plan = NpcCarePlanner.plan(captured.copy(simulationMode = mode))
            assertEquals(NpcCarePlanPreview.Status.MODE_NOT_FULL, plan.status)
            assertEquals(NpcCarePlanPreview.Invalidation.NOT_READY, plan.invalidationReason(captured))
        }
    }

    @Test
    fun `frozen input and proposals survive source changes and reject mutable list access`() {
        val captured = input()
        npc.hungerLevel = 100
        npc.context.isAtHome = true
        npc.homeAnchor = null
        npc.worldName = "other"
        val plan = NpcCarePlanner.plan(captured)

        assertEquals(NPCAction.EAT, plan.selected?.action)
        assertEquals(20, plan.input.agent.cognition.hungerLevel)
        assertEquals("world", plan.input.home?.worldName)
        assertFalse(plan.input.agent.perception.isAtHome)
        assertThrows(UnsupportedOperationException::class.java) {
            (plan.alternatives as MutableList).clear()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (requireNotNull(plan.selected).steps as MutableList).clear()
        }
    }

    @Test
    fun `capture and planning do not call player entity or world APIs`() {
        val player = Proxy.newProxyInstance(Player::class.java.classLoader,
            arrayOf(Player::class.java)) { _, method, _ -> error("Unexpected Player API: ${method.name}") } as Player
        NPCContext::class.java.getDeclaredField("interactingPlayer").apply {
            isAccessible = true
            set(npc.context, player)
        }
        assertEquals(NpcCarePlanPreview.Status.INTERLOCUTOR, NpcCarePlanner.plan(input()).status)
    }

    @Test
    fun `identity and revision changes invalidate even with equal needs`() {
        val captured = input()
        val plan = NpcCarePlanner.plan(captured)
        val replacement = AINPC(null).apply { uuid = npc.uuid }
        assertEquals(NpcCarePlanPreview.Invalidation.IDENTITY_CHANGED, plan.invalidationReason(captured.copy(
            agent = captured.agent.copy(runtimeIdentity = replacement.runtimeIdentity))))
        assertEquals(NpcCarePlanPreview.Invalidation.IDENTITY_CHANGED, plan.invalidationReason(captured.copy(
            agent = captured.agent.copy(npcUuid = java.util.UUID.randomUUID()))))
        listOf(captured.agent.copy(decisionRevision = captured.agent.decisionRevision + 1),
            captured.agent.copy(stateRevision = captured.agent.stateRevision + 1)).forEach { agent ->
            assertEquals(NpcCarePlanPreview.Invalidation.REVISION_CHANGED,
                plan.invalidationReason(captured.copy(agent = agent)))
        }
    }

    @Test
    fun `clock rollback and exact expiry invalidate but repeated reads do not renew the deadline`() {
        val captured = input()
        val plan = NpcCarePlanner.plan(captured)
        assertNull(plan.invalidationReason(input(now + 4_999L)))
        assertNull(plan.invalidationReason(input(now + 4_999L)))
        assertEquals(NpcCarePlanPreview.Invalidation.EXPIRED, plan.invalidationReason(input(now + 5_000L)))
        assertEquals(NpcCarePlanPreview.Invalidation.EXPIRED, plan.invalidationReason(input(now - 1L)))
        val invalidClock = NpcCarePlanner.plan(input(0L))
        assertEquals(NpcCarePlanPreview.Invalidation.EXPIRED, invalidClock.invalidationReason(input(1L)))
    }

    @Test
    fun `needs goal routine and danger changes invalidate without relying on revision increments`() {
        val captured = input()
        val plan = NpcCarePlanner.plan(captured)
        val changedStates = listOf(
            captured.agent.cognition.copy(hungerLevel = 21),
            captured.agent.cognition.copy(energyLevel = 90),
            captured.agent.cognition.copy(isInDanger = true),
            captured.agent.cognition.copy(hasInteractingPlayer = true),
            captured.agent.cognition.copy(currentGoal = "lucrez"),
            captured.agent.cognition.copy(plannedRoutineActivity = "work"),
        )
        changedStates.forEach { cognition ->
            assertEquals(NpcCarePlanPreview.Invalidation.PRECONDITIONS_CHANGED,
                plan.invalidationReason(captured.copy(agent = captured.agent.copy(cognition = cognition))))
        }
        npc.hungerLevel++
        assertEquals(captured.agent.decisionRevision, npc.decisionRevision)
        assertNotNull(plan.invalidationReason(input()))
    }

    @Test
    fun `home world position and mode changes are checked explicitly`() {
        val plan = NpcCarePlanner.plan(input())
        npc.homeAnchor = AINPC.OwnedLocation("home", "nou", "world", 11.0, 64.0, -30.0)
        assertEquals(NpcCarePlanPreview.Invalidation.HOME_CHANGED, plan.invalidationReason(input()))
        npc.homeAnchor = plan.input.home?.let { AINPC.OwnedLocation("home", "acasa", it.worldName, it.x, it.y, it.z) }
        npc.context.isAtHome = true
        assertEquals(NpcCarePlanPreview.Invalidation.PRECONDITIONS_CHANGED, plan.invalidationReason(input()))
        npc.worldName = "other"
        assertEquals(NpcCarePlanPreview.Invalidation.WORLD_CHANGED, plan.invalidationReason(input()))
        assertEquals(NpcCarePlanPreview.Invalidation.MODE_CHANGED,
            plan.invalidationReason(plan.input.copy(simulationMode = NpcSimulationMode.LIGHT)))
    }

    private fun input(at: Long = now): NpcCarePlanningInput =
        NpcCarePlanningInput.capture(npc, NpcAgentSnapshot.capture(npc, npc.context, at))
}
