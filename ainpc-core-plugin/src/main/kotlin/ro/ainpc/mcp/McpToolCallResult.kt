package ro.ainpc.mcp

data class McpToolCallResult(
    val available: Boolean,
    val toolName: String,
    val status: String,
    val contentJson: String,
    val detail: String,
    val durationMillis: Long = 0L
)
