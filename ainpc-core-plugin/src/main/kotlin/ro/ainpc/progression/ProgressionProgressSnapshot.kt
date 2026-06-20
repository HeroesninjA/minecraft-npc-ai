package ro.ainpc.progression

import ro.ainpc.engine.*

class ProgressionProgressSnapshot(
    private val handledValue: Boolean,
    playerName: String?,
    selector: String?,
    normalizedSelector: String?,
    progressionId: String?,
    templateId: String?,
    code: String?,
    title: String?,
    statusDisplay: String?,
    mechanicDisplay: String?,
    private val trackedValue: Boolean,
    private val currentValue: Boolean,
    private val activeValue: Boolean,
    private val offeredValue: Boolean,
    private val archivedValue: Boolean,
    private val missingTemplateValue: Boolean,
    currentStageId: String?,
    currentStageLabel: String?,
    objectives: List<ProgressionObjectiveSnapshot>?,
    systemMessages: List<String>?
) {
    private val playerNameValue: String = valueOrEmpty(playerName)
    private val selectorValue: String = valueOrEmpty(selector)
    private val normalizedSelectorValue: String = valueOrEmpty(normalizedSelector)
    private val progressionIdValue: String = valueOrEmpty(progressionId)
    private val templateIdValue: String = valueOrEmpty(templateId)
    private val codeValue: String = valueOrEmpty(code)
    private val titleValue: String = valueOrEmpty(title)
    private val statusDisplayValue: String = valueOrEmpty(statusDisplay)
    private val mechanicDisplayValue: String = valueOrEmpty(mechanicDisplay)
    private val currentStageIdValue: String = valueOrEmpty(currentStageId)
    private val currentStageLabelValue: String = valueOrEmpty(currentStageLabel)
    private val objectivesValue: List<ProgressionObjectiveSnapshot> = (objectives ?: emptyList()).toList()
    private val systemMessagesValue: List<String> = (systemMessages ?: emptyList()).toList()

    fun handled(): Boolean = handledValue
    fun playerName(): String = playerNameValue
    fun selector(): String = selectorValue
    fun normalizedSelector(): String = normalizedSelectorValue
    fun progressionId(): String = progressionIdValue
    fun templateId(): String = templateIdValue
    fun code(): String = codeValue
    fun title(): String = titleValue
    fun statusDisplay(): String = statusDisplayValue
    fun mechanicDisplay(): String = mechanicDisplayValue
    fun tracked(): Boolean = trackedValue
    fun current(): Boolean = currentValue
    fun active(): Boolean = activeValue
    fun offered(): Boolean = offeredValue
    fun archived(): Boolean = archivedValue
    fun missingTemplate(): Boolean = missingTemplateValue
    fun currentStageId(): String = currentStageIdValue
    fun currentStageLabel(): String = currentStageLabelValue
    fun objectives(): List<ProgressionObjectiveSnapshot> = objectivesValue
    fun systemMessages(): List<String> = systemMessagesValue

    fun toQuestInteractionResult(): QuestInteractionResult {
        if (!handledValue) {
            return QuestInteractionResult.notHandled()
        }
        return QuestInteractionResult.handled(false, emptyList(), systemMessagesValue)
    }

    fun completedObjectiveCount(): Int = objectivesValue.count { it.complete() }

    fun toChatLines(): List<String> {
        if (!handledValue) {
            return listOf("&cProgresul nu a putut fi citit.")
        }
        val lines = mutableListOf<String>()
        lines.addAll(systemMessagesValue)
        if (objectivesValue.isNotEmpty()) {
            val completed = completedObjectiveCount()
            lines.add("&7Obiective: &f$completed/${objectivesValue.size} completate")
            for ((index, objective) in objectivesValue.withIndex()) {
                val prefix = if (objective.complete()) "&a✔" else "&7○"
                lines.add("$prefix &f${objective.label()}")
            }
        }
        return lines
    }

    companion object {
        @JvmStatic
        fun fromResult(
            playerName: String?,
            selector: ProgressionSelector?,
            result: QuestInteractionResult?
        ): ProgressionProgressSnapshot = fromResult(playerName, selector, result, null, null)

        @JvmStatic
        fun fromResult(
            playerName: String?,
            selector: ProgressionSelector?,
            result: QuestInteractionResult?,
            entry: QuestGuiEntry?,
            definition: ProgressionDefinition?
        ): ProgressionProgressSnapshot {
            if (result == null || !result.isHandled) {
                return ProgressionProgressSnapshot(
                    false,
                    playerName,
                    selector?.raw() ?: "",
                    selector?.commandSelector() ?: "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    "",
                    "",
                    emptyList(),
                    emptyList()
                )
            }

            val objectiveSnapshots = if (entry == null) {
                emptyList<ProgressionObjectiveSnapshot>()
            } else {
                entry.objectives.map { ProgressionObjectiveSnapshot.fromQuestGuiObjective(it) }
            }

            return ProgressionProgressSnapshot(
                true,
                playerName,
                selector?.raw() ?: "",
                selector?.commandSelector() ?: "",
                valueOrFallback(definition?.progressionId(), entry?.selector ?: ""),
                entry?.templateId ?: "",
                entry?.questCode ?: "",
                entry?.title ?: "",
                entry?.statusDisplay ?: "",
                entry?.mechanicDisplay ?: "",
                entry?.tracked == true,
                entry?.current == true,
                entry?.active == true,
                entry?.offered == true,
                entry?.archived == true,
                entry?.missingTemplate == true,
                entry?.currentStageId ?: "",
                entry?.currentStageLabel ?: "",
                objectiveSnapshots,
                result.systemMessages
            )
        }

        private fun valueOrFallback(value: String?, fallback: String): String =
            if (value.isNullOrBlank()) valueOrEmpty(fallback) else value

        private fun valueOrEmpty(value: String?): String = value ?: ""
    }
}
