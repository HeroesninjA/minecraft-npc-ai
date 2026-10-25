package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class NpcFoodRequestThrottleTest {
    private val npc = UUID(0L, 1L)
    private val otherNpc = UUID(0L, 2L)
    private val player = UUID(1L, 1L)
    private val otherPlayer = UUID(1L, 2L)
    private val now = 1_000_000L

    @Test
    fun `the same NPC waits five minutes even when the player cooldown has expired`() {
        val throttle = NpcFoodRequestThrottle()
        var sent = 0

        assertTrue(throttle.deliver(npc, player, now) { sent++ })
        assertFalse(throttle.deliver(npc, player, now + 1L) { error("repeated request") })
        assertFalse(throttle.deliver(npc, player, now + 60_000L) { error("NPC cooldown") })
        assertFalse(throttle.deliver(npc, player, now + 299_999L) { error("early boundary") })
        assertTrue(throttle.deliver(npc, player, now + 300_000L) { sent++ })
        assertEquals(2, sent)
    }

    @Test
    fun `a player receives at most one request per minute across different NPCs`() {
        val throttle = NpcFoodRequestThrottle()
        var sent = 0

        assertTrue(throttle.deliver(npc, player, now) { sent++ })
        assertFalse(throttle.deliver(otherNpc, player, now + 59_999L) { error("shared player cooldown") })
        assertTrue(throttle.deliver(otherNpc, player, now + 60_000L) { sent++ })
        assertEquals(2, sent)
    }

    @Test
    fun `switching helpers cannot bypass the NPC cooldown and rejected requests do not cool down a helper`() {
        val throttle = NpcFoodRequestThrottle()

        assertTrue(throttle.deliver(npc, player, now) {})
        assertFalse(throttle.deliver(npc, otherPlayer, now + 60_000L) { error("switched helper") })
        assertTrue(throttle.deliver(otherNpc, otherPlayer, now + 60_001L) {})
        assertTrue(throttle.deliver(npc, otherPlayer, now + 300_000L) {})
    }

    @Test
    fun `a failed send propagates and allows an immediate successful retry`() {
        val throttle = NpcFoodRequestThrottle()
        val failure = IllegalStateException("delivery failed")

        val actual = assertThrows(IllegalStateException::class.java) {
            throttle.deliver(npc, player, now) { throw failure }
        }

        assertEquals(failure, actual)
        assertTrue(throttle.deliver(npc, player, now) {})
        assertFalse(throttle.deliver(npc, player, now) { error("retry already delivered") })
    }

    @Test
    fun `invalid timestamps never deliver or occupy bounded memory`() {
        val throttle = NpcFoodRequestThrottle(maxEntries = 1)

        listOf(0L, -1L, Long.MIN_VALUE).forEach { timestamp ->
            assertFalse(throttle.deliver(npc, player, timestamp) { error("invalid time") })
        }
        assertTrue(throttle.deliver(npc, player, now) {})
    }

    @Test
    fun `a backwards clock preserves future cooldowns until their actual boundaries`() {
        val throttle = NpcFoodRequestThrottle()

        assertTrue(throttle.deliver(npc, player, now) {})
        assertFalse(throttle.deliver(npc, otherPlayer, now - 1L) { error("future NPC timestamp") })
        assertFalse(throttle.deliver(otherNpc, player, now - 1L) { error("future player timestamp") })
        assertFalse(throttle.deliver(otherNpc, player, now + 59_999L) { error("early player boundary") })
        assertTrue(throttle.deliver(otherNpc, player, now + 60_000L) {})
        assertFalse(throttle.deliver(npc, otherPlayer, now + 299_999L) { error("early NPC boundary") })
        assertTrue(throttle.deliver(npc, otherPlayer, now + 300_000L) {})
    }

    @Test
    fun `capacity rejects new identities without evicting live cooldowns and expiry frees memory`() {
        val throttle = NpcFoodRequestThrottle(maxEntries = 2)
        val thirdNpc = UUID(0L, 3L)
        val thirdPlayer = UUID(1L, 3L)

        assertTrue(throttle.deliver(npc, player, now) {})
        assertTrue(throttle.deliver(otherNpc, otherPlayer, now) {})
        assertFalse(throttle.deliver(thirdNpc, thirdPlayer, now + 1L) { error("full memory") })
        assertFalse(throttle.deliver(npc, thirdPlayer, now + 1L) { error("live cooldown retained") })
        assertFalse(throttle.deliver(thirdNpc, player, now + 1L) { error("live helper cooldown retained") })
        assertFalse(throttle.deliver(thirdNpc, thirdPlayer, now + 60_000L) { error("NPC memory still full") })
        assertTrue(throttle.deliver(thirdNpc, thirdPlayer, now + 300_000L) {})
        assertTrue(throttle.deliver(npc, player, now + 300_000L) {})
        assertFalse(throttle.deliver(otherNpc, otherPlayer, now + 300_000L) { error("new bounded memory full") })
    }

    @Test
    fun `future cooldowns are retained at capacity when the clock goes backwards`() {
        val throttle = NpcFoodRequestThrottle(maxEntries = 1)

        assertTrue(throttle.deliver(npc, player, now) {})
        assertFalse(throttle.deliver(otherNpc, otherPlayer, now - 60_000L) { error("future entry must remain") })
        assertFalse(throttle.deliver(npc, player, now + 299_999L) { error("entry must remain blocked") })
        assertTrue(throttle.deliver(otherNpc, otherPlayer, now + 300_000L) {})
    }

    @Test
    fun `reentrant sends cannot bypass cooldown bookkeeping or bounded memory`() {
        val throttle = NpcFoodRequestThrottle(maxEntries = 1)
        var nestedSent = false

        assertTrue(throttle.deliver(npc, player, now) {
            assertFalse(throttle.deliver(otherNpc, otherPlayer, now) { nestedSent = true })
            assertFalse(throttle.deliver(npc, player, now) { nestedSent = true })
        })

        assertFalse(nestedSent)
        assertFalse(throttle.deliver(otherNpc, otherPlayer, now) { error("bounded memory") })
    }

    @Test
    fun `large positive timestamps retain the same cooldown behavior without arithmetic overflow`() {
        val throttle = NpcFoodRequestThrottle()
        val timestamp = Long.MAX_VALUE - 300_000L

        assertTrue(throttle.deliver(npc, player, timestamp) {})
        assertFalse(throttle.deliver(npc, player, 1L) { error("future timestamp") })
        assertTrue(throttle.deliver(npc, player, Long.MAX_VALUE) {})
    }

    @Test
    fun `memory configuration cannot disable the limit or exceed the production maximum`() {
        assertThrows(IllegalArgumentException::class.java) { NpcFoodRequestThrottle(maxEntries = 0) }
        assertThrows(IllegalArgumentException::class.java) { NpcFoodRequestThrottle(maxEntries = -1) }
        assertThrows(IllegalArgumentException::class.java) { NpcFoodRequestThrottle(maxEntries = 4097) }
    }
}
