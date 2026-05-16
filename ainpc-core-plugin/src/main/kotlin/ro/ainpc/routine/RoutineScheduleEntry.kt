package ro.ainpc.routine

import kotlin.math.max

class RoutineScheduleEntry(
    label: String?,
    worldTime: Long,
    private val assignmentValue: RoutineAssignment?
) {
    private val labelValue = label?.trim().orEmpty()
    private val worldTimeValue = max(0L, worldTime)

    fun label(): String = labelValue

    fun worldTime(): Long = worldTimeValue

    fun assignment(): RoutineAssignment? = assignmentValue

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is RoutineScheduleEntry) {
            return false
        }

        return labelValue == other.labelValue &&
            worldTimeValue == other.worldTimeValue &&
            assignmentValue == other.assignmentValue
    }

    override fun hashCode(): Int {
        var result = labelValue.hashCode()
        result = 31 * result + worldTimeValue.hashCode()
        result = 31 * result + (assignmentValue?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "RoutineScheduleEntry[label=$labelValue, worldTime=$worldTimeValue, assignment=$assignmentValue]"
}
