package ro.ainpc.world

import java.math.BigInteger

internal data class WorldBlueprintBaseUnsupportedPositionsRule(
    val configured: Boolean,
    val maximum: BigInteger?,
) {
    val isValid: Boolean get() = !configured || maximum != null

    fun exceeds(bounds: WorldBlueprintRegionBounds, preview: WorldBlueprintFoundationPreview?): Boolean? {
        val limit = maximum ?: return null
        val foundation = preview ?: return null
        val unsupported = BigInteger.valueOf(foundation.unsupportedCount.toLong())
        if (unsupported > limit) return true
        val totalColumns = BigInteger.valueOf(bounds.width).multiply(BigInteger.valueOf(bounds.depth))
        val maximumUnsupported = totalColumns - BigInteger.valueOf(foundation.solidCount.toLong())
        return if (maximumUnsupported <= limit) false else null
    }

    companion object {
        const val KEY = "max_base_unsupported_positions"
        private val decimal = Regex("[0-9]+")
        private val maximumPossible = BigInteger.ONE.shiftLeft(64)

        fun parse(mapRules: Map<String, String>): WorldBlueprintBaseUnsupportedPositionsRule {
            val raw = mapRules[KEY] ?: return WorldBlueprintBaseUnsupportedPositionsRule(false, null)
            val text = raw.trim()
            val digits = if (decimal.matches(text)) text.trimStart('0').ifEmpty { "0" } else ""
            val maximum = digits.takeIf { it.isNotEmpty() && it.length <= 20 }
                ?.let(::BigInteger)
                ?.takeIf { it <= maximumPossible }
            return WorldBlueprintBaseUnsupportedPositionsRule(true, maximum)
        }
    }
}
