package ro.ainpc.ai

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.Strictness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class OpenAIResponseJsonValidationTest {
    private val gson = Gson()
    private val validBody = """{"status":"completed","output_text":"Salut!"}"""

    @Test
    fun `unquoted object names cannot produce application text`() {
        assertRejected("""{status:"completed",output_text:"Salut!"}""")
    }

    @Test
    fun `single quoted envelope strings cannot produce application text`() {
        assertRejected("""{'status':'completed','output_text':'Salut!'}""")
    }

    @Test
    fun `comments before within or after an envelope are rejected`() {
        for (body in listOf(
            "/* private metadata */$validBody",
            "// private metadata\n$validBody",
            "# private metadata\n$validBody",
            """{"status":"completed",/* private metadata */"output_text":"Salut!"}""",
            "$validBody/* private metadata */",
            "$validBody// private metadata",
        )) {
            assertRejected(body)
        }
    }

    @Test
    fun `non executable JSON prefix is not a Responses envelope`() {
        assertRejected(")]}'\n$validBody")
    }

    @Test
    fun `unescaped control characters in strings are rejected`() {
        for (control in listOf('\n', '\r', '\t', '\u0000')) {
            assertRejected("{\"status\":\"completed\",\"output_text\":\"Salut${control}lume!\"}")
        }
    }

    @Test
    fun `non JSON quote escape is rejected`() {
        assertRejected("""{"status":"completed","output_text":"Salut\' lume!"}""")
    }

    @Test
    fun `trailing documents and non JSON suffixes are rejected`() {
        for (suffix in listOf("{}", " null", " private_payload", "\n[]")) {
            assertRejected(validBody + suffix)
        }
    }

    @Test
    fun `non object roots never become application text`() {
        for (body in listOf("null", "[]", "[$validBody]", "true", "12", "\"Salut!\"")) {
            assertRejected(body)
        }
    }

    @Test
    fun `syntax errors retain neither raw payload paths nor nested Gson causes`() {
        val privateField = "PRIVATE_JSON_FIELD_synthetic-secret"
        for (key in listOf(null, privateField)) {
            val failure = assertThrows(IOException::class.java) {
                OpenAITextSupport.extractGeneratedText(gson, "{\"$privateField\":[}", null, false, key)
            }

            assertTrue(failure.message.orEmpty().contains("JSON invalid"))
            assertFalse(failure.toString().contains(privateField))
            assertNull(failure.cause)
            assertTrue(failure.suppressed.isEmpty())
        }
    }

    @Test
    fun `boundary enforces strictness without mutating the supplied Gson`() {
        val lenientGson = GsonBuilder().setStrictness(Strictness.LENIENT).create()
        val body = """{status:'completed',output_text:'Salut!'}"""

        assertRejected(body, lenientGson)

        assertEquals("Salut!", lenientGson.fromJson(body, JsonObject::class.java).get("output_text").asString)
        assertEquals("Salut!", OpenAITextSupport.extractGeneratedText(lenientGson, validBody, null))
    }

    @Test
    fun `valid whitespace Unicode and escaped controls remain accepted`() {
        val body = " \r\n\t{\"status\":\"completed\",\"output_text\":\"Bun\\u0103 ziua!\\nAici\\tacas\\u0103.\"}\t\r\n "

        assertEquals("Bună ziua!\nAici\tacasă.", OpenAITextSupport.extractGeneratedText(gson, body, null, true))
        assertEquals("Salut!", OpenAITextSupport.extractGeneratedText(gson, """{"output_text":"Salut!"}""", null))
    }

    @Test
    fun `strict envelope parsing does not parse or truncate generated structured text`() {
        val text = "{'nota':'// continut, nu comentariu JSON','text':'${"a".repeat(300)}'}"
        val body = gson.toJson(mapOf("status" to "completed", "output_text" to text))

        assertEquals(text, OpenAITextSupport.extractGeneratedText(gson, body, null, true))
    }

    private fun assertRejected(body: String, parser: Gson = gson) {
        for (structured in listOf(false, true)) {
            val failure = assertThrows(IOException::class.java) {
                OpenAITextSupport.extractGeneratedText(parser, body, null, structured)
            }

            assertNull(failure.cause)
            assertTrue(failure.suppressed.isEmpty())
        }
    }
}
