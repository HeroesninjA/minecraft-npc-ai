package ro.ainpc.progression

import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.engine.QuestScenarioContract
import ro.ainpc.engine.ScenarioEngine
import java.util.Locale

class ProgressionDefinition(
    progressionId: String?,
    packId: String?,
    mechanicId: String?,
    kind: String?,
    definitionId: String?,
    templateId: String?,
    code: String?,
    displayName: String?,
    description: String?,
    category: String?,
    scenarioKind: String?,
    baseType: String?,
    label: String?,
    singularLabel: String?,
    pluralLabel: String?,
    maxActive: Int,
    objectiveCount: Int,
    stageCount: Int,
    rewardCount: Int,
    private val repeatableValue: Boolean,
    private val enabledValue: Boolean
) {
    private val progressionIdValue: String = valueOrEmpty(progressionId)
    private val packIdValue: String = valueOrEmpty(packId)
    private val mechanicIdValue: String = valueOrEmpty(mechanicId)
    private val kindValue: String = valueOrEmpty(kind)
    private val definitionIdValue: String = valueOrEmpty(definitionId)
    private val templateIdValue: String = valueOrEmpty(templateId)
    private val codeValue: String = valueOrEmpty(code)
    private val displayNameValue: String = valueOrEmpty(displayName)
    private val descriptionValue: String = valueOrEmpty(description)
    private val categoryValue: String = valueOrEmpty(category)
    private val scenarioKindValue: String = valueOrEmpty(scenarioKind)
    private val baseTypeValue: String = valueOrEmpty(baseType)
    private val labelValue: String = valueOrEmpty(label)
    private val singularLabelValue: String = valueOrEmpty(singularLabel)
    private val pluralLabelValue: String = valueOrEmpty(pluralLabel)
    private val maxActiveValue: Int = maxActive.coerceAtLeast(0)
    private val objectiveCountValue: Int = objectiveCount.coerceAtLeast(0)
    private val stageCountValue: Int = stageCount.coerceAtLeast(0)
    private val rewardCountValue: Int = rewardCount.coerceAtLeast(0)

    fun progressionId(): String = progressionIdValue
    fun packId(): String = packIdValue
    fun mechanicId(): String = mechanicIdValue
    fun kind(): String = kindValue
    fun definitionId(): String = definitionIdValue
    fun templateId(): String = templateIdValue
    fun code(): String = codeValue
    fun displayName(): String = displayNameValue
    fun description(): String = descriptionValue
    fun category(): String = categoryValue
    fun scenarioKind(): String = scenarioKindValue
    fun baseType(): String = baseTypeValue
    fun label(): String = labelValue
    fun singularLabel(): String = singularLabelValue
    fun pluralLabel(): String = pluralLabelValue
    fun maxActive(): Int = maxActiveValue
    fun objectiveCount(): Int = objectiveCountValue
    fun stageCount(): Int = stageCountValue
    fun rewardCount(): Int = rewardCountValue
    fun repeatable(): Boolean = repeatableValue
    fun enabled(): Boolean = enabledValue

    companion object {
        @JvmStatic
        fun fromScenarioDefinition(scenario: FeaturePackLoader.ScenarioDefinition?): ProgressionDefinition {
            if (scenario == null) {
                return empty()
            }

            val contract = QuestScenarioContract.fromScenarioDefinition(scenario)
            val packId = valueOrEmpty(scenario.packId)
            val definitionId = valueOrFallback(scenario.id, valueOrEmpty(scenario.questCode))
            val templateId = templateId(packId, scenario.id)
            val mechanicId = valueOrFallback(scenario.progressionMechanicId, "quest")
            val kind = valueOrFallback(
                scenario.progressionKind,
                valueOrFallback(scenario.questScenarioKind, "quest")
            )
            val label = valueOrFallback(scenario.progressionLabel, mechanicId)
            val singularLabel = valueOrFallback(scenario.progressionSingularLabel, kind)
            val pluralLabel = valueOrFallback(scenario.progressionPluralLabel, label)

            return ProgressionDefinition(
                progressionId(packId, mechanicId, definitionId, templateId),
                packId,
                mechanicId,
                kind,
                definitionId,
                templateId,
                scenario.questCode,
                valueOrFallback(scenario.name, definitionId),
                scenario.description,
                contract.category().name.lowercase(Locale.ROOT),
                contract.kind().name.lowercase(Locale.ROOT),
                scenario.baseType?.name ?: "",
                label,
                singularLabel,
                pluralLabel,
                scenario.progressionMaxActive,
                scenario.objectives.size,
                scenario.questStages.size,
                scenario.rewards.size,
                scenario.isQuestRepeatable,
                isProgressionCandidate(scenario)
            )
        }

        @JvmStatic
        fun isProgressionCandidate(scenario: FeaturePackLoader.ScenarioDefinition?): Boolean {
            if (scenario == null) {
                return false
            }
            return scenario.baseType == ScenarioEngine.ScenarioType.QUEST ||
                (scenario.isProgressionEnabled &&
                    (scenario.questCode.isNotBlank() ||
                        scenario.objectives.isNotEmpty() ||
                        scenario.rewards.isNotEmpty()))
        }

        private fun empty(): ProgressionDefinition = ProgressionDefinition(
            "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
            0, 0, 0, 0, false, false
        )

        private fun progressionId(
            packId: String?,
            mechanicId: String?,
            definitionId: String?,
            fallbackTemplateId: String?
        ): String {
            val safePackId = valueOrEmpty(packId)
            val safeMechanicId = valueOrEmpty(mechanicId)
            val safeDefinitionId = valueOrEmpty(definitionId)
            if (safePackId.isBlank() || safeMechanicId.isBlank() || safeDefinitionId.isBlank()) {
                return valueOrFallback(fallbackTemplateId, safeDefinitionId)
            }
            return "$safePackId:$safeMechanicId:$safeDefinitionId"
        }

        private fun templateId(packId: String?, definitionId: String?): String {
            val safePackId = valueOrEmpty(packId)
            val safeDefinitionId = valueOrEmpty(definitionId)
            if (safePackId.isBlank()) {
                return safeDefinitionId
            }
            if (safeDefinitionId.isBlank()) {
                return safePackId
            }
            return "$safePackId:$safeDefinitionId"
        }

        private fun valueOrFallback(value: String?, fallback: String?): String {
            val safeValue = valueOrEmpty(value)
            return if (safeValue.isBlank()) valueOrEmpty(fallback) else safeValue
        }

        private fun valueOrEmpty(value: String?): String = value?.trim().orEmpty()
    }
}
