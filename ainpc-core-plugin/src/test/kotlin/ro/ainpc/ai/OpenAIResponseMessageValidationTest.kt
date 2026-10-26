package ro.ainpc.ai

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.IOException

class OpenAIResponseMessageValidationTest {
    private val gson = Gson()

    @Test
    fun `unfinished nested message cannot be returned even through direct text`() {
        for (status in listOf("incomplete", "in_progress", "failed", "cancelled")) {
            for (direct in listOf(false, true)) {
                val message = message(text("Raspuns partial.")).apply { addProperty("status", status) }
                assertRejected(response(message, direct))
            }
        }
    }

    @Test
    fun `explicit malformed nested message status is rejected`() {
        for (status in listOf(JsonNull.INSTANCE, gson.toJsonTree(true), gson.toJsonTree(123), JsonObject(), JsonArray())) {
            val message = message(text("Raspuns neverificat.")).apply { add("status", status) }
            assertRejected(response(message, direct = true))
        }
    }

    @Test
    fun `message explicitly attributed to another role is never an NPC reply`() {
        for (role in listOf("user", "system", "developer", "tool", "")) {
            for (direct in listOf(false, true)) {
                val message = message(text("Text atribuit altui rol.")).apply { addProperty("role", role) }
                assertRejected(response(message, direct))
            }
        }
        for (role in listOf(JsonNull.INSTANCE, gson.toJsonTree(true), gson.toJsonTree(123), JsonObject(), JsonArray())) {
            val message = message(text("Text cu rol invalid.")).apply { add("role", role) }
            assertRejected(response(message, direct = true))
        }
    }

    @Test
    fun `refusal after usable text invalidates the response including direct structured output`() {
        val refusal = JsonObject().apply {
            addProperty("type", "refusal")
            addProperty("refusal", "REFUSAL_PRIVATE_DETAIL")
        }
        for (direct in listOf(false, true)) {
            val combinedMessage = message(text("Text care nu trebuie acceptat."), refusal)
            assertRejected(response(combinedMessage, direct))

            val separateMessages = response(message(text("Text care nu trebuie acceptat.")), direct).apply {
                getAsJsonArray("output").add(message(refusal))
            }
            assertRejected(separateMessages)
        }
    }

    @Test
    fun `non string values cannot become generated dialogue or structured JSON text`() {
        for (value in listOf(gson.toJsonTree(123), gson.toJsonTree(true), JsonObject(), JsonArray())) {
            val direct = JsonObject().apply {
                addProperty("status", "completed")
                add("output_text", value)
            }
            assertRejected(direct)

            val nested = response(message(JsonObject().apply {
                addProperty("type", "output_text")
                add("text", value)
            }))
            assertRejected(nested)
            nested.addProperty("output_text", "Replica aparent valida.")
            assertRejected(nested)
        }
    }

    @Test
    fun `completed assistant messages are combined while reasoning and tools remain separate`() {
        val response = response(message(text("Buna ziua!"))).apply {
            val messages = getAsJsonArray("output")
            add("output", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("type", "reasoning")
                    addProperty("status", "in_progress")
                    add("content", JsonArray().apply { add(text("PRIVATE_REASONING")) })
                })
                addAll(messages)
            })
            getAsJsonArray("output").add(JsonObject().apply {
                addProperty("type", "function_call")
                addProperty("status", "in_progress")
                add("content", JsonArray().apply { add(text("TOOL_RESULT")) })
            })
            getAsJsonArray("output").add(message(text("Piata este la nord.")))
        }

        assertEquals(
            "Buna ziua!\nPiata este la nord.",
            OpenAITextSupport.extractGeneratedText(gson, response.toString(), "Bob", true),
        )
    }

    @Test
    fun `legacy nested messages without status or role remain accepted`() {
        val message = message(text("Bob: Buna ziua!").apply { addProperty("type", "text") }).apply {
            remove("status")
            remove("role")
        }

        assertEquals("Buna ziua!", OpenAITextSupport.extractGeneratedText(gson, response(message).toString(), "Bob"))
        val legacyResponse = response(message).apply { remove("status") }
        assertEquals("Buna ziua!", OpenAITextSupport.extractGeneratedText(gson, legacyResponse.toString(), "Bob"))
    }

    @Test
    fun `refusal is not retried as a dialogue formatting problem or network outage`() {
        val refused = response(message(text("Replica aparent valida."), JsonObject().apply {
            addProperty("type", "refusal")
            addProperty("refusal", "REFUSAL_PRIVATE_DETAIL")
        }), direct = true)
        var attempts = 0

        val failure = assertThrows(IOException::class.java) {
            DialogueResponseNovelty.generate(
                "Prompt\nBob: ", "Bob", "Salut!", emptyList(), null,
                retryEnabled = true, retryInvalidReplies = true, retryRepeatedQuestions = true,
            ) {
                attempts++
                OpenAITextSupport.extractGeneratedText(gson, refused.toString(), "Bob")
            }
        }

        assertEquals(1, attempts)
        assertFalse(OpenAITextSupport.shouldEnterOfflineBackoff(failure))
        assertFalse(failure.message.orEmpty().contains("REFUSAL_PRIVATE_DETAIL"))
    }

    private fun assertRejected(response: JsonObject) {
        for (structured in listOf(false, true)) {
            val failure = assertThrows(IOException::class.java, {
                OpenAITextSupport.extractGeneratedText(gson, response.toString(), "Bob", structured)
            }, "structured=$structured response=$response")
            assertFalse(OpenAITextSupport.shouldEnterOfflineBackoff(failure))
            assertFalse(failure.message.orEmpty().contains("REFUSAL_PRIVATE_DETAIL"))
        }
    }

    private fun response(message: JsonObject, direct: Boolean = false): JsonObject = JsonObject().apply {
        addProperty("status", "completed")
        add("output", JsonArray().apply { add(message) })
        if (direct) addProperty("output_text", "{\"reply\":\"Replica aparent valida.\"}")
    }

    private fun message(vararg content: JsonElement): JsonObject = JsonObject().apply {
        addProperty("type", "message")
        addProperty("status", "completed")
        addProperty("role", "assistant")
        add("content", JsonArray().apply { content.forEach { add(it) } })
    }

    private fun text(value: String): JsonObject = JsonObject().apply {
        addProperty("type", "output_text")
        addProperty("text", value)
    }
}
