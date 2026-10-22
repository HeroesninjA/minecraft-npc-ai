package ro.ainpc.world

import java.math.BigInteger

internal data class WorldBlueprintStructureRegionPositionsRule(
    val configured: Boolean,
    val maximum: BigInteger?,
) {
    val isValid: Boolean get() = !configured || maximum != null

    fun exceeds(bounds: WorldBlueprintRegionBounds): Boolean? = maximum?.let { bounds.blockPositions > it }

    companion object {
        const val KEY = "max_structure_region_positions"
        private val decimal = Regex("[0-9]+")
        private val maximumPossible = BigInteger.ONE.shiftLeft(96)

        fun parse(mapRules: Map<String, String>): WorldBlueprintStructureRegionPositionsRule {
            val raw = mapRules[KEY] ?: return WorldBlueprintStructureRegionPositionsRule(false, null)
            val text = raw.trim()
            val digits = if (decimal.matches(text)) text.trimStart('0') else ""
            val maximum = digits.takeIf { it.isNotEmpty() && it.length <= 29 }
                ?.let(::BigInteger)
                ?.takeIf { it <= maximumPossible }
            return WorldBlueprintStructureRegionPositionsRule(true, maximum)
        }
    }
}
