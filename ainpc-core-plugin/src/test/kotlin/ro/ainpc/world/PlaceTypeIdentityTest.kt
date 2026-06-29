package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlaceTypeIdentityTest {

    @Test
    fun everyPlaceTypeHasIdentity() {
        for (type in PlaceType.values()) {
            assertTrue(type.displayName.isNotBlank(), "displayName must not be blank for ${type.id}")
            assertTrue(type.description.isNotBlank(), "description must not be blank for ${type.id}")
            assertNotNull(type.tags, "tags must not be null for ${type.id}")
        }
    }

    @Test
    fun houseHasHomeTag() {
        val house = PlaceType.HOUSE
        assertTrue(house.tags.contains("home"))
        assertTrue(house.tags.contains("residence"))
    }

    @Test
    fun marketHasPublicAndSocialTags() {
        val market = PlaceType.MARKET
        assertTrue(market.tags.contains("public"))
        assertTrue(market.tags.contains("social"))
        assertTrue(market.tags.contains("trade"))
    }

    @Test
    fun fromIdSupportsRomanianAliases() {
        assertEquals(PlaceType.HOUSE, PlaceType.fromId("casa"))
        assertEquals(PlaceType.HOUSE, PlaceType.fromId("locuinta"))
        assertEquals(PlaceType.SHOP, PlaceType.fromId("magazin"))
        assertEquals(PlaceType.FORGE, PlaceType.fromId("fierarie"))
        assertEquals(PlaceType.TAVERN, PlaceType.fromId("taverna"))
        assertEquals(PlaceType.FARM, PlaceType.fromId("ferma"))
        assertEquals(PlaceType.MARKET, PlaceType.fromId("piata"))
        assertEquals(PlaceType.CASTLE_ROOM, PlaceType.fromId("castel"))
        assertEquals(PlaceType.CAVE_ROOM, PlaceType.fromId("pestera"))
    }

    @Test
    fun unknownTypeReturnsCustom() {
        assertEquals(PlaceType.CUSTOM, PlaceType.fromId("nonexistent"))
        assertEquals(PlaceType.CUSTOM, PlaceType.fromId(""))
        assertEquals(PlaceType.CUSTOM, PlaceType.fromId(null))
    }
}
