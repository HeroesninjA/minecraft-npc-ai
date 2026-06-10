package ro.ainpc.ai

data class OpenAIDebugSnapshot(
    val baseUrl: String,
    val model: String,
    val apiKeyPresent: Boolean,
    val connectTimeoutSeconds: Int,
    val readTimeoutSeconds: Int,
    val writeTimeoutSeconds: Int,
    val maxOutputTokens: Int,
    val temperature: Double,
    val storeResponses: Boolean,
    val offlineRetrySeconds: Long,
    val diagnosticsEnabled: Boolean,
    val diagnosticsCheckOnStartup: Boolean,
    val logPromptSummary: Boolean,
    val logResponsePreview: Boolean,
    val offlineMode: Boolean,
    val offlineRetryAfterMillis: Long,
    val nowMillis: Long,
    val lastRequestAtMillis: Long,
    val lastPromptChars: Int,
    val lastPromptPreview: String,
    val lastResponseAtMillis: Long,
    val lastResponseChars: Int,
    val lastResponsePreview: String,
    val lastFailureAtMillis: Long,
    val lastFailureMessage: String,
    val lastFallbackAtMillis: Long,
    val lastFallbackReason: String
) {
    val backoffActive: Boolean
        get() = nowMillis < offlineRetryAfterMillis

    val backoffRemainingSeconds: Long
        get() = maxOf(0L, (offlineRetryAfterMillis - nowMillis + 999L) / 1000L)
}
