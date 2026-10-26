package ro.ainpc.ai

import com.google.gson.Gson
import com.sun.net.httpserver.HttpServer
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

class OpenAIRequestAdmissionTest {
    @Test
    fun `generic generation defers if cooldown begins after its initial availability check`() {
        withServer { baseUrl, requests ->
            val fixture = fixture(baseUrl)
            fixture.onPrompt.set(Runnable { block(fixture.state) })

            assertNull(fixture.service.generateAsync("Salut!").get(5L, TimeUnit.SECONDS))

            assertEquals(0, requests.get())
            assertEquals("offline_backoff", fixture.service.captureDebugSnapshot().lastFallbackReason)
            assertEquals(0L, fixture.service.captureDebugSnapshot().lastFailureAtMillis)
            assertEquals(0L, fixture.service.captureDebugSnapshot().lastRequestAtMillis)
            assertTrue(fixture.service.captureDebugSnapshot().backoffActive)
        }
    }

    @Test
    fun `generic generation defers if the API is suspended while preparing its request`() {
        withServer { baseUrl, requests ->
            val fixture = fixture(baseUrl)
            fixture.onPrompt.set(Runnable { fixture.state.suspendApi("HTTP 401 (invalid_api_key)") })

            assertNull(fixture.service.generateAsync("Salut!").get(5L, TimeUnit.SECONDS))

            assertEquals(0, requests.get())
            assertEquals("api_suspended", fixture.service.captureDebugSnapshot().lastFallbackReason)
            assertEquals(0L, fixture.service.captureDebugSnapshot().lastFailureAtMillis)
            assertEquals("HTTP 401 (invalid_api_key)", fixture.state.suspensionReason)
        }
    }

    @Test
    fun `conversational format retry cannot send through a newly active cooldown`() {
        assertRetryBlocked { block(it) }
    }

    @Test
    fun `conversational format retry cannot send through a new administrative suspension`() {
        assertRetryBlocked { it.suspendApi("HTTP 429 (insufficient_quota)") }
    }

    @Test
    fun `reloaded client observes admission blocks from the previous client for the same backend`() {
        withServer { baseUrl, requests ->
            val fixture = fixture(baseUrl)
            val replacement = fixture.service.reloadedFromConfig()
            block(fixture.state)
            val deadline = fixture.state.retryAfterMillis

            assertThrows(IOException::class.java) { call(replacement, "Salut!") }

            assertEquals(0, requests.get())
            assertEquals(deadline, fixture.state.retryAfterMillis)
            assertFalse(replacement.isAvailable)
        }
    }

    @Test
    fun `changing credentials keeps admission isolated from the previous backend gate`() {
        withServer { baseUrl, requests ->
            val fixture = fixture(baseUrl)
            block(fixture.state)
            fixture.config.set("openai.api_key", "different-local-test-key")
            val replacement = fixture.service.reloadedFromConfig()

            assertEquals("Salut!", call(replacement, "Salut!"))

            assertEquals(1, requests.get())
            assertTrue(replacement.isAvailable)
            assertFalse(fixture.service.isAvailable)
        }
    }

    @Test
    fun `explicit model probes remain available without resetting the generation gate`() {
        val previousProbeResult = OpenAIConnectionProbe.getLastProbeResult()
        try {
            withServer { baseUrl, requests ->
                val fixture = fixture(baseUrl)
                block(fixture.state)
                val deadline = fixture.state.retryAfterMillis

                val status = fixture.service.diagnoseConnection(false).get(5L, TimeUnit.SECONDS)

                assertTrue(status.isReachable())
                assertTrue(status.isModelAvailable())
                assertEquals(1, requests.get())
                assertEquals(deadline, fixture.state.retryAfterMillis)
                assertFalse(fixture.service.isAvailable)
            }
        } finally {
            OpenAIConnectionProbe::class.java.getDeclaredField("lastProbeResult").apply {
                isAccessible = true
                set(null, previousProbeResult)
            }
        }
    }

