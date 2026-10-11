package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlayerProfileGreetingTest {
    @Test
    fun `formal preference adjusts greeting without claiming prior familiarity`() {
        val recognition = PlayerRecognition.from(null, 0, 0)
        val response = PlayerProfileGreeting.adapt(
            "Salut!", PlayerProfile(style = PlayerStyle.FORMAL), recognition, "Ana"
        )

        assertEquals("Buna ziua, Ana. Cu ce va pot ajuta?", response)
    }

    @Test
    fun `tense relationship overrides player style preference`() {
        val recognition = PlayerRecognition.from(
            NPCRelationship(trust = -1.0, interactionCount = 2), 0, 0
        )
        assertEquals(
            "Te ascult, Ana.",
            PlayerProfileGreeting.adapt(
                "Te ascult, Ana.", PlayerProfile(style = PlayerStyle.DIRECT), recognition, "Ana"
            )
        )
    }
}
