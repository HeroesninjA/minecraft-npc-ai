package ro.ainpc.platform

import org.bukkit.configuration.file.FileConfiguration
import ro.ainpc.world.StoryMode
import ro.ainpc.world.WorldMode

class PlatformProfile(
    val runtimeMode: RuntimeMode,
    val worldMode: WorldMode,
    val defaultStoryMode: StoryMode
) {
    companion object {
        @JvmStatic
        fun fromConfig(config: FileConfiguration): PlatformProfile {
            val runtimeMode = RuntimeMode.fromId(config.getString("platform.runtime_mode", "standalone"))
            val worldMode = WorldMode.fromId(config.getString("platform.world_mode", "finite_dynamic"))
            val storyMode = StoryMode.fromId(config.getString("platform.default_story_mode", "evolutive"))
            return PlatformProfile(runtimeMode, worldMode, storyMode)
        }
    }
}
