package ro.ainpc.ai

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.Strictness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class OpenAIErrorEnvelopeValidationTest {
    private val gson = Gson()

    @Test
    fun `non string codes cannot become classification labels`() {
        for (value in listOf("123", "true", "{}", "[]", "[\"insufficient_quota\"]")) {
            assertEquals("", code("""{"error":{"code":$value}}"""), value)
        }
    }

    @Test
    fun `non string fallback types cannot become classification labels`() {
        for (value in listOf("123", "true", "{}", "[]", "[\"insufficient_quota\"]")) {
            assertEquals("", code("""{"error":{"type":$value}}"""), value)
        }
    }

    @Test
    fun `malformed primary codes do not promote a fallback quota type`() {
        for (value in listOf("123", "false", "{}", "[]", "[\"slow_down\"]")) {
            assertEquals("", code("""{"error":{"code":$value,"type":"insufficient_quota"}}"""), value)
        }
    }

    @Test
    fun `duplicate fields anywhere in an error envelope invalidate its classification`() {
        for (body in listOf(
            """{"error":{"code":"slow_down","code":"insufficient_quota"}}""",
            """{"error":{"type":null,"type":"insufficient_quota"}}""",
            """{"error":null,"error":{"code":"model_not_found"}}""",
            """{"error":{"code":"insufficient_quota"},"metadata":{"note":null,"note":true}}""",
            """{"error":{"code":"slow_down","co\u0064e":"insufficient_quota"}}""",
        )) {
            assertEquals("", code(body), body)
        }
    }

    @Test
    fun `nonstandard or incomplete error documents cannot supply classification codes`() {
        for (body in listOf(
            "{error:{code:'insufficient_quota'}}",
            """{"error":{/* private comment */"code":"insufficient_quota"}}""",
            """{"error":{"code":"insufficient_quota"}} {}""",
            """{"error":{"code":"insufficient_quota"},"metadata":""" + "\u0000" + "\"}",
            """{"error":{"code":"insufficient_quota"},"metadata":[}""",
        )) {
            assertEquals("", code(body))
        }
    }

    @Test
    fun `missing invalid or non object error roots have no classification`() {
        for (body in listOf("", " ", "null", "[]", "true", "{}", "{\"error\":null}", "{\"error\":[]}", "{\"error\":\"insufficient_quota\"}")) {
            assertEquals("", code(body), body)
        }
    }

    @Test
    fun `diagnostic fields are not coerced from containers numbers or booleans`() {
        assertEquals("Request failed.", message("""{"error":{"code":{},"type":["insufficient_quota"],"message":"Request failed."}}"""))
        assertEquals("slow_down", message("""{"error":{"code":"slow_down","message":{}}}"""))
        assertEquals("temporary_error", message("""{"error":{"type":"temporary_error","message":["Not a string field"]}}"""))
        assertEquals("Request failed.", message("""{"error":{"code":429,"type":false,"message":"Request failed."}}"""))
    }

    @Test
    fun `invalid envelope diagnostics use redacted complete body before making an excerpt`() {
        val credential = "synthetic.error.key-with-a-long-private-suffix"
        val body = """{"error":{"code":"slow_down","code":"insufficient_quota","message":""" +
            "x".repeat(145) + credential + "\"}}"

        val diagnostic = OpenAITextSupport.extractOpenAIErrorMessage(gson, body, credential)
        val expected = OpenAITextSupport.abbreviate(OpenAITextSupport.redactDiagnosticText(body, credential), 200)

        assertEquals(expected, diagnostic)
        assertEquals("", code(body))
        assertFalse(diagnostic.contains(credential.take(12)))
    }

    @Test
    fun `absent null and blank primary code preserve valid string type fallback`() {
        for (prefix in listOf("", "\"code\":null,", "\"code\":\"\",")) {
            val body = """{"error":{${prefix}"type":"insufficient_quota","message":"Credit unavailable."}}"""

            assertEquals("insufficient_quota", code(body))
            assertEquals("insufficient_quota: Credit unavailable.", message(body))
        }
    }

    @Test
    fun `valid code precedence and raw code spelling stay unchanged`() {
        val body = """{"error":{"code":"Slow_Down","type":"insufficient_quota","message":"Wait."}}"""

        assertEquals("Slow_Down", code(body))
        assertEquals("Slow_Down: Wait.", message(body))
    }

    @Test
    fun `strict error parsing does not mutate a supplied lenient Gson`() {
        val parser = GsonBuilder().setStrictness(Strictness.LENIENT).create()
        val body = "{error:{code:'insufficient_quota',message:'Wait.'}}"

        assertEquals("", OpenAITextSupport.extractOpenAIErrorCode(parser, body))
        assertEquals(OpenAITextSupport.abbreviate(body, 200), OpenAITextSupport.extractOpenAIErrorMessage(parser, body))
        assertEquals("insufficient_quota", parser.fromJson(body, JsonObject::class.java).getAsJsonObject("error").get("code").asString)
    }

    @Test
    fun `separate object names and duplicate looking quoted values do not reject valid errors`() {
        val body = """{"error":{"code":"slow_down","type":{},"message":"{\"code\":1,\"code\":2}"},"metadata":{"code":null,"CODE":1,"items":[{"code":2},{"code":3}]}}"""

        assertEquals("slow_down", code(body))
        assertEquals("slow_down: {\"code\":1,\"code\":2}", message(body))
    }

    private fun code(body: String): String = OpenAITextSupport.extractOpenAIErrorCode(gson, body)

    private fun message(body: String): String = OpenAITextSupport.extractOpenAIErrorMessage(gson, body)
}
