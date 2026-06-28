package ro.ainpc.mcp

interface McpRuntimeClient {
    fun health(): McpHealthResult
    fun callTool(toolName: String, argumentsJson: String = "{}"): McpToolCallResult
}
