package ro.ainpc.engine

import ro.ainpc.addons.AddonType
import java.util.Locale

object FeaturePackSupport {
    @JvmStatic
    fun inferAddonType(pack: FeaturePackLoader.FeaturePack): AddonType {
        val id = pack.id.lowercase(Locale.ROOT)
        if (id.startsWith("core_")) {
            return AddonType.FEATURE
        }
        if (id.contains("vault") || id.contains("worldedit") || id.contains("integration")) {
            return AddonType.INTEGRATION
        }
        if (pack.professions.isNotEmpty() || pack.topologies.isNotEmpty() || pack.hasScenarioDefinitions()) {
            return AddonType.SCENARIO
        }
        return AddonType.FEATURE
    }

    @JvmStatic
    fun detectCapabilities(pack: FeaturePackLoader.FeaturePack): List<String> {
        val capabilities = mutableListOf<String>()
        if (pack.traits.isNotEmpty()) {
            capabilities.add("traits")
        }
        if (pack.professions.isNotEmpty()) {
            capabilities.add("professions")
        }
        if (pack.dialogues.isNotEmpty()) {
            capabilities.add("dialogues")
        }
        if (pack.topologies.isNotEmpty()) {
            capabilities.add("topology")
        }
        if (pack.hasScenarioDefinitions()) {
            capabilities.add("scenarios")
        }
        if (pack.progressionMechanics.isNotEmpty()) {
            capabilities.add("progression")
        }
        return capabilities
    }

    @JvmStatic
    fun sanitizeCapabilities(
        configuredCapabilities: List<String>?,
        pack: FeaturePackLoader.FeaturePack,
    ): List<String> {
        val sanitized = LinkedHashSet(configuredCapabilities ?: emptyList())
        if (pack.hasScenarioDefinitions()) {
            sanitized.add("scenarios")
        } else {
            sanitized.remove("scenarios")
        }
        if (pack.progressionMechanics.isNotEmpty()) {
            sanitized.add("progression")
        } else {
            sanitized.remove("progression")
        }
        return ArrayList(sanitized)
    }

    @JvmStatic
    fun normalizeText(value: String?): String = value?.trim()?.lowercase(Locale.ROOT).orEmpty()

    @JvmStatic
    fun firstNonBlank(vararg values: String?): String {
        for (value in values) {
            if (!value.isNullOrBlank()) {
                return value
            }
        }
        return ""
    }
}
