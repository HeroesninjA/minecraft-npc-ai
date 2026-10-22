package ro.ainpc.world

import java.math.BigInteger

internal data class WorldBlueprintStructureFluidRule(
    val configured: Boolean,
    val maximum: BigInteger?,
) {
    val isValid: Boolean get() = !configured || maximum != null

    fun exceeds(bounds: WorldBlueprintRegionBounds, occupancy: WorldBlueprintBlockOccupancyPreview?): Boolean? {
        val limit = maximum ?: return null
        val observed = occupancy ?: return null
        val observedFluids = BigInteger.valueOf(observed.fluidPositionCount.toLong())
        if (observedFluids > limit) return true
        val observedDry = BigInteger.valueOf((observed.observedCount - observed.fluidPositionCount).toLong())
        return if (bounds.blockPositions - observedDry <= limit) false else null
    }

    companion object {
        const val KEY = "max_structure_fluid_positions"
        private val decimal = Regex("[0-9]+")
        private val maximumPossible = BigInteger.ONE.shiftLeft(96)

        fun parse(mapRules: Map<String, String>): WorldBlueprintStructureFluidRule {
            val raw = mapRules[KEY] ?: return WorldBlueprintStructureFluidRule(false, null)
            val text = raw.trim()
            val digits = if (decimal.matches(text)) text.trimStart('0').ifEmpty { "0" } else ""
            val maximum = digits.takeIf { it.isNotEmpty() && it.length <= 29 }
                ?.let(::BigInteger)
                ?.takeIf { it <= maximumPossible }
            return WorldBlueprintStructureFluidRule(true, maximum)
        }
    }
}
