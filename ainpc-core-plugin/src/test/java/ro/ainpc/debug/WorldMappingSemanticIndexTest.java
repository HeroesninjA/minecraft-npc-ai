package ro.ainpc.debug;

import org.junit.jupiter.api.Test;
import ro.ainpc.world.PlaceType;
import ro.ainpc.world.StoryMode;
import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegionInfo;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldMappingSemanticIndexTest {

    @Test
    void indexesMarketPlaceAndQuestBoardNodeForMappedContracts() {
        WorldRegionInfo region = new WorldRegionInfo(
            "demo_sat",
            "Demo Sat",
            "world",
            "settlement",
            0,
            50,
            0,
            100,
            90,
            100,
            List.of("village", "public"),
            StoryMode.EVOLUTIVE,
            "default",
            List.of()
        );
        WorldPlaceInfo market = new WorldPlaceInfo(
            "demo_sat:piata",
            "demo_sat",
            "Piata satului",
            "world",
            PlaceType.MARKET,
            20,
            60,
            20,
            40,
            75,
            40,
            List.of("market", "public", "social"),
            "",
            true,
            Map.of("role", "social")
        );
        WorldNodeInfo board = new WorldNodeInfo(
            "demo_sat:piata:quest_board",
            "demo_sat",
            "demo_sat:piata",
            "quest_trigger",
            "world",
            30.0,
            65.0,
            30.0,
            2.0,
            Map.of("semantic", "quest_board")
        );

        WorldMappingSemanticIndex index = WorldMappingSemanticIndex.from(
            List.of(region),
            List.of(market),
            List.of(board)
        );

        assertEquals(List.of("demo_sat:piata"), index.placeTags().get("market"));
        assertEquals(List.of("demo_sat:piata"), index.placeTypes().get("market"));
        assertEquals(List.of("demo_sat:piata"), index.placeCandidates().get("market"));
        assertEquals(List.of("demo_sat:piata:quest_board"), index.nodeTypes().get("quest_trigger"));
        assertEquals(List.of("demo_sat:piata:quest_board"), index.nodeMetadataValues().get("semantic_quest_board"));
        assertEquals(List.of("demo_sat:piata:quest_board"), index.nodeCandidates().get("quest_board"));
        assertTrue(index.regionCandidates().get("settlement").contains("demo_sat"));
        assertTrue(index.hasReference("place", "tag:market"));
        assertTrue(index.hasReference("node", "quest_board"));
        assertEquals(List.of("demo_sat:piata"), index.matchingIds("place", "type:market"));
        assertEquals(List.of("demo_sat:piata:quest_board"), index.matchingIds("node", "type:quest_trigger"));
        assertTrue(index.matchingIds("place", "tag:castle").isEmpty());
    }
}
