package ro.ainpc.engine

import ro.ainpc.progression.ProgressionDefinition

class QuestDirectorDecision(
    status: Status?,
    reason: String?,
    selectedProgressionId: String?,
    selectedTemplateId: String?,
    selectedMechanicId: String?,
    selectedDefinitionId: String?,
    matchedSignals: List<String>?,
    candidateTemplateIds: List<String>?,
    blockedReasons: List<String>?,
    warnings: List<String>?,
    runtimeExecutable: Boolean
) {
    private val statusValue: Status = status ?: Status.NO_ACTION
    private val reasonValue: String = valueOrEmpty(reason)
    private val selectedProgressionIdValue: String = valueOrEmpty(selectedProgressionId)
    private val selectedTemplateIdValue: String = valueOrEmpty(selectedTemplateId)
    private val selectedMechanicIdValue: String = valueOrEmpty(selectedMechanicId)
    private val selectedDefinitionIdValue: String = valueOrEmpty(selectedDefinitionId)
    private val matchedSignalsValue: List<String> = sanitizeStrings(matchedSignals)
    private val candidateTemplateIdsValue: List<String> = sanitizeStrings(candidateTemplateIds)
    private val blockedReasonsValue: List<String> = sanitizeStrings(blockedReasons)
    private val warningsValue: List<String> = sanitizeStrings(warnings)
    private val runtimeExecutableValue: Boolean = false

    init {
        runtimeExecutable
    }

    fun status(): Status = statusValue
    fun reason(): String = reasonValue
    fun selectedProgressionId(): String = selectedProgressionIdValue
    fun selectedTemplateId(): String = selectedTemplateIdValue
    fun selectedMechanicId(): String = selectedMechanicIdValue
    fun selectedDefinitionId(): String = selectedDefinitionIdValue
    fun matchedSignals(): List<String> = matchedSignalsValue
    fun candidateTemplateIds(): List<String> = candidateTemplateIdsValue
    fun blockedReasons(): List<String> = blockedReasonsValue
    fun warnings(): List<String> = warningsValue
    fun runtimeExecutable(): Boolean = runtimeExecutableValue

    enum class Status(private val idValue: String) {
        NO_ACTION("no_action"),
        CANDIDATE_FOUND("candidate_found"),
        SEED_SUGGESTED("seed_suggested"),
        BLOCKED("blocked");

        fun id(): String = idValue
    }

    companion object {
        @JvmStatic
        fun noAction(reason: String?, warnings: List<String>?): QuestDirectorDecision =
            QuestDirectorDecision(
                Status.NO_ACTION,
                reason,
                "",
                "",
                "",
                "",
                emptyList(),
                emptyList(),
                emptyList(),
                warnings,
                false
            )

        @JvmStatic
        fun blocked(reason: String?, blockedReasons: List<String>?, warnings: List<String>?): QuestDirectorDecision =
            QuestDirectorDecision(
                Status.BLOCKED,
                reason,
                "",
                "",
                "",
                "",
                emptyList(),
                emptyList(),
                blockedReasons,
                warnings,
                false
            )

        @JvmStatic
        fun seedSuggested(reason: String?, matchedSignals: List<String>?, warnings: List<String>?): QuestDirectorDecision =
            QuestDirectorDecision(
                Status.SEED_SUGGESTED,
                reason,
                "",
                "",
                "",
                "",
                matchedSignals,
                emptyList(),
                emptyList(),
                warnings,
                false
            )

        @JvmStatic
        fun candidateFound(
            definition: ProgressionDefinition?,
            matchedSignals: List<String>?,
            candidateTemplateIds: List<String>?,
            warnings: List<String>?
        ): QuestDirectorDecision {
            val safeDefinition = definition ?: ProgressionDefinition(
                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                0, 0, 0, 0, false, false
            )
            return QuestDirectorDecision(
                Status.CANDIDATE_FOUND,
                "matching_progression_definition",
                safeDefinition.progressionId(),
                safeDefinition.templateId(),
                safeDefinition.mechanicId(),
                safeDefinition.definitionId(),
                matchedSignals,
                candidateTemplateIds,
                emptyList(),
                warnings,
                false
            )
        }

        private fun sanitizeStrings(values: List<String>?): List<String> {
            if (values.isNullOrEmpty()) {
                return emptyList()
            }
            val sanitized = ArrayList<String>()
            for (value in values) {
                val safeValue = valueOrEmpty(value)
                if (safeValue.isNotBlank()) {
                    sanitized.add(safeValue)
                }
            }
            return sanitized.toList()
        }

        private fun valueOrEmpty(value: String?): String = value?.trim().orEmpty()
    }
}
