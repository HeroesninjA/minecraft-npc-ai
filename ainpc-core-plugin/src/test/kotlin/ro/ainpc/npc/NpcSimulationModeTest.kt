package ro.ainpc.npc

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NpcSimulationModeTest {
    @Test
    fun `only full simulation runs autonomous routine and social activities`() {
        assertTrue(NpcSimulationMode.FULL.runsAutonomousActivities())
        assertFalse(NpcSimulationMode.LIGHT.runsAutonomousActivities())
        assertFalse(NpcSimulationMode.NONE.runsAutonomousActivities())
    }
}
