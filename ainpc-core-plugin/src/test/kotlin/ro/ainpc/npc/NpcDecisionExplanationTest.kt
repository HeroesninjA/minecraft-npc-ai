package ro.ainpc.npc

import org.bukkit.Location
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NpcDecisionExplanationTest {
    private val now = 1_000_000L
    private val decision = NpcDecisionExplanation(
        NPCAction.EAT, now, true, NpcDecisionExplanation.Factor.NEEDS, NpcDecisionExplanation.Factor.EXPERIENCE,
    )

    @Test
    fun `applied care plan is distinguished from utility and cannot assert recovery`() {
        val explanation = NpcDecisionExplanation(NPCAction.EAT, now, false, fromCarePlan = true)
        val description = explanation.description(now)

        assertTrue(description.contains("planul local de ingrijire"))
        assertTrue(description.contains("refacerea nu este confirmata"))
        assertFalse(description.contains("factorii favorabili"))
        assertFalse(description.contains("alegerea putea varia"))
        assertFalse(description.contains("am mancat"))
        val npc = AINPC(null)
        npc.rememberDecision(explanation)
        assertEquals(description, NpcCognitiveState.capture(npc, npc.context, now).decisionDescription)
    }

    @Test
    fun `description reports past intent and positive influences without asserting a completed result`() {
        val description = decision.description(now)

        assertTrue(description.contains("La ultima decizie"))
        assertTrue(description.contains("sa ma ocup de hrana"))
        assertTrue(description.contains("nevoile mele de atunci"))
        assertTrue(description.contains("experientele mele recente de ingrijire"))
        assertTrue(description.contains("alegerea putea varia"))
        assertFalse(description.contains("am mancat"))
        assertFalse(description.contains("mi-ai oferit"))
        assertFalse(description.contains("inventar"))
        assertFalse(decision.copy(variedChoice = false).description(now).contains("putea varia"))
    }

    @Test
    fun `future invalid and expired decisions have no description and reads do not renew them`() {
        assertEquals("", decision.description(now - 1L))
        assertTrue(decision.description(now + 299_999L).isNotBlank())
        assertEquals("", decision.description(now + 300_000L))
        assertEquals("", decision.description(now + 300_001L))
        assertEquals("", decision.copy(selectedAt = 0L).description(now))
        assertEquals("", decision.copy(selectedAt = -1L).description(now))
        assertEquals("", decision.copy(selectedAt = Long.MAX_VALUE).description(1L))
        assertEquals(now, decision.selectedAt)
    }

    @Test
    fun `conversation transitions preserve historical explanation while lifecycle and mode changes clear it`() {
        val npc = AINPC(null)
        npc.rememberDecision(decision)
        npc.changeStateFromSimulation(NPCState.TALKING)
        assertEquals(decision, npc.lastDecision)

        npc.simulationMode = NpcSimulationMode.LIGHT
        assertNull(npc.lastDecision)
        npc.simulationMode = NpcSimulationMode.FULL
        val lifecycleChanges: List<() -> Unit> = listOf(
            { npc.despawn() }, { npc.markEntityUnavailable() },
            { npc.restorePersistedState(NPCState.IDLE) },
            { npc.teleport(Location(null, 1.0, 64.0, 1.0)) }, { npc.spawn() },
        )
        lifecycleChanges.forEach { change ->
            npc.rememberDecision(decision)
            change()
            assertNull(npc.lastDecision)
        }
    }

    @Test
    fun `cognitive capture freezes decision explanation independently from newer choices`() {
        val npc = AINPC(null)
        npc.rememberDecision(decision.copy(selectedAt = System.currentTimeMillis()))
        val snapshot = NpcCognitiveState.capture(npc)

        npc.rememberDecision(null)

        assertTrue(snapshot.decisionDescription.contains("sa ma ocup de hrana"))
        assertEquals("", NpcCognitiveState.capture(npc).decisionDescription)
    }
}
