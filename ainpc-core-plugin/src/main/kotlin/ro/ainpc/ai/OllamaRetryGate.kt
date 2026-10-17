package ro.ainpc.ai

internal class OllamaRetryGate(private val clockMillis: () -> Long = System::currentTimeMillis) {
    @Volatile
    private var retryAfterMillis = 0L

    fun canAttempt(): Boolean = clockMillis() >= retryAfterMillis

    fun recordFailure(delayMillis: Long) {
        val retryAt = OpenAIHttpBackoff.saturatingAdd(clockMillis(), delayMillis.coerceAtLeast(0L))
        synchronized(this) {
            retryAfterMillis = maxOf(retryAfterMillis, retryAt)
        }
    }

    fun recordSuccess(requestStartedAtMillis: Long): Boolean = synchronized(this) {
        if (retryAfterMillis > requestStartedAtMillis) return@synchronized false
        retryAfterMillis = 0L
        true
    }

    fun reset() {
        synchronized(this) {
            retryAfterMillis = 0L
        }
    }
}
