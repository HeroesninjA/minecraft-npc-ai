package ro.ainpc.platform.features

import java.util.Locale

enum class RuntimeFeatureKey(
    val id: String,
    val configPath: String,
    val defaultEnabled: Boolean
) {
    GUI("gui", "features.gui", true),
    QUEST("quest", "features.quest", true),
    PROGRESSION("progression", "features.progression", true),
    STORY("story", "features.story", true),
    MAPPING("mapping", "features.mapping", true),
    ROUTINE("routine", "features.routine", false),
    SIMULATION("simulation", "features.simulation", false),
    AI("ai", "features.ai", false),
    GENERATION("generation", "features.generation", false),
    ADDONS("addons", "addons.enabled", true),
    DEMO("demo", "demo.enabled", false);

    companion object {
        @JvmStatic
        fun fromId(value: String?): RuntimeFeatureKey? {
            val normalized = normalize(value)
            if (normalized.isBlank()) {
                return null
            }
            return entries.firstOrNull { key ->
                key.id == normalized ||
                    key.name.lowercase(Locale.ROOT) == normalized ||
                    key.configPath.lowercase(Locale.ROOT) == normalized
            }
        }

        private fun normalize(value: String?): String =
            value?.trim()
                ?.lowercase(Locale.ROOT)
                ?.replace('_', '-')
                ?.replace("features-", "features.")
                ?: ""
    }
}
