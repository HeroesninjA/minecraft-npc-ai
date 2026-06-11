package ro.ainpc.progression

import ro.ainpc.engine.ScenarioEngine

class ProgressionObjectiveSnapshot(
    key: String?,
    type: String?,
    label: String?,
    description: String?,
    stageId: String?,
    stageLabel: String?,
    stateId: String?,
    stateDisplay: String?,
    currentAmount: Int,
    requiredAmount: Int,
    private val completeValue: Boolean,
    private val activeValue: Boolean
) {
    private val keyValue: String = valueOrEmpty(key)
    private val typeValue: String = valueOrEmpty(type)
    private val labelValue: String = valueOrEmpty(label)
    private val descriptionValue: String = valueOrEmpty(description)
    private val stageIdValue: String = valueOrEmpty(stageId)
    private val stageLabelValue: String = valueOrEmpty(stageLabel)
    private val stateIdValue: String = valueOrEmpty(stateId)
    private val stateDisplayValue: String = valueOrEmpty(stateDisplay)
    private val currentAmountValue: Int = currentAmount.coerceAtLeast(0)
    private val requiredAmountValue: Int = requiredAmount.coerceAtLeast(1)

    fun key(): String = keyValue
    fun type(): String = typeValue
    fun label(): String = labelValue
    fun description(): String = descriptionValue
    fun stageId(): String = stageIdValue
    fun stageLabel(): String = stageLabelValue
    fun stateId(): String = stateIdValue
    fun stateDisplay(): String = stateDisplayValue
    fun currentAmount(): Int = currentAmountValue
    fun requiredAmount(): Int = requiredAmountValue
    fun complete(): Boolean = completeValue
    fun active(): Boolean = activeValue

    companion object {
        @JvmStatic
        fun fromQuestGuiObjective(objective: ScenarioEngine.QuestGuiObjective?): ProgressionObjectiveSnapshot {
            if (objective == null) {
                return ProgressionObjectiveSnapshot("", "", "", "", "", "", "", "", 0, 1, false, false)
            }
            return ProgressionObjectiveSnapshot(
                objective.key(),
                objective.type(),
                objective.label(),
                objective.description(),
                objective.stageId(),
                objective.stageLabel(),
                objective.stateId(),
                objective.stateDisplay(),
                objective.currentAmount(),
                objective.requiredAmount(),
                objective.complete(),
                objective.active()
            )
        }

        private fun valueOrEmpty(value: String?): String = value?.trim().orEmpty()
    }
}
