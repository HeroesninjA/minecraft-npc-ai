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

class OpenAIResponseDuplicateFieldsTest {
    private val gson = Gson()
    private val content = """[{"type":"output_text","text":"Salut!"}]"""

    @Test
    fun `duplicate response status cannot replace an incomplete state with completed`() {
        for (statuses in listOf("\"incomplete\",\"status\":\"completed\"", "\"completed\",\"status\":\"incomplete\"")) {
            assertRejected("""{"status":$statuses,"output_text":"Salut!"}""")
        }
    }

    @Test
    fun `duplicate error members cannot erase an error or exploit an earlier null`() {
        for (errors in listOf("""{"message":"PRIVATE_ERROR"},"error":null""", """null,"error":null""")) {
            assertRejected("""{"status":"completed","error":$errors,"output_text":"Salut!"}""")
        }
    }

    @Test
    fun `duplicate direct and nested text members are rejected even for identical values`() {
        for (first in listOf("null", "123", "\"Salut!\"", "\"Alt text\"")) {
            assertRejected("""{"status":"completed","output_text":$first,"output_text":"Salut!"}""")
            assertRejected("""{"status":"completed","output":[{"type":"message","content":[{"type":"output_text","text":$first,"text":"Salut!"}]}]}""")
        }
    }

    @Test
    fun `duplicate output or content containers cannot hide a refusal`() {
        val refusal = """[{"type":"refusal","refusal":"PRIVATE_REFUSAL"}]"""
        assertRejected("""{"status":"completed","output":[{"type":"message","content":$refusal}],"output":[],"output_text":"Salut!"}""")
        assertRejected("""{"status":"completed","output":[{"type":"message","content":$refusal,"content":$content}]}""")
    }

    @Test
    fun `duplicate message role status and phase cannot bypass message gates`() {
        for (fields in listOf(
            """"role":"user","role":"assistant"""",
            """"status":"incomplete","status":"completed"""",
            """"phase":"commentary","phase":"final_answer"""",
        )) {
            assertRejected("""{"status":"completed","output":[{"type":"message",$fields,"content":$content}]}""")
        }
    }

    @Test
    fun `duplicate item discriminators cannot turn refusals or invalid items into generated text`() {
        assertRejected("""{"status":"completed","output":[{"type":null,"type":"message","content":$content}]}""")
        assertRejected("""{"status":"completed","output":[{"type":"message","content":[{"type":"refusal","type":"output_text","text":"Salut!"}]}]}""")
    }

    @Test
    fun `duplicate unused metadata is rejected throughout nested arrays and objects`() {
        for (metadata in listOf(
            """{"note":null,"note":true}""",
            """[{"inner":[{"note":"first","note":"second"}]}]""",
            """{"values":[],"values":{}}""",
        )) {
            assertRejected("""{"status":"completed","output_text":"Salut!","metadata":$metadata}""")
        }
    }

    @Test
    fun `escaped JSON names are compared after decoding`() {
        assertRejected("""{"status":"incomplete","st\u0061tus":"completed","output_text":"Salut!"}""")
        assertRejected("""{"status":"completed","output_text":"Salut!","metadata":{"\u006eote":null,"note":1}}""")
    }

    @Test
    fun `duplicate diagnostics retain no private member name value path or cause`() {
        val privateName = "PRIVATE_DUPLICATE_FIELD_synthetic-secret\nfield"
        val encodedName = gson.toJson(privateName)
        val body = """{"status":"completed","output_text":"Salut!","metadata":{$encodedName:"PRIVATE_VALUE",$encodedName:null}}"""

        for (key in listOf(null, privateName)) {
            val failure = assertThrows(IOException::class.java) {
                OpenAITextSupport.extractGeneratedText(gson, body, null, false, key)
            }

            assertEquals("OpenAI a returnat JSON invalid.", failure.message)
            assertFalse(failure.toString().contains("PRIVATE_DUPLICATE_FIELD"))
            assertFalse(failure.toString().contains("PRIVATE_VALUE"))
            assertNull(failure.cause)
            assertTrue(failure.suppressed.isEmpty())
        }
    }

    @Test
    fun `the same names in separate sibling and ancestor objects remain valid`() {
        val body = """{"status":"completed","output":[{"type":"message","content":[{"type":"output_text","text":"Prima."},{"type":"output_text","text":"A doua."}]}],"metadata":{"status":null,"note":{"note":1},"list":[{"note":2},{"note":3}]}}"""

        assertEquals("Prima.\nA doua.", OpenAITextSupport.extractGeneratedText(gson, body, null, true))
    }

    @Test
    fun `member names remain case sensitive without Unicode normalization`() {
        val body = """{"status":"completed","STATUS":"incomplete","output_text":"Salut!","metadata":{"note":null,"NOTE":1,"\u00e9":2,"e\u0301":3}}"""

        assertEquals("Salut!", OpenAITextSupport.extractGeneratedText(gson, body, null, true))
    }

    @Test
    fun `duplicate looking JSON inside generated text is not parsed or truncated`() {
        val text = """{"note":"${"a".repeat(300)}","note":null}"""
        val body = gson.toJson(mapOf("status" to "completed", "output_text" to text))

        assertEquals(text, OpenAITextSupport.extractGeneratedText(gson, body, null, true))
    }

    @Test
    fun `duplicate checks do not mutate the supplied Gson configuration`() {
        val parser = GsonBuilder().setStrictness(Strictness.LENIENT).create()
        val body = """{"status":"incomplete","status":"completed","output_text":"Salut!"}"""

        assertRejected(body, parser)

        assertEquals("completed", parser.fromJson(body, JsonObject::class.java).get("status").asString)
        assertEquals("value", parser.fromJson("{note:'value'}", JsonObject::class.java).get("note").asString)
    }

    @Test
    fun `deep unknown containers do not require recursive member validation`() {
        val metadata = "[".repeat(1500) + """{"note":null}""" + "]".repeat(1500)
        val body = """{"status":"completed","output_text":"Salut!","metadata":$metadata}"""

        assertEquals("Salut!", OpenAITextSupport.extractGeneratedText(gson, body, null, true))
    }

    private fun assertRejected(body: String, parser: Gson = gson) {
        for (structured in listOf(false, true)) {
            val failure = assertThrows(IOException::class.java) {
                OpenAITextSupport.extractGeneratedText(parser, body, null, structured)
            }

            assertEquals("OpenAI a returnat JSON invalid.", failure.message)
            assertNull(failure.cause)
            assertTrue(failure.suppressed.isEmpty())
        }
    }
}
