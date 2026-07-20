package ro.ainpc.npc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AINPCStateTransitionTest {
    @Test
    fun acceptedTransitionPublishesStableStateNamesOnce() {
        val events = mutableListOf<Triple<String, String, String>>()
        val npc = AINPC(null) { npcUuid, oldState, newState ->
            events += Triple(npcUuid, oldState, newState)
        }

        assertTrue(npc.changeState(NPCState.WALKING))
        assertTrue(npc.changeState(NPCState.WALKING))

        assertEquals(
            listOf(Triple(npc.uuid.toString(), NPCState.IDLE.name, NPCState.WALKING.name)),
            events
        )
    }

    @Test
    fun rejectedPriorityTransitionDoesNotPublish() {
        val events = mutableListOf<Triple<String, String, String>>()
        val npc = AINPC(null) { npcUuid, oldState, newState ->
            events += Triple(npcUuid, oldState, newState)
        }
        npc.restorePersistedState(NPCState.COMBAT)

        assertFalse(npc.changeState(NPCState.IDLE))

        assertEquals(NPCState.COMBAT, npc.currentState)
        assertTrue(events.isEmpty())
    }

    @Test
    fun simulationTransitionBypassesPriorityAndPublishes() {
        val events = mutableListOf<Triple<String, String, String>>()
        val npc = AINPC(null) { npcUuid, oldState, newState ->
            events += Triple(npcUuid, oldState, newState)
        }
        npc.restorePersistedState(NPCState.SLEEPING)

        assertTrue(npc.changeStateFromSimulation(NPCState.WORKING))

        assertEquals(NPCState.WORKING, npc.currentState)
        assertEquals(
            listOf(Triple(npc.uuid.toString(), NPCState.SLEEPING.name, NPCState.WORKING.name)),
            events
        )
    }

    @Test
    fun persistedStateRestoreIsSilent() {
        val events = mutableListOf<Triple<String, String, String>>()
        val npc = AINPC(null) { npcUuid, oldState, newState ->
            events += Triple(npcUuid, oldState, newState)
        }

        npc.restorePersistedState(NPCState.RESTING)

        assertEquals(NPCState.RESTING, npc.currentState)
        assertTrue(events.isEmpty())
    }
}
