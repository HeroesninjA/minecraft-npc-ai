package ro.ainpc.debug

import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.progression.ProgressionDefinition

internal data class DebugDumpStoryActionSnapshot(
    val type: String,
    val entryId: String,
    val itemId: String,
    val description: String,
    val metadata: Map<String, String>,
    val payload: Map<String, String>,
) {
    fun metadataValue(vararg keys: String): String = keys.firstNotNullOfOrNull { key ->
        metadata[key]?.takeIf { it.isNotBlank() }
    }.orEmpty()
}

internal data class DebugDumpStoryFeatureScenarioSnapshot(
    val packId: String,
    val id: String,
    val name: String,
    val baseType: String,
    val progressionMechanicId: String,
    val questScenarioKind: String,
    val actions: List<DebugDumpStoryActionSnapshot>,
) {
    fun findRecordStoryEventAction(eventKey: String?): DebugDumpStoryActionSnapshot? {
        val normalizedEventKey = DebugDumpSupport.normalizeKey(eventKey)
        return actions.firstOrNull { action ->
            normalizedEventKey.isNotBlank() &&
                DebugDumpSupport.normalizeKey(action.metadataValue("event_key", "key")) == normalizedEventKey
        } ?: actions.firstOrNull()
    }
}

internal class DebugDumpStoryFeatureCapture private constructor(
    private val bySelector: Map<String, DebugDumpStoryFeatureScenarioSnapshot>,
) {
    fun isEmpty(): Boolean = bySelector.isEmpty()

    fun find(templateId: String?, questCode: String?): DebugDumpStoryFeatureScenarioSnapshot? =
        bySelector[DebugDumpSupport.normalizeKey(templateId)]
            ?: bySelector[DebugDumpSupport.normalizeKey(questCode)]
            ?: bySelector[DebugDumpSupport.normalizeKey(DebugDumpSupport.lastSelectorSegment(templateId))]

    companion object {
        fun from(scenarios: List<FeaturePackLoader.ScenarioDefinition>): DebugDumpStoryFeatureCapture {
            val lookup = LinkedHashMap<String, DebugDumpStoryFeatureScenarioSnapshot>()
            for (scenario in scenarios) {
                if (!ProgressionDefinition.isProgressionCandidate(scenario)) continue
                val definition = ProgressionDefinition.fromScenarioDefinition(scenario)
                val snapshot = DebugDumpStoryFeatureScenarioSnapshot(
                    scenario.packId,
                    scenario.id,
                    scenario.name,
                    scenario.baseType.name,
                    scenario.progressionMechanicId,
                    scenario.questScenarioKind,
                    scenario.rewards.asSequence()
                        .filter { DebugDumpSupport.normalizeQuestRewardType(it.type) == "record_story_event" }
                        .map { reward ->
                            DebugDumpStoryActionSnapshot(
                                reward.type,
                                reward.entryId,
                                reward.itemId,
                                reward.description,
                                reward.metadata.toMap(),
                                reward.payload.toMap(),
                            )
                        }.toList(),
                )
                val keys = listOf(
                    definition.templateId(),
                    definition.progressionId(),
                    definition.definitionId(),
                    definition.code(),
                    definition.packId() + ":" + definition.definitionId(),
                    definition.packId() + ":" + definition.mechanicId() + ":" + definition.definitionId(),
                )
                for (key in keys) {
                    val normalized = DebugDumpSupport.normalizeKey(key)
                    if (normalized.isNotBlank()) lookup.putIfAbsent(normalized, snapshot)
                }
            }
            return DebugDumpStoryFeatureCapture(lookup)
        }
    }
}
