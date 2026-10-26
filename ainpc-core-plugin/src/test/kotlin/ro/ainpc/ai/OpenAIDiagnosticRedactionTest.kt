package ro.ainpc.ai

import com.google.gson.Gson
import com.sun.net.httpserver.HttpServer
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import java.io.IOException
import java.net.InetSocketAddress
import java.net.http.HttpClient
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

class OpenAIDiagnosticRedactionTest {
    private val testKey = "gateway.TEST+key.with\$symbols-and-a-long-synthetic-value-for-tests"
    private val previousProbeResult = OpenAIConnectionProbe.getLastProbeResult()

    @AfterEach
    fun restoreProbeCache() {
        OpenAIConnectionProbe::class.java.getDeclaredField("lastProbeResult").apply {
            isAccessible = true
            set(null, previousProbeResult)
        }
    }

    @Test
    fun `blank known credentials do not corrupt ordinary diagnostics`() {
        val text = "HTTP 429 at 127.0.0.1; request=req_test"
        for (key in listOf(null, "", "   ")) {
            assertEquals(text, OpenAITextSupport.redactDiagnosticText(text, key))
        }
        assertEquals("", OpenAITextSupport.redactDiagnosticText(null, testKey))
    }

    @Test
    fun `failure cause chains are redacted before storage and logging without causing network backoff`() {
        val fixture = fixture()
        val failure = IOException("Credential $testKey", IOException("Bearer $testKey; sk-${"z".repeat(24)}"))

        invoke(fixture.service, "handleGenerationFailure", failure)

        assertSafe(fixture)
        assertTrue(fixture.service.captureDebugSnapshot().lastFailureMessage.contains("<redacted>"))
        assertTrue(fixture.service.isAvailable)
        assertTrue(fixture.messages.any { it.contains("IOException") })
    }

    @Test
    fun `HTTP authentication failures redact echoed key without changing the authorization header`() {
        withServer(401, errorBody("Credential $testKey", "invalid_api_key")) { baseUrl, authorizations ->
            val fixture = fixture(baseUrl)

            assertNull(fixture.service.generateAsync("Salut!").get(10L, TimeUnit.SECONDS))

            assertEquals(listOf("Bearer $testKey"), authorizations.toList())
            assertEquals("HTTP 401 (invalid_api_key)", fixture.service.captureDebugSnapshot().apiSuspendedReason)
            assertSafe(fixture)
        }
    }

    @Test
    fun `server error codes cannot leak a case folded credential through suspension diagnostics`() {
        val fixture = fixture()

        invoke(fixture.service, "handleGenerationFailure", OpenAIHttpException(401, testKey, null, "Credential $testKey"))

        assertSafe(fixture)
        assertFalse(fixture.service.captureDebugSnapshot().apiSuspendedReason.orEmpty().contains(testKey.lowercase()))
        assertFalse(fixture.service.isAvailable)
    }

    @Test
    fun `prompt and response previews mask full secrets before shortening and preserve original lengths`() {
        val fixture = fixture()
        val text = "x".repeat(160) + testKey + " tail"
        invoke(fixture.service, "recordPrompt", text)
        invoke(fixture.service, "recordResponse", text)
        invoke(fixture.service, "recordInteractionSuccess", "Ion", "Steve", text, text)
        val snapshot = fixture.service.captureDebugSnapshot()

        assertSafe(fixture)
        assertFalse(snapshot.toString().contains(testKey.take(12)))
        assertTrue(snapshot.lastPromptPreview.contains("<redacted>"))
        assertTrue(snapshot.lastResponsePreview.contains("<redacted>"))
        assertEquals(text.length, snapshot.lastPromptChars)
        assertEquals(text.length, snapshot.lastResponseChars)
        assertEquals(text.length, snapshot.recentInteractions.single().promptChars)
    }

