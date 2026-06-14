package ro.ainpc.engine

data class QuestGuiObjective @JvmOverloads constructor(
    val key: String,
    val type: String,
    val label: String,
    val description: String,
    val stageId: String,
    val stageLabel: String,
    val stateId: String = "",
    val stateDisplay: String = "",
    val currentAmount: Int = 0,
    val requiredAmount: Int = 1,
    val complete: Boolean = false,
    val active: Boolean = false
) {
    constructor(
        key: String,
        type: String,
        label: String,
        description: String,
        stageId: String,
        stageLabel: String,
        currentAmount: Int,
        requiredAmount: Int,
        complete: Boolean,
        active: Boolean
    ) : this(
        key = key,
        type = type,
        label = label,
        description = description,
        stageId = stageId,
        stageLabel = stageLabel,
        stateId = legacyObjectiveState(currentAmount, requiredAmount, complete, active).id(),
        stateDisplay = legacyObjectiveState(currentAmount, requiredAmount, complete, active).displayName(),
        currentAmount = currentAmount,
        requiredAmount = requiredAmount,
        complete = complete,
        active = active
    )

    companion object {
        @JvmStatic
        private fun legacyObjectiveState(
            currentAmount: Int,
            requiredAmount: Int,
            complete: Boolean,
            active: Boolean
        ): QuestObjectiveState {
            val cappedCurrent = currentAmount.coerceAtLeast(0)
            val cappedRequired = requiredAmount.coerceAtLeast(1)
            return when {
                complete || cappedCurrent >= cappedRequired -> QuestObjectiveState.COMPLETED
                !active -> QuestObjectiveState.PENDING
                cappedCurrent > 0 -> QuestObjectiveState.IN_PROGRESS
                else -> QuestObjectiveState.STARTED
            }
        }
    }
}
