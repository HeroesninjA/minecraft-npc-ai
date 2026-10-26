package ro.ainpc.ai

import com.sun.net.httpserver.HttpServer
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import java.net.InetSocketAddress
import java.net.http.HttpRequest
import java.net.http.HttpTimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger

class OpenAIServiceReloadTest {
    @Test
    fun `unchanged reload preserves server cooldown and availability gate`() {
        val (original, _) = fixture()
        fail(original, OpenAIHttpException(429, "slow_down", "600", "Wait"))
        val deadline = snapshot(original).offlineRetryAfterMillis

        val replacement = original.reloadedFromConfig()

        assertNotSame(original, replacement)
        assertEquals(deadline, snapshot(replacement).offlineRetryAfterMillis)
        assertTrue(snapshot(replacement).backoffActive)
        assertFalse(replacement.isAvailable)
    }

    @Test
    fun `failure progression survives repeated reloads`() {
        val (original, _) = fixture()
        fail(original, HttpTimeoutException("Timed out"))
        val replacement = original.reloadedFromConfig()
        assertNextDelay(replacement, 30_000L)

        val nextReplacement = replacement.reloadedFromConfig()

        assertNextDelay(nextReplacement, 60_000L)
    }

    @Test
    fun `in flight failure on old instance extends cooldown seen by replacement`() {
        val (original, _) = fixture()
        val oldRequestStartedAt = System.currentTimeMillis()
        val replacement = original.reloadedFromConfig()

        fail(original, HttpTimeoutException("Late timeout"))

        val deadline = snapshot(original).offlineRetryAfterMillis
        assertTrue(deadline > System.currentTimeMillis())
        assertEquals(deadline, snapshot(replacement).offlineRetryAfterMillis)
        clearOfflineState(original, oldRequestStartedAt)
        assertEquals(deadline, snapshot(replacement).offlineRetryAfterMillis)
        assertFalse(replacement.isAvailable)
    }

    @Test
    fun `new transport settings apply without changing old client or discarding cooldown`() {
        val (original, config) = fixture()
        fail(original, HttpTimeoutException("Timed out"))
        val oldSnapshot = snapshot(original)
        config.set("openai.connect_timeout", 4)
        config.set("openai.read_timeout", 9)
        config.set("openai.write_timeout", 7)
        config.set("openai.max_output_tokens", 333)
        config.set("openai.temperature", 0.25)
        config.set("openai.store", true)
        config.set("openai.model", "configured-other-model")

        val replacement = original.reloadedFromConfig()
        val active = snapshot(replacement)

        assertEquals(4, active.connectTimeoutSeconds)
        assertEquals(9, active.readTimeoutSeconds)
        assertEquals(7, active.writeTimeoutSeconds)
        assertEquals(333, active.maxOutputTokens)
        assertEquals(0.25, active.temperature)
        assertTrue(active.storeResponses)
        assertEquals("configured-other-model", active.model)
        assertEquals(oldSnapshot.model, snapshot(original).model)
        assertEquals(oldSnapshot.connectTimeoutSeconds, snapshot(original).connectTimeoutSeconds)
        assertEquals(oldSnapshot.offlineRetryAfterMillis, active.offlineRetryAfterMillis)
        assertEquals(9L, request(replacement).timeout().orElseThrow().seconds)
        assertFalse(active.toString().contains("reload-test-key"))
    }

    @Test
    fun `changed key or endpoint starts isolated gate and uses new settings`() {
        for ((path, value) in mapOf(
            "openai.api_key" to "replacement-test-key",
            "openai.base_url" to "http://127.0.0.1:2/v1",
        )) {
            val (original, config) = fixture()
            fail(original, HttpTimeoutException("Timed out"))
            config.set(path, value)

            val replacement = original.reloadedFromConfig()

            assertEquals(0L, snapshot(replacement).offlineRetryAfterMillis, path)
            assertTrue(replacement.isAvailable, path)
            when (path) {
                "openai.api_key" -> {
                    assertEquals("Bearer replacement-test-key", request(replacement).headers().firstValue("Authorization").orElseThrow())
                    assertEquals("Bearer reload-test-key", request(original).headers().firstValue("Authorization").orElseThrow())
                }
                "openai.base_url" -> assertEquals(value, snapshot(replacement).baseUrl)
            }
            fail(original, OpenAIHttpException(401, "invalid_api_key", null, "Old credentials"))
            assertNull(snapshot(replacement).apiSuspendedReason, path)
            assertEquals(0L, snapshot(replacement).offlineRetryAfterMillis, path)
        }
    }

