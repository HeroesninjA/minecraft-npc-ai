package ro.ainpc.story

import ro.ainpc.world.StoryMode
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo

internal object StoryStateViewResolver {
    data class RegionView(
        val id: String,
        val mode: StoryMode,
        val stateKey: String,
        val pool: List<String>,
        val source: String,
        val description: String = "",
    )

    data class PlaceView(val id: String, val stateKey: String, val source: String, val description: String = "")

    fun region(mapped: WorldRegionInfo?, persistent: RegionStoryState?): RegionView? {
        if (persistent != null && (mapped == null || persistent.regionId() == mapped.id())) {
            return RegionView(
                persistent.regionId(), persistent.storyMode(), persistent.stateKey(), persistent.storyPool(), "persistent",
                description(persistent.variables(), persistent.stateKey()),
            )
        }
        return mapped?.let {
            val stateKey = it.storyStateKey().ifBlank { "default" }
            RegionView(
                it.id(), it.storyMode(), stateKey, it.storyPool(), "mapping",
                cleanDescription(it.storyDescriptions()[stateKey]),
            )
        }
    }

    fun place(mapped: WorldPlaceInfo?, persistent: PlaceStoryState?): PlaceView? {
        if (persistent != null && (mapped == null ||
                (persistent.placeId() == mapped.id() &&
                    (persistent.regionId().isBlank() || persistent.regionId() == mapped.regionId()))
            )) {
            return PlaceView(
                persistent.placeId(), persistent.stateKey(), "persistent",
                description(persistent.variables(), persistent.stateKey()),
            )
        }
        if (mapped == null) return null
        val state = mapped.metadata()["story_state"]?.takeIf { it.isNotBlank() }
            ?: mapped.metadata()["state"]?.takeIf { it.isNotBlank() }
            ?: return null
        return PlaceView(mapped.id(), state, "mapping", description(mapped.metadata(), state))
    }

    private fun description(properties: Map<String, String>, stateKey: String): String =
        cleanDescription(properties["story_description_$stateKey"])

    private fun cleanDescription(value: String?): String =
        value.orEmpty().trim()
            .takeUnless { it == "<none>" }.orEmpty()
            .replace(Regex("\\s+"), " ").take(200).trim()
}
