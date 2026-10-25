package ro.ainpc.managers

import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.ai.DialogueAddressCandidate
import ro.ainpc.ai.DialogueAddressReply
import ro.ainpc.npc.AINPC
import java.lang.reflect.Proxy
import java.util.UUID

class ConversationSessionManagerTest {
    private var nowMillis = 1_000L
    private val manager = ConversationSessionManager(newPluginInstance()) { nowMillis }
    private val player = newPlayer()
    private val bob = AINPC(null).apply { name = "Bob" }
    private val maria = AINPC(null).apply { name = "Maria" }
    private val addressCandidates = listOf(
        DialogueAddressCandidate(bob.uuid, bob.name),
        DialogueAddressCandidate(maria.uuid, maria.name),
    )

    @Test
    fun `npc stays busy until every live participant leaves or expires`() {
        val otherPlayer = newPlayer()
        manager.startConversation(player, bob)
        nowMillis += 1_000L
        manager.startConversation(otherPlayer, bob)
        assertTrue(manager.hasActiveConversation(bob.uuid))
        assertFalse(manager.hasActiveConversation(maria.uuid))

        manager.clearConversation(player)
        assertTrue(manager.hasActiveConversation(bob.uuid))
        nowMillis += ConversationSessionManager.IDLE_TIMEOUT_MILLIS
        assertFalse(manager.hasActiveConversation(bob.uuid))
        assertEquals(bob.uuid, manager.getConversationNpcId(otherPlayer))

        manager.startConversation(otherPlayer, maria)
        assertFalse(manager.hasActiveConversation(bob.uuid))
        assertTrue(manager.hasActiveConversation(maria.uuid))
        manager.clearConversation(otherPlayer)
        assertFalse(manager.hasActiveConversation(maria.uuid))
    }

    @Test
    fun `returning to a previous npc invalidates both earlier sessions`() {
        manager.startConversation(player, bob)
        val firstBobSession = manager.getConversationSessionId(player)
        assertNotNull(firstBobSession)

        manager.startConversation(player, maria)
        val mariaSession = manager.getConversationSessionId(player)
        assertFalse(manager.isCurrentSession(player, firstBobSession))

        manager.startConversation(player, bob)
        val secondBobSession = manager.getConversationSessionId(player)

        assertEquals(bob.uuid, manager.getConversationNpcId(player))
        assertNotEquals(firstBobSession, secondBobSession)
        assertFalse(manager.isCurrentSession(player, firstBobSession))
        assertFalse(manager.isCurrentSession(player, mariaSession))
        assertTrue(manager.isCurrentSession(player, secondBobSession))
    }

    @Test
    fun `restarting a conversation with the same npc invalidates pending replies`() {
        manager.startConversation(player, bob)
        val previousSession = manager.getConversationSessionId(player)

        manager.startConversation(player, bob)

        assertFalse(manager.isCurrentSession(player, previousSession))
        assertTrue(manager.isCurrentSession(player, manager.getConversationSessionId(player)))
    }

    @Test
    fun `refreshing conversation activity preserves its identity`() {
        manager.startConversation(player, bob)
        val sessionId = manager.getConversationSessionId(player)

        manager.touchConversation(player)
        manager.touchConversation(player)

        assertEquals(sessionId, manager.getConversationSessionId(player))
        assertEquals(bob.uuid, manager.getConversationNpcId(player))
        assertTrue(manager.isCurrentSession(player, sessionId))
    }

    @Test
    fun `clearing a conversation invalidates its identity and touch cannot restore it`() {
        manager.startConversation(player, bob)
        val sessionId = manager.getConversationSessionId(player)

        manager.clearConversation(player)
        manager.touchConversation(player)

        assertNull(manager.getConversationSessionId(player))
        assertFalse(manager.isInConversation(player))
        assertFalse(manager.isCurrentSession(player, sessionId))
        assertFalse(manager.isCurrentSession(player, null))
    }

