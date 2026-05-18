package ro.ainpc.ai

data class DialogHistory(
    val playerMessage: String,
    val npcResponse: String,
    val timestamp: Long
)
