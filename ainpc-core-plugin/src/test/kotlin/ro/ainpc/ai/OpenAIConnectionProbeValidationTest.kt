package ro.ainpc.ai

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.Strictness
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

    @Test
    fun `duplicate model identities cannot confirm the last value even when values agree`() {
        assertInvalidBodies(
            """{"id":null,"id":"local-test-model"}""",
            """{"id":123,"id":"local-test-model"}""",
            """{"id":"other-model","id":"local-test-model"}""",
            """{"id":"local-test-model","id":"local-test-model"}""",
        )
    }

    @Test
    fun `duplicate object markers cannot replace invalid types with model`() {
        assertInvalidBodies(
            """{"id":"local-test-model","object":"list","object":"model"}""",
            """{"id":"local-test-model","object":null,"object":"model"}""",
            """{"id":"local-test-model","object":"model","object":"model"}""",
        )
    }

    @Test
    fun `duplicate error fields cannot clear an error or exploit an earlier null`() {
        assertInvalidBodies(
            """{"id":"local-test-model","error":{"message":"PRIVATE_ERROR $testKey"},"error":null}""",
            """{"id":"local-test-model","error":false,"error":null}""",
            """{"id":"local-test-model","error":null,"error":null}""",
        )
    }

    @Test
    fun `escaped member names cannot bypass model duplicate detection`() {
        assertInvalidBodies(
            """{"id":null,"\u0069d":"local-test-model"}""",
            """{"id":"local-test-model","object":"list","obj\u0065ct":"model"}""",
            """{"id":"local-test-model","error":true,"\u0065rror":null}""",
        )
    }

    @Test
    fun `duplicate unknown model metadata is rejected inside nested objects and arrays`() {
        assertInvalidBodies(
            """{"id":"local-test-model","metadata":{"note":null,"note":true}}""",
            """{"id":"local-test-model","metadata":[{"inner":[{"note":1,"note":2}]}]}""",
            """{"id":"local-test-model","future":[],"future":[]}""",
        )
    }

    @Test
    fun `duplicate member diagnostics do not disclose private names values or parser paths`() {
        val encodedName = Gson().toJson("PRIVATE_DUPLICATE_NAME $testKey\nfield")
        val body = """{"id":"local-test-model","metadata":{$encodedName:"PRIVATE_DUPLICATE_VALUE",$encodedName:null}}"""
        withServer(AtomicReference(body)) { baseUrl ->
            val status = probe(baseUrl)

            assertInvalidStatus(status, baseUrl)
            assertSame(status, OpenAIConnectionProbe.getLastProbeResult())
            for (marker in listOf(testKey, "PRIVATE_DUPLICATE_NAME", "PRIVATE_DUPLICATE_VALUE", "JsonSyntaxException", "MalformedJsonException")) {
                assertFalse(status.summary().contains(marker), marker)
            }
        }
    }

    @Test
    fun `duplicate metadata replaces a green probe cache and a later valid probe can recover`() {
        val body = AtomicReference("""{"id":"canonical-model","object":"model"}""")
        withServer(body) { baseUrl ->
            assertTrue(probe(baseUrl).isModelAvailable())
            body.set("""{"id":"canonical-model","id":"canonical-model"}""")

            val rejected = probe(baseUrl)

            assertInvalidStatus(rejected, baseUrl)
            assertSame(rejected, OpenAIConnectionProbe.getLastProbeResult())
            body.set("""{"id":"recovered-model"}""")

            val recovered = probe(baseUrl)

            assertTrue(recovered.isReachable())
            assertTrue(recovered.isModelAvailable())
            assertEquals(listOf("recovered-model"), recovered.availableModels)
            assertTrue(recovered.errors.isEmpty())
            assertSame(recovered, OpenAIConnectionProbe.getLastProbeResult())
        }
    }

    @Test
    fun `asynchronous checks reject duplicates without changing generation availability or backoff`() {
        val body = """{"id":"local-test-model","error":{"message":"PRIVATE_DUPLICATE_VALUE"},"error":null}"""
        withServer(AtomicReference(body)) { baseUrl ->
            val service = service(baseUrl)

            val status = service.diagnoseConnection(false).get(4L, TimeUnit.SECONDS)

            assertInvalidStatus(status, baseUrl)
            assertFalse(service.checkConnection().get(4L, TimeUnit.SECONDS))
            assertTrue(service.isAvailable)
            val snapshot = service.captureDebugSnapshot()
            assertFalse(snapshot.backoffActive)
            assertFalse((status.summary() + snapshot.toString()).contains("PRIVATE_DUPLICATE_VALUE"))
        }
    }

    @Test
    fun `separate objects case sensitive names and duplicate looking string values remain valid`() {
        val quotedData = Gson().toJson("""{"id":null,"id":1}""")
        val body = """{"id":"canonical-model","ID":null,"object":"model","metadata":{"id":null,"ID":1,"note":{"id":2},"items":[{"note":null},{"note":true}],"\u00e9":1,"e\u0301":2,"raw":$quotedData}}"""
        withServer(AtomicReference(body)) { baseUrl ->
            val status = probe(baseUrl)

            assertTrue(status.isReachable())
            assertTrue(status.isModelAvailable())
            assertEquals(listOf("canonical-model"), status.availableModels)
            assertTrue(status.errors.isEmpty())
        }
    }

    @Test
    fun `probe duplicate validation does not change a supplied lenient Gson`() {
        val parser = GsonBuilder().setStrictness(Strictness.LENIENT).create()
        val body = """{"id":null,"id":"canonical-model"}"""
        withServer(AtomicReference(body)) { baseUrl ->
            val status = OpenAIConnectionProbe.probeConnection("local-test-model", baseUrl, testKey, httpClient, parser, 2)

            assertInvalidStatus(status, baseUrl)
            assertEquals("canonical-model", parser.fromJson(body, JsonObject::class.java).get("id").asString)
            assertEquals("value", parser.fromJson("{note:'value'}", JsonObject::class.java).get("note").asString)
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
