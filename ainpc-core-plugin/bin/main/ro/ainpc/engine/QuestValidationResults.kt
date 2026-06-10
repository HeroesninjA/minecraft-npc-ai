package ro.ainpc.engine

class QuestAvailability private constructor(
    private val available: Boolean,
    issues: List<String>?,
) {
    private val issues: List<String> = java.util.List.copyOf(issues ?: emptyList())

    fun available(): Boolean = available

    fun issues(): List<String> = issues

    companion object {
        @JvmStatic
        fun allowed(): QuestAvailability = QuestAvailability(true, emptyList())

        @JvmStatic
        fun unavailable(issues: List<String>?): QuestAvailability = QuestAvailability(false, issues)
    }
}

class QuestRewardCheck private constructor(
    private val canGrant: Boolean,
    issues: List<String>?,
) {
    private val issues: List<String> = java.util.List.copyOf(issues ?: emptyList())

    fun canGrant(): Boolean = canGrant

    fun issues(): List<String> = issues

    companion object {
        @JvmStatic
        fun allowed(): QuestRewardCheck = QuestRewardCheck(true, emptyList())

        @JvmStatic
        fun blocked(issues: List<String>?): QuestRewardCheck = QuestRewardCheck(false, issues)
    }
}
