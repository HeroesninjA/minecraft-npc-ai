package ro.ainpc.mcp

data class McpHealthResult(
    val enabled: Boolean,
    val available: Boolean,
    val status: String,
    val detail: String,
    val endpoint: String,
    val durationMillis: Long = 0L
)
