package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NPCState

class ActionTargetAvailabilityTest {
    @Test
    fun `loaded destination chunk permits work and social destinations`() {
        val anchor = anchor("village", 12.0, 64.0, -4.0)
        val hasDestination = ActionTargetAvailability.hasUsableAnchor(anchor) { world, chunkX, chunkZ ->
            world == "village" && chunkX == 0 && chunkZ == -1
        }
        val situation = ActionSituation(false, hasDestination, hasDestination)

        assertTrue(hasDestination)
        assertTrue(NPCAction.START_WORK in DecisionEngine.eligibleActions(NPCState.IDLE, false, situation))
        assertTrue(NPCAction.SOCIALIZE in DecisionEngine.eligibleActions(NPCState.IDLE, false, situation))
    }

    @Test
    fun `missing world unloaded chunk or nonfinite coordinates block destination actions`() {
        val anchors = listOf(
            null,
            anchor("", 12.0, 64.0, -4.0),
            anchor("village", Double.NaN, 64.0, -4.0),
            anchor("village", 12.0, Double.POSITIVE_INFINITY, -4.0),
            anchor("village", 12.0, 64.0, Double.NEGATIVE_INFINITY),
            anchor("unloaded", 12.0, 64.0, -4.0),
            anchor("village", 16.0, 64.0, -16.1),
        )

        for (anchor in anchors) {
            val hasDestination = ActionTargetAvailability.hasUsableAnchor(anchor) { world, chunkX, chunkZ ->
                world == "village" && chunkX == 0 && chunkZ == -1
            }
            val situation = ActionSituation(false, hasDestination, hasDestination)
            val eligible = DecisionEngine.eligibleActions(NPCState.IDLE, false, situation)
            assertFalse(NPCAction.START_WORK in eligible)
            assertFalse(NPCAction.SOCIALIZE in eligible)
        }
    }

    @Test
    fun `chunk coordinates match negative block boundaries`() {
        val anchor = anchor("village", 16.0, 64.0, -16.1)
        val hasDestination = ActionTargetAvailability.hasUsableAnchor(anchor) { world, chunkX, chunkZ ->
            world == "village" && chunkX == 1 && chunkZ == -2
        }

        assertTrue(hasDestination)
    }

    private fun anchor(worldName: String, x: Double, y: Double, z: Double): AINPC.OwnedLocation =
        AINPC.OwnedLocation("work", "atelier", worldName, x, y, z)
}
