package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.story.PlaceStoryState
import ro.ainpc.story.RegionStoryState
import ro.ainpc.story.StoryContextSnapshot
import ro.ainpc.world.PlaceType
import ro.ainpc.world.StoryMode
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.WorldContextSnapshot
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.util.logging.Logger

class OpenAISemanticWorldContextBuilderTest {
    @Test
    fun `semantic serialization preserves the descriptions used by direct dialogue`() {
        val region = WorldRegionInfo(
            "sat", "Sat", "world", "village", 0, 0, 0, 20, 100, 20,
            emptyList(), StoryMode.STATIC, "peaceful", listOf("peaceful"),
            mapOf("peaceful" to "Satul își urmează liniștit rutina."),
        )
        val place = WorldPlaceInfo(
            "atelier", "sat", "Atelier", "world", PlaceType.CUSTOM,
            0, 0, 0, 10, 100, 10, emptyList(), null, true,
            mapOf("story_state" to "open", "story_description_open" to "Atelierul primește comenzi, zilnic."),
        )
        val worldContext = WorldContextSnapshot(region, place, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val regionState = RegionStoryState(
            "sat", StoryMode.EVOLUTIVE, "shortage", emptyList(),
            mapOf("story_description_shortage" to "  Lipsesc proviziile,\n așteptăm livrările. "), 0, 0, "", "",
        )
        val placeState = PlaceStoryState(
            "atelier", "sat", "closed", mapOf("story_description_closed" to "Atelierul este închis, temporar."), 0, 0, "", "",
        )

        for (persistentRegion in listOf(null, regionState)) for (persistentPlace in listOf(null, placeState)) {
            val snapshot = StoryContextSnapshot(
                "Ana", "", "Player", worldContext, persistentRegion, persistentPlace,
                emptyList(), emptyList(), emptyList(), emptyList(),
            )
            val block = OpenAISemanticWorldContextBuilder.buildWorldHistoryBlock(region, place, snapshot)
            val directFacts = StoryFactResolver.fromWorldContext(worldContext, persistentRegion, persistentPlace, emptyList())
            val semanticFacts = StoryFactResolver.fromSemanticLines(
                block.lineSequence().map(String::trim).toList(),
                listOf("- region=sat, name=Sat, type=village", "- current_place=atelier, name=Atelier, type=custom"),
            )

            assertEquals(directFacts, semanticFacts)
            assertTrue(block.contains("- region_story_description=${directFacts.regionDescription}\n"))
            assertTrue(block.contains("- place_story_description=${directFacts.placeDescription}\n"))
            assertEquals(
                StoryFactResolver.resolve("Care este starea poveștii?", directFacts),
                StoryFactResolver.resolve("Care este starea poveștii?", semanticFacts),
            )
            assertEquals(
                StoryFactResolver.resolve("Ce se întâmplă la Atelier?", directFacts),
                StoryFactResolver.resolve("Ce se întâmplă la Atelier?", semanticFacts),
            )
        }
    }

    @Test
    fun `workplace is resolved from anchor coordinates even when its label is a display name`() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("WorkplaceContextTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        val forge = worldAdmin.getPlace("demo_sat:fierarie")!!
        val anchor = AINPC.OwnedLocation(
            "work", forge.displayName(), forge.worldName(),
            (forge.minX() + forge.maxX()) / 2.0,
            (forge.minY() + forge.maxY()) / 2.0,
            (forge.minZ() + forge.maxZ()) / 2.0,
        )

        assertEquals(forge.id(), OpenAISemanticWorldContextBuilder.resolveWorkPlace(worldAdmin, anchor)?.id())
        assertNull(OpenAISemanticWorldContextBuilder.resolveWorkPlace(
            worldAdmin, AINPC.OwnedLocation("work", forge.displayName(), "alta_lume", anchor.x(), anchor.y(), anchor.z()),
        ))
        assertNull(OpenAISemanticWorldContextBuilder.resolveWorkPlace(worldAdmin, null))
    }
}
