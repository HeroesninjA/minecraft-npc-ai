package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.story.PlaceStoryState
import ro.ainpc.story.RegionStoryState
import ro.ainpc.story.StoryEvent
import ro.ainpc.world.PlaceType
import ro.ainpc.world.StoryMode
import ro.ainpc.world.WorldContextSnapshot
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo

class StoryFactResolverTest {
    @Test
    fun `named questions only use the current mapped region and place`() {
        val facts = StoryFactResolver.Facts(
            regionState = "shortage", placeState = "closed",
            regionDescription = "Lipsesc proviziile.", placeDescription = "Fierăria este închisă.",
            regionName = "Valea Verde", placeName = "Fierărie",
        )

        assertTrue(StoryFactResolver.isLookupQuestion("Cum e situația în Valea Verde?", facts))
        assertEquals(
            "Starea curenta a regiunii: Lipsesc proviziile.",
            StoryFactResolver.resolve("Cum e situația în Valea Verde?", facts),
        )
        assertEquals(
            "Starea curenta a locului: Fierăria este închisă.",
            StoryFactResolver.resolve("Ce se întâmplă la Fierărie?", facts),
        )
        assertFalse(StoryFactResolver.isLookupQuestion("Ce se întâmplă la Taverna Veche?", facts))
        assertNull(StoryFactResolver.resolve("Ce se întâmplă la Taverna Veche?", facts))
        assertFalse(StoryFactResolver.isLookupQuestion("Ce se întâmplă în Satul Vecin?", facts))
        assertFalse(StoryFactResolver.isLookupQuestion("Cum e situația în clădirea vecină?", facts))
        assertNull(StoryFactResolver.resolve("Ce se întâmplă cu Fierărie?", facts))
    }

    @Test
    fun `natural questions select the region place or both without treating recent events as active`() {
        val facts = StoryFactResolver.Facts(
            regionState = "shortage", placeState = "closed",
            regionDescription = "Lipsesc proviziile.", placeDescription = "Atelierul este închis.",
            recentEventTitles = listOf("Târgul s-a deschis"),
        )

        assertEquals(
            "Starea curenta a regiunii: Lipsesc proviziile.",
            StoryFactResolver.resolve("Ce se întâmplă acum în sat?", facts),
        )
        assertEquals(
            "Starea curenta a regiunii: Lipsesc proviziile.",
            StoryFactResolver.resolve("Ce se întâmplă în sat acum?", facts),
        )
        assertEquals(
            "Starea curenta a locului: Atelierul este închis.",
            StoryFactResolver.resolve("Cum merg lucrurile în clădire?", facts),
        )
        assertEquals(
            "Starea curenta a regiunii: Lipsesc proviziile. Starea curenta a locului: Atelierul este închis.",
            StoryFactResolver.resolve("Cum e situația aici?", facts),
        )
        assertFalse(StoryFactResolver.resolve("Cum e situația aici?", facts)!!.contains("Târgul"))
        assertNull(StoryFactResolver.resolve("Ce se întâmplă în sat?", facts.copy(regionDescription = "")))
        assertEquals(
            "Starea curenta a locului: Atelierul este închis.",
            StoryFactResolver.resolve("Cum e situația aici?", facts.copy(regionDescription = "")),
        )
        assertNull(StoryFactResolver.resolve("Ce se întâmplă aici?", StoryFactResolver.Facts(regionState = "default")))
    }

    @Test
    fun `past future personal and special event questions do not receive current story state`() {
        val facts = StoryFactResolver.Facts(regionState = "shortage", regionDescription = "Lipsesc proviziile.")

        for (question in listOf(
            "Cum era situația în sat ieri?",
            "Cum va fi situația în sat mâine?",
            "De ce se întâmplă asta în sat?",
            "Ce se întâmplă cu tine aici?",
            "Ce se întâmplă la fierar?",
            "Ce evenimente speciale sunt acum în sat?",
        )) {
            assertFalse(StoryFactResolver.wantsCurrentState(question), question)
            assertNull(StoryFactResolver.resolve(question, facts), question)
        }
        assertEquals(
            "Starea curenta a regiunii: Lipsesc proviziile.",
            StoryFactResolver.resolve("Ce se petrece în regiune?", facts),
        )
    }

    @Test
    fun `descriptions are spoken only for the requested current state and keep punctuation`() {
        val facts = StoryFactResolver.Facts(
            regionState = "shortage", placeState = "closed",
            regionDescription = "Lipsesc proviziile, așteptăm livrările.",
            placeDescription = "Atelierul este închis",
        )

        assertEquals(
            "Starea curenta a regiunii: Lipsesc proviziile, așteptăm livrările.",
            StoryFactResolver.resolve("Care este starea regiunii?", facts),
        )
        assertEquals(
            "Starea curenta a locului: Atelierul este închis.",
            StoryFactResolver.resolve("Care este starea clădirii?", facts),
        )
        assertNull(StoryFactResolver.resolve("Ce s-a întâmplat aici?", facts))
        assertNull(StoryFactResolver.resolve("Ce evenimente au fost?", facts))
        assertEquals(
            "Starea curenta a locului este closed.",
            StoryFactResolver.resolve("Care este starea locului?", facts.copy(placeDescription = "")),
        )
    }

