package ro.ainpc.story

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.world.PlaceType
import ro.ainpc.world.StoryMode
import ro.ainpc.world.WorldContextSnapshot
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo

class StoryStateViewResolverTest {
    private val mappedRegion = WorldRegionInfo(
        "sat", "Sat", "world", "village", 0, 0, 0, 20, 100, 20,
        emptyList(), StoryMode.STATIC, "peaceful", listOf("peaceful"),
        mapOf("peaceful" to "Satul trăiește liniștit.", "shortage" to "Text din mapping."),
    )
    private val mappedPlace = WorldPlaceInfo(
        "atelier", "sat", "Atelier", "world", PlaceType.CUSTOM,
        0, 0, 0, 10, 100, 10, emptyList(), null, true,
        mapOf("story_state" to "open", "story_description_open" to "Atelierul primește comenzi."),
    )

    @Test
    fun persistentStateWinsOverMappingAndPromptShowsTheEffectiveSource() {
        val regionState = RegionStoryState(
            "sat", StoryMode.ROTATIVE, "shortage", listOf("shortage"),
            mapOf("story_description_shortage" to "Lipsesc proviziile, așteptăm livrările."), 0, 0, "", "",
        )
        val placeState = PlaceStoryState(
            "atelier", "sat", "closed", mapOf("story_description_closed" to "Atelierul este închis."), 0, 0, "", "",
        )
        val regionView = StoryStateViewResolver.region(mappedRegion, regionState)!!
        val placeView = StoryStateViewResolver.place(mappedPlace, placeState)!!
        val snapshot = StoryContextSnapshot(
            "Ana", "", "Player",
            WorldContextSnapshot(mappedRegion, mappedPlace, emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
            regionState, placeState, emptyList(), emptyList(), emptyList(), emptyList(),
        )

        assertEquals("shortage", regionView.stateKey)
        assertEquals(StoryMode.ROTATIVE, regionView.mode)
        assertEquals(listOf("shortage"), regionView.pool)
        assertEquals("persistent", regionView.source)
        assertEquals("closed", placeView.stateKey)
        assertEquals("persistent", placeView.source)
        assertEquals("Lipsesc proviziile, așteptăm livrările.", regionView.description)
        assertEquals("Atelierul este închis.", placeView.description)
        assertTrue(snapshot.toPromptBlock().contains("effective_region_story: sat, state=shortage, mode=rotative, source=persistent"))
        assertTrue(snapshot.toPromptBlock().contains("effective_place_story: atelier, state=closed, source=persistent"))
        assertTrue(snapshot.toPromptBlock().contains("- effective_region_story_description: ${regionView.description}\n"))
        assertTrue(snapshot.toPromptBlock().contains("- effective_place_story_description: ${placeView.description}\n"))
    }

    @Test
    fun mappingIsFallbackAndMismatchedRowsCannotOverrideAnotherLocation() {
        val wrongRegion = RegionStoryState(
            "alt_sat", StoryMode.ROTATIVE, "danger", emptyList(),
            mapOf("story_description_danger" to "Alt sat în pericol."), 0, 0, "", "",
        )
        val variables = mapOf("story_description_closed" to "Alt atelier închis.")
        val wrongPlace = PlaceStoryState("alt_atelier", "sat", "closed", variables, 0, 0, "", "")
        val wrongPlaceRegion = PlaceStoryState("atelier", "alt_sat", "closed", variables, 0, 0, "", "")

        assertEquals("peaceful", StoryStateViewResolver.region(mappedRegion, wrongRegion)?.stateKey)
        assertEquals("mapping", StoryStateViewResolver.region(mappedRegion, null)?.source)
        assertEquals("open", StoryStateViewResolver.place(mappedPlace, wrongPlace)?.stateKey)
        assertEquals("open", StoryStateViewResolver.place(mappedPlace, wrongPlaceRegion)?.stateKey)
        assertEquals("mapping", StoryStateViewResolver.place(mappedPlace, null)?.source)
        assertEquals("Satul trăiește liniștit.", StoryStateViewResolver.region(mappedRegion, wrongRegion)?.description)
        assertEquals("Atelierul primește comenzi.", StoryStateViewResolver.place(mappedPlace, wrongPlace)?.description)
        assertEquals("Atelierul primește comenzi.", StoryStateViewResolver.place(mappedPlace, wrongPlaceRegion)?.description)
        assertNull(StoryStateViewResolver.place(mappedPlaceWithoutState(), null))
    }

    @Test
    fun descriptionsFollowTheExactStateAndSourceWithoutReusingStaleText() {
        val staleVariables = mapOf("story_description_shortage" to "Lipsesc proviziile.")
        val region = RegionStoryState("sat", StoryMode.EVOLUTIVE, "peaceful", emptyList(), staleVariables, 0, 0, "", "")
        val reset = RegionStoryState("sat", StoryMode.EVOLUTIVE, "default", emptyList(), staleVariables, 0, 0, "", "")
        val place = PlaceStoryState("atelier", "sat", "open", staleVariables, 0, 0, "", "")

        assertEquals("", StoryStateViewResolver.region(mappedRegion, region)?.description)
        assertEquals("", StoryStateViewResolver.region(mappedRegion, reset)?.description)
        assertEquals("", StoryStateViewResolver.place(mappedPlace, place)?.description)
        val sameStateWithoutDescription = RegionStoryState(
            "sat", StoryMode.EVOLUTIVE, "peaceful", emptyList(), emptyMap(), 0, 0, "", "",
        )
        assertEquals("", StoryStateViewResolver.region(mappedRegion, sameStateWithoutDescription)?.description)
    }

    @Test
    fun descriptionsAreSingleLineBoundedAndOptional() {
        val state = PlaceStoryState(
            "atelier", "sat", "closed",
            mapOf("story_description_closed" to "  Atelierul\r\n este\tînchis, temporar.  "), 0, 0, "", "",
        )
        assertEquals("Atelierul este închis, temporar.", StoryStateViewResolver.place(mappedPlace, state)?.description)

        for (emptyDescription in listOf("", " \n\t ", " <none> ")) {
            val emptyState = PlaceStoryState(
                "atelier", "sat", "closed", mapOf("story_description_closed" to emptyDescription), 0, 0, "", "",
            )
            assertEquals("", StoryStateViewResolver.place(mappedPlace, emptyState)?.description)
        }

        val longState = PlaceStoryState(
            "atelier", "sat", "closed", mapOf("story_description_closed" to "Text".repeat(100)), 0, 0, "", "",
        )
        assertEquals(200, StoryStateViewResolver.place(mappedPlace, longState)?.description?.length)
    }

    @Test
    fun explicitPersistentDefaultStillOverridesConfiguredState() {
        val reset = RegionStoryState("sat", StoryMode.EVOLUTIVE, "default", emptyList(), emptyMap(), 0, 0, "", "")

        assertEquals("default", StoryStateViewResolver.region(mappedRegion, reset)?.stateKey)
    }

    private fun mappedPlaceWithoutState(): WorldPlaceInfo = WorldPlaceInfo(
        "gol", "sat", "Gol", "world", PlaceType.CUSTOM,
        0, 0, 0, 10, 100, 10, emptyList(), null, true, emptyMap(),
    )
}
