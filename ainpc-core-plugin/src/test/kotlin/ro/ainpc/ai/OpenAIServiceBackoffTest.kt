package ro.ainpc.ai

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import java.io.IOException
import java.net.http.HttpTimeoutException
import java.util.logging.Level
import java.util.logging.Logger

class OpenAIServiceBackoffTest {
    @Test
    fun `repeated HTTP failures increase cooldown up to five minutes`() {
        val service = newService()

        for (minimumDelay in listOf(15_000L, 30_000L, 60_000L, 120_000L, 240_000L, 300_000L, 300_000L)) {
            assertFailureDelay(service, OpenAIHttpException(503, "server_is_overloaded", null, "Unavailable"), minimumDelay)
        }
    }

    @Test
    fun `network timeouts escalate but malformed responses do not count as outages`() {
        val service = newService()
        assertFailureDelay(service, HttpTimeoutException("Timed out"), 15_000L)
        val previousDeadline = deadline(service)

        fail(service, IOException("Invalid response"))

        assertEquals(previousDeadline, deadline(service))
        assertFailureDelay(service, HttpTimeoutException("Timed out"), 30_000L)
    }

    @Test
    fun `stale success does not clear newer cooldown and eligible success resets escalation`() {
        val service = newService()
        val oldRequestStartedAt = System.currentTimeMillis()
        assertFailureDelay(service, HttpTimeoutException("Timed out"), 15_000L)
        assertFailureDelay(service, HttpTimeoutException("Timed out"), 30_000L)
        val activeDeadline = deadline(service)

        clearOfflineState(service, oldRequestStartedAt)

        assertEquals(activeDeadline, deadline(service))
        assertFailureDelay(service, HttpTimeoutException("Timed out"), 60_000L)

        clearOfflineState(service, deadline(service) + 1L)

        assertEquals(0L, deadline(service))
        assertFailureDelay(service, HttpTimeoutException("Timed out"), 15_000L)
    }

    @Test
    fun `long server delay survives later failures with shorter retry hints`() {
        val service = newService()
        assertFailureDelay(service, OpenAIHttpException(429, "slow_down", "600", "Slow down"), 600_000L)
        val serverDeadline = deadline(service)

        fail(service, OpenAIHttpException(503, "server_is_overloaded", "1", "Unavailable"))

        assertEquals(serverDeadline, deadline(service))
    }

    @Test
    fun `billing errors suspend without starting a transient cooldown`() {
        val service = newService()

        fail(service, OpenAIHttpException(429, "insufficient_quota", "600", "Quota exhausted"))

        assertEquals(0L, deadline(service))
        assertEquals("HTTP 429 (insufficient_quota)", service.captureDebugSnapshot().apiSuspendedReason)
    }

    private fun assertFailureDelay(service: OpenAIService, failure: Exception, expectedMinimum: Long) {
        val beforeFailure = System.currentTimeMillis()
        fail(service, failure)
        val afterFailure = System.currentTimeMillis()
        val retryAt = deadline(service)
        assertTrue(retryAt >= beforeFailure + expectedMinimum, "Deadline $retryAt is too early for $expectedMinimum")
        assertTrue(retryAt <= afterFailure + expectedMinimum + 500L, "Deadline $retryAt is too late for $expectedMinimum")
    }

    private fun fail(service: OpenAIService, failure: Exception) {
        OpenAIService::class.java.getDeclaredMethod("handleGenerationFailure", Exception::class.java).apply {
            isAccessible = true
        }.invoke(service, failure)
    }

    private fun clearOfflineState(service: OpenAIService, requestStartedAt: Long) {
        OpenAIService::class.java.getDeclaredMethod("clearOfflineState", Long::class.javaPrimitiveType).apply {
            isAccessible = true
        }.invoke(service, requestStartedAt)
    }

    private fun deadline(service: OpenAIService): Long =
        service.captureDebugSnapshot().offlineRetryAfterMillis

    private fun newService(): OpenAIService {
        val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
            field.isAccessible = true
            field.get(null) as sun.misc.Unsafe
        }
        val plugin = unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
        JavaPlugin::class.java.getDeclaredField("newConfig").apply {
            isAccessible = true
            set(plugin, YamlConfiguration().apply {
                set("features.ai", false)
                set("openai.offline_retry_seconds", 15)
            })
        }
        JavaPlugin::class.java.getDeclaredField("logger").apply {
            isAccessible = true
            set(plugin, Logger.getAnonymousLogger().apply { level = Level.OFF })
        }
        return OpenAIService(plugin)
    }
}
