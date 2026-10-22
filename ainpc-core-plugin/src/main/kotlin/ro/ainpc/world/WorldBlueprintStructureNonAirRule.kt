package ro.ainpc.world

import java.math.BigInteger

internal data class WorldBlueprintStructureNonAirRule(
    val configured: Boolean,
    val maximum: BigInteger?,
) {
    val isValid: Boolean get() = !configured || maximum != null

    fun exceeds(bounds: WorldBlueprintRegionBounds, occupancy: WorldBlueprintBlockOccupancyPreview?): Boolean? {
        val limit = maximum ?: return null
        val observed = occupancy ?: return null
        val observedNonAir = BigInteger.valueOf(observed.nonAirCount.toLong())
        if (observedNonAir > limit) return true
        val observedAir = BigInteger.valueOf((observed.observedCount - observed.nonAirCount).toLong())
        return if (bounds.blockPositions - observedAir <= limit) false else null
    }

    companion object {
        const val KEY = "max_structure_non_air_positions"
        private val decimal = Regex("[0-9]+")
        private val maximumPossible = BigInteger.ONE.shiftLeft(96)

        fun parse(mapRules: Map<String, String>): WorldBlueprintStructureNonAirRule {
            val raw = mapRules[KEY] ?: return WorldBlueprintStructureNonAirRule(false, null)
            val text = raw.trim()
            val digits = if (decimal.matches(text)) text.trimStart('0').ifEmpty { "0" } else ""
            val maximum = digits.takeIf { it.isNotEmpty() && it.length <= 29 }
                ?.let(::BigInteger)
                ?.takeIf { it <= maximumPossible }
            return WorldBlueprintStructureNonAirRule(true, maximum)
        }
    }
}
