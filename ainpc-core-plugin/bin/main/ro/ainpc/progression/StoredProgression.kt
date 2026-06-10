package ro.ainpc.progression

class StoredProgression(
    playerUuid: String?,
    progressionId: String?,
    packId: String?,
    mechanicId: String?,
    kind: String?,
    category: String?,
    scenarioKind: String?,
    baseType: String?,
    definitionId: String?,
    templateId: String?,
    code: String?,
    status: String?,
    startedAt: Long,
    completedAt: Long,
    currentPhase: String?,
    currentStageId: String?,
    objectiveProgressJson: String?,
    variablesJson: String?,
    updatedAt: Long,
    private val trackedValue: Boolean,
    private val definitionResolvedValue: Boolean,
    mechanicLabel: String?,
    singularLabel: String?,
    pluralLabel: String?,
    compatibilitySource: String?
) {
    private val playerUuidValue: String = valueOrEmpty(playerUuid)
    private val progressionIdValue: String = valueOrEmpty(progressionId)
    private val packIdValue: String = valueOrEmpty(packId)
    private val mechanicIdValue: String = valueOrEmpty(mechanicId)
    private val kindValue: String = valueOrEmpty(kind)
    private val categoryValue: String = valueOrEmpty(category)
    private val scenarioKindValue: String = valueOrEmpty(scenarioKind)
    private val baseTypeValue: String = valueOrEmpty(baseType)
    private val definitionIdValue: String = valueOrEmpty(definitionId)
    private val templateIdValue: String = valueOrEmpty(templateId)
    private val codeValue: String = valueOrEmpty(code)
    private val statusValue: String = valueOrEmpty(status)
    private val startedAtValue: Long = startedAt.coerceAtLeast(0L)
    private val completedAtValue: Long = completedAt.coerceAtLeast(0L)
    private val currentPhaseValue: String = valueOrEmpty(currentPhase)
    private val currentStageIdValue: String = valueOrEmpty(currentStageId)
    private val objectiveProgressJsonValue: String = jsonOrEmptyObject(objectiveProgressJson)
    private val variablesJsonValue: String = jsonOrEmptyObject(variablesJson)
    private val updatedAtValue: Long = updatedAt.coerceAtLeast(0L)
    private val mechanicLabelValue: String = valueOrEmpty(mechanicLabel)
    private val singularLabelValue: String = valueOrEmpty(singularLabel)
    private val pluralLabelValue: String = valueOrEmpty(pluralLabel)
    private val compatibilitySourceValue: String = valueOrFallback(compatibilitySource, "player_quests")

    fun playerUuid(): String = playerUuidValue
    fun progressionId(): String = progressionIdValue
    fun packId(): String = packIdValue
    fun mechanicId(): String = mechanicIdValue
    fun kind(): String = kindValue
    fun category(): String = categoryValue
    fun scenarioKind(): String = scenarioKindValue
    fun baseType(): String = baseTypeValue
    fun definitionId(): String = definitionIdValue
    fun templateId(): String = templateIdValue
    fun code(): String = codeValue
    fun status(): String = statusValue
    fun startedAt(): Long = startedAtValue
    fun completedAt(): Long = completedAtValue
    fun currentPhase(): String = currentPhaseValue
    fun currentStageId(): String = currentStageIdValue
    fun objectiveProgressJson(): String = objectiveProgressJsonValue
    fun variablesJson(): String = variablesJsonValue
    fun updatedAt(): Long = updatedAtValue
    fun tracked(): Boolean = trackedValue
    fun definitionResolved(): Boolean = definitionResolvedValue
    fun mechanicLabel(): String = mechanicLabelValue
    fun singularLabel(): String = singularLabelValue
    fun pluralLabel(): String = pluralLabelValue
    fun compatibilitySource(): String = compatibilitySourceValue

    fun current(): Boolean =
        "active".equals(statusValue, ignoreCase = true) || "offered".equals(statusValue, ignoreCase = true)

    fun archived(): Boolean =
        "completed".equals(statusValue, ignoreCase = true) || "failed".equals(statusValue, ignoreCase = true)

    companion object {
        private fun jsonOrEmptyObject(value: String?): String {
            val safeValue = valueOrEmpty(value)
            return if (safeValue.isBlank()) "{}" else safeValue
        }

        private fun valueOrFallback(value: String?, fallback: String): String {
            val safeValue = valueOrEmpty(value)
            return if (safeValue.isBlank()) valueOrEmpty(fallback) else safeValue
        }

        private fun valueOrEmpty(value: String?): String = value?.trim().orEmpty()
    }
}
