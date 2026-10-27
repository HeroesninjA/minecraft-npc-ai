package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.engine.DecisionEngine
import ro.ainpc.engine.NpcCarePlanner
import ro.ainpc.engine.NpcCarePlanningInput
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NpcAgentSnapshot
import ro.ainpc.npc.NpcSimulationMode

class AINPCCommandCarePlanTest {
    @Test
    fun `disabled shadow planner adds no command output`() {
        val preview = DecisionEngine.ActionDecisionPreview(emptyMap(), emptyMap(), false, false)
        assertTrue(formatDecisionCarePlan(preview).isEmpty())
    }

    @Test
    fun `formatter distinguishes proposals probabilities and unverified recovery without exposing home text`() {
        val npc = AINPC(null).apply {
            simulationMode = NpcSimulationMode.FULL
            worldName = "world"
            hungerLevel = 100
            energyLevel = 10
            homeAnchor = AINPC.OwnedLocation("home", "&4private\n".repeat(100), "world", 1.0, 64.0, 3.0)
        }
        val plan = NpcCarePlanner.plan(NpcCarePlanningInput.capture(npc, NpcAgentSnapshot.capture(npc)))
        val preview = DecisionEngine.ActionDecisionPreview(emptyMap(), mapOf(NPCAction.SLEEP to 0.25),
            true, false, carePlan = plan)

        val lines = formatDecisionCarePlan(preview)

        assertEquals(7, lines.size)
        assertTrue(lines.any { it.contains("SHADOW, fara executie") && it.contains("neexecutata") })
        assertTrue(lines.any { it.contains("ENERGY") && it.contains("60/100") })
        assertTrue(lines.any { it.contains("preferata: SLEEP") && it.contains("Utility acum: 25.0%") })
        assertTrue(lines.any { it.contains("alternativa: REST") && it.contains("Utility acum: 0.0%") })
        assertTrue(lines.any { it.contains("RETURN_HOME -> SLEEP_AT_HOME -> OBSERVE_RECOVERY") })
        assertTrue(lines.last().contains("succesul raman neverificate"))
        assertFalse(lines.any { it.contains("private") })
        assertTrue(lines.all { it.length < 380 && '\n' !in it && '\r' !in it })
        assertEquals(10, npc.energyLevel)
    }
}
