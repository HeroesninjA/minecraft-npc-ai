package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.world.PlaceType
import ro.ainpc.world.StoryMode
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo

class WorldMappingSemanticIndexTest {
    @Test
    fun indexesMarketPlaceAndQuestBoardNodeForMappedContracts() {
        val region = WorldRegionInfo(
            "demo_sat", "Demo Sat", "world", "settlement",
            0, 50, 0, 100, 90, 100,
            listOf("village", "public"), StoryMode.EVOLUTIVE, "default", listOf()
        )
        val market = WorldPlaceInfo(
            "demo_sat:piata", "demo_sat", "Piata satului", "world", PlaceType.MARKET,
            20, 60, 20, 40, 75, 40,
            listOf("market", "public", "social"), "", true, mapOf("role" to "social")
        )
        val board = WorldNodeInfo(
            "demo_sat:piata:quest_board", "demo_sat", "demo_sat:piata", "quest_trigger", "world",
            30.0, 65.0, 30.0, 2.0, mapOf("semantic" to "quest_board")
        )

        val index = WorldMappingSemanticIndex.from(listOf(region), listOf(market), listOf(board))

        assertEquals(listOf("demo_sat:piata"), index.placeTags()["market"])
        assertEquals(listOf("demo_sat:piata"), index.placeTypes()["market"])
        assertEquals(listOf("demo_sat:piata"), index.placeCandidates()["market"])
        assertEquals(listOf("demo_sat:piata:quest_board"), index.nodeTypes()["quest_trigger"])
        assertEquals(listOf("demo_sat:piata:quest_board"), index.nodeMetadataValues()["semantic_quest_board"])
        assertEquals(listOf("demo_sat:piata:quest_board"), index.nodeCandidates()["quest_board"])
        assertTrue(index.regionCandidates()["settlement"]!!.contains("demo_sat"))
        assertTrue(index.hasReference("place", "tag:market"))
        assertTrue(index.hasReference("node", "quest_board"))
        assertEquals(listOf("demo_sat:piata"), index.matchingIds("place", "type:market"))
        assertEquals(listOf("demo_sat:piata:quest_board"), index.matchingIds("node", "type:quest_trigger"))
        assertTrue(index.matchingIds("place", "tag:castle").isEmpty())
    }
}
