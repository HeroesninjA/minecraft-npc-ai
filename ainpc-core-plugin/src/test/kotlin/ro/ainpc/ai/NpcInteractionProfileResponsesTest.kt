package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.NpcInteractionProfile
import java.util.UUID

class NpcInteractionProfileResponsesTest {
    @Test
    fun `blocked profile replies rotate without immediate repetition`() {
        val responses = NpcInteractionProfileResponses()
        val npcUuid = UUID.randomUUID()
        val playerUuid = UUID.randomUUID()
        val replies = List(4) {
            responses.resolve(
                NpcInteractionProfile.MINIMAL, npcUuid, playerUuid, "Alex",
                questAvailable = false, shopAvailable = false,
            )!!
        }

        assertEquals(3, replies.take(3).distinct().size)
        assertEquals(replies.first(), replies.last())
        assertTrue(replies.any { it.contains("Alex") })
    }

    @Test
    fun `rotation stays isolated per player and interaction profile`() {
        val responses = NpcInteractionProfileResponses()
        val npcUuid = UUID.randomUUID()
        val firstPlayer = UUID.randomUUID()
        val secondPlayer = UUID.randomUUID()
        val firstReply = responses.resolve(
            NpcInteractionProfile.MINIMAL, npcUuid, firstPlayer, "Alex", false, false,
        )

        responses.resolve(NpcInteractionProfile.MINIMAL, npcUuid, secondPlayer, "Mara", false, false)

        assertNotEquals(
            firstReply,
            responses.resolve(NpcInteractionProfile.MINIMAL, npcUuid, firstPlayer, "Alex", false, false),
        )
    }

    @Test
    fun `quest and shop profiles open only when their capability is available`() {
        val responses = NpcInteractionProfileResponses()
        val npcUuid = UUID.randomUUID()
        val playerUuid = UUID.randomUUID()

        assertNotNull(responses.resolve(NpcInteractionProfile.QUEST_ONLY, npcUuid, playerUuid, "Alex", false, false))
        assertNull(responses.resolve(NpcInteractionProfile.QUEST_ONLY, npcUuid, playerUuid, "Alex", true, false))
        assertNotNull(responses.resolve(NpcInteractionProfile.SHOP_ONLY, npcUuid, playerUuid, "Alex", false, false))
        assertNull(responses.resolve(NpcInteractionProfile.SHOP_ONLY, npcUuid, playerUuid, "Alex", false, true))
        assertNull(responses.resolve(NpcInteractionProfile.FULL_AI, npcUuid, playerUuid, "Alex", false, false))
    }

    @Test
    fun `shop profile explains missing permission instead of missing stock`() {
        val responses = NpcInteractionProfileResponses()
        val npcUuid = UUID.randomUUID()
        val playerUuid = UUID.randomUUID()
        val replies = List(3) {
            responses.resolve(
                NpcInteractionProfile.SHOP_ONLY, npcUuid, playerUuid, "Alex",
                questAvailable = false, shopAvailable = false, shopAccessDenied = true,
            )!!
        }

        assertEquals(3, replies.distinct().size)
        assertTrue(replies.all { it.contains("acces") || it.contains("permisiunea") || it.contains("Nu iti pot vinde") })
    }

    @Test
    fun `scene only profile blocks direct free conversation`() {
        val response = NpcInteractionProfileResponses().resolve(
            NpcInteractionProfile.SCENE_ONLY, UUID.randomUUID(), UUID.randomUUID(), "Alex", false, false,
        )

        assertNotNull(response)
    }

    @Test
    fun `conversation rotation cache stays bounded`() {
        val responses = NpcInteractionProfileResponses(maxConversations = 8)
        repeat(20) {
            responses.resolve(
                NpcInteractionProfile.MINIMAL, UUID.randomUUID(), UUID.randomUUID(), "Alex", false, false,
            )
        }

        assertEquals(8, responses.trackedConversationCount())
    }
}
