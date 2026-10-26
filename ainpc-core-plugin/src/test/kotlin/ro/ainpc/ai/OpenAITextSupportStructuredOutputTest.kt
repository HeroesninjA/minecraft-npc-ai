package ro.ainpc.ai

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonNull
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
    fun `plain dialogue rejects unfinished responses even when they contain usable text`() {
        for (status in listOf("incomplete", "failed", "cancelled", "queued", "in_progress")) {
            for (nested in listOf(false, true)) {
                val response = JsonObject().apply {
                    addProperty("status", status)
                    if (nested) {
                        add("output", JsonArray().apply {
                            add(JsonObject().apply {
                                addProperty("type", "message")
                                add("content", JsonArray().apply {
                                    add(JsonObject().apply {
                                        addProperty("type", "output_text")
                                        addProperty("text", "Am gasit un raspuns partial.")
                                    })
                                })
                            })
                        })
                    } else addProperty("output_text", "Am gasit un raspuns partial.")
                }

                val failure = assertThrows(IOException::class.java, {
                    OpenAITextSupport.extractGeneratedText(gson, response.toString(), null)
                }, "$status nested=$nested")
                assertTrue(!OpenAITextSupport.shouldEnterOfflineBackoff(failure))
            }
        }
    }

    @Test
    fun `plain dialogue rejects an explicitly malformed status without returning partial text`() {
        for (status in listOf(JsonNull.INSTANCE, gson.toJsonTree(true), gson.toJsonTree(123), JsonObject())) {
            val response = JsonObject().apply {
                add("status", status)
                addProperty("output_text", "Raspuns neverificat.")
            }

            assertThrows(IOException::class.java) {
                OpenAITextSupport.extractGeneratedText(gson, response.toString(), null)
            }
        }
    }

    @Test
    fun `completed dialogue and legacy text without status retain speaker cleanup`() {
        for (completed in listOf(false, true)) {
            val response = JsonObject().apply {
                if (completed) addProperty("status", "completed")
                addProperty("output_text", "Bob: Buna ziua!")
            }

            assertEquals("Buna ziua!", OpenAITextSupport.extractGeneratedText(gson, response.toString(), "Bob"))
        }
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
