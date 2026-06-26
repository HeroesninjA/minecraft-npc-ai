package ro.ainpc.api

import ro.ainpc.platform.RuntimeMode
import ro.ainpc.world.StoryMode
import ro.ainpc.world.WorldMode
import java.nio.file.Path
import java.util.Locale

interface AINPCPlatformApi {
    val runtimeMode: RuntimeMode

    val worldMode: WorldMode

    val defaultStoryMode: StoryMode

    val addonRegistry: AddonRegistryApi

    val worldAdmin: WorldAdminApi

    val dataDirectory: Path

    val packDirectory: Path

    fun getAddonConfigDirectory(addonId: String?): Path {
        val safeAddonId = sanitizePathSegment(addonId, "unknown-addon")
        return dataDirectory.resolve("addons").resolve(safeAddonId)
    }

    fun reloadContent()

    fun registerObjectiveHandler(
        type: String,
        handler: (playerUuid: String, currentProgress: Int, requiredAmount: Int) -> Int
    )

    private fun sanitizePathSegment(value: String?, fallback: String): String {
        if (value.isNullOrBlank()) {
            return fallback
        }
        val normalized = value.trim().lowercase(Locale.ROOT)
        val sanitized = normalized.replace(Regex("[^a-z0-9._-]"), "-")
        return if (sanitized.isBlank()) fallback else sanitized
    }
}