    @Test
    fun `players have independent sessions even with the same npc`() {
        val otherPlayer = newPlayer()
        manager.startConversation(player, bob)
        manager.startConversation(otherPlayer, bob)
        val sessionId = manager.getConversationSessionId(player)
        val otherSessionId = manager.getConversationSessionId(otherPlayer)

        assertNotEquals(sessionId, otherSessionId)
        assertFalse(manager.isCurrentSession(otherPlayer, sessionId))
        manager.clearConversation(player.uniqueId)

        assertFalse(manager.isCurrentSession(player, sessionId))
        assertTrue(manager.isCurrentSession(otherPlayer, otherSessionId))
        assertEquals(bob.uuid, manager.getConversationNpcId(otherPlayer))
    }

    @Test
    fun `clarification preserves active session and returns original message only once`() {
        manager.startConversation(player, bob)
        val sessionId = manager.getConversationSessionId(player)
        val originalMessage = "Bob, Maria, ce vindeți?"

        assertNotNull(manager.beginAddressClarification(player, originalMessage, addressCandidates))
        manager.touchConversation(player)

        assertEquals(sessionId, manager.getConversationSessionId(player))
        assertEquals(bob.uuid, manager.getConversationNpcId(player))
        assertEquals(
            DialogueAddressReply.Selected(maria.uuid, originalMessage),
            manager.replyToAddressClarification(player, "2", 60_000L),
        )
        assertEquals(DialogueAddressReply.None, manager.replyToAddressClarification(player, "2", 60_000L))
        assertEquals(sessionId, manager.getConversationSessionId(player))
    }

    @Test
    fun `starting another conversation or restarting same npc clears clarification`() {
        for (nextNpc in listOf(maria, bob)) {
            manager.startConversation(player, bob)
            val previousSessionId = manager.getConversationSessionId(player)
            manager.beginAddressClarification(player, "Bob, Maria, salut!", addressCandidates)

            manager.startConversation(player, nextNpc)

            assertNotEquals(previousSessionId, manager.getConversationSessionId(player))
            assertEquals(nextNpc.uuid, manager.getConversationNpcId(player))
            assertEquals(DialogueAddressReply.None, manager.replyToAddressClarification(player, "2", 60_000L))
        }
    }

    @Test
    fun `clearing conversation through either overload discards clarification`() {
        manager.startConversation(player, bob)
        manager.beginAddressClarification(player, "Bob, Maria, salut!", addressCandidates)

        manager.clearConversation(player)

        assertEquals(DialogueAddressReply.None, manager.replyToAddressClarification(player, "2", 60_000L))
        manager.startConversation(player, bob)
        manager.beginAddressClarification(player, "Bob, Maria, salut!", addressCandidates)

        manager.clearConversation(player.uniqueId)

        assertEquals(DialogueAddressReply.None, manager.replyToAddressClarification(player, "2", 60_000L))
        assertNull(manager.getConversationSessionId(player))
    }

    @Test
    fun `clearing only clarification preserves active conversation`() {
        manager.startConversation(player, bob)
        val sessionId = manager.getConversationSessionId(player)
        manager.beginAddressClarification(player, "Bob, Maria, salut!", addressCandidates)

        manager.clearAddressClarification(player)

        assertEquals(DialogueAddressReply.None, manager.replyToAddressClarification(player, "2", 60_000L))
        assertEquals(sessionId, manager.getConversationSessionId(player))
        assertEquals(bob.uuid, manager.getConversationNpcId(player))
    }

    @Test
    fun `recipient can be clarified before a conversation is started`() {
        val originalMessage = "Bob, Maria, ce vindeți?"

        assertNotNull(manager.beginAddressClarification(player, originalMessage, addressCandidates))

        assertEquals(
            DialogueAddressReply.Selected(bob.uuid, originalMessage),
            manager.replyToAddressClarification(player, "1", 60_000L),
        )
        assertFalse(manager.isInConversation(player))
        assertNull(manager.getConversationSessionId(player))
    }

    @Test
    fun `idle session expires at its deadline without requiring another message`() {
        manager.startConversation(player, bob)
        val sessionId = manager.getConversationSessionId(player)
        nowMillis += ConversationSessionManager.IDLE_TIMEOUT_MILLIS - 1

        assertTrue(manager.isCurrentSession(player, sessionId))
        assertTrue(manager.isInConversation(player))
        assertFalse(manager.isExpired(player))

        nowMillis += 1

        assertFalse(manager.isCurrentSession(player, sessionId))
        assertFalse(manager.isInConversation(player))
        assertTrue(manager.isExpired(player))
        assertEquals(sessionId, manager.getConversationSessionId(player))
        assertEquals(bob.uuid, manager.getConversationNpcId(player))
    }

