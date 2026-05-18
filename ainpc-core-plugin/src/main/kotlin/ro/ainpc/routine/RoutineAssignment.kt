package ro.ainpc.routine

import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState

class RoutineAssignment(
    private val slotValue: RoutineSlot?,
    private val activityValue: String?,
    private val goalValue: String?,
    private val targetStateValue: NPCState?,
    private val targetAnchorValue: AINPC.OwnedLocation?
) {
    fun slot(): RoutineSlot? = slotValue

    fun activity(): String? = activityValue

    fun goal(): String? = goalValue

    fun targetState(): NPCState? = targetStateValue

    fun targetAnchor(): AINPC.OwnedLocation? = targetAnchorValue

    fun hasTargetAnchor(): Boolean = targetAnchorValue != null

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is RoutineAssignment) {
            return false
        }

        return slotValue == other.slotValue &&
            activityValue == other.activityValue &&
            goalValue == other.goalValue &&
            targetStateValue == other.targetStateValue &&
            targetAnchorValue == other.targetAnchorValue
    }

    override fun hashCode(): Int {
        var result = slotValue?.hashCode() ?: 0
        result = 31 * result + (activityValue?.hashCode() ?: 0)
        result = 31 * result + (goalValue?.hashCode() ?: 0)
        result = 31 * result + (targetStateValue?.hashCode() ?: 0)
        result = 31 * result + (targetAnchorValue?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "RoutineAssignment[slot=$slotValue, activity=$activityValue, goal=$goalValue, " +
            "targetState=$targetStateValue, targetAnchor=$targetAnchorValue]"
}
