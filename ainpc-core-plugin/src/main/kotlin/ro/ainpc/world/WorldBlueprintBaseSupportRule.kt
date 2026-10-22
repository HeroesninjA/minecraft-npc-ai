package ro.ainpc.world

internal data class WorldBlueprintBaseSupportRule(
    val configured: Boolean,
    val maximum: Int?,
) {
    val isValid: Boolean get() = !configured || maximum != null

    fun exceeds(preview: WorldBlueprintFoundationPreview?): Boolean? {
        val limit = maximum ?: return null
        val foundation = preview ?: return null
        if (foundation.unsupportedCount > limit) return true
        return if (foundation.unsupportedCount + foundation.unknownCount <= limit) false else null
    }

    companion object {
        const val KEY = "max_base_unsupported_samples"

        fun parse(mapRules: Map<String, String>): WorldBlueprintBaseSupportRule {
            val raw = mapRules[KEY] ?: return WorldBlueprintBaseSupportRule(false, null)
            return WorldBlueprintBaseSupportRule(true, raw.trim().toIntOrNull()?.takeIf { it in 0..5 })
        }
    }
}
