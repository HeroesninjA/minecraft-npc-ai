package ro.ainpc.world

internal data class WorldBlueprintWorldSeedRule(
    val configured: Boolean,
    val seed: Long?,
) {
    val isValid: Boolean get() = !configured || seed != null

    companion object {
        const val KEY = "world_seed"
        private val decimal = Regex("-?(0|[1-9][0-9]*)")

        fun parse(mapRules: Map<String, String>): WorldBlueprintWorldSeedRule {
            val raw = mapRules[KEY] ?: return WorldBlueprintWorldSeedRule(false, null)
            val value = raw.trim()
            return WorldBlueprintWorldSeedRule(true, value.takeIf(decimal::matches)?.toLongOrNull())
        }
    }
}
