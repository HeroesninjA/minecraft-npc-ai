package ro.ainpc.mcp

class NoopMcpRuntimeClient(private val config: McpRuntimeConfig) : McpRuntimeClient {
    override fun health(): McpHealthResult = McpHealthResult(
        enabled = false,
        available = false,
        status = "disabled",
        detail = "MCP runtime este dezactivat prin features.mcp sau mcp.enabled.",
        endpoint = config.baseUrl
    )

    override fun callTool(toolName: String, argumentsJson: String): McpToolCallResult = McpToolCallResult(
        available = false,
        toolName = toolName,
        status = "disabled",
        contentJson = "{}",
        detail = "MCP runtime este dezactivat prin features.mcp sau mcp.enabled."
    )
}
