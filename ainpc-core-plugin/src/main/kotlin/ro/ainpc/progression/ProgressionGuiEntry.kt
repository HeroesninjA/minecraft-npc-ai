package ro.ainpc.progression

import ro.ainpc.engine.ScenarioEngine
import java.util.Locale

class ProgressionGuiEntry(
    selector: String?,
    progressionId: String?,
    packId: String?,
    mechanicId: String?,
    kind: String?,
    definitionId: String?,
    templateId: String?,
    code: String?,
    title: String?,
    statusDisplay: String?,
    categoryDisplay: String?,
    mechanicDisplay: String?,
    label: String?,
    singularLabel: String?,
    pluralLabel: String?,
    private val trackedValue: Boolean,
    private val currentValue: Boolean,
    private val activeValue: Boolean,
    private val offeredValue: Boolean,
    private val archivedValue: Boolean,
    private val missingTemplateValue: Boolean,
    currentStageId: String?,
    currentStageLabel: String?,
    updatedAt: Long,
    actorName: String?,
    statusLines: List<String>?,
    objectives: List<ProgressionObjectiveSnapshot>?,
    stages: List<ProgressionStageSnapshot>?,
    rewardLines: List<String>?,
    actionLines: List<String>?
) {
    private val selectorValue: String = valueOrEmpty(selector)
    private val progressionIdValue: String = valueOrEmpty(progressionId)
    private val packIdValue: String = valueOrEmpty(packId)
    private val mechanicIdValue: String = valueOrEmpty(mechanicId)
    private val kindValue: String = valueOrEmpty(kind)
    private val definitionIdValue: String = valueOrEmpty(definitionId)
    private val templateIdValue: String = valueOrEmpty(templateId)
    private val codeValue: String = valueOrEmpty(code)
    private val titleValue: String = valueOrEmpty(title)
    private val statusDisplayValue: String = valueOrEmpty(statusDisplay)
    private val categoryDisplayValue: String = valueOrEmpty(categoryDisplay)
    private val mechanicDisplayValue: String = valueOrEmpty(mechanicDisplay)
    private val labelValue: String = valueOrEmpty(label)
    private val singularLabelValue: String = valueOrEmpty(singularLabel)
    private val pluralLabelValue: String = valueOrEmpty(pluralLabel)
    private val currentStageIdValue: String = valueOrEmpty(currentStageId)
    private val currentStageLabelValue: String = valueOrEmpty(currentStageLabel)
    private val updatedAtValue: Long = updatedAt.coerceAtLeast(0L)
    private val actorNameValue: String = valueOrEmpty(actorName)
    private val statusLinesValue: List<String> = (statusLines ?: emptyList()).toList()
    private val objectivesValue: List<ProgressionObjectiveSnapshot> = (objectives ?: emptyList()).toList()
    private val stagesValue: List<ProgressionStageSnapshot> = (stages ?: emptyList()).toList()
    private val rewardLinesValue: List<String> = (rewardLines ?: emptyList()).toList()
    private val actionLinesValue: List<String> = (actionLines ?: emptyList()).toList()

    fun selector(): String = selectorValue
    fun progressionId(): String = progressionIdValue
    fun packId(): String = packIdValue
    fun mechanicId(): String = mechanicIdValue
    fun kind(): String = kindValue
    fun definitionId(): String = definitionIdValue
    fun templateId(): String = templateIdValue
    fun code(): String = codeValue
    fun title(): String = titleValue
    fun statusDisplay(): String = statusDisplayValue
    fun categoryDisplay(): String = categoryDisplayValue
    fun mechanicDisplay(): String = mechanicDisplayValue
    fun label(): String = labelValue
    fun singularLabel(): String = singularLabelValue
    fun pluralLabel(): String = pluralLabelValue
    fun tracked(): Boolean = trackedValue
    fun current(): Boolean = currentValue
    fun active(): Boolean = activeValue
    fun offered(): Boolean = offeredValue
    fun archived(): Boolean = archivedValue
    fun missingTemplate(): Boolean = missingTemplateValue
    fun currentStageId(): String = currentStageIdValue
    fun currentStageLabel(): String = currentStageLabelValue
    fun updatedAt(): Long = updatedAtValue
    fun actorName(): String = actorNameValue
    fun statusLines(): List<String> = statusLinesValue
    fun objectives(): List<ProgressionObjectiveSnapshot> = objectivesValue
    fun stages(): List<ProgressionStageSnapshot> = stagesValue
    fun rewardLines(): List<String> = rewardLinesValue
    fun actionLines(): List<String> = actionLinesValue

    fun commandRoot(): String {
        return when (kindValue.lowercase(Locale.ROOT)) {
            "quest" -> "quest"
            "contract" -> "contract"
            "duty" -> "duty"
            "bounty" -> "bounty"
            "event" -> "event"
            "tutorial" -> "tutorial"
            "ritual" -> "ritual"
            else -> if (kindValue.isBlank()) "quest" else "progression"
        }
    }

    fun commandSelector(): String {
        val preferredSelector = firstNonBlank(
            selectorValue,
            if (mechanicIdValue.isNotBlank() && codeValue.isNotBlank()) "$mechanicIdValue:$codeValue" else "",
            if (mechanicIdValue.isNotBlank() && definitionIdValue.isNotBlank()) "$mechanicIdValue:$definitionIdValue" else "",
            progressionIdValue,
            codeValue,
            templateIdValue,
            definitionIdValue
        )
        return if (preferredSelector.isBlank()) "tracked" else preferredSelector
    }

    fun guiDetailSelector(): String = commandSelector()

    fun guiFilter(): String {
        return when (kindValue.lowercase(Locale.ROOT)) {
            "quest", "contract", "duty", "bounty", "event", "tutorial", "ritual" -> kindValue.lowercase(Locale.ROOT)
            else -> "all"
        }
    }

    fun command(mode: String?): String {
        val safeMode = valueOrEmpty(mode)
        return if (safeMode.isBlank()) {
            "ainpc ${commandRoot()}"
        } else {
            "ainpc ${commandRoot()} $safeMode ${commandSelector()}"
        }
    }

    fun trackStartCommand(): String = "ainpc ${commandRoot()} track start ${commandSelector()}"

    fun trackStopCommand(): String = "ainpc ${commandRoot()} track stop"

    companion object {
        @JvmStatic
        fun fromQuestGuiEntry(
            entry: ScenarioEngine.QuestGuiEntry?,
            definition: ProgressionDefinition?
        ): ProgressionGuiEntry {
            if (entry == null) {
                return empty()
            }
            return ProgressionGuiEntry(
                entry.selector(),
                valueOrFallback(definition?.progressionId(), entry.selector()),
                definition?.packId() ?: "",
                definition?.mechanicId() ?: "",
                definition?.kind() ?: "",
                definition?.definitionId() ?: "",
                entry.templateId(),
                entry.questCode(),
                entry.title(),
                entry.statusDisplay(),
                entry.categoryDisplay(),
                entry.mechanicDisplay(),
                definition?.label() ?: entry.mechanicDisplay(),
                definition?.singularLabel() ?: "",
                definition?.pluralLabel() ?: entry.mechanicDisplay(),
                entry.tracked(),
                entry.current(),
                entry.active(),
                entry.offered(),
                entry.archived(),
                entry.missingTemplate(),
                entry.currentStageId(),
                entry.currentStageLabel(),
                entry.updatedAt(),
                entry.questGiverName(),
                entry.statusLines(),
                entry.objectives().map { ProgressionObjectiveSnapshot.fromQuestGuiObjective(it) },
                entry.stages().map { ProgressionStageSnapshot.fromQuestGuiStage(it) },
                entry.rewardLines(),
                entry.actionLines()
            )
        }

        private fun empty(): ProgressionGuiEntry = ProgressionGuiEntry(
            "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
            false, false, false, false, false, false, "", "", 0L, "",
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
        )

        private fun valueOrFallback(value: String?, fallback: String?): String {
            val safeValue = valueOrEmpty(value)
            return if (safeValue.isBlank()) valueOrEmpty(fallback) else safeValue
        }

        private fun valueOrEmpty(value: String?): String = value?.trim().orEmpty()

        private fun firstNonBlank(vararg values: String?): String {
            for (value in values) {
                val safeValue = valueOrEmpty(value)
                if (safeValue.isNotBlank()) {
                    return safeValue
                }
            }
            return ""
        }
    }
}
