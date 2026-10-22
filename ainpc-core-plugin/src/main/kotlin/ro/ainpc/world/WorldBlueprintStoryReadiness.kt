package ro.ainpc.world

import ro.ainpc.story.RegionStoryState

internal enum class WorldBlueprintStoryReadinessStatus(val label: String) {
    MAPPING_UNAVAILABLE("mapping indisponibil"),
    REGION_MISSING("regiune nemapata"),
    WRONG_WORLD("ID mapat in alta lume"),
    PERSISTENCE_UNAVAILABLE("stare persistenta necunoscuta"),
    PERSISTENT_STATE_PRESENT("stare persistenta existenta"),
    PERSISTENT_CURRENT_BLUEPRINT("story persistent cu sursa reviziei curente"),
    PERSISTENT_OTHER_BLUEPRINT("story persistent cu sursa altei revizii blueprint"),
    MAPPING_DESCRIPTION_PRESENT("descriere story existenta in mapping"),
    NO_OBSERVED_CONFLICT("fara conflict observat"),
}

internal data class WorldBlueprintStoryTargetReadiness(
    val regionId: String,
    val status: WorldBlueprintStoryReadinessStatus,
)

internal object WorldBlueprintStoryReadiness {
    fun inspect(
        worldName: String,
        regionIds: List<String>,
        mappedRegions: Collection<WorldRegionInfo>?,
        persistentStates: Map<String, RegionStoryState>?,
        currentFingerprint: String? = null,
        currentStory: String? = null,
    ): List<WorldBlueprintStoryTargetReadiness> {
        val regionsById = mappedRegions.orEmpty().groupBy { it.id() }
        return regionIds.map { regionId ->
            val matches = regionsById[regionId].orEmpty()
            val region = matches.firstOrNull { it.worldName() == worldName }
            val status = when {
                mappedRegions == null -> WorldBlueprintStoryReadinessStatus.MAPPING_UNAVAILABLE
                region == null && matches.isNotEmpty() -> WorldBlueprintStoryReadinessStatus.WRONG_WORLD
                region == null -> WorldBlueprintStoryReadinessStatus.REGION_MISSING
                persistentStates == null -> WorldBlueprintStoryReadinessStatus.PERSISTENCE_UNAVAILABLE
                regionId in persistentStates -> {
                    val state = persistentStates.getValue(regionId)
                    val sourceFingerprint = WorldBlueprintStoryProvenance.fingerprint(worldName, state.source())
                    when {
                        sourceFingerprint == null || currentFingerprint == null ->
                            WorldBlueprintStoryReadinessStatus.PERSISTENT_STATE_PRESENT
                        sourceFingerprint != currentFingerprint -> WorldBlueprintStoryReadinessStatus.PERSISTENT_OTHER_BLUEPRINT
                        currentStory != null && state.storyMode() == region.storyMode() &&
                            state.stateKey() == region.storyStateKey().ifBlank { "default" } &&
                            state.storyPool() == region.storyPool() &&
                            state.variables() == mapOf("story_description_${state.stateKey()}" to currentStory) ->
                            WorldBlueprintStoryReadinessStatus.PERSISTENT_CURRENT_BLUEPRINT
                        else -> WorldBlueprintStoryReadinessStatus.PERSISTENT_STATE_PRESENT
                    }
                }
                region.storyDescriptions().values.any { description ->
                    description.trim().let { it.isNotEmpty() && it != "<none>" }
                } ->
                    WorldBlueprintStoryReadinessStatus.MAPPING_DESCRIPTION_PRESENT
                else -> WorldBlueprintStoryReadinessStatus.NO_OBSERVED_CONFLICT
            }
            WorldBlueprintStoryTargetReadiness(regionId, status)
        }
    }
}
