package ro.ainpc.npc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NpcAgentSnapshotTest {
    @Test
    fun `snapshot captures identity revisions and authoritative needs without updating the context`() {
        val npc = AINPC(null).apply {
            hungerLevel = 12
            energyLevel = 23
            socialNeedLevel = 34
            comfortLevel = 45
            safetyLevel = 56
            currentGoal = "sa ajung acasa"
            plannedRoutineActivity = "home"
            context.hungerLevel = 100
            context.energyLevel = 100
            context.isInDanger = true
        }

        val snapshot = NpcAgentSnapshot.capture(npc, npc.context, 1_000L)

        assertEquals(2, snapshot.schemaVersion)
        assertEquals(npc.uuid, snapshot.npcUuid)
        assertEquals(npc.runtimeIdentity, snapshot.runtimeIdentity)
        assertEquals(npc.decisionRevision, snapshot.decisionRevision)
        assertEquals(npc.stateRevision, snapshot.stateRevision)
        assertEquals(1_000L, snapshot.capturedAtMillis)
        assertEquals(listOf(12, 23, 34, 45, 56), snapshot.cognition.let {
            listOf(it.hungerLevel, it.energyLevel, it.socialNeedLevel, it.comfortLevel, it.safetyLevel)
        })
        assertTrue(snapshot.cognition.isInDanger)
        assertEquals(100, npc.context.hungerLevel)
        assertEquals(100, npc.context.energyLevel)
        assertNull(npc.lastDecision)
    }

    @Test
    fun `both descriptions can retain the same captured needs and intention after live state changes`() {
        val npc = AINPC(null).apply {
            hungerLevel = 10
            currentGoal = "sa ajung acasa"
            plannedRoutineActivity = "home"
        }
        val snapshot = NpcAgentSnapshot.capture(npc, npc.context, 1_000L)
        npc.hungerLevel = 90
        npc.currentGoal = "sa lucrez la atelier"
        npc.plannedRoutineActivity = "work"
        npc.context.currentGoal = "un obiectiv vechi din context"
        npc.changeState(NPCState.WORKING)

        val identityDescription = npc.generateContextDescription(snapshot.cognition)
        val environmentDescription = npc.context.generateContextDescription(null, null, snapshot.cognition)

        assertTrue(identityDescription.contains("Nevoi: satietate 10/100"))
        assertTrue(identityDescription.contains("Obiectiv imediat: sa ajung acasa"))
        assertTrue(identityDescription.contains("Stare curenta: ${NPCState.IDLE.displayName}"))
        assertTrue(environmentDescription.contains("Mi-e foame."))
        assertTrue(environmentDescription.contains("sa ajung acasa"))
        assertFalse(environmentDescription.contains("un obiectiv vechi"))
        assertFalse(identityDescription.contains("sa lucrez la atelier"))
        assertEquals(NPCState.IDLE, snapshot.cognition.currentState)
        assertNotEquals(npc.stateRevision, snapshot.stateRevision)
    }

    @Test
    fun `capture uses one explicit instant for evidence expiry without clearing the live evidence`() {
        val now = 1_000_000L
        val npc = AINPC(null)
        val decision = NpcDecisionExplanation(NPCAction.EAT, now, false)
        npc.rememberDecision(decision)
        npc.selfCareGoal.applied(
            NPCAction.EAT, NpcCognitiveState(hungerLevel = 20, currentState = NPCState.WALKING),
            "world:home", false, now,
        )

        val snapshot = NpcAgentSnapshot.capture(npc, npc.context, now)
        val expired = NpcAgentSnapshot.capture(npc, npc.context, now + 300_000L)
        npc.selfCareGoal.clear()

        assertEquals(decision.description(now), snapshot.cognition.decisionDescription)
        assertEquals(NpcSelfCareGoal.Stage.RETURNING_HOME, snapshot.cognition.selfCareProgress?.stage)
        assertEquals("", expired.cognition.decisionDescription)
        assertNull(expired.cognition.selfCareProgress)
        assertEquals(decision, npc.lastDecision)
    }

    @Test
    fun `recreated actors retain distinct runtime identities even with the same persistent UUID`() {
        val original = AINPC(null)
        val replacement = AINPC(null).apply { uuid = original.uuid }

        val before = NpcAgentSnapshot.capture(original)
        val after = NpcAgentSnapshot.capture(replacement)

        assertEquals(before.npcUuid, after.npcUuid)
        assertNotEquals(before.runtimeIdentity, after.runtimeIdentity)
    }
}
