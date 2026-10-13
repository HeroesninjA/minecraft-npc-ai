package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class OpenAITextSupportFallbackRotationTest {
    @Test
    fun `same npc and message rotate through fallback replies`() {
        val npcUuid = UUID.randomUUID()
        val variants = arrayOf("prima", "a doua", "a treia")

        val replies = List(variants.size + 1) {
            OpenAITextSupport.pickResponse(npcUuid, "salut", variants)
        }

        assertEquals(variants.size, replies.take(variants.size).distinct().size)
        assertEquals(replies.first(), replies.last())
    }

    @Test
    fun `other message has its own rotation`() {
        val npcUuid = UUID.randomUUID()
        val variants = arrayOf("prima", "a doua", "a treia")
        val first = OpenAITextSupport.pickResponse(npcUuid, "salut", variants)

        OpenAITextSupport.pickResponse(npcUuid, "multumesc", variants)

        assertNotEquals(first, OpenAITextSupport.pickResponse(npcUuid, "salut", variants))
    }
}