    @Test
    fun `disabled preview flags also apply to success fallback and error interaction history`() {
        val fixture = fixture()
        fixture.config.set("openai.diagnostics.log_prompt_summary", false)
        fixture.config.set("openai.diagnostics.log_response_preview", false)
        val prompt = "PROMPT_PREVIEW_MARKER"
        val reply = "REPLY_PREVIEW_MARKER"
        invoke(fixture.service, "recordPrompt", prompt)
        invoke(fixture.service, "recordResponse", reply)
        invoke(fixture.service, "recordInteractionSuccess", "Ion", "Steve", prompt, reply)
        invoke(fixture.service, "recordInteractionFallback", "Ion", "Steve", reply, "unavailable")
        invoke(fixture.service, "recordInteractionError", "Ion", "Steve", prompt, reply, "network failure")
        val snapshot = fixture.service.captureDebugSnapshot()

        assertEquals("<disabled>", snapshot.lastPromptPreview)
        assertEquals("<disabled>", snapshot.lastResponsePreview)
        assertEquals(3, snapshot.recentInteractions.size)
        for (interaction in snapshot.recentInteractions) {
            assertEquals("<disabled>", interaction.responsePreview)
            assertTrue(interaction.promptPreview.isEmpty() || interaction.promptPreview == "<disabled>")
        }
        assertFalse(snapshot.toString().contains(prompt))
        assertFalse(snapshot.toString().contains(reply))
        assertEquals(prompt.length, snapshot.lastPromptChars)
        assertEquals(reply.length, snapshot.lastResponseChars)
    }

    @Test
    fun `fallback and interaction metadata are redacted independently of exception formatting`() {
        val fixture = fixture()
        invoke(fixture.service, "recordFallback", "Credential $testKey")
        invoke(fixture.service, "recordInteractionSuccess", testKey, testKey, testKey, testKey)
        invoke(fixture.service, "recordInteractionFallback", testKey, testKey, testKey, "Credential $testKey")
        invoke(fixture.service, "recordInteractionError", testKey, testKey, testKey, testKey, "Credential $testKey")

        assertSafe(fixture)
        assertEquals(3, fixture.service.captureDebugSnapshot().recentInteractions.size)
    }

    @Test
    fun `diagnostic model and endpoint mask credentials without changing runtime settings`() {
        val url = "http://user:password@127.0.0.1:1/v1/$testKey"
        val fixture = fixture(url, "model-$testKey")
        val snapshot = fixture.service.captureDebugSnapshot()

        assertSafe(fixture)
        assertFalse(snapshot.baseUrl.contains("user:password"))
        assertTrue(snapshot.apiKeyPresent)
        assertTrue(snapshot.baseUrl.contains("127.0.0.1"))
        val runtimeUrl = OpenAIService::class.java.getDeclaredField("baseUrl").apply { isAccessible = true }.get(fixture.service)
        assertEquals(url, runtimeUrl)
    }

    @Test
    fun `HTTP probe errors are redacted before returning or caching connection status`() {
        for (statusCode in listOf(401, 404)) {
            withServer(statusCode, errorBody("Credential $testKey", "invalid_api_key")) { baseUrl, authorizations ->
                val status = OpenAIConnectionProbe.probeConnection("local-test-model", baseUrl, testKey, HttpClient.newHttpClient(), Gson())

                assertEquals(listOf("Bearer $testKey"), authorizations.toList())
                assertFalse(status.errors.joinToString().contains(testKey))
                assertFalse(status.summary().contains(testKey))
                assertFalse(OpenAIConnectionProbe.getLastProbeResult()!!.summary().contains(testKey))
                assertEquals(statusCode == 404, status.isReachable())
                assertFalse(status.isModelAvailable())
            }
        }
    }

    @Test
    fun `successful probe model metadata is sanitized before caching`() {
        val body = Gson().toJson(mapOf("id" to "echo-$testKey"))
        withServer(200, body) { baseUrl, _ ->
            val status = OpenAIConnectionProbe.probeConnection("local-test-model", baseUrl, testKey, HttpClient.newHttpClient(), Gson())

            assertTrue(status.isReachable())
            assertTrue(status.isModelAvailable())
            assertFalse(status.availableModels.joinToString().contains(testKey))
            assertTrue(status.availableModels.single().contains("<redacted>"))
        }
    }

