package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class DialogueAddressClarificationsTest {
    private val playerId = UUID(0L, 1L)
    private val otherPlayerId = UUID(0L, 2L)
    private val sessionId = UUID(0L, 3L)
    private val bob = DialogueAddressCandidate(UUID(1L, 1L), "Bob")
    private val maria = DialogueAddressCandidate(UUID(1L, 2L), "Maria", "Măria Fierarul")
    private val candidates = listOf(bob, maria)
    private val originalMessage = "Bob, Maria, ce vindeți?"
    private var now = 1_000L
    private val clarifications = DialogueAddressClarifications({ now }, lifetimeMillis = 100L)

    @Test
    fun `selection consumes the original message once`() {
        clarifications.begin(playerId, originalMessage, candidates, sessionId)

        assertEquals(
            DialogueAddressReply.Selected(maria.npcId, originalMessage),
            clarifications.reply(playerId, "2", sessionId),
        )
        assertEquals(DialogueAddressReply.None, clarifications.reply(playerId, "2", sessionId))
    }

    @Test
    fun `numbering survives source mutation and duplicate uuids`() {
        val mutableCandidates = mutableListOf(maria, bob, maria.copy(name = "Alias"))
        val pending = clarifications.begin(playerId, originalMessage, mutableCandidates, sessionId)
        mutableCandidates.clear()

        assertEquals(listOf(maria, bob), pending?.candidates)
        assertEquals(
            DialogueAddressReply.Selected(bob.npcId, originalMessage),
            clarifications.reply(playerId, "2", sessionId),
        )
    }

    @Test
    fun `exact names and display names normalize accents case and spacing`() {
        for (input in listOf("  mArIa  ", "MĂRIA   FIERARUL")) {
            clarifications.begin(playerId, originalMessage, candidates, sessionId)

            assertEquals(
                DialogueAddressReply.Selected(maria.npcId, originalMessage),
                clarifications.reply(playerId, input, sessionId),
            )
        }
    }

    @Test
    fun `shared aliases require a numbered choice`() {
        val sameName = bob.copy(name = maria.name, displayName = maria.displayName)
        val pending = clarifications.begin(playerId, originalMessage, listOf(sameName, maria), sessionId)

        assertEquals(DialogueAddressReply.Retry(pending!!), clarifications.reply(playerId, "Maria", sessionId))
        assertEquals(DialogueAddressReply.Retry(pending), clarifications.reply(playerId, "Maria Fierarul", sessionId))
        assertEquals(
            DialogueAddressReply.Selected(bob.npcId, originalMessage),
            clarifications.reply(playerId, "1", sessionId),
        )
    }

    @Test
    fun `invalid numbers retry without extending the deadline`() {
        val pending = clarifications.begin(playerId, originalMessage, candidates, sessionId)
        now = 1_099L

        for (input in listOf("0", "3", "-1", "999999999999999999999999999999999999")) {
            assertEquals(DialogueAddressReply.Retry(pending!!), clarifications.reply(playerId, input, sessionId))
        }
        now = 1_100L
        assertEquals(DialogueAddressReply.Expired, clarifications.reply(playerId, "2", sessionId))
    }

    @Test
    fun `pending clarification supports absence of an active session`() {
        clarifications.begin(playerId, originalMessage, candidates, null)

        assertEquals(
            DialogueAddressReply.Selected(bob.npcId, originalMessage),
            clarifications.reply(playerId, "Bob", null),
        )
    }

    @Test
    fun `expired clarification reports expiration once for recognizable replies`() {
        for (input in listOf("Maria", "2", "9", "cancel")) {
            clarifications.begin(playerId, originalMessage, candidates, sessionId)
            now += 100L

            assertEquals(DialogueAddressReply.Expired, clarifications.reply(playerId, input, sessionId))
            assertEquals(DialogueAddressReply.None, clarifications.reply(playerId, "1", sessionId))
        }
    }

    @Test
    fun `expired clarification lets an unrelated new message route normally`() {
        clarifications.begin(playerId, originalMessage, candidates, sessionId)
        now += 100L

        assertEquals(DialogueAddressReply.None, clarifications.reply(playerId, "Ce oră este?", sessionId))
        assertEquals(DialogueAddressReply.None, clarifications.reply(playerId, "1", sessionId))
    }

    @Test
    fun `session changes invalidate a pending selection including null transitions`() {
        for ((previousSession, currentSession) in listOf(
            sessionId to UUID(0L, 4L),
            sessionId to null,
            null to sessionId,
        )) {
            clarifications.begin(playerId, originalMessage, candidates, previousSession)

            assertEquals(DialogueAddressReply.Expired, clarifications.reply(playerId, "1", currentSession))
            assertEquals(DialogueAddressReply.None, clarifications.reply(playerId, "1", previousSession))
        }
    }

    @Test
    fun `session expiration invalidates selection even before clarification deadline`() {
        clarifications.begin(playerId, originalMessage, candidates, sessionId)

        assertEquals(DialogueAddressReply.Expired, clarifications.reply(playerId, "Bob", sessionId, sessionExpired = true))
        assertEquals(DialogueAddressReply.None, clarifications.reply(playerId, "1", sessionId))
    }

    @Test
    fun `unrelated text clears pending and does not hijack a normal message`() {
        for (input in listOf("Îl caut pe Maria", "Bob, unde ești?", "", "Bob!")) {
            clarifications.begin(playerId, originalMessage, candidates, sessionId)

            assertEquals(DialogueAddressReply.None, clarifications.reply(playerId, input, sessionId))
            assertEquals(DialogueAddressReply.None, clarifications.reply(playerId, "2", sessionId))
        }
    }

    @Test
    fun `session mismatch lets unrelated text proceed normally`() {
        clarifications.begin(playerId, originalMessage, candidates, sessionId)

        assertEquals(DialogueAddressReply.None, clarifications.reply(playerId, "Salut!", null))
        assertEquals(DialogueAddressReply.None, clarifications.reply(playerId, "2", sessionId))
    }

    @Test
    fun `explicit cancellation consumes pending without selecting a recipient`() {
        for (input in listOf("Anulează", "cancel", "renunță", "niciunul", "niciuna")) {
            clarifications.begin(playerId, originalMessage, candidates, sessionId)

            assertEquals(DialogueAddressReply.Cancelled, clarifications.reply(playerId, input, sessionId))
            assertEquals(DialogueAddressReply.None, clarifications.reply(playerId, "1", sessionId))
        }
    }

    @Test
    fun `clearing one player leaves other pending selections intact`() {
        clarifications.begin(playerId, originalMessage, candidates, sessionId)
        clarifications.begin(otherPlayerId, "Altă întrebare", candidates, null)
        clarifications.clear(playerId)

        assertEquals(DialogueAddressReply.None, clarifications.reply(playerId, "1", sessionId))
        assertEquals(
            DialogueAddressReply.Selected(maria.npcId, "Altă întrebare"),
            clarifications.reply(otherPlayerId, "2", null),
        )
    }

    @Test
    fun `replacement keeps only the newest message and numbered choices`() {
        clarifications.begin(playerId, originalMessage, candidates, sessionId)
        clarifications.begin(playerId, "Mesaj nou", candidates.reversed(), sessionId)

        assertEquals(
            DialogueAddressReply.Selected(maria.npcId, "Mesaj nou"),
            clarifications.reply(playerId, "1", sessionId),
        )
    }

    @Test
    fun `capacity evicts oldest pending request without refreshing retries`() {
        val limited = DialogueAddressClarifications({ now }, maxPending = 2)
        limited.begin(playerId, originalMessage, candidates, null)
        limited.begin(otherPlayerId, originalMessage, candidates, null)
        limited.reply(playerId, "9", null)
        limited.begin(UUID(0L, 5L), originalMessage, candidates, null)

        assertEquals(DialogueAddressReply.None, limited.reply(playerId, "1", null))
        assertEquals(
            DialogueAddressReply.Selected(bob.npcId, originalMessage),
            limited.reply(otherPlayerId, "1", null),
        )
    }

    @Test
    fun `begin prunes expired requests before adding a pending request`() {
        val limited = DialogueAddressClarifications({ now }, lifetimeMillis = 100L, maxPending = 2)
        limited.begin(playerId, originalMessage, candidates, null)
        now += 100L
        limited.begin(otherPlayerId, originalMessage, candidates, null)

        assertEquals(DialogueAddressReply.None, limited.reply(playerId, "1", null))
        assertEquals(
            DialogueAddressReply.Selected(bob.npcId, originalMessage),
            limited.reply(otherPlayerId, "1", null),
        )
    }

    @Test
    fun `candidate limits apply after deduplication and never silently truncate`() {
        val eightCandidates = (1L..8L).map { DialogueAddressCandidate(UUID(2L, it), "NPC $it") }
        assertNotNull(clarifications.begin(playerId, originalMessage, eightCandidates, null))
        assertEquals(
            DialogueAddressReply.Selected(eightCandidates.last().npcId, originalMessage),
            clarifications.reply(playerId, "8", null),
        )
        assertNull(clarifications.begin(playerId, originalMessage, listOf(bob, bob), null))
        assertNull(clarifications.begin(playerId, originalMessage, emptyList(), null))
        assertNull(clarifications.begin(playerId, originalMessage, eightCandidates + bob, null))
        assertEquals(DialogueAddressReply.None, clarifications.reply(playerId, "1", null))
    }

    @Test
    fun `invalid replacement clears the old pending message`() {
        clarifications.begin(playerId, originalMessage, candidates, null)

        assertNull(clarifications.begin(playerId, "Mesaj nou", listOf(bob), null))
        assertEquals(DialogueAddressReply.None, clarifications.reply(playerId, "1", null))
    }

    @Test
    fun `constructor rejects nonpositive bounds`() {
        assertThrows(IllegalArgumentException::class.java) { DialogueAddressClarifications(lifetimeMillis = 0L) }
        assertThrows(IllegalArgumentException::class.java) { DialogueAddressClarifications(maxPending = 0) }
    }
}
