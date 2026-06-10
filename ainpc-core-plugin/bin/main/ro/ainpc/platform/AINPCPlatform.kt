package ro.ainpc.platform

import ro.ainpc.AINPCPlugin
import ro.ainpc.addons.AddonDescriptor
import ro.ainpc.addons.AddonRegistry
import ro.ainpc.addons.AddonType
import ro.ainpc.api.AINPCPlatformApi
import ro.ainpc.api.AddonRegistryApi
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.platform.features.RuntimeFeatureResolver
import ro.ainpc.platform.features.RuntimeFeatureSnapshot
import ro.ainpc.world.StoryMode
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.WorldMode
import java.nio.file.Path
import java.util.EnumSet
import java.util.Locale

class AINPCPlatform(
    private val plugin: AINPCPlugin
) : AINPCPlatformApi {
    override val addonRegistry: AddonRegistry = AddonRegistry(this)
    val worldAdminService: WorldAdminService = WorldAdminService(plugin)
    private val featureResolver: RuntimeFeatureResolver = RuntimeFeatureResolver()
    private var profile: PlatformProfile = PlatformProfile.fromConfig(plugin.config)
    private var featureSnapshot: RuntimeFeatureSnapshot = featureResolver.resolve(plugin.config)

    fun initialize() {
        reloadFromConfig()
        registerCoreDescriptor()
    }

    fun reloadFromConfig() {
        profile = PlatformProfile.fromConfig(plugin.config)
        addonRegistry.configure(
            plugin.config.getBoolean("addons.enabled", true),
            plugin.config.getBoolean("addons.strict_validation", true),
            plugin.config.getStringList("addons.disabled"),
            plugin.config.getStringList("addons.load_order")
        )
        worldAdminService.reloadFromConfig(plugin.config, profile)
        refreshFeatureSnapshot()
    }

    private fun registerCoreDescriptor() {
        addonRegistry.registerDescriptor(
            AddonDescriptor(
                AddonDescriptor.ORIGIN_CORE,
                "ainpc-core",
                "AINPC Core",
                plugin.pluginMeta.version,
                "Nucleul universal al platformei NPC AI",
                AddonType.CORE,
                false,
                EnumSet.allOf(RuntimeMode::class.java),
                listOf("ai-engine", "context-system", "dialog-system", "world-admin-api", "world-place-api", "addon-api"),
                emptyList()
            )
        )
        refreshFeatureSnapshot()
    }

    override val runtimeMode: RuntimeMode
        get() = profile.runtimeMode

    override val worldMode: WorldMode
        get() = profile.worldMode

    override val defaultStoryMode: StoryMode
        get() = profile.defaultStoryMode

    override val worldAdmin: WorldAdminApi
        get() = worldAdminService

    override val dataDirectory: Path
        get() = plugin.dataFolder.toPath()

    override val packDirectory: Path
        get() = dataDirectory.resolve("packs")

    override fun getAddonConfigDirectory(addonId: String?): Path {
        return dataDirectory
            .resolve(sanitizeRelativeDirectory(plugin.config.getString("addons.config_directory", "addons")))
            .resolve(sanitizePathSegment(addonId, "unknown-addon"))
    }

    override fun reloadContent() {
        plugin.reloadContent()
    }

    fun getProfile(): PlatformProfile = profile

    fun runtimeFeatures(): RuntimeFeatureSnapshot = featureSnapshot

    fun shutdown() {
        addonRegistry.shutdown()
        refreshFeatureSnapshot()
    }

    private fun refreshFeatureSnapshot() {
        featureSnapshot = featureResolver.resolve(plugin.config, profile, addonRegistry.descriptors)
    }

    private fun sanitizeRelativeDirectory(directory: String?): String {
        if (directory.isNullOrBlank()) {
            return "addons"
        }
        val normalized = directory.trim().replace('\\', '/')
        if (normalized.startsWith("/") || normalized.contains(":") || normalized.contains("..")) {
            return "addons"
        }
        return normalized
    }

    private fun sanitizePathSegment(value: String?, fallback: String): String {
        if (value.isNullOrBlank()) {
            return fallback
        }
        val normalized = value.trim().lowercase(Locale.ROOT)
        val sanitized = normalized.replace(Regex("[^a-z0-9._-]"), "-")
        return if (sanitized.isBlank()) fallback else sanitized
    }
}
