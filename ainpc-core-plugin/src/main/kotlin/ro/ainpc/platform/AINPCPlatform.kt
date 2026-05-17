package ro.ainpc.platform

import ro.ainpc.AINPCPlugin
import ro.ainpc.addons.AddonDescriptor
import ro.ainpc.addons.AddonRegistry
import ro.ainpc.addons.AddonType
import ro.ainpc.api.AINPCPlatformApi
import ro.ainpc.api.AddonRegistryApi
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.world.StoryMode
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.WorldMode
import java.nio.file.Path
import java.util.EnumSet
import java.util.Locale

class AINPCPlatform(
    private val plugin: AINPCPlugin
) : AINPCPlatformApi {
    private val addonRegistry: AddonRegistry = AddonRegistry(this)
    val worldAdminService: WorldAdminService = WorldAdminService(plugin)
    private var profile: PlatformProfile = PlatformProfile.fromConfig(plugin.config)

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
    }

    override fun getRuntimeMode(): RuntimeMode = profile.runtimeMode

    override fun getWorldMode(): WorldMode = profile.worldMode

    override fun getDefaultStoryMode(): StoryMode = profile.defaultStoryMode

    override fun getAddonRegistry(): AddonRegistryApi = addonRegistry

    override fun getWorldAdmin(): WorldAdminApi = worldAdminService

    override fun getDataDirectory(): Path = plugin.dataFolder.toPath()

    override fun getPackDirectory(): Path = dataDirectory.resolve("packs")

    override fun getAddonConfigDirectory(addonId: String): Path {
        return dataDirectory
            .resolve(sanitizeRelativeDirectory(plugin.config.getString("addons.config_directory", "addons")))
            .resolve(sanitizePathSegment(addonId, "unknown-addon"))
    }

    override fun reloadContent() {
        plugin.reloadContent()
    }

    fun getProfile(): PlatformProfile = profile

    fun shutdown() {
        addonRegistry.shutdown()
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
