package ro.ainpc.engine

data class QuestGuiStage(
    val id: String,
    val label: String,
    val description: String,
    val completionMode: String,
    val nextStageId: String,
    val active: Boolean,
    val complete: Boolean,
    val objectiveIds: List<String>
) {
    constructor(
        id: String,
        label: String,
        description: String,
        completionMode: String,
        nextStageId: String,
        active: Boolean,
        complete: Boolean
    ) : this(id, label, description, completionMode, nextStageId, active, complete, emptyList())

    init {
        require(id.isNotEmpty()) { "Stage ID cannot be empty" }
    }
}
