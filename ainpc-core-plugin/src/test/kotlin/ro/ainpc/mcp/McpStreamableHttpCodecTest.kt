package ro.ainpc.mcp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class McpStreamableHttpCodecTest {
    @Test
    fun `extracts json payload from streamable http event`() {
        val event = """
            id:session-1
            event:message
            data:{"jsonrpc":"2.0","id":"2","result":{"content":[{"type":"text","text":"{\"status\":\"ok\"}"}],"isError":false}}
        """.trimIndent()

        val payload = McpStreamableHttpCodec.extractJsonPayload(event)

        assertTrue(payload.startsWith("{\"jsonrpc\""))
        assertEquals("{\"status\":\"ok\"}", McpStreamableHttpCodec.firstTextContent(payload))
    }

    @Test
    fun `extracts last json payload from mixed stream events`() {
        val event = """
            event:ping
            data:
            data:{"jsonrpc":"2.0","id":"1","result":{"content":[{"type":"text","text":"{\"status\":\"warmup\"}"}],"isError":false}}
            event:message
            data:{"jsonrpc":"2.0","id":"2","result":{"content":[{"type":"text","text":"{\"status\":\"ok\"}"}],"isError":false}}
        """.trimIndent()

        val payload = McpStreamableHttpCodec.extractJsonPayload(event)

        assertEquals("{\"status\":\"ok\"}", McpStreamableHttpCodec.firstTextContent(payload))
    }

    @Test
    fun `falls back to raw structured content when text is absent`() {
        val payload = """{"jsonrpc":"2.0","id":"4","result":{"content":[{"type":"resource","data":"{\"blockCount\":3}"}],"isError":false}}"""

        assertEquals("{\"blockCount\":3}", McpStreamableHttpCodec.firstTextContent(payload))
    }

    @Test
    fun `builds tool call request with object arguments only`() {
        val request = McpStreamableHttpCodec.toolCallRequest("7", "ainpc.feature.state", "[]")

        assertTrue(request.contains("\"method\":\"tools/call\""))
        assertTrue(request.contains("\"name\":\"ainpc.feature.state\""))
        assertTrue(request.contains("\"arguments\":{}"))
    }

    @Test
    fun `extracts json error message`() {
        val error = """{"jsonrpc":"2.0","id":"3","error":{"code":-32601,"message":"Tool not found"}}"""

        assertEquals("Tool not found", McpStreamableHttpCodec.errorMessage(error))
    }
}