    @Test
    fun `unstructured authentication errors mask credentials before clipping the fallback excerpt`() {
        for (echoedKey in listOf(testKey, testKey.uppercase())) {
            val body = "Gateway error: ".padEnd(180, 'x') + echoedKey + " request rejected"
            withServer(401, body) { baseUrl, authorizations ->
                val fixture = fixture(baseUrl)

                assertNull(fixture.service.generateAsync("Salut!").get(10L, TimeUnit.SECONDS))

                assertNoCredentialFragments(fixture)
                assertEquals(listOf("Bearer $testKey"), authorizations.toList())
                assertFalse(fixture.service.isAvailable)
                assertTrue(fixture.service.captureDebugSnapshot().lastFailureMessage.contains("HTTP 401"))
            }
        }
    }

    @Test
    fun `JSON without an error object also masks full credentials before clipping`() {
        val body = "{\"detail\":\"".padEnd(180, 'x') + testKey + "\"}"
        withServer(500, body) { baseUrl, _ ->
            val fixture = fixture(baseUrl)

            assertNull(fixture.service.generateAsync("Salut!").get(10L, TimeUnit.SECONDS))

            assertNoCredentialFragments(fixture)
            assertTrue(fixture.service.captureDebugSnapshot().backoffActive)
            assertTrue(fixture.service.captureDebugSnapshot().lastFailureMessage.contains("HTTP 500"))
        }
    }

    @Test
    fun `error envelopes in HTTP success responses cannot leak clipped credential prefixes`() {
        val body = "{\"error\":{},\"detail\":\"".padEnd(180, 'x') + testKey + "\"}"
        withServer(200, body) { baseUrl, _ ->
            val fixture = fixture(baseUrl)

            assertNull(fixture.service.generateAsync("Salut!").get(10L, TimeUnit.SECONDS))

            assertNoCredentialFragments(fixture)
            assertTrue(fixture.service.isAvailable)
            assertFalse(fixture.service.captureDebugSnapshot().backoffActive)
        }
    }

    @Test
    fun `probe error excerpts and their cached status do not retain clipped credential prefixes`() {
        val body = "Gateway error: ".padEnd(180, 'x') + testKey + " request rejected"
        for (statusCode in listOf(401, 404)) {
            withServer(statusCode, body) { baseUrl, authorizations ->
                HttpClient.newHttpClient().use { client ->
                    val status = OpenAIConnectionProbe.probeConnection("local-test-model", baseUrl, testKey, client, Gson())

                    assertFalse(status.summary().contains(testKey.take(12), ignoreCase = true))
                    assertFalse(OpenAIConnectionProbe.getLastProbeResult()!!.summary().contains(testKey.take(12), ignoreCase = true))
                    assertEquals(statusCode == 404, status.isReachable())
                    assertFalse(status.isModelAvailable())
                    assertEquals(listOf("Bearer $testKey"), authorizations.toList())
                }
            }
        }
    }

    @Test
    fun `legacy error extraction masks complete private key blocks before making an excerpt`() {
        val body = "Proxy error: -----BEGIN PRIVATE KEY-----\n" + "PRIVATE_KEY_MATERIAL".repeat(30) +
            "\n-----END PRIVATE KEY----- request rejected"

        val diagnostic = OpenAITextSupport.extractOpenAIErrorMessage(Gson(), body)

        assertTrue(diagnostic.contains("<redacted>"))
        assertFalse(diagnostic.contains("PRIVATE_KEY_MATERIAL"))
        assertFalse(diagnostic.contains("BEGIN PRIVATE KEY"))
        assertTrue(diagnostic.length <= 200)
    }

