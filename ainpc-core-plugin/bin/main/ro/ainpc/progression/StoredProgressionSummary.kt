package ro.ainpc.progression

import java.util.LinkedHashMap
import java.util.LinkedHashSet

class StoredProgressionSummary(
    rowCount: Int,
    playerCount: Int,
    currentCount: Int,
    archivedCount: Int,
    trackedCount: Int,
    unresolvedDefinitionCount: Int,
    byStatus: Map<String, Int>?,
    byTemplate: Map<String, Int>?,
    byPack: Map<String, Int>?,
    byMechanic: Map<String, Int>?,
    byKind: Map<String, Int>?,
    byCategory: Map<String, Int>?,
    byScenarioKind: Map<String, Int>?,
    byBaseType: Map<String, Int>?
) {
    private val rowCountValue: Int = rowCount.coerceAtLeast(0)
    private val playerCountValue: Int = playerCount.coerceAtLeast(0)
    private val currentCountValue: Int = currentCount.coerceAtLeast(0)
    private val archivedCountValue: Int = archivedCount.coerceAtLeast(0)
    private val trackedCountValue: Int = trackedCount.coerceAtLeast(0)
    private val unresolvedDefinitionCountValue: Int = unresolvedDefinitionCount.coerceAtLeast(0)
    private val byStatusValue: Map<String, Int> = (byStatus ?: emptyMap()).toMap()
    private val byTemplateValue: Map<String, Int> = (byTemplate ?: emptyMap()).toMap()
    private val byPackValue: Map<String, Int> = (byPack ?: emptyMap()).toMap()
    private val byMechanicValue: Map<String, Int> = (byMechanic ?: emptyMap()).toMap()
    private val byKindValue: Map<String, Int> = (byKind ?: emptyMap()).toMap()
    private val byCategoryValue: Map<String, Int> = (byCategory ?: emptyMap()).toMap()
    private val byScenarioKindValue: Map<String, Int> = (byScenarioKind ?: emptyMap()).toMap()
    private val byBaseTypeValue: Map<String, Int> = (byBaseType ?: emptyMap()).toMap()

    fun rowCount(): Int = rowCountValue
    fun playerCount(): Int = playerCountValue
    fun currentCount(): Int = currentCountValue
    fun archivedCount(): Int = archivedCountValue
    fun trackedCount(): Int = trackedCountValue
    fun unresolvedDefinitionCount(): Int = unresolvedDefinitionCountValue
    fun byStatus(): Map<String, Int> = byStatusValue
    fun byTemplate(): Map<String, Int> = byTemplateValue
    fun byPack(): Map<String, Int> = byPackValue
    fun byMechanic(): Map<String, Int> = byMechanicValue
    fun byKind(): Map<String, Int> = byKindValue
    fun byCategory(): Map<String, Int> = byCategoryValue
    fun byScenarioKind(): Map<String, Int> = byScenarioKindValue
    fun byBaseType(): Map<String, Int> = byBaseTypeValue

    companion object {
        @JvmStatic
        fun from(progressions: Collection<StoredProgression?>?): StoredProgressionSummary {
            if (progressions.isNullOrEmpty()) {
                return StoredProgressionSummary(
                    0, 0, 0, 0, 0, 0,
                    emptyMap(), emptyMap(), emptyMap(), emptyMap(),
                    emptyMap(), emptyMap(), emptyMap(), emptyMap()
                )
            }

            val players = LinkedHashSet<String>()
            val byStatus = LinkedHashMap<String, Int>()
            val byTemplate = LinkedHashMap<String, Int>()
            val byPack = LinkedHashMap<String, Int>()
            val byMechanic = LinkedHashMap<String, Int>()
            val byKind = LinkedHashMap<String, Int>()
            val byCategory = LinkedHashMap<String, Int>()
            val byScenarioKind = LinkedHashMap<String, Int>()
            val byBaseType = LinkedHashMap<String, Int>()
            var currentCount = 0
            var archivedCount = 0
            var trackedCount = 0
            var unresolvedDefinitionCount = 0

            for (progression in progressions) {
                if (progression == null) {
                    continue
                }
                if (progression.playerUuid().isNotBlank()) {
                    players.add(progression.playerUuid())
                }
                increment(byStatus, progression.status())
                increment(byTemplate, progression.templateId())
                increment(byPack, progression.packId())
                increment(byMechanic, progression.mechanicId())
                increment(byKind, progression.kind())
                increment(byCategory, progression.category())
                increment(byScenarioKind, progression.scenarioKind())
                increment(byBaseType, progression.baseType())
                if (progression.current()) {
                    currentCount++
                }
                if (progression.archived()) {
                    archivedCount++
                }
                if (progression.tracked()) {
                    trackedCount++
                }
                if (!progression.definitionResolved()) {
                    unresolvedDefinitionCount++
                }
            }

            return StoredProgressionSummary(
                progressions.size,
                players.size,
                currentCount,
                archivedCount,
                trackedCount,
                unresolvedDefinitionCount,
                byStatus,
                byTemplate,
                byPack,
                byMechanic,
                byKind,
                byCategory,
                byScenarioKind,
                byBaseType
            )
        }

        private fun increment(counts: MutableMap<String, Int>, rawKey: String?) {
            val key = if (rawKey.isNullOrBlank()) "unknown" else rawKey
            counts[key] = (counts[key] ?: 0) + 1
        }
    }
}
