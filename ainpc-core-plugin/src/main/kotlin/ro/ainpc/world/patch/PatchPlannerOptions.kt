package ro.ainpc.world.patch

import java.util.Locale

class PatchPlannerOptions(
    targetPopulation: Int,
    requiredProfessions: List<String>?,
    minHouseCount: Int,
    maxPatchCount: Int,
    private val requireSocialHub: Boolean,
    private val requireQuestTriggerNode: Boolean,
    allowedCapabilities: Set<String>?
) {
    private val targetPopulation: Int = maxOf(0, targetPopulation)
    private val requiredProfessions: List<String> = (requiredProfessions ?: emptyList()).toList()
    private val minHouseCount: Int = maxOf(0, minHouseCount)
    private val maxPatchCount: Int = if (maxPatchCount <= 0) 12 else maxPatchCount
    private val allowedCapabilities: Set<String> = (allowedCapabilities ?: defaultCapabilities()).toSet()

    fun targetPopulation(): Int = targetPopulation
    fun requiredProfessions(): List<String> = requiredProfessions
    fun minHouseCount(): Int = minHouseCount
    fun maxPatchCount(): Int = maxPatchCount
    fun requireSocialHub(): Boolean = requireSocialHub
    fun requireQuestTriggerNode(): Boolean = requireQuestTriggerNode
    fun allowedCapabilities(): Set<String> = allowedCapabilities

    fun hasCapability(capability: String?): Boolean {
        val normalized = normalize(capability)
        return normalized.isNotBlank() &&
            allowedCapabilities.asSequence().map(::normalize).any { it == normalized }
    }

    fun normalizedRequiredProfessions(): List<String> {
        return requiredProfessions.asSequence()
            .map(::normalize)
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    companion object {
        @JvmStatic
        fun forTargetPopulation(targetPopulation: Int): PatchPlannerOptions {
            return PatchPlannerOptions(
                targetPopulation,
                emptyList(),
                0,
                12,
                true,
                true,
                defaultCapabilities()
            )
        }

        @JvmStatic
        fun forTargetPopulation(targetPopulation: Int, requiredProfessions: List<String>?): PatchPlannerOptions {
            return PatchPlannerOptions(
                targetPopulation,
                requiredProfessions,
                0,
                12,
                true,
                true,
                defaultCapabilities()
            )
        }

        @JvmStatic
        fun defaultCapabilities(): Set<String> = setOf("semantic-place-mapping")

        private fun normalize(value: String?): String {
            return value?.trim()?.lowercase(Locale.ROOT)?.replace(' ', '_')?.replace('-', '_') ?: ""
        }
    }
}
