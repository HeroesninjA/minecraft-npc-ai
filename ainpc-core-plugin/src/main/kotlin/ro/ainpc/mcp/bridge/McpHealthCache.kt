package ro.ainpc.mcp.bridge

import ro.ainpc.mcp.McpHealthResult
import ro.ainpc.mcp.McpRuntimeClient
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

class McpHealthCache(private val client: McpRuntimeClient) {
    private val cached = AtomicReference<CachedHealth>()

    fun get(): McpHealthResult {
        val c = cached.get()
        if (c != null && !c.isExpired()) return c.result
        return refresh()
    }

    fun refreshAsync(): CompletableFuture<McpHealthResult> =
        client.healthAsync().thenApply { result ->
            cached.set(CachedHealth(result, Instant.now()))
            result
        }

    private fun refresh(): McpHealthResult {
        val result = client.health()
        cached.set(CachedHealth(result, Instant.now()))
        return result
    }

    private data class CachedHealth(
        val result: McpHealthResult,
        val loadedAt: Instant
    ) {
        fun isExpired(): Boolean = loadedAt.plusSeconds(5).isBefore(Instant.now())
    }
}