    @Test
    fun `equivalent normalized credentials and endpoint do not bypass cooldown`() {
        val (original, config) = fixture()
        fail(original, HttpTimeoutException("Timed out"))
        config.set("openai.api_key", "  reload-test-key  ")
        config.set("openai.base_url", "  http://127.0.0.1:1/v1/  ")

        val replacement = original.reloadedFromConfig()

        assertEquals(snapshot(original).offlineRetryAfterMillis, snapshot(replacement).offlineRetryAfterMillis)
        assertFalse(replacement.isAvailable)
    }

    @Test
    fun `manual reload clears suspension without shortening a transient cooldown`() {
        val (original, _) = fixture()
        fail(original, HttpTimeoutException("Timed out"))
        fail(original, OpenAIHttpException(429, "insufficient_quota", null, "Quota exhausted"))
        val deadline = snapshot(original).offlineRetryAfterMillis

        val replacement = original.reloadedFromConfig()

        assertNull(snapshot(replacement).apiSuspendedReason)
        assertEquals(deadline, snapshot(replacement).offlineRetryAfterMillis)
        assertFalse(replacement.isAvailable)
    }

    @Test
    fun `invalid replacement configuration leaves original gate and settings intact`() {
        val (original, config) = fixture()
        fail(original, OpenAIHttpException(429, "insufficient_quota", null, "Quota exhausted"))
        val previous = snapshot(original)
        config.set("openai.connect_timeout", 0)

        assertThrows(IllegalArgumentException::class.java) { original.reloadedFromConfig() }

        assertEquals(previous.connectTimeoutSeconds, snapshot(original).connectTimeoutSeconds)
        assertEquals(previous.apiSuspendedReason, snapshot(original).apiSuspendedReason)
    }

    @Test
    fun `nonpositive read timeout is rejected during initial construction`() {
        for (timeout in listOf(0, -1)) {
            val failure = assertThrows(IllegalArgumentException::class.java) {
                fixture { configuration -> configuration.set("openai.read_timeout", timeout) }
            }

            assertTrue(failure.message.orEmpty().contains("openai.read_timeout"))
            assertNull(failure.cause)
        }
    }

    @Test
    fun `nonpositive connect timeout has a safe configuration error`() {
        for (timeout in listOf(0, -1)) {
            val failure = assertThrows(IllegalArgumentException::class.java) {
                fixture { configuration -> configuration.set("openai.connect_timeout", timeout) }
            }

            assertTrue(failure.message.orEmpty().contains("openai.connect_timeout"))
            assertNull(failure.cause)
        }
    }

    @Test
    fun `invalid read timeout reload preserves shared suspension cooldown and failure progression`() {
        val (original, config) = fixture()
        val shared = original.reloadedFromConfig()
        fail(original, HttpTimeoutException("Timed out"))
        fail(original, OpenAIHttpException(429, "insufficient_quota", null, "Quota exhausted"))
        val previous = snapshot(original)

        for (timeout in listOf(0, -1)) {
            config.set("openai.read_timeout", timeout)
            config.set("openai.model", "invalid-replacement-model")

            assertThrows(IllegalArgumentException::class.java) { original.reloadedFromConfig() }

            for (client in listOf(original, shared)) {
                assertEquals(previous.apiSuspendedReason, snapshot(client).apiSuspendedReason)
                assertEquals(previous.offlineRetryAfterMillis, snapshot(client).offlineRetryAfterMillis)
                assertEquals(previous.model, snapshot(client).model)
                assertEquals(previous.readTimeoutSeconds.toLong(), request(client).timeout().orElseThrow().seconds)
                assertFalse(client.isAvailable)
            }
        }

        config.set("openai.read_timeout", 9)
        val replacement = original.reloadedFromConfig()
        assertNull(snapshot(replacement).apiSuspendedReason)
        assertEquals(previous.offlineRetryAfterMillis, snapshot(replacement).offlineRetryAfterMillis)
        assertNextDelay(replacement, 30_000L)
        assertEquals(snapshot(original).offlineRetryAfterMillis, snapshot(replacement).offlineRetryAfterMillis)
    }

    @Test
    fun `unsupported or hostless endpoint is rejected during initial construction`() {
        for (endpoint in listOf("ftp://127.0.0.1/v1", "http:///v1", "http://")) {
            val failure = assertThrows(IllegalArgumentException::class.java) {
                fixture { configuration -> configuration.set("openai.base_url", endpoint) }
            }

            assertTrue(failure.message.orEmpty().contains("openai.base_url"))
            assertNull(failure.cause)
        }
    }

