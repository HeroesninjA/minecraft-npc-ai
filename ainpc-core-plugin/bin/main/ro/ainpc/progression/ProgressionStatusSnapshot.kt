package ro.ainpc.progression

import ro.ainpc.engine.ScenarioEngine

class ProgressionStatusSnapshot(
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
    fun systemMessages(): List<String> = systemMessagesValue

    fun toQuestInteractionResult(): ScenarioEngine.QuestInteractionResult {
        if (!handledValue) {
            return ScenarioEngine.QuestInteractionResult.notHandled()
        }
        return ScenarioEngine.QuestInteractionResult.handled(false, emptyList(), systemMessagesValue)
    }

    companion object {
        @JvmStatic
        fun fromResult(
            playerName: String?,
            selector: ProgressionSelector?,
            result: ScenarioEngine.QuestInteractionResult?
        ): ProgressionStatusSnapshot = fromResult(playerName, selector, result, null, null)

        @JvmStatic
        fun fromResult(
            playerName: String?,
            selector: ProgressionSelector?,
            result: ScenarioEngine.QuestInteractionResult?,
            entry: ScenarioEngine.QuestGuiEntry?,
            definition: ProgressionDefinition?
        ): ProgressionStatusSnapshot {
            if (result == null || !result.isHandled) {
                return ProgressionStatusSnapshot(
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
                    emptyList()
                )
            }

            return ProgressionStatusSnapshot(
                true,
                playerName,
                selector?.raw() ?: "",
                selector?.commandSelector() ?: "",
                valueOrFallback(definition?.progressionId(), entry?.selector() ?: ""),
                entry?.templateId() ?: "",
                entry?.questCode() ?: "",
                entry?.title() ?: "",
                entry?.statusDisplay() ?: "",
                entry?.mechanicDisplay() ?: "",
                entry?.tracked() == true,
                entry?.current() == true,
                entry?.active() == true,
                entry?.offered() == true,
                entry?.archived() == true,
                entry?.missingTemplate() == true,
                entry?.currentStageId() ?: "",
                entry?.currentStageLabel() ?: "",
                result.systemMessages
            )
        }

        private fun valueOrFallback(value: String?, fallback: String): String =
            if (value.isNullOrBlank()) valueOrEmpty(fallback) else value

        private fun valueOrEmpty(value: String?): String = value ?: ""
    }
}
