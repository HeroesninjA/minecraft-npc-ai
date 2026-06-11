package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ScenarioRoleScoringTest {
    @Test
    fun scoreBooleanReturnsConfiguredScoreOnlyWhenConditionMatches() {
        assertEquals(25, scoreBoolean(true, 25))
        assertEquals(0, scoreBoolean(false, 25))
        assertEquals(-5, scoreBoolean(true, -5))
    }

    @Test
    fun normalizeScenarioTokenTrimsAndLowercasesWithoutChangingInternalSpacing() {
        assertEquals("", normalizeScenarioToken(null))
        assertEquals("", normalizeScenarioToken(" "))
        assertEquals("fierar", normalizeScenarioToken(" FIERAR "))
        assertEquals("guard captain", normalizeScenarioToken(" Guard Captain "))
    }
}
