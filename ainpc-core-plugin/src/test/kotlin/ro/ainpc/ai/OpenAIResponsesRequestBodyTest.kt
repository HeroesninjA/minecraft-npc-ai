package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpenAIResponsesRequestBodyTest {
    @Test
    fun gpt6LunaOmitsTemperatureAtDefaultReasoningEffort() {
        val body = OpenAIService.buildResponsesRequestBody("gpt-6-luna", "Salut", 0.7, 256, false)

        assertEquals("gpt-6-luna", body.get("model").asString)
        assertEquals("Salut", body.get("input").asString)
        assertFalse(body.has("temperature"))
        assertEquals(256, body.get("max_output_tokens").asInt)
        assertFalse(body.get("store").asBoolean)
        assertEquals("text", body.getAsJsonObject("text").getAsJsonObject("format").get("type").asString)
    }

    @Test
    fun gpt54NanoKeepsConfiguredTemperature() {
        val body = OpenAIService.buildResponsesRequestBody("gpt-5.4-nano", "Salut", 0.7, 256, false)

        assertTrue(body.has("temperature"))
        assertEquals(0.7, body.get("temperature").asDouble)
    }
}
