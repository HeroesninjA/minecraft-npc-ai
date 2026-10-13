package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import ro.ainpc.ai.PlayerProfile
import ro.ainpc.ai.PlayerProfileGreeting
import ro.ainpc.ai.PlayerRecognition
import ro.ainpc.ai.PlayerStyle

class DialogueTemplateRotationTest {
    @Test
    fun `does not repeat a greeting until all variants are used`() {
        val recent = mutableListOf<String>()
        val variants = listOf("Buna ziua", "Va ascult", "Bine ati venit")
        val firstCycle = List(variants.size) { DialogueTemplateRotation.choose(variants, recent) }

        assertEquals(variants.size, firstCycle.distinct().size)
        assertNotEquals(firstCycle.last(), DialogueTemplateRotation.choose(variants, recent))
    }

    @Test
    fun `keeps bounded history when switching intents`() {
        val recent = mutableListOf<String>()
        repeat(10) { index -> DialogueTemplateRotation.choose(listOf("raspuns $index"), recent) }

        assertEquals(5, recent.size)
    }

    @Test
    fun `formal player preference does not collapse rotating greetings into one reply`() {
        val recognition = PlayerRecognition.from(null, 0, 0)
        val recent = mutableListOf<String>()
        val greetings = List(3) {
            val templates = PlayerProfileGreeting.templates(PlayerProfile(style = PlayerStyle.FORMAL), recognition)!!
            DialogueTemplateRotation.choose(templates, recent)
        }

        assertEquals(3, greetings.distinct().size)
    }
}
