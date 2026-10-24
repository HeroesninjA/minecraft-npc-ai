package ro.ainpc.ai

import ro.ainpc.world.WorldBlueprintSuggestionField
import ro.ainpc.world.WorldBlueprintSuggestionParser
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

    @Test
    fun structuredSuggestionUsesStrictFormatWithoutChangingTextDefault() {
        val format = WorldBlueprintSuggestionParser.responseFormat(WorldBlueprintSuggestionField.PROFESSION)
        val body = OpenAIService.buildResponsesRequestBody("gpt-5.4-nano", "Sugereaza", 0.7, 256, false, format)
        val sentFormat = body.getAsJsonObject("text").getAsJsonObject("format")

        assertEquals(format, sentFormat)
        assertEquals("json_schema", sentFormat.get("type").asString)
        sentFormat.addProperty("name", "changed")
        assertEquals("ainpc_world_blueprint_profession", format.get("name").asString)
        assertEquals("text", OpenAIService.buildResponsesRequestBody("gpt-5.4-nano", "Salut", 0.7, 256, false)
            .getAsJsonObject("text").getAsJsonObject("format").get("type").asString)
    }
}
