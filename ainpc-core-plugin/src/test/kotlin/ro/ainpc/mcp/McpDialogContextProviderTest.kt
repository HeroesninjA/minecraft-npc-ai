package ro.ainpc.mcp

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.mcp.bridge.McpDialogContextProvider

class McpDialogContextProviderTest {
    @Test
    fun `uses direct MCP text payload and requests the NPC region`() {
        val client = DialogClient(
            """{"available":true,"snapshotState":"fresh","local":{"regionId":"demo_sat","npcCount":6,"npcExamples":[{"name":"Gheorghe","profession":"Fierar"}]}}"""
        )

        val context = McpDialogContextProvider.enrich(client, true, true, "demo_sat")

        assertEquals("demo_sat", JsonParser.parseString(client.arguments).asJsonObject.get("regionId").asString)
        assertTrue(context.contains("6 NPC-uri inregistrate"))
        assertTrue(context.contains("Gheorghe (Fierar)"))
        assertTrue(context.contains("lista incompleta"))
        assertFalse(context.contains("jucatori online"))
    }

    @Test
    fun `ignores stale or unrelated region data`() {
        val staleClient = DialogClient(
            """{"available":true,"snapshotState":"stale","local":{"regionId":"demo_sat","npcCount":6}}"""
        )
        val otherRegionClient = DialogClient(
            """{"available":true,"snapshotState":"fresh","local":{"regionId":"alt_sat","npcCount":2}}"""
        )
        val blankRegionClient = DialogClient("")

        assertEquals("", McpDialogContextProvider.enrich(staleClient, true, true, "demo_sat"))
        assertEquals("", McpDialogContextProvider.enrich(otherRegionClient, true, true, "demo_sat"))
        assertEquals("", McpDialogContextProvider.enrich(blankRegionClient, true, true, ""))
        assertEquals("", blankRegionClient.arguments)
    }

    private class DialogClient(private val content: String) : McpRuntimeClient {
        var arguments: String = ""

        override fun health(): McpHealthResult = McpHealthResult(true, true, "healthy", "", "")

        override fun callTool(toolName: String, argumentsJson: String): McpToolCallResult {
            assertEquals("ainpc.dialog.context", toolName)
            arguments = argumentsJson
            return McpToolCallResult(true, toolName, "success", content, "")
        }
    }
}