    @Test
    fun `already admitted request may finish without clearing a newer cooldown`() {
        val received = CountDownLatch(1)
        val release = CountDownLatch(1)
        try {
            withServer(onRequest = {
                received.countDown()
                release.await(5L, TimeUnit.SECONDS)
            }) { baseUrl, requests ->
                val fixture = fixture(baseUrl)
                val response = fixture.service.generateAsync("Salut!")
                try {
                    assertTrue(received.await(2L, TimeUnit.SECONDS))
                    block(fixture.state)
                    val deadline = fixture.state.retryAfterMillis
                    release.countDown()

                    assertEquals("Salut!", response.get(5L, TimeUnit.SECONDS))
                    assertEquals(deadline, fixture.state.retryAfterMillis)
                    assertNull(fixture.service.generateAsync("Mai incearca.").get(5L, TimeUnit.SECONDS))
                    assertEquals(1, requests.get())
                } finally {
                    release.countDown()
                }
            }
        } finally {
            release.countDown()
        }
    }

    private fun assertRetryBlocked(closeGate: (OpenAIBackoffState) -> Unit) {
        withServer(responseText = "{\"reply\":\"Replica invalida\"}") { baseUrl, requests ->
            val fixture = fixture(baseUrl)
            var attempts = 0

            assertThrows(IOException::class.java) {
                DialogueResponseNovelty.generate(
                    "Ion: ", "Ion", "Salut!", emptyList(), null, false, retryInvalidReplies = true,
                ) { prompt ->
                    attempts++
                    val response = call(fixture.service, prompt)
                    if (attempts == 1) {
                        closeGate(fixture.state)
                    }
                    response
                }
            }

            assertEquals(2, attempts)
            assertEquals(1, requests.get())
            assertFalse(fixture.service.isAvailable)
        }
    }

    private fun block(state: OpenAIBackoffState) {
        state.scheduleFailure(System.currentTimeMillis()) { 600_000L }
    }

    private fun call(service: OpenAIService, prompt: String): String {
        val method = OpenAIService::class.java.declaredMethods.single { it.name == "callOpenAI" }
            .apply { isAccessible = true }
        return try {
            method.invoke(service, prompt, "Ion", null, OpenAIPromptBuilder.dialogueInstructions) as String
        } catch (exception: InvocationTargetException) {
            throw exception.targetException
        }
    }

    private fun withServer(onRequest: () -> Unit = {}, responseText: String = "Salut!", action: (String, AtomicInteger) -> Unit) {
        val requests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requests.incrementAndGet()
            exchange.requestBody.use { it.readAllBytes() }
            onRequest()
            val body = if (exchange.requestURI.path.endsWith("/responses")) {
                Gson().toJson(mapOf("status" to "completed", "output_text" to responseText))
            } else {
                """{"id":"local-test-model"}"""
            }.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            action("http://127.0.0.1:${server.address.port}/v1", requests)
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
            set("openai.api_key", "local-admission-test-key")
            set("openai.base_url", baseUrl)
            set("openai.model", "local-test-model")
            set("openai.connect_timeout", 1)
            set("openai.read_timeout", 3)
            set("openai.diagnostics.enabled", true)
        }
        val onPrompt = AtomicReference<Runnable?>()
        val logger = Logger.getAnonymousLogger().apply {
            level = Level.ALL
            useParentHandlers = false
            addHandler(object : Handler() {
                override fun publish(record: LogRecord) {
                    if (record.message.contains("Cerere Responses:")) onPrompt.getAndSet(null)?.run()
                }
                override fun flush() = Unit
                override fun close() = Unit
            })
        }
        JavaPlugin::class.java.getDeclaredField("newConfig").apply { isAccessible = true; set(plugin, config) }
        JavaPlugin::class.java.getDeclaredField("logger").apply { isAccessible = true; set(plugin, logger) }
        val service = OpenAIService(plugin)
        val state = OpenAIService::class.java.getDeclaredField("backoffState").apply { isAccessible = true }
            .get(service) as OpenAIBackoffState
        return Fixture(service, config, state, onPrompt)
    }

    private data class Fixture(
        val service: OpenAIService,
        val config: YamlConfiguration,
        val state: OpenAIBackoffState,
        val onPrompt: AtomicReference<Runnable?>,
    )
}
