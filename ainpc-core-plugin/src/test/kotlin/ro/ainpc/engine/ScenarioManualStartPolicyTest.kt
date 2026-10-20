package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScenarioManualStartPolicyTest {
    @Test
    fun `requires the minimum number of active nearby NPCs`() {
        val template = ScenarioTemplate(ScenarioType.THEFT).apply { minimumNpcCount = 2 }

        assertFalse(ScenarioManualStartPolicy.inspect(template, 1).meetsMinimum)
        assertTrue(ScenarioManualStartPolicy.inspect(template, 2).meetsMinimum)
        assertTrue(ScenarioManualStartPolicy.inspect(template, 3).meetsMinimum)
        assertEquals(2, ScenarioManualStartPolicy.inspect(template, 1).minimumNpcCount)
        assertEquals(1, ScenarioManualStartPolicy.inspect(template, 1).activeNpcCount)
    }

    @Test
    fun `normalizes malformed counts without raising the threshold`() {
        val template = ScenarioTemplate(ScenarioType.THEFT).apply { minimumNpcCount = -1 }

        val readiness = ScenarioManualStartPolicy.inspect(template, -5)

        assertEquals(0, readiness.minimumNpcCount)
        assertEquals(0, readiness.activeNpcCount)
        assertTrue(readiness.meetsMinimum)
    }
}
