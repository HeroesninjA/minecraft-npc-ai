package ro.ainpc.ai

data class DialogHistory(
    val playerMessage: String,
    val npcResponse: String,
    val timestamp: Long,
    val branchStatus: BranchStatus = BranchStatus.EXECUTED,
    val branchReason: String = ""
) {
    enum class BranchStatus {
        PROPOSED,
        SELECTED,
        EXECUTED,
        REJECTED,
        FAILED
    }

    fun isStable(): Boolean = branchStatus == BranchStatus.EXECUTED || branchStatus == BranchStatus.SELECTED
    fun isFailed(): Boolean = branchStatus == BranchStatus.FAILED || branchStatus == BranchStatus.REJECTED

    companion object {
        fun recentStable(history: List<DialogHistory>?): List<DialogHistory> = history.orEmpty()
            .filter { it.isStable() && it.playerMessage.isNotBlank() && it.npcResponse.isNotBlank() }
            .sortedBy { it.timestamp }
            .takeLast(5)
    }
}