    @Test
    fun `legacy error extraction masks URI credentials even when the host would have been clipped`() {
        val password = "URI_PASSWORD_MATERIAL".repeat(15)
        val body = "Proxy error: ".padEnd(150, 'x') + " http://private-user:$password@127.0.0.1/v1"

        val diagnostic = OpenAITextSupport.extractOpenAIErrorMessage(Gson(), body)

        assertTrue(diagnostic.contains("<redacted>"))
        assertFalse(diagnostic.contains("private-user"))
        assertFalse(diagnostic.contains("URI_PASSWORD_MATERIAL"))
        assertTrue(diagnostic.length <= 200)
    }

    @Test
    fun `ordinary fallback errors keep their existing excerpt length and normalization`() {
        val body = "Proxy error\r\n" + "x".repeat(250)

        assertEquals(OpenAITextSupport.abbreviate(body, 200), OpenAITextSupport.extractOpenAIErrorMessage(Gson(), body))
        assertEquals("", OpenAITextSupport.extractOpenAIErrorMessage(Gson(), " "))
    }

    @Test
    fun `ordinary structured error formatting and raw classification remain unchanged`() {
        val body = errorBody("Wait before retrying.", "slow_down")

        assertEquals("slow_down: Wait before retrying.", OpenAITextSupport.extractOpenAIErrorMessage(Gson(), body))
        assertEquals("slow_down", OpenAITextSupport.extractOpenAIErrorCode(Gson(), body))
    }

    @Test
    fun `credential aware response parsing does not rewrite successful application text`() {
        val text = "Valoare $testKey"
        val body = Gson().toJson(mapOf("status" to "completed", "output_text" to text))

        assertEquals(text, OpenAITextSupport.extractGeneratedText(Gson(), body, null, false, testKey))
        assertEquals("invalid_api_key: Credential <redacted>", OpenAITextSupport.extractOpenAIErrorMessage(
            Gson(), errorBody("Credential $testKey", "invalid_api_key"), testKey,
        ))
    }

    @Test
    fun `legacy JVM extraction entry points remain callable`() {
        val errorExtractor = OpenAITextSupport::class.java.getMethod(
            "extractOpenAIErrorMessage", Gson::class.java, String::class.java,
        )
        val responseExtractor = OpenAITextSupport::class.java.getMethod(
            "extractGeneratedText", Gson::class.java, String::class.java, String::class.java, java.lang.Boolean.TYPE,
        )

        assertEquals("Proxy error", errorExtractor.invoke(null, Gson(), "Proxy error"))
        assertEquals("Salut!", responseExtractor.invoke(null, Gson(), """{"output_text":"Salut!"}""", null, false))
    }

    @Test
    fun `malformed response containers reach generic fallback without exposing payloads or entering backoff`() {
        val body = """{"status":"completed","output_text":"Salut!","output":{"type":"message","content":{"type":"refusal","refusal":"PRIVATE_CONTAINER_DETAIL"}}}"""
        withServer(200, body) { baseUrl, authorizations ->
            val fixture = fixture(baseUrl)

            assertNull(fixture.service.generateAsync("Salut!").get(10L, TimeUnit.SECONDS))

            assertTrue(fixture.service.isAvailable)
            assertFalse(fixture.service.captureDebugSnapshot().backoffActive)
            assertFalse((fixture.service.captureDebugSnapshot().toString() + fixture.messages.joinToString()).contains("PRIVATE_CONTAINER_DETAIL"))
            assertEquals(1, authorizations.size)
        }
    }

    @Test
    fun `nonstandard JSON envelopes fall back without exposing metadata or entering backoff`() {
        val body = """{status:"completed",output_text:"Salut!",metadata:"PRIVATE_JSON_DETAIL $testKey"}"""
        withServer(200, body) { baseUrl, authorizations ->
            val fixture = fixture(baseUrl)

            assertNull(fixture.service.generateAsync("Salut!").get(10L, TimeUnit.SECONDS))

            assertTrue(fixture.service.isAvailable)
            assertFalse(fixture.service.captureDebugSnapshot().backoffActive)
            assertFalse((fixture.service.captureDebugSnapshot().toString() + fixture.messages.joinToString()).contains("PRIVATE_JSON_DETAIL"))
            assertEquals(listOf("Bearer $testKey"), authorizations.toList())
            assertSafe(fixture)
        }
    }