    @Test
    fun `invalid endpoint reload leaves original transport and gate intact`() {
        val (original, config) = fixture()
        fail(original, OpenAIHttpException(429, "slow_down", "600", "Wait"))
        fail(original, OpenAIHttpException(401, "invalid_api_key", null, "Old credentials"))
        val previous = snapshot(original)

        for (endpoint in listOf("ftp://127.0.0.1/v1", "http:///v1")) {
            config.set("openai.base_url", endpoint)

            assertThrows(IllegalArgumentException::class.java) { original.reloadedFromConfig() }

            assertEquals(previous.baseUrl, snapshot(original).baseUrl)
            assertEquals(previous.offlineRetryAfterMillis, snapshot(original).offlineRetryAfterMillis)
            assertEquals(previous.apiSuspendedReason, snapshot(original).apiSuspendedReason)
            assertEquals("Bearer reload-test-key", request(original).headers().firstValue("Authorization").orElseThrow())
        }
    }

    @Test
    fun `invalid authorization header is rejected without exposing credentials`() {
        val credential = "synthetic-header-secret\r\nInjected: synthetic-private-value"
        val failure = assertThrows(IllegalArgumentException::class.java) {
            fixture { configuration -> configuration.set("openai.api_key", credential) }
        }

        assertTrue(failure.message.orEmpty().contains("openai.api_key"))
        assertFalse(failure.toString().contains("synthetic-header-secret"))
        assertFalse(failure.toString().contains("synthetic-private-value"))
        assertNull(failure.cause)
        assertTrue(failure.suppressed.isEmpty())
    }

    @Test
    fun `invalid credential reload leaves original authorization and gate intact`() {
        val (original, config) = fixture()
        fail(original, OpenAIHttpException(429, "slow_down", "600", "Wait"))
        fail(original, OpenAIHttpException(401, "invalid_api_key", null, "Old credentials"))
        val previous = snapshot(original)
        config.set("openai.api_key", "synthetic-header-secret\ninvalid")

        assertThrows(IllegalArgumentException::class.java) { original.reloadedFromConfig() }

        assertEquals(previous.offlineRetryAfterMillis, snapshot(original).offlineRetryAfterMillis)
        assertEquals(previous.apiSuspendedReason, snapshot(original).apiSuspendedReason)
        assertEquals("Bearer reload-test-key", request(original).headers().firstValue("Authorization").orElseThrow())
    }

