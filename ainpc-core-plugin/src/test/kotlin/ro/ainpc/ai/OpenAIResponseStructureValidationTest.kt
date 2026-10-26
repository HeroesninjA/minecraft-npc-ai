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

class OpenAIResponseStructureValidationTest {
    private val gson = Gson()

    @Test
    fun `explicit output must be an array even when aggregated text looks valid`() {
        for (output in listOf(
            JsonNull.INSTANCE, message(refusal()), gson.toJsonTree("PRIVATE_CONTAINER_DETAIL"),
            gson.toJsonTree(123), gson.toJsonTree(true),
        )) {
            assertRejected(response().apply { add("output", output) })
        }
    }

    @Test
    fun `non object output entries cannot be hidden after a useful message`() {
        for (output in invalidContainers()) {
            assertRejected(response(message(text("Salut!")), output))
        }
    }

    @Test
    fun `message content must be a present array rather than a skipped container`() {
        for (content in listOf<JsonElement?>(null, JsonNull.INSTANCE, refusal(), gson.toJsonTree("PRIVATE_CONTAINER_DETAIL"),
            gson.toJsonTree(123), gson.toJsonTree(true))) {
            val malformed = message().apply {
                if (content == null) remove("content") else add("content", content)
            }
            assertRejected(response(message(text("Salut!")), malformed))
        }
    }

    @Test
    fun `non object content entries invalidate the response even after valid text`() {
        for (content in invalidContainers()) {
            assertRejected(response(message(text("Salut!"), content)))
        }
    }

    @Test
    fun `output items require nonblank string type discriminators`() {
        for (type in invalidTypes()) {
            val malformed = message(refusal()).apply {
                if (type == null) remove("type") else add("type", type)
            }
            assertRejected(response(message(text("Salut!")), malformed))
        }
    }

    @Test
    fun `message content items require nonblank string type discriminators`() {
        for (type in invalidTypes()) {
            val malformed = refusal().apply {
                if (type == null) remove("type") else add("type", type)
            }
            assertRejected(response(message(text("Salut!"), malformed)))
        }
    }

    @Test
    fun `unknown well typed items stay separate without applying message rules to their payloads`() {
        val futureItem = JsonObject().apply {
            addProperty("type", "future_output_item")
            addProperty("role", "user")
            addProperty("status", "in_progress")
            add("content", refusal())
        }
        val futureContent = JsonObject().apply {
            addProperty("type", "future_content_item")
            add("text", refusal())
        }
        val response = response(futureItem, message(futureContent, text("Bob: Salut!")))

        assertEquals("Salut!", extract(response))
        assertEquals("Bob: Salut!", extract(response, structured = true))
    }

    @Test
    fun `legacy flat responses empty arrays and legacy text messages retain their behavior`() {
        val flat = JsonObject().apply {
            addProperty("status", "completed")
            addProperty("output_text", "Bob: Salut!")
        }
        for (response in listOf(
            flat,
            flat.deepCopy().apply { add("output", JsonArray()) },
            flat.deepCopy().apply { add("output", JsonArray().apply { add(message()) }) },
        )) {
            assertEquals("Salut!", extract(response))
            assertEquals("Bob: Salut!", extract(response, structured = true))
        }
        val legacyMessage = message(text("Bob: Salut!").apply { addProperty("type", "text") }).apply {
            remove("status")
            remove("role")
        }
        assertEquals("Salut!", extract(response(legacyMessage)))
    }

    @Test
    fun `malformed structures are parser failures not formatting retries or network outages`() {
        val malformed = response().apply {
            add("output", message(refusal()))
            addProperty("output_text", "Salut!")
        }
        var attempts = 0

        val failure = assertThrows(IOException::class.java) {
            DialogueResponseNovelty.generate(
                "Prompt\nBob: ", "Bob", "Salut!", emptyList(), null,
                retryEnabled = true, retryInvalidReplies = true, retryRepeatedQuestions = true,
            ) {
                attempts++
                extract(malformed)
            }
        }

        assertEquals(1, attempts)
        assertFalse(OpenAITextSupport.shouldEnterOfflineBackoff(failure))
        assertFalse(failure.message.orEmpty().contains("PRIVATE_CONTAINER_DETAIL"))
    }

    private fun assertRejected(response: JsonObject) {
        for (flat in listOf(false, true)) {
            val candidate = response.deepCopy().apply {
                if (flat) addProperty("output_text", "Replica aparent valida.")
            }
            for (structured in listOf(false, true)) {
                val failure = assertThrows(IOException::class.java) { extract(candidate, structured) }
                assertFalse(OpenAITextSupport.shouldEnterOfflineBackoff(failure))
                assertFalse(failure.message.orEmpty().contains("PRIVATE_CONTAINER_DETAIL"))
            }
        }
    }

    private fun invalidContainers(): List<JsonElement> = listOf(
        JsonNull.INSTANCE, gson.toJsonTree("PRIVATE_CONTAINER_DETAIL"), gson.toJsonTree(123),
        gson.toJsonTree(true), JsonArray().apply { add(refusal()) },
    )

    private fun invalidTypes(): List<JsonElement?> = listOf(
        null, JsonNull.INSTANCE, gson.toJsonTree(""), gson.toJsonTree("   "), gson.toJsonTree(123),
        gson.toJsonTree(true), JsonObject(), JsonArray().apply { add("message") },
    )

    private fun extract(response: JsonObject, structured: Boolean = false): String =
        OpenAITextSupport.extractGeneratedText(gson, response.toString(), "Bob", structured)

    private fun response(vararg items: JsonElement): JsonObject = JsonObject().apply {
        addProperty("status", "completed")
        add("output", JsonArray().apply { items.forEach { add(it) } })
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

    private fun refusal(): JsonObject = JsonObject().apply {
        addProperty("type", "refusal")
        addProperty("refusal", "PRIVATE_CONTAINER_DETAIL")
    }
}
