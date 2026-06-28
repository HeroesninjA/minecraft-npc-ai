package ro.ainpc.mcp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.CookieHandler
import java.net.ProxySelector
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.Principal
import java.util.Optional
import javax.net.ssl.SSLSession

class HttpMcpRuntimeClientTest {
    @Test
    fun `reuses session and retries once on stale session`() {
        val requests = mutableListOf<HttpRequest>()
        val responseQueue = ArrayDeque<HttpResponse<String>>(
            listOf(
                response(200, """{"jsonrpc":"2.0","id":"1","result":{"protocolVersion":"2025-06-18"}}""", mapOf("Mcp-Session-Id" to listOf("session-1"))),
                response(200, """{"jsonrpc":"2.0","method":"notifications/initialized"}"""),
                response(200, """{"jsonrpc":"2.0","id":"2","error":{"message":"session invalid"}}"""),
                response(200, """{"jsonrpc":"2.0","id":"3","result":{"protocolVersion":"2025-06-18"}}""", mapOf("Mcp-Session-Id" to listOf("session-2"))),
                response(200, """{"jsonrpc":"2.0","method":"notifications/initialized"}"""),
                response(200, """{"jsonrpc":"2.0","id":"4","result":{"content":[{"type":"text","text":"{\"ok\":true}"}],"isError":false}}""")
            )
        )

        val client = HttpMcpRuntimeClient(testConfig()) { request ->
            requests += request
            responseQueue.removeFirst()
        }

        val result = client.callTool("ainpc.feature.state")

        assertTrue(result.available)
        assertEquals("{\"ok\":true}", result.contentJson)
        assertEquals(6, requests.size)
        assertTrue(requests[0].headers().firstValue("Mcp-Session-Id").isEmpty)
        assertTrue(requests[1].headers().firstValue("Mcp-Session-Id").isPresent)
        assertEquals("session-1", requests[1].headers().firstValue("Mcp-Session-Id").orElse(""))
        assertEquals("session-1", requests[2].headers().firstValue("Mcp-Session-Id").orElse(""))
        assertEquals("session-2", requests[4].headers().firstValue("Mcp-Session-Id").orElse(""))
        assertEquals("POST", requests[2].method())
    }

    @Test
    fun `caches session across successive tool calls`() {
        val requests = mutableListOf<HttpRequest>()
        val responseQueue = ArrayDeque<HttpResponse<String>>(
            listOf(
                response(200, """{"jsonrpc":"2.0","id":"1","result":{"protocolVersion":"2025-06-18"}}""", mapOf("Mcp-Session-Id" to listOf("session-1"))),
                response(200, """{"jsonrpc":"2.0","method":"notifications/initialized"}"""),
                response(200, """{"jsonrpc":"2.0","id":"2","result":{"content":[{"type":"text","text":"{\"ok\":1}"}],"isError":false}}"""),
                response(200, """{"jsonrpc":"2.0","id":"3","result":{"content":[{"type":"text","text":"{\"ok\":2}"}],"isError":false}}""")
            )
        )

        val client = HttpMcpRuntimeClient(testConfig()) { request ->
            requests += request
            responseQueue.removeFirst()
        }

        val first = client.callTool("ainpc.feature.state")
        val second = client.callTool("ainpc.feature.state")

        assertTrue(first.available)
        assertTrue(second.available)
        assertEquals("{\"ok\":1}", first.contentJson)
        assertEquals("{\"ok\":2}", second.contentJson)
        assertEquals(4, requests.size)
        assertEquals("session-1", requests[1].headers().firstValue("Mcp-Session-Id").orElse(""))
        assertEquals("session-1", requests[2].headers().firstValue("Mcp-Session-Id").orElse(""))
        assertEquals("session-1", requests[3].headers().firstValue("Mcp-Session-Id").orElse(""))
    }

    private fun testConfig(): McpRuntimeConfig = McpRuntimeConfig(
        enabled = true,
        baseUrl = "http://127.0.0.1:39841/mcp",
        token = "",
        connectTimeout = java.time.Duration.ofSeconds(1),
        readTimeout = java.time.Duration.ofSeconds(1),
        failOpen = true
    )

    private fun response(
        statusCode: Int,
        body: String,
        headers: Map<String, List<String>> = emptyMap()
    ): HttpResponse<String> = object : HttpResponse<String> {
        override fun statusCode(): Int = statusCode
        override fun request(): HttpRequest? = null
        override fun previousResponse(): Optional<HttpResponse<String>> = Optional.empty()
        override fun headers(): HttpHeaders = HttpHeaders.of(headers) { _, _ -> true }
        override fun body(): String = body
        override fun sslSession(): Optional<SSLSession> = Optional.empty()
        override fun uri(): URI = URI.create("http://127.0.0.1:39841/mcp")
        override fun version(): HttpClient.Version = HttpClient.Version.HTTP_1_1
    }
}
