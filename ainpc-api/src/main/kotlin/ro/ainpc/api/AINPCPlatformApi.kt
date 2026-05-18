package ro.ainpc.api

import ro.ainpc.platform.RuntimeMode
import ro.ainpc.world.StoryMode
import ro.ainpc.world.WorldMode
import java.nio.file.Path

interface AINPCPlatformApi {
    val runtimeMode: RuntimeMode

    val worldMode: WorldMode

    val defaultStoryMode: StoryMode

    val addonRegistry: AddonRegistryApi

    val worldAdmin: WorldAdminApi

    val dataDirectory: Path

    val packDirectory: Path

    fun getAddonConfigDirectory(addonId: String?): Path {
        val safeAddonId = if (addonId.isNullOrBlank()) "unknown-addon" else addonId.trim()
        return dataDirectory.resolve("addons").resolve(safeAddonId)
    }

    fun reloadContent()
}