    @Test
    fun `activity before expiration extends deadline without changing session identity`() {
        manager.startConversation(player, bob)
        val sessionId = manager.getConversationSessionId(player)
        nowMillis += ConversationSessionManager.IDLE_TIMEOUT_MILLIS - 1

        manager.touchConversation(player)
        nowMillis += ConversationSessionManager.IDLE_TIMEOUT_MILLIS - 1

        assertEquals(sessionId, manager.getConversationSessionId(player))
        assertTrue(manager.isCurrentSession(player, sessionId))
        assertFalse(manager.isExpired(player))

        nowMillis += 1

        assertFalse(manager.isCurrentSession(player, sessionId))
        assertTrue(manager.isExpired(player))
    }

    @Test
    fun `touch cannot revive an expired session and reopening creates another identity`() {
        manager.startConversation(player, bob)
        val expiredSessionId = manager.getConversationSessionId(player)
        nowMillis += ConversationSessionManager.IDLE_TIMEOUT_MILLIS

        manager.touchConversation(player)

        assertTrue(manager.isExpired(player))
        assertFalse(manager.isCurrentSession(player, expiredSessionId))
        nowMillis += 1
        manager.touchConversation(player)

        assertFalse(manager.isInConversation(player))
        assertFalse(manager.isCurrentSession(player, expiredSessionId))
        assertEquals(expiredSessionId, manager.getConversationSessionId(player))

        manager.startConversation(player, bob)

        assertNotEquals(expiredSessionId, manager.getConversationSessionId(player))
        assertFalse(manager.isCurrentSession(player, expiredSessionId))
        assertTrue(manager.isCurrentSession(player, manager.getConversationSessionId(player)))
        assertTrue(manager.isInConversation(player))
        assertFalse(manager.isExpired(player))
    }

    @Test
    fun `activity only extends the deadline of the interacting player`() {
        val otherPlayer = newPlayer()
        manager.startConversation(player, bob)
        manager.startConversation(otherPlayer, bob)
        val sessionId = manager.getConversationSessionId(player)
        val otherSessionId = manager.getConversationSessionId(otherPlayer)
        nowMillis += ConversationSessionManager.IDLE_TIMEOUT_MILLIS - 1

        manager.touchConversation(otherPlayer)
        nowMillis += 1

        assertFalse(manager.isCurrentSession(player, sessionId))
        assertTrue(manager.isExpired(player))
        assertTrue(manager.isCurrentSession(otherPlayer, otherSessionId))
        assertFalse(manager.isExpired(otherPlayer))
    }

    @Test
    fun `session expiration rejects clarification even before its own deadline`() {
        manager.startConversation(player, bob)
        nowMillis += ConversationSessionManager.IDLE_TIMEOUT_MILLIS - 1
        assertNotNull(manager.beginAddressClarification(player, "Bob, Maria, salut!", addressCandidates))
        nowMillis += 1

        assertEquals(DialogueAddressReply.Expired, manager.replyToAddressClarification(player, "2"))
        assertEquals(DialogueAddressReply.None, manager.replyToAddressClarification(player, "2"))
        assertTrue(manager.isExpired(player))
    }

    @Test
    fun `missing session is neither active nor expired`() {
        manager.touchConversation(player)

        assertFalse(manager.isExpired(player))
        assertFalse(manager.isInConversation(player))
        assertFalse(manager.isCurrentSession(player, UUID.randomUUID()))
        assertFalse(manager.isCurrentSession(player, null))
        assertNull(manager.getConversationSessionId(player))
    }

    private fun newPlayer(): Player {
        val playerUuid = UUID.randomUUID()
        return Proxy.newProxyInstance(
            Player::class.java.classLoader,
            arrayOf(Player::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getUniqueId" -> playerUuid
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Player
    }

    private fun newPluginInstance(): AINPCPlugin {
        val field = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        val unsafe = field.get(null) as sun.misc.Unsafe
        return unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
    }
}
