package ro.ainpc.ai

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class NpcSocialPromptDataTest {
    @Test
    fun `raw help receipts are removed while identity simulation and narrative remain`() {
        val original = """{"name":"Radu","narrative":{"background":"fierar"},"simulation":{"hunger_level":20,"social_experience":{"receipts":[{"helper_name":"OtherPlayer"}]}}}"""

        val sanitized = OpenAITextSupport.profileForPrompt(original)
        val profile = JsonParser.parseString(sanitized).asJsonObject

        assertFalse(sanitized.contains("OtherPlayer"))
        assertFalse(profile.getAsJsonObject("simulation").has("social_experience"))
        assertEquals(20, profile.getAsJsonObject("simulation").get("hunger_level").asInt)
        assertEquals("Radu", profile.get("name").asString)
        assertEquals("fierar", profile.getAsJsonObject("narrative").get("background").asString)
        assertEquals("OtherPlayer", JsonParser.parseString(original).asJsonObject.getAsJsonObject("simulation")
            .getAsJsonObject("social_experience").getAsJsonArray("receipts")[0].asJsonObject.get("helper_name").asString)
    }

    @Test
    fun `malformed profiles cannot forward unfiltered receipt text`() {
        for (profile in listOf("", "null", "[]", "invalid social_experience OtherPlayer")) {
            assertEquals("{}", OpenAITextSupport.profileForPrompt(profile))
        }
    }
}
