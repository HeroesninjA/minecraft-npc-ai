package ro.ainpc.spawn

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo

class SpawnSemanticRulesTest {
    @Test
    fun classifiesCommonPlaceTypesAndMetadata() {
        val house = place("demo:house", PlaceType.HOUSE, tags = listOf("home"))
        val forge = place("demo:forge", PlaceType.FORGE)
        val market = place("demo:market", PlaceType.MARKET)

        assertTrue(SpawnSemanticRules.isHousePlace(house))
        assertTrue(SpawnSemanticRules.isWorkplace(forge))
        assertTrue(SpawnSemanticRules.isSocialPlace(market))
        assertEquals(3, SpawnSemanticRules.parsePositiveIntMetadata(place("demo:house", PlaceType.HOUSE, metadata = mapOf("capacity" to "3")), "capacity"))
    }

    @Test
    fun selectsBestNodeByAnchorRoleAndDistance() {
        val house = place("demo:house", PlaceType.HOUSE)
        val bed = node("demo:house:bed", "demo:house", "bed", 2.0, 61.0, 2.0)
        val entry = node("demo:house:entry", "demo:house", "interaction", 3.0, 61.0, 3.0)
        val unrelated = node("demo:house:torch", "demo:house", "torch", 1.0, 61.0, 1.0)

        assertEquals(bed, SpawnSemanticRules.bestNodeForPlace(house, listOf(unrelated, entry, bed), "home"))
        assertEquals(entry, SpawnSemanticRules.bestNodeForPlace(house, listOf(unrelated, entry, bed), "social"))
        assertNull(SpawnSemanticRules.bestNodeForPlace(house, listOf(unrelated), "work"))
    }

    private fun place(
        id: String,
        placeType: PlaceType,
        tags: List<String> = emptyList(),
        metadata: Map<String, String> = emptyMap()
    ): WorldPlaceInfo =
        WorldPlaceInfo(
            id,
            "demo",
            id.substringAfter(':'),
            "world",
            placeType,
            0, 0, 0,
            10, 10, 10,
            tags,
            "",
            true,
            metadata
        )

    private fun node(
        id: String,
        placeId: String,
        typeId: String,
        x: Double,
        y: Double,
        z: Double,
        metadata: Map<String, String> = emptyMap()
    ): WorldNodeInfo =
        WorldNodeInfo(
            id,
            "demo",
            placeId,
            typeId,
            "world",
            x,
            y,
            z,
            1.5,
            metadata
        )
}
