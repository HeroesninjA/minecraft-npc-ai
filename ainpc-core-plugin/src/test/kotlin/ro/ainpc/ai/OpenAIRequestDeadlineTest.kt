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
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger

class OpenAIRequestDeadlineTest {
    private val previousProbeResult = OpenAIConnectionProbe.getLastProbeResult()

    @AfterEach
    fun restoreProbeCache() {
        OpenAIConnectionProbe::class.java.getDeclaredField("lastProbeResult").apply {
            isAccessible = true
            set(null, previousProbeResult)
        }
    }

    @Test
    fun `model probe uses the configured deadline while waiting for headers`() {
        assertProbeDeadline(partialBody = false)
    }

    @Test
    fun `model probe deadline also bounds a partially received body`() {
        assertProbeDeadline(partialBody = true)
    }

    @Test
    fun `generation times out waiting for headers and enters cooldown without retrying`() {
        assertGenerationDeadline(partialBody = false)
    }

    @Test
    fun `generation deadline also bounds a partially received body`() {
        assertGenerationDeadline(partialBody = true)
    }

    @Test
    fun `successful probe and generation preserve their payloads and authorization`() {
        val authorizations = ConcurrentLinkedQueue<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            authorizations.add(exchange.requestHeaders.getFirst("Authorization"))
            exchange.requestBody.use { it.readAllBytes() }
            val body = if (exchange.requestURI.path.endsWith("/responses")) {
                """{"status":"completed","output_text":"Salut!"}"""
            } else {
                """{"id":"local-test-model"}"""
            }.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            val service = service("http://127.0.0.1:${server.address.port}/v1")

            val status = service.diagnoseConnection(false).get(4L, TimeUnit.SECONDS)
            assertTrue(status.isReachable())
            assertTrue(status.isModelAvailable())
            assertEquals("Salut!", service.generateAsync("Salut!").get(4L, TimeUnit.SECONDS))
            assertEquals(listOf("Bearer local-deadline-test-key", "Bearer local-deadline-test-key"), authorizations.toList())
            assertTrue(service.isAvailable)
        } finally {
            server.stop(0)
        }
    }

    private fun assertProbeDeadline(partialBody: Boolean) {
        withStalledServer(partialBody) { baseUrl, requests ->
            val service = service(baseUrl)

            val status = service.diagnoseConnection(false).get(4L, TimeUnit.SECONDS)

            assertFalse(status.isReachable())
            assertTrue(status.getSummary().contains("HttpTimeoutException"), status.getSummary())
            assertEquals(1, requests.get())
            assertTrue(service.isAvailable)
        }
    }

    private fun assertGenerationDeadline(partialBody: Boolean) {
        withStalledServer(partialBody) { baseUrl, requests ->
            val service = service(baseUrl)

            assertNull(service.generateAsync("Salut!").get(4L, TimeUnit.SECONDS))

            assertFalse(service.isAvailable)
            assertTrue(service.captureDebugSnapshot().lastFailureMessage.contains("HttpTimeoutException"))
            assertNull(service.generateAsync("Mai incearca.").get(4L, TimeUnit.SECONDS))
            assertEquals(1, requests.get())
        }
    }

    private fun withStalledServer(partialBody: Boolean, action: (String, AtomicInteger) -> Unit) {
        val release = CountDownLatch(1)
        val requests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requests.incrementAndGet()
            exchange.requestBody.use { it.readAllBytes() }
            try {
                if (partialBody) {
                    exchange.sendResponseHeaders(200, 64L)
                    exchange.responseBody.write('{'.code)
                    exchange.responseBody.flush()
                }
                release.await(10L, TimeUnit.SECONDS)
            } finally {
                exchange.close()
            }
        }
        server.start()
        try {
            action("http://127.0.0.1:${server.address.port}/v1", requests)
        } finally {
            release.countDown()
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
            set("openai.api_key", "local-deadline-test-key")
            set("openai.base_url", baseUrl)
            set("openai.model", "local-test-model")
            set("openai.connect_timeout", 1)
            set("openai.read_timeout", 1)
            set("openai.diagnostics.enabled", false)
        }
        val logger = Logger.getAnonymousLogger().apply { useParentHandlers = false }
        JavaPlugin::class.java.getDeclaredField("newConfig").apply { isAccessible = true; set(plugin, config) }
        JavaPlugin::class.java.getDeclaredField("logger").apply { isAccessible = true; set(plugin, logger) }
        return OpenAIService(plugin)
    }
}
