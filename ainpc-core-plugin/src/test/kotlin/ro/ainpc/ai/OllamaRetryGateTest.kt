package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OllamaRetryGateTest {
    @Test
    fun `failure pauses requests and allows retry after delay`() {
        var nowMillis = 1_000L
        val gate = OllamaRetryGate { nowMillis }

        assertTrue(gate.canAttempt())
        gate.recordFailure(15_000L)
        assertFalse(gate.canAttempt())
        nowMillis = 15_999L
        assertFalse(gate.canAttempt())
        nowMillis = 16_000L
        assertTrue(gate.canAttempt())
    }

    @Test
    fun `older success cannot clear a newer failure`() {
        var nowMillis = 1_000L
        val gate = OllamaRetryGate { nowMillis }
        val requestStartedAtMillis = nowMillis

        nowMillis = 2_000L
        gate.recordFailure(15_000L)
        assertFalse(gate.recordSuccess(requestStartedAtMillis))

        assertFalse(gate.canAttempt())
        nowMillis = 17_000L
        assertTrue(gate.recordSuccess(nowMillis))
        assertTrue(gate.canAttempt())
    }

    @Test
    fun `repeated failure extends pause and reload resets it`() {
        var nowMillis = 1_000L
        val gate = OllamaRetryGate { nowMillis }
        gate.recordFailure(15_000L)
        nowMillis = 4_000L
        gate.recordFailure(15_000L)
        nowMillis = 16_000L
        assertFalse(gate.canAttempt())

        gate.reset()
        assertTrue(gate.canAttempt())
    }
}
