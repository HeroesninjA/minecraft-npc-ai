package ro.ainpc.mcp

import java.net.ConnectException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import ro.ainpc.mcp.bridge.McpCircuitBreaker

class HttpMcpRuntimeClient(
    private val config: McpRuntimeConfig,
    private val requestExecutor: (HttpRequest) -> HttpResponse<String>
) : McpRuntimeClient {
    private val requestIds = AtomicLong(1L)
    private val sessionId = AtomicReference<String?>(null)
    private val sessionLock = Any()
    private val circuitBreaker = McpCircuitBreaker()

    // Pentru health check, timeout mai scurt (1s fail-fast)
    private val healthClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(1))
        .build()
    private val healthExecutor: (HttpRequest) -> HttpResponse<String> =
        { req -> healthClient.send(req, BodyHandlers.ofString()) }

    constructor(config: McpRuntimeConfig) : this(
        config,
        HttpClient.newBuilder()
            .connectTimeout(config.connectTimeout)
            .build()
            .let { client -> { request -> client.send(request, BodyHandlers.ofString()) } }
    )

    override fun health(): McpHealthResult {
        if (circuitBreaker.isOpen()) {
            return McpHealthResult(
                enabled = true, available = false,
                status = "unavailable_circuit_open",
                detail = "MCP indisponibil (circuit breaker activ). Reincerc in 5s.",
                endpoint = config.baseUrl, durationMillis = 0L
            )
        }
        val startedAt = System.nanoTime()
        return try {
            val requestBuilder = HttpRequest.newBuilder(config.healthUrl)
                .timeout(Duration.ofSeconds(2))
                .GET()
            if (config.token.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer ${config.token}")
            }
            val response = healthExecutor(requestBuilder.build())
            val durationMillis = elapsedMillis(startedAt)
            circuitBreaker.markUp()
            if (response.statusCode() in 200..299) {
                McpHealthResult(
                    enabled = true, available = true,
                    status = "healthy",
                    detail = "Sidecar MCP disponibil (${response.statusCode()}).",
                    endpoint = config.baseUrl, durationMillis = durationMillis
                )
            } else {
                McpHealthResult(
                    enabled = true, available = false,
                    status = "degraded",
                    detail = "Health endpoint a raspuns cu HTTP ${response.statusCode()}.",
                    endpoint = config.baseUrl, durationMillis = durationMillis
                )
            }
        } catch (_: ConnectException) {
            circuitBreaker.markDown()
            unavailable(startedAt, "Sidecar MCP indisponibil: conexiune refuzata.")
        } catch (e: Exception) {
            circuitBreaker.markDown()
            unavailable(startedAt, "Sidecar MCP indisponibil: ${e.javaClass.simpleName}.")
        }
    }

    private fun unavailable(startedAt: Long, detail: String): McpHealthResult = McpHealthResult(
        enabled = true,
        available = false,
        status = if (config.failOpen) "unavailable_fail_open" else "unavailable",
        detail = detail,
        endpoint = config.baseUrl,
        durationMillis = elapsedMillis(startedAt)
    )

    override fun callTool(toolName: String, argumentsJson: String): McpToolCallResult {
        if (circuitBreaker.isOpen()) {
            return McpToolCallResult(
                available = false, toolName = toolName,
                status = "unavailable_circuit_open", contentJson = "{}",
                detail = "MCP indisponibil (circuit breaker activ). Reincerc in 5s.", durationMillis = 0L
            )
        }
        val startedAt = System.nanoTime()
        return try {
            executeToolCall(toolName, argumentsJson, startedAt, allowRetry = true)
        } catch (_: ConnectException) {
            circuitBreaker.markDown()
            toolUnavailable(startedAt, toolName, "Sidecar MCP indisponibil: conexiune refuzata.")
        } catch (e: Exception) {
            circuitBreaker.markDown()
            toolUnavailable(startedAt, toolName, "MCP tool call indisponibil: ${e.javaClass.simpleName}.")
        }
    }

    override fun healthAsync(): CompletableFuture<McpHealthResult> =
        CompletableFuture.supplyAsync { health() }

    override fun callToolAsync(toolName: String, argumentsJson: String): CompletableFuture<McpToolCallResult> =
        CompletableFuture.supplyAsync { callTool(toolName, argumentsJson) }

    private fun executeToolCall(
        toolName: String,
        argumentsJson: String,
        startedAt: Long,
        allowRetry: Boolean
    ): McpToolCallResult {
        val currentSessionId = getOrCreateSession()
        val callPayload = McpStreamableHttpCodec.toolCallRequest(nextId(), toolName, argumentsJson)
        val response = sendMcpRequest(callPayload, currentSessionId)
        val jsonPayload = McpStreamableHttpCodec.extractJsonPayload(response.body())
        val error = McpStreamableHttpCodec.errorMessage(jsonPayload)
        if (response.statusCode() !in 200..299 || error != null) {
            if (allowRetry && isStaleSessionError(response.statusCode(), jsonPayload, error)) {
                invalidateSession(currentSessionId)
                return executeToolCall(toolName, argumentsJson, startedAt, allowRetry = false)
            }
            return McpToolCallResult(
                available = false,
                toolName = toolName,
                status = "error",
                contentJson = "{}",
                detail = error ?: "MCP tool call a raspuns cu HTTP ${response.statusCode()}.",
                durationMillis = elapsedMillis(startedAt)
            )
        }
        return McpToolCallResult(
            available = true,
            toolName = toolName,
            status = "success",
            contentJson = McpStreamableHttpCodec.firstTextContent(jsonPayload),
            detail = "MCP tool call executat.",
            durationMillis = elapsedMillis(startedAt)
        )
    }

    private fun getOrCreateSession(): String {
        sessionId.get()?.let { return it }
        return synchronized(sessionLock) {
            sessionId.get() ?: createSession().also { sessionId.set(it) }
        }
    }

    private fun createSession(): String {
        val response = sendMcpRequest(McpStreamableHttpCodec.initializeRequest(nextId()), null)
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("MCP initialize HTTP ${response.statusCode()}")
        }
        val newSessionId = response.headers().firstValue("Mcp-Session-Id")
            .orElseThrow { IllegalStateException("MCP initialize missing session id") }
        sendInitialized(newSessionId)
        return newSessionId
    }

    private fun sendInitialized(sessionId: String) {
        val response = sendMcpRequest(McpStreamableHttpCodec.initializedNotification(), sessionId)
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("MCP initialized HTTP ${response.statusCode()}")
        }
    }

    private fun sendMcpRequest(payload: String, sessionId: String?): HttpResponse<String> {
        val requestBuilder = HttpRequest.newBuilder(java.net.URI.create(config.baseUrl))
            .timeout(config.readTimeout)
            .header("Accept", "application/json, text/event-stream")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
        if (sessionId != null) {
            requestBuilder.header("Mcp-Session-Id", sessionId)
        }
        if (config.token.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${config.token}")
        }
        return requestExecutor(requestBuilder.build())
    }

    private fun toolUnavailable(startedAt: Long, toolName: String, detail: String): McpToolCallResult =
        McpToolCallResult(
            available = false,
            toolName = toolName,
            status = if (config.failOpen) "unavailable_fail_open" else "unavailable",
            contentJson = "{}",
            detail = detail,
            durationMillis = elapsedMillis(startedAt)
        )

    private fun invalidateSession(expectedSessionId: String) {
        sessionId.compareAndSet(expectedSessionId, null)
    }

    private fun isStaleSessionError(statusCode: Int, jsonPayload: String, error: String?): Boolean {
        if (statusCode in 401..404 || statusCode == 409 || statusCode == 412) {
            return true
        }
        val combined = listOfNotNull(error, jsonPayload)
            .joinToString(" ")
            .lowercase()
        return combined.contains("session") && (
            combined.contains("invalid") ||
                combined.contains("missing") ||
                combined.contains("expired") ||
                combined.contains("stale") ||
                combined.contains("not found")
            )
    }

    private fun nextId(): String = requestIds.getAndIncrement().toString()

    private fun elapsedMillis(startedAt: Long): Long = (System.nanoTime() - startedAt) / 1_000_000L
}