    @Test
    fun `malformed endpoint error does not retain raw URI or nested secret bearing cause`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            fixture { configuration ->
                configuration.set("openai.base_url", "http://private-user:synthetic-uri-secret@invalid host/v1")
            }
        }

        assertTrue(failure.message.orEmpty().contains("openai.base_url"))
        assertFalse(failure.toString().contains("private-user"))
        assertFalse(failure.toString().contains("synthetic-uri-secret"))
        assertNull(failure.cause)
        assertTrue(failure.suppressed.isEmpty())
    }

    @Test
    fun `valid normalized endpoints remain accepted without a connectivity check`() {
        for ((configured, expected) in mapOf(
            "  http://127.0.0.1:1/v1/  " to "http://127.0.0.1:1/v1",
            "  127.0.0.1:1/v1/  " to "https://127.0.0.1:1/v1",
            "https://127.0.0.1:1/v1" to "https://127.0.0.1:1/v1",
        )) {
            val (service, _) = fixture { configuration ->
                configuration.set("openai.base_url", configured)
                configuration.set("openai.connect_timeout", 1)
                configuration.set("openai.read_timeout", 1)
                configuration.set("openai.api_key", "  reload-test-key  ")
            }

            assertEquals(expected, snapshot(service).baseUrl)
            assertEquals(1L, request(service).timeout().orElseThrow().seconds)
            assertEquals("Bearer reload-test-key", request(service).headers().firstValue("Authorization").orElseThrow())
        }
    }

    @Test
    fun `constructor and replacement validation do not send HTTP requests`() {
        val requests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1") { exchange ->
            requests.incrementAndGet()
            exchange.sendResponseHeaders(401, -1)
            exchange.close()
        }
        server.start()
        try {
            val (service, _) = fixture { configuration ->
                configuration.set("openai.base_url", "http://127.0.0.1:${server.address.port}/v1")
            }

            service.reloadedFromConfig()

            assertEquals(0, requests.get())
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `local reload after a configured key edit preserves the immutable clients cooldown`() {
        val (original, config) = fixture()
        fail(original, OpenAIHttpException(429, "slow_down", "600", "Wait"))
        val previous = snapshot(original)
        config.set("openai.api_key", "replacement-test-key")
        config.set("openai.model", "replacement-model")

        original.reloadFromConfig()

        assertEquals(previous.offlineRetryAfterMillis, snapshot(original).offlineRetryAfterMillis)
        assertTrue(snapshot(original).backoffActive)
        assertFalse(original.isAvailable)
        assertEquals(previous.model, snapshot(original).model)
        assertEquals("Bearer reload-test-key", request(original).headers().firstValue("Authorization").orElseThrow())
    }

    @Test
    fun `local reload cannot release another immutable client sharing the old backend gate`() {
        val (original, config) = fixture()
        val sameBackendClient = original.reloadedFromConfig()
        fail(original, OpenAIHttpException(429, "slow_down", "600", "Wait"))
        val deadline = snapshot(original).offlineRetryAfterMillis
        config.set("openai.api_key", "replacement-test-key")

        original.reloadFromConfig()

        assertEquals(deadline, snapshot(original).offlineRetryAfterMillis)
        assertEquals(deadline, snapshot(sameBackendClient).offlineRetryAfterMillis)
        assertFalse(sameBackendClient.isAvailable)
        assertEquals("Bearer reload-test-key", request(sameBackendClient).headers().firstValue("Authorization").orElseThrow())
    }

    @Test
    fun `configured key edits followed by local resets do not restart failure progression`() {
        val (original, config) = fixture()
        fail(original, HttpTimeoutException("Timed out"))

        for ((key, minimumDelay) in listOf("replacement-key-1" to 30_000L, "replacement-key-2" to 60_000L)) {
            config.set("openai.api_key", key)
            original.reloadFromConfig()

            assertNextDelay(original, minimumDelay)
        }
    }

    @Test
    fun `local reset clears administrative suspension but only replacement adopts a fresh key and gate`() {
        val (original, config) = fixture()
        fail(original, OpenAIHttpException(429, "slow_down", "600", "Wait"))
        fail(original, OpenAIHttpException(401, "invalid_api_key", null, "Old credentials"))
        val deadline = snapshot(original).offlineRetryAfterMillis
        config.set("openai.api_key", "replacement-test-key")

        original.reloadFromConfig()

        assertNull(snapshot(original).apiSuspendedReason)
        assertEquals(deadline, snapshot(original).offlineRetryAfterMillis)
        assertFalse(original.isAvailable)
        val replacement = original.reloadedFromConfig()
        assertTrue(replacement.isAvailable)
        assertEquals(0L, snapshot(replacement).offlineRetryAfterMillis)
        assertEquals("Bearer replacement-test-key", request(replacement).headers().firstValue("Authorization").orElseThrow())
        assertNextDelay(replacement, 15_000L)
        assertEquals(deadline, snapshot(original).offlineRetryAfterMillis)
    }

    private fun assertNextDelay(service: OpenAIService, expectedMinimum: Long) {
        val beforeFailure = System.currentTimeMillis()
        fail(service, HttpTimeoutException("Timed out"))
        val afterFailure = System.currentTimeMillis()
        val deadline = snapshot(service).offlineRetryAfterMillis
        assertTrue(deadline >= beforeFailure + expectedMinimum)
        assertTrue(deadline <= afterFailure + expectedMinimum + 500L)
    }

    private fun fail(service: OpenAIService, exception: Exception) {
        OpenAIService::class.java.getDeclaredMethod("handleGenerationFailure", Exception::class.java).apply {
            isAccessible = true
        }.invoke(service, exception)
    }

    private fun clearOfflineState(service: OpenAIService, requestStartedAt: Long) {
        OpenAIService::class.java.getDeclaredMethod("clearOfflineState", Long::class.javaPrimitiveType).apply {
            isAccessible = true
        }.invoke(service, requestStartedAt)
    }

    private fun request(service: OpenAIService): HttpRequest =
        (OpenAIService::class.java.getDeclaredMethod("newRequestBuilder", String::class.java).apply {
            isAccessible = true
        }.invoke(service, "http://127.0.0.1:1/v1/responses") as HttpRequest.Builder).build()

    private fun snapshot(service: OpenAIService): OpenAIDebugSnapshot = service.captureDebugSnapshot()

    private fun fixture(configure: (YamlConfiguration) -> Unit = {}): Pair<OpenAIService, YamlConfiguration> {
        val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
            field.isAccessible = true
            field.get(null) as sun.misc.Unsafe
        }
        val plugin = unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
        val config = YamlConfiguration().apply {
            set("features.ai", false)
            set("openai.api_key", "reload-test-key")
            set("openai.base_url", "http://127.0.0.1:1/v1")
            set("openai.model", "gpt-5.4-nano")
            set("openai.offline_retry_seconds", 15)
        }
        configure(config)
        JavaPlugin::class.java.getDeclaredField("newConfig").apply {
            isAccessible = true
            set(plugin, config)
        }
        JavaPlugin::class.java.getDeclaredField("logger").apply {
            isAccessible = true
            set(plugin, Logger.getAnonymousLogger().apply { level = Level.OFF })
        }
        return OpenAIService(plugin) to config
    }
}
