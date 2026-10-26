package ro.ainpc.ai

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
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

class OpenAIResponseBodyLimitTest {
    private val responseLimitBytes = 2 * 1024 * 1024
    private val successBody = """{"status":"completed","output_text":"Salut!"}"""
    private val previousProbeResult = OpenAIConnectionProbe.getLastProbeResult()

    @AfterEach
    fun restoreProbeCache() {
        OpenAIConnectionProbe::class.java.getDeclaredField("lastProbeResult").apply {
            isAccessible = true
            set(null, previousProbeResult)
        }
    }

    @Test
    fun `oversized successful response falls back without poisoning the next generation`() {
        withServer(200, largeResponse(), followingBody = successBody) { baseUrl, requests, authorizations ->
            val fixture = fixture(baseUrl)

            assertNull(fixture.service.generateAsync("Salut!").get(6L, TimeUnit.SECONDS))

            assertTrue(fixture.service.isAvailable)
            assertFalse(fixture.service.captureDebugSnapshot().backoffActive)
            assertTrue(fixture.service.captureDebugSnapshot().lastFailureMessage.contains("limita"))
            assertPrivateBodyAbsent(fixture)
            assertEquals("Salut!", fixture.service.generateAsync("Salut din nou!").get(6L, TimeUnit.SECONDS))
            assertEquals(2, requests.get())
            assertEquals(List(2) { "Bearer local-body-limit-key" }, authorizations.toList())
        }
    }

    @Test
    fun `chunked response cannot bypass the received byte limit`() {
        withServer(200, largeResponse(), chunked = true) { baseUrl, requests, _ ->
            val fixture = fixture(baseUrl)

            assertNull(fixture.service.generateAsync("Salut!").get(6L, TimeUnit.SECONDS))

            assertEquals(1, requests.get())
            assertTrue(fixture.service.isAvailable)
            assertTrue(fixture.service.captureDebugSnapshot().lastFailureMessage.contains("limita"))
            assertPrivateBodyAbsent(fixture)
        }
    }

    @Test
    fun `complete response exactly at the byte limit is not truncated`() {
        val envelope = """{"status":"completed","output_text":"Salut!","metadata":""}"""
        val body = envelope.replace("\"metadata\":\"\"", "\"metadata\":\"${"x".repeat(responseLimitBytes - envelope.length)}\"")
        assertEquals(responseLimitBytes, body.toByteArray(Charsets.UTF_8).size)
        withServer(200, body) { baseUrl, requests, _ ->
            val fixture = fixture(baseUrl)

            assertEquals("Salut!", fixture.service.generateAsync("Salut!").get(6L, TimeUnit.SECONDS))

            assertEquals(1, requests.get())
            assertTrue(fixture.service.isAvailable)
        }
    }

    @Test
    fun `oversized rate limit body still honors status and Retry After without an immediate retry`() {
        withServer(429, largeError(), headers = mapOf("Retry-After" to "600")) { baseUrl, requests, _ ->
            val fixture = fixture(baseUrl)
            val beforeRequest = System.currentTimeMillis()

            assertNull(fixture.service.generateAsync("Salut!").get(6L, TimeUnit.SECONDS))

            val snapshot = fixture.service.captureDebugSnapshot()
            assertTrue(snapshot.backoffActive)
            assertTrue(snapshot.offlineRetryAfterMillis >= beforeRequest + 600_000L)
            assertNull(snapshot.apiSuspendedReason)
            assertTrue(snapshot.lastFailureMessage.contains("HTTP 429"))
            assertTrue(snapshot.lastFailureMessage.contains("limita"))
            assertPrivateBodyAbsent(fixture)
            assertNull(fixture.service.generateAsync("Mai incearca.").get(6L, TimeUnit.SECONDS))
            assertEquals(1, requests.get())
        }
    }

