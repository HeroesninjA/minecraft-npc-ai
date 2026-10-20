package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ScenarioActorExpirationTest {
    @Test
    fun `deadline uses wall clock and rejects invalid durations`() {
        assertEquals(31_000L, ScenarioActorExpiration.deadline(1_000L, 30L))
        assertNull(ScenarioActorExpiration.deadline(1_000L, null))
        assertNull(ScenarioActorExpiration.deadline(1_000L, 0L))
        assertNull(ScenarioActorExpiration.deadline(1_000L, -1L))
        assertNull(ScenarioActorExpiration.deadline(1_000L, Long.MAX_VALUE))
        assertNull(ScenarioActorExpiration.deadline(Long.MAX_VALUE - 500L, 1L))
    }

    @Test
    fun `recovery delay rounds up and rejects expired or inconsistent deadlines`() {
        assertEquals(1L, ScenarioActorExpiration.recoveryDelayTicks(1_999L, 1_000L, 1L, 2_000L))
        assertEquals(20L, ScenarioActorExpiration.recoveryDelayTicks(1_000L, 0L, 1L, 2_000L))
        assertNull(ScenarioActorExpiration.recoveryDelayTicks(2_000L, 1_000L, 1L, 2_000L))
        assertNull(ScenarioActorExpiration.recoveryDelayTicks(999L, 1_000L, 1L, 2_000L))
        assertNull(ScenarioActorExpiration.recoveryDelayTicks(1_000L, 1_001L, 1L, 2_000L))
        assertNull(ScenarioActorExpiration.recoveryDelayTicks(1_000L, 0L, null, 2_000L))
        assertNull(ScenarioActorExpiration.recoveryDelayTicks(1_000L, 0L, Long.MAX_VALUE, Long.MAX_VALUE))
    }

    @Test
    fun `recovery status distinguishes elapsed deadlines from inconsistent evidence`() {
        assertEquals(ScenarioActorExpiration.RecoveryStatus.PENDING,
            ScenarioActorExpiration.recoveryStatus(1_999L, 1_000L, 1L, 2_000L))
        assertEquals(ScenarioActorExpiration.RecoveryStatus.EXPIRED,
            ScenarioActorExpiration.recoveryStatus(2_000L, 1_000L, 1L, 2_000L))
        assertEquals(ScenarioActorExpiration.RecoveryStatus.EXPIRED,
            ScenarioActorExpiration.recoveryStatus(2_001L, 1_000L, 1L, 2_000L))
        assertEquals(ScenarioActorExpiration.RecoveryStatus.INCONSISTENT,
            ScenarioActorExpiration.recoveryStatus(999L, 1_000L, 1L, 2_000L))
        assertEquals(ScenarioActorExpiration.RecoveryStatus.INCONSISTENT,
            ScenarioActorExpiration.recoveryStatus(2_000L, 1_001L, 1L, 2_000L))
        assertEquals(ScenarioActorExpiration.RecoveryStatus.INCONSISTENT,
            ScenarioActorExpiration.recoveryStatus(2_000L, 1_000L, 1L, null))
        assertEquals(ScenarioActorExpiration.RecoveryStatus.INCONSISTENT,
            ScenarioActorExpiration.recoveryStatus(Long.MAX_VALUE, 1_000L, Long.MAX_VALUE, Long.MAX_VALUE))
    }
}
