package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ro.ainpc.npc.NPCPersonality

class DialoguePersonalityPolicyTest {
    private val variants = listOf(
        "Buna ziua.",
        "Te ascult.",
        "Ce te aduce pe aici?",
        "Cum te pot ajuta?",
    )

    @Test
    fun `reserved and outgoing npcs choose different conversational acts`() {
        val reserved = NPCPersonality().apply { extraversion = 0.2 }
        val outgoing = NPCPersonality().apply { extraversion = 0.8 }

        for (intent in listOf(DialogueEngine.DialogueIntent.GREET, DialogueEngine.DialogueIntent.CONVERSATION)) {
            assertEquals(variants.take(2), DialoguePersonalityPolicy.variants(intent, reserved, variants))
            assertEquals(variants.drop(2), DialoguePersonalityPolicy.variants(intent, outgoing, variants))
        }
    }

    @Test
    fun `neutral personality and relationship based intents keep their choices`() {
        val neutral = NPCPersonality()
        val reserved = NPCPersonality().apply { extraversion = 0.2 }

        assertEquals(variants, DialoguePersonalityPolicy.variants(DialogueEngine.DialogueIntent.CONVERSATION, neutral, variants))
        assertEquals(variants, DialoguePersonalityPolicy.variants(DialogueEngine.DialogueIntent.GREET_GUARDED, reserved, variants))
    }

    @Test
    fun `missing preferred act keeps a usable response`() {
        val outgoing = NPCPersonality().apply { extraversion = 0.8 }
        val statements = variants.take(2)

        assertEquals(statements, DialoguePersonalityPolicy.variants(DialogueEngine.DialogueIntent.CONVERSATION, outgoing, statements))
    }
}
