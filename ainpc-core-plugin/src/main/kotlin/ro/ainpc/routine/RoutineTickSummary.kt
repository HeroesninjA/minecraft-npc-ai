package ro.ainpc.routine

class RoutineTickSummary(
    private val enabledValue: Boolean,
    private val totalNpcsValue: Int,
    private val evaluatedNpcsValue: Int,
    private val movedNpcsValue: Int,
    private val skippedBusyValue: Int,
    private val skippedMissingTargetValue: Int,
    private val skippedInvalidTargetValue: Int
) {
    fun enabled(): Boolean = enabledValue

    fun totalNpcs(): Int = totalNpcsValue

    fun evaluatedNpcs(): Int = evaluatedNpcsValue

    fun movedNpcs(): Int = movedNpcsValue

    fun skippedBusy(): Int = skippedBusyValue

    fun skippedMissingTarget(): Int = skippedMissingTargetValue

    fun skippedInvalidTarget(): Int = skippedInvalidTargetValue

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is RoutineTickSummary) {
            return false
        }

        return enabledValue == other.enabledValue &&
            totalNpcsValue == other.totalNpcsValue &&
            evaluatedNpcsValue == other.evaluatedNpcsValue &&
            movedNpcsValue == other.movedNpcsValue &&
            skippedBusyValue == other.skippedBusyValue &&
            skippedMissingTargetValue == other.skippedMissingTargetValue &&
            skippedInvalidTargetValue == other.skippedInvalidTargetValue
    }

    override fun hashCode(): Int {
        var result = enabledValue.hashCode()
        result = 31 * result + totalNpcsValue
        result = 31 * result + evaluatedNpcsValue
        result = 31 * result + movedNpcsValue
        result = 31 * result + skippedBusyValue
        result = 31 * result + skippedMissingTargetValue
        result = 31 * result + skippedInvalidTargetValue
        return result
    }

    override fun toString(): String =
        "RoutineTickSummary[enabled=$enabledValue, totalNpcs=$totalNpcsValue, evaluatedNpcs=$evaluatedNpcsValue, " +
            "movedNpcs=$movedNpcsValue, skippedBusy=$skippedBusyValue, " +
            "skippedMissingTarget=$skippedMissingTargetValue, skippedInvalidTarget=$skippedInvalidTargetValue]"

    companion object {
        @JvmStatic
        fun disabled(totalNpcs: Int): RoutineTickSummary =
            RoutineTickSummary(false, totalNpcs, 0, 0, 0, 0, 0)
    }
}
