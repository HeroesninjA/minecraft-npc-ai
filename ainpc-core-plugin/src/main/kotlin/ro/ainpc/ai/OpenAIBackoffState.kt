package ro.ainpc.ai

internal class OpenAIBackoffState {
    @Volatile
    var retryAfterMillis = 0L
        private set

    @Volatile
    var offlineMode = false
        private set

    @Volatile
    var suspensionReason: String? = null
        private set

    private var consecutiveFailures = 0

    @Synchronized
    fun blockedReason(nowMillis: Long): String? = when {
        suspensionReason != null -> "api_suspended"
        nowMillis < retryAfterMillis -> "offline_backoff"
        else -> null
    }

    @Synchronized
    fun scheduleFailure(nowMillis: Long, delayForAttempt: (Int) -> Long): Pair<Boolean, Long> {
        consecutiveFailures = minOf(consecutiveFailures + 1, 64)
        val delay = delayForAttempt(consecutiveFailures)
        retryAfterMillis = maxOf(retryAfterMillis, OpenAIHttpBackoff.saturatingAdd(nowMillis, delay))
        val previouslyOffline = offlineMode
        offlineMode = true
        return (!previouslyOffline) to delay
    }

    @Synchronized
    fun clearOnSuccess(requestStartedAtMillis: Long): Boolean {
        if (suspensionReason != null || retryAfterMillis > requestStartedAtMillis) return false
        val previouslyOffline = offlineMode
        offlineMode = false
        retryAfterMillis = 0L
        consecutiveFailures = 0
        return previouslyOffline
    }

    @Synchronized
    fun suspendApi(reason: String): Boolean {
        if (suspensionReason != null) return false
        suspensionReason = reason
        return true
    }

    @Synchronized
    fun reset(clearCooldown: Boolean) {
        if (clearCooldown) {
            retryAfterMillis = 0L
            offlineMode = false
            consecutiveFailures = 0
        }
        suspensionReason = null
    }
}
