package ro.ainpc.ai

import java.io.IOException
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ThreadLocalRandom

internal class OpenAIHttpException(
    val statusCode: Int,
    val errorCode: String,
    val retryAfter: String?,
    message: String,
) : IOException(message)

internal object OpenAIHttpBackoff {
    private val permanentRateLimitCodes = setOf(
        "insufficient_quota",
        "credit_balance_exhausted",
        "billing_hard_limit_reached",
        "organization_spend_limit_exceeded",
        "project_spend_limit_exceeded",
        "organization_usage_limit_exceeded",
    )

    fun suspensionReason(statusCode: Int, errorCode: String): String? {
        val normalizedCode = errorCode.lowercase(Locale.ROOT)
        val needsConfiguration = statusCode == 401 || statusCode == 403 ||
            statusCode == 404 && normalizedCode == "model_not_found" ||
            statusCode == 429 && normalizedCode in permanentRateLimitCodes
        if (!needsConfiguration) return null
        return "HTTP $statusCode" + if (normalizedCode.isBlank()) "" else " ($normalizedCode)"
    }

    fun delayMillis(
        statusCode: Int,
        errorCode: String,
        retryAfter: String?,
        nowMillis: Long,
        minimumDelayMillis: Long,
    ): Long? {
        if (suspensionReason(statusCode, errorCode) != null) return null
        if (statusCode !in setOf(408, 409, 429) && statusCode !in 500..599) return null

        val serverDelay = parseRetryAfterMillis(retryAfter, nowMillis) ?: 0L
        val delay = maxOf(minimumDelayMillis.coerceAtLeast(0L), serverDelay)
        return saturatingAdd(delay, ThreadLocalRandom.current().nextLong(100L, 501L))
    }

    internal fun parseRetryAfterMillis(value: String?, nowMillis: Long): Long? {
        val header = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        header.toLongOrNull()?.let { seconds ->
            if (seconds < 0L) return null
            return seconds.coerceAtMost(Long.MAX_VALUE / 1000L) * 1000L
        }
        return runCatching {
            val retryAt = ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME)
            Duration.between(java.time.Instant.ofEpochMilli(nowMillis), retryAt.toInstant())
                .toMillis()
                .coerceAtLeast(0L)
        }.getOrNull()
    }

    internal fun saturatingAdd(first: Long, second: Long): Long =
        if (Long.MAX_VALUE - first < second) Long.MAX_VALUE else first + second
}
