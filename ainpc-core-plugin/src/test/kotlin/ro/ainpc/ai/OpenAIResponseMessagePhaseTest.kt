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

class OpenAIResponseMessagePhaseTest {
    private val gson = Gson()

    @Test
    fun `dialogue returns final answer without commentary even when flat text contains the preamble`() {
        val commentary = "INTERMEDIATE_MARKER: " + "Verific informatiile. ".repeat(30)
        for (flat in listOf(false, true)) {
            val response = response(
                message(commentary, "commentary"), message("Bob: Piata este la nord.", "final_answer"),
            ).apply { if (flat) addProperty("output_text", "$commentary\nBob: Piata este la nord.") }

            assertEquals("Piata este la nord.", extract(response))
        }
    }

    @Test
    fun `structured final text is preserved without commentary or dialogue truncation`() {
        val json = "{\"reply\":\"${"detaliu ".repeat(60)}\"}"
        val response = response(message("Verific schema.", "commentary"), message(json, "final_answer")).apply {
            addProperty("output_text", "Verific schema.\n$json")
        }

        assertEquals(json, extract(response, structured = true))
    }

    @Test
    fun `commentary alone cannot become a final reply or trigger a formatting retry`() {
        for (flat in listOf(false, true)) {
            val response = response(message("Verific informatiile.", "commentary")).apply {
                if (flat) addProperty("output_text", "Verific informatiile.")
            }
            assertRejected(response)
        }
        var attempts = 0
        val response = response(message("Verific informatiile.", "commentary"))

        val failure = assertThrows(IOException::class.java) {
            DialogueResponseNovelty.generate(
                "Prompt\nBob: ", "Bob", "Salut!", emptyList(), null,
                retryEnabled = true, retryInvalidReplies = true, retryRepeatedQuestions = true,
            ) {
                attempts++
                extract(response)
            }
        }

        assertEquals(1, attempts)
        assertFalse(OpenAITextSupport.shouldEnterOfflineBackoff(failure))
    }

    @Test
    fun `explicit unknown or malformed message phase cannot be hidden by valid flat text`() {
        for (phase in listOf(
            gson.toJsonTree("UNKNOWN_PRIVATE_PHASE"), gson.toJsonTree(""), gson.toJsonTree("COMMENTARY"),
            gson.toJsonTree(true), gson.toJsonTree(123), JsonObject(), JsonArray(),
        )) {
            val message = message("Text neverificat.").apply { add("phase", phase) }
            val response = response(message).apply { addProperty("output_text", "Replica aparent valida.") }

            assertRejected(response)
        }
    }

    @Test
    fun `absent and null phases retain legacy nested and flat text behavior`() {
        for (explicitNull in listOf(false, true)) {
            val message = message("Bob: Raspuns imbricat.").apply {
                if (explicitNull) add("phase", JsonNull.INSTANCE)
            }
            val response = response(message)
            assertEquals("Raspuns imbricat.", extract(response))

            response.addProperty("output_text", "Bob: Raspuns direct.")
            assertEquals("Raspuns direct.", extract(response))
            assertEquals("Bob: Raspuns direct.", extract(response, structured = true))
        }
    }

    @Test
    fun `phase words inside generated JSON are data not message metadata`() {
        val json = """{"phase":"commentary","reply":"Textul literal final_answer ramane date."}"""

        assertEquals(json, extract(response(message(json)), structured = true))
        assertEquals(json, extract(response(message(json, "final_answer")), structured = true))
    }

    @Test
    fun `mixed legacy and final messages keep their order while later commentary stays excluded`() {
        val response = response(
            message("Preambul.", "commentary"),
            message("Prima parte.", "final_answer"),
            message("A doua parte.").apply { add("phase", JsonNull.INSTANCE) },
            message("Alte verificari.", "commentary"),
        ).apply { addProperty("output_text", "Preambul.\nPrima parte.\nA doua parte.\nAlte verificari.") }

        assertEquals("Prima parte.\nA doua parte.", extract(response, structured = true))
    }

    @Test
    fun `ignoring commentary does not bypass refusals status role or text type validation`() {
        val invalidMessages = listOf(
            message("Raspuns partial.", "commentary").apply { addProperty("status", "incomplete") },
            message("Rol gresit.", "commentary").apply { addProperty("role", "user") },
            message("", "commentary").apply {
                getAsJsonArray("content").add(JsonObject().apply {
                    addProperty("type", "refusal")
                    addProperty("refusal", "REFUSAL_PRIVATE_DETAIL")
                })
            },
            message("", "commentary").apply {
                getAsJsonArray("content")[0].asJsonObject.addProperty("text", 123)
            },
        )
        for (invalidMessage in invalidMessages) {
            val response = response(message("Replica aparent valida.", "final_answer"), invalidMessage)
                .apply { addProperty("output_text", "Replica aparent valida.") }

            assertRejected(response)
        }
    }

    @Test
    fun `phase on reasoning or tool items is not interpreted as assistant message phase`() {
        val response = response(message("Text imbricat legacy.")).apply {
            for (type in listOf("reasoning", "function_call")) {
                getAsJsonArray("output").add(JsonObject().apply {
                    addProperty("type", type)
                    addProperty("phase", "UNKNOWN_PRIVATE_PHASE")
                    add("content", JsonArray().apply { add(text("Nu este replica.")) })
                })
            }
            addProperty("output_text", "Bob: Raspuns direct legacy.")
        }

        assertEquals("Raspuns direct legacy.", extract(response))
    }

    private fun assertRejected(response: JsonObject) {
        for (structured in listOf(false, true)) {
            val failure = assertThrows(IOException::class.java) { extract(response, structured) }
            assertFalse(OpenAITextSupport.shouldEnterOfflineBackoff(failure))
            assertFalse(failure.message.orEmpty().contains("REFUSAL_PRIVATE_DETAIL"))
            assertFalse(failure.message.orEmpty().contains("UNKNOWN_PRIVATE_PHASE"))
        }
    }

    private fun extract(response: JsonObject, structured: Boolean = false): String =
        OpenAITextSupport.extractGeneratedText(gson, response.toString(), "Bob", structured)

    private fun response(vararg messages: JsonElement): JsonObject = JsonObject().apply {
        addProperty("status", "completed")
        add("output", JsonArray().apply { messages.forEach { add(it) } })
    }

    private fun message(value: String, phase: String? = null): JsonObject = JsonObject().apply {
        addProperty("type", "message")
        addProperty("status", "completed")
        addProperty("role", "assistant")
        if (phase != null) addProperty("phase", phase)
        add("content", JsonArray().apply { add(text(value)) })
    }

    private fun text(value: String): JsonObject = JsonObject().apply {
        addProperty("type", "output_text")
        addProperty("text", value)
    }
}
