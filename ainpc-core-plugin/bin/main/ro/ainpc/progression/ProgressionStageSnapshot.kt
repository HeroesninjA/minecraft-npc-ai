package ro.ainpc.progression

import ro.ainpc.engine.ScenarioEngine

class ProgressionStageSnapshot(
    id: String?,
    label: String?,
    description: String?,
    completionMode: String?,
    nextStageId: String?,
    private val activeValue: Boolean,
    private val completeValue: Boolean,
    objectiveIds: List<String>?
) {
    private val idValue: String = valueOrEmpty(id)
    private val labelValue: String = valueOrEmpty(label)
    private val descriptionValue: String = valueOrEmpty(description)
    private val completionModeValue: String = valueOrEmpty(completionMode)
    private val nextStageIdValue: String = valueOrEmpty(nextStageId)
    private val objectiveIdsValue: List<String> = (objectiveIds ?: emptyList()).toList()

    fun id(): String = idValue
    fun label(): String = labelValue
    fun description(): String = descriptionValue
    fun completionMode(): String = completionModeValue
    fun nextStageId(): String = nextStageIdValue
    fun active(): Boolean = activeValue
    fun complete(): Boolean = completeValue
    fun objectiveIds(): List<String> = objectiveIdsValue

    companion object {
        @JvmStatic
        fun fromQuestGuiStage(stage: ScenarioEngine.QuestGuiStage?): ProgressionStageSnapshot {
            if (stage == null) {
                return ProgressionStageSnapshot("", "", "", "", "", false, false, emptyList())
            }
            return ProgressionStageSnapshot(
                stage.id(),
                stage.label(),
                stage.description(),
                stage.completionMode(),
                stage.nextStageId(),
                stage.active(),
                stage.complete(),
                stage.objectiveIds()
            )
        }

        private fun valueOrEmpty(value: String?): String = value?.trim().orEmpty()
    }
}
