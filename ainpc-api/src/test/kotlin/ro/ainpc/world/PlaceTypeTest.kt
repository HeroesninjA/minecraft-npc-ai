package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlaceTypeTest {

    @Test
    fun englishIds() {
        assertEquals(PlaceType.HOUSE, PlaceType.fromId("house"))
        assertEquals(PlaceType.SHOP, PlaceType.fromId("shop"))
        assertEquals(PlaceType.FORGE, PlaceType.fromId("forge"))
        assertEquals(PlaceType.TAVERN, PlaceType.fromId("tavern"))
        assertEquals(PlaceType.FARM, PlaceType.fromId("farm"))
        assertEquals(PlaceType.MARKET, PlaceType.fromId("market"))
    }

    @Test
    fun romanianAliases() {
        assertEquals(PlaceType.HOUSE, PlaceType.fromId("casa"))
        assertEquals(PlaceType.HOUSE, PlaceType.fromId("locuinta"))
        assertEquals(PlaceType.SHOP, PlaceType.fromId("magazin"))
        assertEquals(PlaceType.SHOP, PlaceType.fromId("comert"))
        assertEquals(PlaceType.FORGE, PlaceType.fromId("fierarie"))
        assertEquals(PlaceType.FORGE, PlaceType.fromId("atelier"))
        assertEquals(PlaceType.TAVERN, PlaceType.fromId("taverna"))
        assertEquals(PlaceType.TAVERN, PlaceType.fromId("han"))
        assertEquals(PlaceType.FARM, PlaceType.fromId("ferma"))
        assertEquals(PlaceType.MARKET, PlaceType.fromId("piata"))
        assertEquals(PlaceType.CASTLE_ROOM, PlaceType.fromId("castel"))
        assertEquals(PlaceType.CAVE_ROOM, PlaceType.fromId("pestera"))
        assertEquals(PlaceType.CAMP, PlaceType.fromId("tabara"))
    }

    @Test
    fun unknownReturnsCustom() {
        assertEquals(PlaceType.CUSTOM, PlaceType.fromId("unknown_type"))
        assertEquals(PlaceType.CUSTOM, PlaceType.fromId(null))
        assertEquals(PlaceType.CUSTOM, PlaceType.fromId(""))
    }

    @Test
    fun hyphenNormalization() {
        assertEquals(PlaceType.CASTLE_ROOM, PlaceType.fromId("castle-room"))
        assertEquals(PlaceType.CAVE_ROOM, PlaceType.fromId("cave-room"))
    }
}
