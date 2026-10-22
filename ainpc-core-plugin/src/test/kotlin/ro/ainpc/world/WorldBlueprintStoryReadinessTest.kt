package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.story.RegionStoryState

class WorldBlueprintStoryReadinessTest {
    @Test
    fun `preflight distinguishes unknown mapping and persistence from absent regions`() {
        val requested = listOf("keep", "missing")
        val unknownMapping = WorldBlueprintStoryReadiness.inspect("world", requested, null, null)
        val unknownPersistence = WorldBlueprintStoryReadiness.inspect("world", requested,
            listOf(region("keep")), null)

        assertEquals(listOf(WorldBlueprintStoryReadinessStatus.MAPPING_UNAVAILABLE,
            WorldBlueprintStoryReadinessStatus.MAPPING_UNAVAILABLE), unknownMapping.map { it.status })
        assertEquals(listOf(WorldBlueprintStoryReadinessStatus.PERSISTENCE_UNAVAILABLE,
            WorldBlueprintStoryReadinessStatus.REGION_MISSING), unknownPersistence.map { it.status })
    }

    @Test
    fun `preflight reports existing state and mapping description before an unobstructed target`() {
        val requested = listOf("persistent", "described", "other_state", "placeholder", "clean", "foreign", "missing")
        val regions = listOf(
            region("persistent"),
            region("described", descriptions = mapOf("default" to "Poveste veche")),
            region("other_state", descriptions = mapOf("siege" to "Asediu vechi")),
            region("placeholder", descriptions = mapOf("default" to "<none>")),
            region("clean"),
            region("foreign", worldName = "nether"),
        )
        val states = mapOf("persistent" to RegionStoryState("persistent", StoryMode.EVOLUTIVE,
            "default", emptyList(), emptyMap(), 1L, 2L, "admin", "test"))

        val result = WorldBlueprintStoryReadiness.inspect("world", requested, regions, states)

        assertEquals(requested, result.map { it.regionId })
        assertEquals(listOf(
            WorldBlueprintStoryReadinessStatus.PERSISTENT_STATE_PRESENT,
            WorldBlueprintStoryReadinessStatus.MAPPING_DESCRIPTION_PRESENT,
            WorldBlueprintStoryReadinessStatus.MAPPING_DESCRIPTION_PRESENT,
            WorldBlueprintStoryReadinessStatus.NO_OBSERVED_CONFLICT,
            WorldBlueprintStoryReadinessStatus.NO_OBSERVED_CONFLICT,
            WorldBlueprintStoryReadinessStatus.WRONG_WORLD,
            WorldBlueprintStoryReadinessStatus.REGION_MISSING,
        ), result.map { it.status })
    }

    @Test
    fun `preflight distinguishes current blueprint story from an older revision`() {
        val fingerprint = "a".repeat(64)
        val olderFingerprint = "b".repeat(64)
        val story = "Povestea cetatii"
        val current = state(story, WorldBlueprintStoryProvenance.source("world", fingerprint))
        val older = state(story, WorldBlueprintStoryProvenance.source("world", olderFingerprint))

        assertEquals(WorldBlueprintStoryReadinessStatus.PERSISTENT_CURRENT_BLUEPRINT,
            readiness(current, fingerprint, story))
        assertEquals(WorldBlueprintStoryReadinessStatus.PERSISTENT_OTHER_BLUEPRINT,
            readiness(older, fingerprint, story))
    }

    @Test
    fun `preflight does not claim current revision without matching source and payload`() {
        val fingerprint = "a".repeat(64)
        val source = WorldBlueprintStoryProvenance.source("world", fingerprint)
        val story = "Povestea cetatii"
        val otherSources = listOf(
            "test",
            WorldBlueprintStoryProvenance.source("nether", fingerprint),
            "world_blueprint:world:${"a".repeat(63)}",
            "world_blueprint:world:${"A".repeat(64)}",
            "$source:extra",
        )

        otherSources.forEach { otherSource ->
            assertEquals(WorldBlueprintStoryReadinessStatus.PERSISTENT_STATE_PRESENT,
                readiness(state(story, otherSource), fingerprint, story))
        }
        assertEquals(WorldBlueprintStoryReadinessStatus.PERSISTENT_STATE_PRESENT,
            readiness(state("Alta poveste", source), fingerprint, story))
        assertEquals(WorldBlueprintStoryReadinessStatus.PERSISTENT_STATE_PRESENT,
            readiness(state(story, source, stateKey = "other"), fingerprint, story))
        assertEquals(WorldBlueprintStoryReadinessStatus.PERSISTENT_STATE_PRESENT,
            readiness(state(story, source, storyPool = listOf("other")), fingerprint, story))
        assertEquals(WorldBlueprintStoryReadinessStatus.PERSISTENT_STATE_PRESENT,
            readiness(state(story, source), null, story))
    }

    @Test
    fun `world plan forwards draft fingerprint to story preflight`() {
        val fingerprint = "a".repeat(64)
        val draft = WorldBlueprint("world", "Povestea cetatii",
            mapRules = mapOf("story_region_ids" to "keep"))
        val state = state(draft.story, WorldBlueprintStoryProvenance.source("world", fingerprint))

        val plan = WorldBlueprintPlanner.compile(draft, emptyList(), mappedRegions = listOf(region("keep")),
            persistentStoryStates = mapOf("keep" to state), currentFingerprint = fingerprint)

        assertEquals(WorldBlueprintStoryReadinessStatus.PERSISTENT_CURRENT_BLUEPRINT,
            plan.storyReadiness.single().status)
    }

    @Test
    fun `world plan includes only valid declared story ids in read only preflight`() {
        val draft = WorldBlueprint("world", mapRules = mapOf("story_region_ids" to "keep, keep,missing"))
        val plan = WorldBlueprintPlanner.compile(draft, emptyList(), mappedRegions = listOf(region("keep")),
            persistentStoryStates = emptyMap())
        val invalid = WorldBlueprintPlanner.compile(draft.copy(mapRules = mapOf("story_region_ids" to "keep,")),
            emptyList(), mappedRegions = listOf(region("keep")), persistentStoryStates = emptyMap())

        assertEquals(listOf("keep", "missing"), plan.storyReadiness.map { it.regionId })
        assertEquals(WorldBlueprintStoryReadinessStatus.NO_OBSERVED_CONFLICT, plan.storyReadiness.first().status)
        assertEquals(WorldBlueprintStoryReadinessStatus.REGION_MISSING, plan.storyReadiness.last().status)
        assertTrue(invalid.storyReadiness.isEmpty())
    }

    private fun region(
        id: String,
        worldName: String = "world",
        descriptions: Map<String, String> = emptyMap(),
    ) = WorldRegionInfo(id, id, worldName, RegionType.CASTLE.id, 0, 0, 0, 1, 1, 1,
        emptyList(), StoryMode.EVOLUTIVE, "default", emptyList(), descriptions)

    private fun state(
        story: String,
        source: String,
        stateKey: String = "default",
        storyPool: List<String> = emptyList(),
    ) = RegionStoryState("keep", StoryMode.EVOLUTIVE, stateKey, storyPool,
        mapOf("story_description_default" to story), 1L, 2L, "admin", source)

    private fun readiness(state: RegionStoryState, fingerprint: String?, story: String): WorldBlueprintStoryReadinessStatus =
        WorldBlueprintStoryReadiness.inspect("world", listOf("keep"), listOf(region("keep")),
            mapOf("keep" to state), fingerprint, story).single().status
}
