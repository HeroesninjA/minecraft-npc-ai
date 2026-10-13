package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerProfileGreetingTest {
    @Test
    fun `formal preference offers varied greetings without claiming prior familiarity`() {
        val recognition = PlayerRecognition.from(null, 0, 0)
        val responses = PlayerProfileGreeting.templates(PlayerProfile(style = PlayerStyle.FORMAL), recognition)!!

        assertEquals(3, responses.distinct().size)
        assertTrue(responses.all { "{player}" in it || it.startsWith("Va ascult") })
        assertTrue(responses.none { "revad" in it || "revenit" in it })
    }

    @Test
    fun `tense relationship overrides player style preference`() {
        val recognition = PlayerRecognition.from(
            NPCRelationship(trust = -1.0, interactionCount = 2), 0, 0
        )
        assertNull(PlayerProfileGreeting.templates(PlayerProfile(style = PlayerStyle.DIRECT), recognition))
    }

    @Test
    fun `direct preference offers distinct greetings to known players`() {
        val recognition = PlayerRecognition.from(null, 2, 0)

        assertEquals(
            3,
            PlayerProfileGreeting.templates(PlayerProfile(style = PlayerStyle.DIRECT), recognition)!!.distinct().size
        )
    }
}
