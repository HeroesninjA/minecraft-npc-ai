package ro.ainpc.engine

data class QuestGuiEntry @JvmOverloads constructor(
    val selector: String = "",
    val templateId: String = "",
    val questCode: String = "",
    val title: String = "",
    val statusDisplay: String = "",
    val categoryDisplay: String = "",
    val mechanicDisplay: String = "",
    val tracked: Boolean = false,
    val current: Boolean = false,
    val active: Boolean = false,
    val offered: Boolean = false,
    val archived: Boolean = false,
    val missingTemplate: Boolean = false,
    val currentStageId: String = "",
    val currentStageLabel: String = "",
    val updatedAt: Long = 0L,
    val questGiverName: String = "",
    val statusLines: List<String> = emptyList(),
    val objectives: List<QuestGuiObjective> = emptyList(),
    val stages: List<QuestGuiStage> = emptyList(),
    val rewardLines: List<String> = emptyList(),
    val actionLines: List<String> = emptyList()
)
