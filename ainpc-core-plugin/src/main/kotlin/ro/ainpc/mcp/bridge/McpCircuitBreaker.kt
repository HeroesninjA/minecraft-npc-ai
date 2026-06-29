package ro.ainpc.mcp.bridge

import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

class McpCircuitBreaker(
    private val retryAfterMs: Long = 5000L
) {
    @Volatile
    private var downSince: Long = 0L

    fun isOpen(): Boolean {
        if (downSince == 0L) return false
        if (System.currentTimeMillis() - downSince > retryAfterMs) {
            downSince = 0L
            return false
        }
        return true
    }

    fun markDown() { if (downSince == 0L) downSince = System.currentTimeMillis() }
    fun markUp() { downSince = 0L }
}