    @Test
    fun `orphan semantic descriptions do not invent a current state or history`() {
        val facts = StoryFactResolver.fromSemanticLines(listOf(
            "- region_story_description=Lipsesc proviziile.",
            "- place_story_description=Atelierul este închis.",
        ))

        assertNull(StoryFactResolver.resolve("Care este starea poveștii?", facts))
        assertNull(StoryFactResolver.resolve("Ce s-a întâmplat aici?", facts))
    }

    @Test
    fun `direct dialogue uses effective persistent state and only titled recent events`() {
        val region = WorldRegionInfo(
            "sat", "Sat", "world", "village", 0, 0, 0, 20, 100, 20,
            emptyList(), StoryMode.STATIC, "peaceful", listOf("peaceful"),
        )
        val place = WorldPlaceInfo(
            "atelier", "sat", "Atelier", "world", PlaceType.CUSTOM,
            0, 0, 0, 10, 100, 10, emptyList(), null, true,
            mapOf("story_state" to "open", "history" to "Atelierul a fost reconstruit"),
        )
        val worldContext = WorldContextSnapshot(region, place, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val persistedRegion = RegionStoryState("sat", StoryMode.ROTATIVE, "shortage", emptyList(), emptyMap(), 0, 0, "", "")
        val persistedPlace = PlaceStoryState("atelier", "sat", "closed", emptyMap(), 0, 0, "", "")
        val recentEvent = StoryEvent(
            1, "region", "sat", "sat", null, "festival", "market_day", "Targul s-a deschis",
            null, null, null, null, null, null, 0,
        )
        val facts = StoryFactResolver.fromWorldContext(worldContext, persistedRegion, persistedPlace, listOf(recentEvent))

        assertEquals("shortage", facts.regionState)
        assertEquals("closed", facts.placeState)
        assertEquals(
            "Starea curenta a regiunii este shortage. Starea curenta a locului este closed.",
            StoryFactResolver.resolve("Care este starea povestii aici?", facts),
        )
        assertEquals(
            "Istoria locului: Atelierul a fost reconstruit. Eveniment recent: Targul s-a deschis.",
            StoryFactResolver.resolve("Ce s-a intamplat aici?", facts),
        )
        assertEquals(
            "Starea curenta a regiunii este shortage. Starea curenta a locului este closed. " +
                "Istoria locului: Atelierul a fost reconstruit. Eveniment recent: Targul s-a deschis.",
            StoryFactResolver.resolve("Care este starea povestii si ce s-a intamplat aici?", facts),
        )

        val wrongRegion = RegionStoryState("alt_sat", StoryMode.ROTATIVE, "danger", emptyList(), emptyMap(), 0, 0, "", "")
        val wrongPlace = PlaceStoryState("alt_atelier", "sat", "closed", emptyMap(), 0, 0, "", "")
        val mappedFacts = StoryFactResolver.fromWorldContext(worldContext, wrongRegion, wrongPlace, emptyList())
        assertEquals("peaceful", mappedFacts.regionState)
        assertEquals("open", mappedFacts.placeState)
        assertEquals("Sat", mappedFacts.regionName)
        assertEquals("Atelier", mappedFacts.placeName)
    }

    @Test
    fun `semantic and direct sources use the same answer format`() {
        val facts = StoryFactResolver.fromSemanticLines(listOf(
            "- region_story_mode=evolutive, state=market_day, pool=market_day, source=persistent",
            "- place_story_state=forge_open, source=mapping",
            "- place_history=atelier, lore=<none>, history=Atelierul a fost reconstruit",
            "- recent_events:",
            "- region:sat market_open/market_day - Targul s-a deschis",
        ))

        assertEquals("market_day", facts.regionState)
        assertEquals("forge_open", facts.placeState)
        assertEquals("Starea curenta a locului este forge_open.", StoryFactResolver.resolve("Care este starea locului?", facts))
        assertEquals(
            "Eveniment recent: Targul s-a deschis.",
            StoryFactResolver.resolve("Ce evenimente au fost?", facts),
        )
        assertFalse(StoryFactResolver.isLookupQuestion("Ce evenimente sunt acum?"))
        assertFalse(StoryFactResolver.isLookupQuestion("Care este istoria fierarului?"))
        assertTrue(StoryFactResolver.isLookupQuestion("Ce evenimente au fost?"))
        assertTrue(StoryFactResolver.wantsCurrentState("Care este starea poveștii?"))
    }
}
