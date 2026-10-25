package ro.ainpc.npc

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class NpcFoodRequestPreferencesTest {
    private val player = UUID.randomUUID()
    private val now = 1_000_000L

    @Test
    fun `decline and promise delay only this helper for their full durations without renewal on reads`() {
        for ((reply, duration) in listOf(
            NpcFoodRequestPreferences.Reply.DECLINE to 900_000L,
            NpcFoodRequestPreferences.Reply.PROMISE to 600_000L,
        )) {
            val preferences = NpcFoodRequestPreferences()
            assertTrue(preferences.defer(player, reply, now))
            assertTrue(preferences.isDeferred(player, now - 1L))
            assertFalse(preferences.canAsk(player, now - 1L))
            assertTrue(preferences.isDeferred(player, now + duration - 1L))
            assertFalse(preferences.canAsk(player, now + duration - 1L))
            assertTrue(preferences.canAsk(UUID.randomUUID(), now))
            assertFalse(preferences.isDeferred(player, now + duration))
            assertTrue(preferences.canAsk(player, now + duration))
        }
    }

    @Test
    fun `delivery resolves only the matching active promise and frees its bounded slot`() {
        val preferences = NpcFoodRequestPreferences(maxEntries = 2)
        val other = UUID.randomUUID()
        val newcomer = UUID.randomUUID()
        assertTrue(preferences.defer(player, NpcFoodRequestPreferences.Reply.PROMISE, now))
        assertTrue(preferences.defer(other, NpcFoodRequestPreferences.Reply.DECLINE, now))
        assertFalse(preferences.canAsk(newcomer, now))
        assertFalse(preferences.confirmDelivery(newcomer, now))
        assertFalse(preferences.confirmDelivery(player, 0L))
        assertFalse(preferences.confirmDelivery(player, now - 1L))
        assertTrue(preferences.isDeferred(player, now))
        assertTrue(preferences.confirmDelivery(player, now))
        assertFalse(preferences.isDeferred(player, now))
        assertTrue(preferences.canAsk(newcomer, now))
        assertTrue(preferences.isDeferred(other, now))
        assertFalse(preferences.confirmDelivery(other, now))
        assertFalse(preferences.confirmDelivery(player, now + 1L))
    }

    @Test
    fun `expired promises and receipts preceding a newer promise do not resolve it`() {
        val preferences = NpcFoodRequestPreferences()
        assertTrue(preferences.defer(player, NpcFoodRequestPreferences.Reply.PROMISE, now))
        assertFalse(preferences.confirmDelivery(player, now + 600_000L))
        assertTrue(preferences.canAsk(player, now + 600_000L))
        assertTrue(preferences.defer(player, NpcFoodRequestPreferences.Reply.PROMISE, now + 600_001L))
        assertFalse(preferences.confirmDelivery(player, now + 600_000L))
        assertTrue(preferences.isDeferred(player, now + 600_001L))
        assertTrue(preferences.confirmDelivery(player, now + 600_001L))
        assertFalse(preferences.isDeferred(player, now + 600_001L))
    }

    @Test
    fun `reading the active reply preserves its kind without expiry deletion or renewal`() {
        val preferences = NpcFoodRequestPreferences()
        assertTrue(preferences.defer(player, NpcFoodRequestPreferences.Reply.PROMISE, now))
        val captured = preferences.activeReply(player, now)
        assertEquals(NpcFoodRequestPreferences.Reply.PROMISE, captured)
        assertNull(preferences.activeReply(UUID.randomUUID(), now))
        assertEquals(captured, preferences.activeReply(player, now - 1L))
        assertNull(preferences.activeReply(player, now + 600_000L))
        assertEquals(captured, preferences.activeReply(player, now + 599_999L))
        assertTrue(preferences.confirmDelivery(player, now + 1L))
        assertNull(preferences.activeReply(player, now + 1L))
        assertEquals(NpcFoodRequestPreferences.Reply.PROMISE, captured)
    }

    @Test
    fun `bounded preference storage retains active pauses and releases expired ones`() {
        val preferences = NpcFoodRequestPreferences(maxEntries = 1)
        val other = UUID.randomUUID()
        assertFalse(preferences.defer(player, NpcFoodRequestPreferences.Reply.DECLINE, 0L))
        assertFalse(preferences.canAsk(player, -1L))
        assertTrue(preferences.defer(player, NpcFoodRequestPreferences.Reply.DECLINE, now))
        assertFalse(preferences.canAsk(other, now))
        assertFalse(preferences.defer(other, NpcFoodRequestPreferences.Reply.PROMISE, now))
        assertTrue(preferences.isDeferred(player, now))
        assertTrue(preferences.canAsk(other, now + 900_000L))
        assertTrue(preferences.defer(other, NpcFoodRequestPreferences.Reply.PROMISE, now + 900_000L))
    }
}
