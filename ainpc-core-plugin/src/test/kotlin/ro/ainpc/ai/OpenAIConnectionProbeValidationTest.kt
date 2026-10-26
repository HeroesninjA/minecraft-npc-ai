package ro.ainpc.ai

import com.google.gson.Gson
import com.sun.net.httpserver.HttpServer
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import java.net.InetSocketAddress
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Logger

class OpenAIConnectionProbeValidationTest {
    private val previousProbeResult = OpenAIConnectionProbe.getLastProbeResult()
    private val httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()
    private val testKey = "synthetic-probe-validation-key"

    @AfterEach
    fun cleanup() {
        httpClient.shutdownNow()
        OpenAIConnectionProbe::class.java.getDeclaredField("lastProbeResult").apply {
            isAccessible = true
            set(null, previousProbeResult)
        }
    }

    @Test
    fun `empty and null success bodies cannot confirm the configured model`() {
        assertInvalidBodies("", "   ", "null", "{}")
    }

    @Test
    fun `non object success bodies become diagnostic results rather than parser exceptions`() {
        assertInvalidBodies("[]", "true", "123", "\"local-test-model\"")
    }

    @Test
    fun `truncated JSON and HTML success bodies do not escape the probe`() {
        assertInvalidBodies("{\"id\":", "<html>Proxy error</html>")
    }

    @Test
    fun `missing model identity does not fall back to the requested model`() {
        assertInvalidBodies("""{"object":"model"}""", """{"data":[]}""", """{"models":[]}""")
    }

    @Test
    fun `model identity must be a nonblank JSON string`() {
        assertInvalidBodies(
            """{"id":null}""", """{"id":123}""", """{"id":true}""",
            """{"id":{}}""", """{"id":["local-test-model"]}""", """{"id":""}""", """{"id":" \t\n"}""",
        )
    }

    @Test
    fun `an explicit object marker must identify a model`() {
        assertInvalidBodies(
            """{"id":"local-test-model","object":"list"}""",
            """{"id":"local-test-model","object":"response"}""",
            """{"id":"local-test-model","object":null}""",
            """{"id":"local-test-model","object":true}""",
            """{"id":"local-test-model","object":{}}""",
        )
    }

    @Test
    fun `error envelopes cannot be successful models even with a valid identity`() {
        assertInvalidBodies(
            """{"id":"local-test-model","error":{"message":"Credential $testKey"}}""",
            """{"id":"local-test-model","error":"failed"}""",
            """{"id":"local-test-model","error":false}""",
        )
    }

    @Test
    fun `nonstandard JSON syntax is rejected at the model probe boundary`() {
        assertInvalidBodies(
            "{'id':'local-test-model'}",
            "{id:\"local-test-model\"}",
            """{"id":"local-test-model"/* proxy comment */}""",
            """{"id":"local-test-model"} {"id":"other-model"}""",
        )
    }

    @Test
    fun `valid canonical model metadata and minimal compatible responses remain accepted`() {
        for (body in listOf(
            """{"id":"canonical-model","object":"model","created":1,"owned_by":"test","error":null,"future_field":{}}""",
            """{"id":"canonical-model"}""",
        )) {
            withServer(AtomicReference(body)) { baseUrl ->
                val status = probe(baseUrl)

                assertTrue(status.isReachable())
                assertTrue(status.isModelAvailable())
                assertEquals(listOf("canonical-model"), status.availableModels)
                assertTrue(status.errors.isEmpty())
                assertSame(status, OpenAIConnectionProbe.getLastProbeResult())
            }
        }
    }

    @Test
    fun `an invalid response replaces a previously successful cached probe`() {
        val body = AtomicReference("""{"id":"local-test-model"}""")
        withServer(body) { baseUrl ->
            assertTrue(probe(baseUrl).isModelAvailable())
            body.set("""{"id": """ + testKey)

            val status = probe(baseUrl)

            assertInvalidStatus(status, baseUrl)
            assertSame(status, OpenAIConnectionProbe.getLastProbeResult())
            assertFalse(status.summary().contains(testKey))
            assertFalse(status.summary().contains("JsonSyntaxException"))
        }
    }

    @Test
    fun `public asynchronous checks return a negative result without changing generation availability`() {
        withServer(AtomicReference("<html>Invalid model response</html>")) { baseUrl ->
            val service = service(baseUrl)

            val status = service.diagnoseConnection(false).get(4L, TimeUnit.SECONDS)

            assertInvalidStatus(status, baseUrl)
            assertFalse(service.checkConnection().get(4L, TimeUnit.SECONDS))
            assertTrue(service.isAvailable)
            assertFalse(service.captureDebugSnapshot().backoffActive)
        }
    }

    private fun assertInvalidBodies(vararg bodies: String) {
        for (body in bodies) {
            withServer(AtomicReference(body)) { baseUrl ->
                val status = probe(baseUrl)
                assertInvalidStatus(status, baseUrl)
                assertSame(status, OpenAIConnectionProbe.getLastProbeResult())
                assertFalse(status.summary().contains(testKey))
            }
        }
    }

    private fun assertInvalidStatus(status: ConnectionStatus, baseUrl: String) {
        assertTrue(status.isReachable())
        assertFalse(status.isModelAvailable())
        assertTrue(status.availableModels.isEmpty())
        assertEquals(baseUrl, status.respondingUrl)
        assertEquals(listOf(baseUrl), status.triedUrls)
        assertTrue(status.errors.single().contains("HTTP 200"))
    }

    private fun probe(baseUrl: String): ConnectionStatus =
        OpenAIConnectionProbe.probeConnection("local-test-model", baseUrl, testKey, httpClient, Gson(), 2)

    private fun withServer(body: AtomicReference<String>, action: (String) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            exchange.requestBody.use { it.readAllBytes() }
            val bytes = body.get().toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            action("http://127.0.0.1:${server.address.port}/v1")
        } finally {
            server.stop(0)
        }
    }

    private fun service(baseUrl: String): OpenAIService {
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
            set("openai.model", "local-test-model")
            set("openai.connect_timeout", 1)
            set("openai.read_timeout", 2)
            set("openai.diagnostics.enabled", false)
        }
        val logger = Logger.getAnonymousLogger().apply { useParentHandlers = false }
        JavaPlugin::class.java.getDeclaredField("newConfig").apply { isAccessible = true; set(plugin, config) }
        JavaPlugin::class.java.getDeclaredField("logger").apply { isAccessible = true; set(plugin, logger) }
        return OpenAIService(plugin)
    }
}