    @Test
    fun `oversized authentication error retains administrative suspension`() {
        withServer(401, largeError()) { baseUrl, requests, _ ->
            val fixture = fixture(baseUrl)

            assertNull(fixture.service.generateAsync("Salut!").get(6L, TimeUnit.SECONDS))

            val snapshot = fixture.service.captureDebugSnapshot()
            assertFalse(fixture.service.isAvailable)
            assertEquals("HTTP 401", snapshot.apiSuspendedReason)
            assertFalse(snapshot.backoffActive)
            assertTrue(snapshot.lastFailureMessage.contains("limita"))
            assertPrivateBodyAbsent(fixture)
            assertNull(fixture.service.generateAsync("Mai incearca.").get(6L, TimeUnit.SECONDS))
            assertEquals(1, requests.get())
        }
    }

    @Test
    fun `oversized model probe cannot confirm a model or close the generation gate`() {
        val body = """{"id":"local-test-model","metadata":"PRIVATE_BODY_DETAIL${"x".repeat(responseLimitBytes)}"}"""
        withServer(200, body) { baseUrl, requests, _ ->
            val fixture = fixture(baseUrl)

            val status = fixture.service.diagnoseConnection(false).get(6L, TimeUnit.SECONDS)

            assertFalse(status.isReachable())
            assertFalse(status.isModelAvailable())
            assertTrue(status.summary().contains("limita"))
            assertFalse(status.summary().contains("PRIVATE_BODY_DETAIL"))
            assertFalse(OpenAIConnectionProbe.getLastProbeResult()!!.summary().contains("PRIVATE_BODY_DETAIL"))
            assertTrue(fixture.service.isAvailable)
            assertFalse(fixture.service.captureDebugSnapshot().backoffActive)
            assertEquals(1, requests.get())
        }
    }

    private fun assertPrivateBodyAbsent(fixture: Fixture) {
        assertFalse((fixture.service.captureDebugSnapshot().toString() + fixture.messages.joinToString()).contains("PRIVATE_BODY_DETAIL"))
    }

    private fun largeResponse(): String =
        """{"status":"completed","output_text":"Salut!","metadata":"PRIVATE_BODY_DETAIL${"x".repeat(responseLimitBytes)}"}"""

    private fun largeError(): String =
        """{"error":{"message":"PRIVATE_BODY_DETAIL${"x".repeat(responseLimitBytes)}"}}"""

    private fun withServer(
        status: Int,
        body: String,
        chunked: Boolean = false,
        headers: Map<String, String> = emptyMap(),
        followingBody: String? = null,
        action: (String, AtomicInteger, ConcurrentLinkedQueue<String>) -> Unit,
    ) {
        val requests = AtomicInteger()
        val authorizations = ConcurrentLinkedQueue<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val requestNumber = requests.incrementAndGet()
            authorizations.add(exchange.requestHeaders.getFirst("Authorization").orEmpty())
            try {
                exchange.requestBody.use { it.readAllBytes() }
                val bytes = (if (requestNumber > 1 && followingBody != null) followingBody else body).toByteArray(Charsets.UTF_8)
                headers.forEach { (name, value) -> exchange.responseHeaders.set(name, value) }
                exchange.sendResponseHeaders(status, if (chunked) 0L else bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            } catch (_: IOException) {
            } finally {
                exchange.close()
            }
        }
        server.start()
        try {
            action("http://127.0.0.1:${server.address.port}/v1", requests, authorizations)
        } finally {
            server.stop(0)
        }
    }

    private fun fixture(baseUrl: String): Fixture {
        val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
            field.isAccessible = true
            field.get(null) as sun.misc.Unsafe
        }
        val plugin = unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
        val config = YamlConfiguration().apply {
            set("features.ai", true)
            set("ai_provider", "openai")
            set("openai.api_key", "local-body-limit-key")
            set("openai.base_url", baseUrl)
            set("openai.model", "local-test-model")
            set("openai.connect_timeout", 1)
            set("openai.read_timeout", 4)
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
        return Fixture(OpenAIService(plugin), messages)
    }

    private data class Fixture(val service: OpenAIService, val messages: ConcurrentLinkedQueue<String>)
}
