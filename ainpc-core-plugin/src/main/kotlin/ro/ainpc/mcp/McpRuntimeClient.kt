package ro.ainpc.mcp

import java.util.concurrent.CompletableFuture

interface McpRuntimeClient {
    fun health(): McpHealthResult
    fun callTool(toolName: String, argumentsJson: String = "{}"): McpToolCallResult

    fun healthAsync(): CompletableFuture<McpHealthResult> = CompletableFuture.supplyAsync { health() }
    fun callToolAsync(toolName: String, argumentsJson: String = "{}"): CompletableFuture<McpToolCallResult> =
        CompletableFuture.supplyAsync { callTool(toolName, argumentsJson) }
}
