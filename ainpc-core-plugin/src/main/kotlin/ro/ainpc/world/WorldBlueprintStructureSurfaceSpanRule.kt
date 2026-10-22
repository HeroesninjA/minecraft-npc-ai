package ro.ainpc.world

internal data class WorldBlueprintStructureSurfaceSpanRule(
    val configured: Boolean,
    val maximum: Int?,
) {
    val isValid: Boolean get() = !configured || maximum != null

    fun exceeds(preview: WorldBlueprintTerrainPreview): Boolean? {
        val limit = maximum ?: return null
        val span = preview.observedSpan ?: return null
        return span > limit
    }

    companion object {
        const val KEY = "max_structure_surface_span"

        fun parse(mapRules: Map<String, String>): WorldBlueprintStructureSurfaceSpanRule {
            val raw = mapRules[KEY] ?: return WorldBlueprintStructureSurfaceSpanRule(false, null)
            return WorldBlueprintStructureSurfaceSpanRule(true, raw.trim().toIntOrNull()?.takeIf { it >= 0 })
        }
    }
}
