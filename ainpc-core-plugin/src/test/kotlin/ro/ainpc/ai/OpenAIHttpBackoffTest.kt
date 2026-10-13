package ro.ainpc.ai

import com.google.gson.Gson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class OpenAIHttpBackoffTest {
    @Test
    fun `temporary rate limits honor retry after`() {
        val delay = OpenAIHttpBackoff.delayMillis(429, "slow_down", "56", 0L, 15_000L)

        assertTrue(delay != null && delay in 56_100L..56_500L)
    }

    @Test
    fun `invalid retry after falls back to configured cooldown`() {
        val delay = OpenAIHttpBackoff.delayMillis(503, "server_is_overloaded", "invalid", 0L, 15_000L)

        assertTrue(delay != null && delay in 15_100L..15_500L)
    }

    @Test
    fun `http date retry after is interpreted relative to response time`() {
        val nowMillis = Instant.parse("2026-10-12T04:00:00Z").toEpochMilli()
        val retryAt = DateTimeFormatter.RFC_1123_DATE_TIME.format(
            Instant.ofEpochMilli(nowMillis + 30_000L).atZone(ZoneOffset.UTC)
        )

        assertEquals(30_000L, OpenAIHttpBackoff.parseRetryAfterMillis(retryAt, nowMillis))
    }

    @Test
    fun `quota and authentication errors require configuration intervention`() {
        for (code in listOf(
            "insufficient_quota",
            "credit_balance_exhausted",
            "organization_spend_limit_exceeded",
            "project_spend_limit_exceeded",
            "organization_usage_limit_exceeded",
        )) {
            assertNull(OpenAIHttpBackoff.delayMillis(429, code, "60", 0L, 15_000L), code)
            assertEquals("HTTP 429 ($code)", OpenAIHttpBackoff.suspensionReason(429, code))
        }
        assertEquals("HTTP 401 (invalid_api_key)", OpenAIHttpBackoff.suspensionReason(401, "invalid_api_key"))
        assertEquals("HTTP 403", OpenAIHttpBackoff.suspensionReason(403, ""))
        assertEquals("HTTP 404 (model_not_found)", OpenAIHttpBackoff.suspensionReason(404, "model_not_found"))
        assertNull(OpenAIHttpBackoff.delayMillis(401, "invalid_api_key", "60", 0L, 15_000L))
        assertNull(OpenAIHttpBackoff.delayMillis(400, "invalid_request_error", null, 0L, 15_000L))
        assertNull(OpenAIHttpBackoff.suspensionReason(400, "invalid_request_error"))
        assertNull(OpenAIHttpBackoff.suspensionReason(429, "slow_down"))
    }

    @Test
    fun `error code is extracted from structured api response`() {
        val gson = Gson()

        assertEquals(
            "slow_down",
            OpenAITextSupport.extractOpenAIErrorCode(gson, """{"error":{"code":"slow_down","message":"wait"}}""")
        )
        assertEquals(
            "insufficient_quota",
            OpenAITextSupport.extractOpenAIErrorCode(gson, """{"error":{"type":"insufficient_quota"}}""")
        )
        assertEquals("", OpenAITextSupport.extractOpenAIErrorCode(gson, "invalid json"))
    }
}
