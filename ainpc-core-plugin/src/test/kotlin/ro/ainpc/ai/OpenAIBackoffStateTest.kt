package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class OpenAIBackoffStateTest {
    @Test
    fun `admission opens at the exact deadline without resetting failure progression`() {
        val state = OpenAIBackoffState()
        state.scheduleFailure(100L) { 50L }

        assertEquals("offline_backoff", state.blockedReason(149L))
        assertNull(state.blockedReason(150L))
        assertNull(state.blockedReason(151L))
        assertEquals(150L, state.retryAfterMillis)
        assertTrue(state.offlineMode)
        state.scheduleFailure(151L) { attempt ->
            assertEquals(2, attempt)
            100L
        }
    }

    @Test
    fun `administrative suspension takes precedence and clearing it preserves active cooldown`() {
        val state = OpenAIBackoffState()
        state.scheduleFailure(100L) { 50L }
        state.suspendApi("HTTP 401 (invalid_api_key)")

        assertEquals("api_suspended", state.blockedReason(149L))
        assertEquals("api_suspended", state.blockedReason(151L))
        state.reset(clearCooldown = false)
        assertEquals("offline_backoff", state.blockedReason(149L))
        assertNull(state.blockedReason(150L))
    }

    @Test
    fun `shared state serializes concurrent failure progression without losing attempts`() {
        val state = OpenAIBackoffState()
        val attempts = ConcurrentLinkedQueue<Int>()
        val executor = Executors.newFixedThreadPool(4)
        try {
            val pending = (1..32).map {
                CompletableFuture.runAsync({
                    state.scheduleFailure(0L) { attempt ->
                        attempts.add(attempt)
                        attempt * 1000L
                    }
                }, executor)
            }
            pending.forEach { it.get(10L, TimeUnit.SECONDS) }

            assertEquals((1..32).toList(), attempts.sorted())
            assertEquals(32_000L, state.retryAfterMillis)
        } finally {
            executor.shutdownNow()
        }
    }
}
