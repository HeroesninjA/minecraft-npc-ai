package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class NpcFoodRequestFollowUpsTest {
    private val now = 1_000_000L
    private val request = NpcFoodRequestFollowUps.Request(
        UUID.randomUUID(), UUID.randomUUID(), 0L, UUID.randomUUID(), UUID.randomUUID(), now,
    )

    @Test
    fun `only the exact recent delivered request can be consumed once by its player`() {
        val followUps = NpcFoodRequestFollowUps()
        assertTrue(followUps.record(request))
        assertNull(followUps.find(UUID.randomUUID(), now))
        assertNull(followUps.find(request.playerUuid, now - 1L))
        assertFalse(followUps.consume(request.copy(npcUuid = UUID.randomUUID()), now))
        assertEquals(request, followUps.find(request.playerUuid, now + 44_999L))
        assertTrue(followUps.consume(request, now + 44_999L))
        assertFalse(followUps.consume(request, now + 44_999L))
    }

    @Test
    fun `expiry logout and bounded capacity do not overwrite a live or future request`() {
        val followUps = NpcFoodRequestFollowUps(maxEntries = 1)
        val other = request.copy(playerUuid = UUID.randomUUID())
        assertFalse(followUps.record(request.copy(sentAt = 0L)))
        assertTrue(followUps.record(request))
        assertFalse(followUps.record(other))
        assertFalse(followUps.canRecord(other.playerUuid, now - 1L))
        assertNull(followUps.find(request.playerUuid, now + 45_000L))
        assertTrue(followUps.record(other.copy(sentAt = now + 45_000L)))
        followUps.forgetPlayer(other.playerUuid)
        assertNull(followUps.find(other.playerUuid, now + 45_000L))
        assertTrue(followUps.canRecord(request.playerUuid, now + 45_000L))
    }
}