    @Test
    fun `syntax failure diagnostics omit raw parser paths without changing availability`() {
        val body = """{"PRIVATE_JSON_DETAIL":[}"""
        withServer(200, body) { baseUrl, authorizations ->
            val fixture = fixture(baseUrl)

            assertNull(fixture.service.generateAsync("Salut!").get(10L, TimeUnit.SECONDS))

            assertTrue(fixture.service.isAvailable)
            val snapshot = fixture.service.captureDebugSnapshot()
            assertFalse(snapshot.backoffActive)
            assertTrue(snapshot.lastFailureMessage.contains("JSON invalid"))
            assertFalse((snapshot.toString() + fixture.messages.joinToString()).contains("PRIVATE_JSON_DETAIL"))
            assertEquals(1, authorizations.size)
            assertSafe(fixture)
        }
    }

    private fun assertNoCredentialFragments(fixture: Fixture) {
        assertSafe(fixture)
        val diagnostic = fixture.service.captureDebugSnapshot().toString() + fixture.messages.joinToString()
        assertFalse(diagnostic.contains(testKey.take(12), ignoreCase = true), "Diagnostic contains a clipped credential prefix")
    }

    private fun assertSafe(fixture: Fixture) {
        val snapshot = fixture.service.captureDebugSnapshot()
        val diagnostic = snapshot.toString() + snapshot.connectionSummary + fixture.messages.joinToString()
        assertFalse(diagnostic.contains(testKey), "Diagnostic contains the known credential")
        assertFalse(diagnostic.contains(testKey.lowercase()), "Diagnostic contains the case-folded credential")
        assertFalse(diagnostic.contains("sk-${"z".repeat(24)}"), "Diagnostic contains another recognizable key")
    }

    private fun invoke(service: OpenAIService, name: String, vararg arguments: Any) {
        OpenAIService::class.java.declaredMethods.single { it.name == name && it.parameterCount == arguments.size }
            .apply { isAccessible = true }.invoke(service, *arguments)
    }

    private fun errorBody(message: String, code: String): String = Gson().toJson(mapOf("error" to mapOf("message" to message, "code" to code)))

    private fun <Result> withServer(statusCode: Int, body: String, action: (String, ConcurrentLinkedQueue<String>) -> Result): Result {
        val authorizations = ConcurrentLinkedQueue<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            authorizations.add(exchange.requestHeaders.getFirst("Authorization").orEmpty())
            exchange.requestBody.use { it.readAllBytes() }
            val bytes = body.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            return action("http://127.0.0.1:${server.address.port}/v1", authorizations)
        } finally {
            server.stop(0)
        }
    }

    private fun fixture(baseUrl: String = "http://127.0.0.1:1/v1", model: String = "local-test-model"): Fixture {
        val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
            field.isAccessible = true
            field.get(null) as sun.misc.Unsafe
        }
        val plugin = unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
        val config = YamlConfiguration().apply {
            set("features.ai", true)
            set("ai_provider", "openai")
            set("openai.api_key", testKey)
            set("openai.base_url", baseUrl)
            set("openai.model", model)
            set("openai.connect_timeout", 2)
            set("openai.read_timeout", 2)
            set("openai.diagnostics.enabled", true)
        }
        val messages = ConcurrentLinkedQueue<String>()
        val logger = Logger.getAnonymousLogger().apply {
            level = Level.ALL
            useParentHandlers = false
            addHandler(object : Handler() {
                override fun publish(record: LogRecord) { messages.add(record.message) }
                override fun flush() = Unit
                override fun close() = Unit
            })
        }
        JavaPlugin::class.java.getDeclaredField("newConfig").apply { isAccessible = true; set(plugin, config) }
        JavaPlugin::class.java.getDeclaredField("logger").apply { isAccessible = true; set(plugin, logger) }
        return Fixture(OpenAIService(plugin), config, messages)
    }

    private data class Fixture(val service: OpenAIService, val config: YamlConfiguration, val messages: ConcurrentLinkedQueue<String>)
}
