package ro.ainpc.ai

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpenAITextSupportStructuredOutputTest {
    private val gson = Gson()
    private val structuredText = "{\"text\":\"${"a".repeat(250)}\"}"

    @Test
    fun `preserves long direct structured output without dialogue truncation`() {
        val response = JsonObject().apply {
            addProperty("status", "completed")
            addProperty("output_text", structuredText)
        }.toString()

        assertEquals(structuredText, OpenAITextSupport.extractGeneratedText(gson, response, null, true))
        assertTrue(OpenAITextSupport.extractGeneratedText(gson, response, null).length <= 203)
    }

    @Test
    fun `preserves long nested structured output`() {
        val content = JsonObject().apply {
            addProperty("type", "output_text")
            addProperty("text", structuredText)
        }
        val message = JsonObject().apply {
            addProperty("type", "message")
            add("content", JsonArray().apply { add(content) })
        }
        val response = JsonObject().apply {
            addProperty("status", "completed")
            add("output", JsonArray().apply { add(message) })
        }.toString()

        assertEquals(structuredText, OpenAITextSupport.extractGeneratedText(gson, response, null, true))
    }

    @Test
    fun `rejects incomplete response even when JSON text looks valid`() {
        val response = JsonObject().apply {
            addProperty("status", "incomplete")
            addProperty("output_text", "{\"limit\":2}")
        }.toString()

        assertThrows(IOException::class.java) {
            OpenAITextSupport.extractGeneratedText(gson, response, null, true)
        }
    }
}
