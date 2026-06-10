package ro.ainpc.platform.features

import org.bukkit.configuration.ConfigurationSection
import ro.ainpc.addons.AddonDescriptor
import ro.ainpc.platform.PlatformProfile
import java.util.Locale

class RuntimeFeatureResolver {
    fun resolve(config: ConfigurationSection?, addonDescriptors: Collection<AddonDescriptor>?): RuntimeFeatureSnapshot {
        return resolve(config, null, addonDescriptors)
    }

    fun resolve(
        config: ConfigurationSection?,
        profile: PlatformProfile?,
        addonDescriptors: Collection<AddonDescriptor>?
    ): RuntimeFeatureSnapshot {
        val descriptors = addonDescriptors ?: emptyList()
        val states = RuntimeFeatureKey.entries.map { feature ->
            resolveFeature(config, profile, descriptors, feature)
        }
        return RuntimeFeatureSnapshot(states)
    }

    fun resolve(config: ConfigurationSection?): RuntimeFeatureSnapshot =
        resolve(config, emptyList())

    private fun resolveFeature(
        config: ConfigurationSection?,
        profile: PlatformProfile?,
        descriptors: Collection<AddonDescriptor>,
        feature: RuntimeFeatureKey
    ): RuntimeFeatureState {
        val configured = config?.contains(feature.configPath) == true
        val enabled = config?.getBoolean(feature.configPath, feature.defaultEnabled) ?: feature.defaultEnabled
        val sources = mutableListOf(
            RuntimeFeatureSource.of(
                if (configured) "config" else "default",
                feature.configPath,
                enabled.toString(),
                if (configured) 100 else 0
            )
        )
        val reasons = mutableListOf(
            if (configured) "${feature.configPath}=$enabled" else "default ${feature.configPath}=$enabled"
        )

        if (profile != null) {
            sources.add(RuntimeFeatureSource.of("server-profile", "runtime_mode", profile.runtimeMode.id, 5))
            sources.add(RuntimeFeatureSource.of("server-profile", "world_mode", profile.worldMode.id, 5))
            sources.add(RuntimeFeatureSource.of("server-profile", "story_mode", profile.defaultStoryMode.id, 5))
            reasons.add(
                "server profile runtime=${profile.runtimeMode.id}, world=${profile.worldMode.id}, story=${profile.defaultStoryMode.id}"
            )
        }

        if (feature == RuntimeFeatureKey.ADDONS) {
            sources.add(RuntimeFeatureSource.of("addon-registry", "active_descriptors", descriptors.size.toString(), 10))
            reasons.add("active addon descriptors=${descriptors.size}")
        } else {
            val providers = descriptors
                .filter { descriptor -> descriptor.capabilities.any { capability -> capabilityMatchesFeature(capability, feature) } }
                .map { descriptor -> descriptor.id }
                .distinct()
            if (providers.isNotEmpty()) {
                sources.add(RuntimeFeatureSource.of("addon-capability", feature.id, providers.joinToString(","), 20))
                reasons.add("provided by addon capabilities: ${providers.joinToString(",")}")
            }
        }

        return RuntimeFeatureState.of(
            feature,
            if (enabled) RuntimeFeatureStatus.ENABLED else RuntimeFeatureStatus.DISABLED,
            reasons,
            sources
        )
    }

    private fun capabilityMatchesFeature(capability: String?, feature: RuntimeFeatureKey): Boolean {
        val normalized = normalize(capability)
        if (normalized.isBlank()) {
            return false
        }
        val featureId = normalize(feature.id)
        if (normalized == featureId || normalized == "${featureId}s") {
            return true
        }
        return when (feature) {
            RuntimeFeatureKey.QUEST -> normalized in setOf("quest", "quests", "scenarios", "progressions")
            RuntimeFeatureKey.PROGRESSION -> normalized in setOf("progression", "progressions", "scenarios")
            RuntimeFeatureKey.STORY -> normalized in setOf("story", "story-hooks", "story-state")
            RuntimeFeatureKey.MAPPING -> normalized in setOf("mapping", "world-admin", "world-place-api", "world-mapping")
            RuntimeFeatureKey.AI -> normalized in setOf("ai", "ai-engine")
            RuntimeFeatureKey.GUI -> normalized in setOf("gui", "ui")
            RuntimeFeatureKey.GENERATION -> normalized in setOf("generation", "world-generation", "spawn")
            RuntimeFeatureKey.ROUTINE -> normalized in setOf("routine", "routines")
            RuntimeFeatureKey.SIMULATION -> normalized in setOf("simulation", "simulations")
            RuntimeFeatureKey.ADDONS -> normalized in setOf("addon", "addons", "addon-api")
            RuntimeFeatureKey.DEMO -> normalized in setOf("demo", "demo-content")
        }
    }

    private fun normalize(value: String?): String =
        value?.trim()
            ?.lowercase(Locale.ROOT)
            ?.replace('_', '-')
            ?: ""
}
