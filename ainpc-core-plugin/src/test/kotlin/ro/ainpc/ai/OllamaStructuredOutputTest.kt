package ro.ainpc.ai

import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.world.WorldBlueprintSuggestionField
import ro.ainpc.world.WorldBlueprintSuggestionParser

class OllamaStructuredOutputTest {
    @Test
    fun `schema is sent as format without changing the plain request`() {
        val responseFormat = WorldBlueprintSuggestionParser.responseFormat(WorldBlueprintSuggestionField.PROFESSION)
        val schema = responseFormat.getAsJsonObject("schema")
        val structured = OllamaService.generateRequestBody("llama3", "Sugereaza", 0.4, 256, schema)
        val plain = OllamaService.generateRequestBody("llama3", "Salut", 0.4, 256)

        assertEquals(schema, structured.getAsJsonObject("format"))
        assertFalse(plain.has("format"))
        assertFalse(structured.get("stream").asBoolean)
        structured.getAsJsonObject("format").addProperty("type", "changed")
        assertEquals("object", schema.get("type").asString)
    }

    @Test
    fun `structured response preserves long JSON and rejects unfinished generation`() {
        val jsonText = "{\"text\":\"${"a".repeat(250)}\"}"
        val completed = JsonObject().apply {
            addProperty("done", true)
            addProperty("response", jsonText)
        }.toString()
        val unfinished = JsonObject().apply {
            addProperty("done", false)
            addProperty("response", "{\"limit\":2}")
        }.toString()
        val tokenLimited = JsonObject().apply {
            addProperty("done", true)
            addProperty("done_reason", "length")
            addProperty("response", "{\"limit\":2}")
        }.toString()

        assertEquals(jsonText, OllamaService.extractGeneratedResponse(completed, true))
        assertNull(OllamaService.extractGeneratedResponse(unfinished, true))
        assertNull(OllamaService.extractGeneratedResponse(tokenLimited, true))
        assertEquals("{\"limit\":2}", OllamaService.extractGeneratedResponse(unfinished, false))
        assertTrue(jsonText.length > 200)
    }
}
