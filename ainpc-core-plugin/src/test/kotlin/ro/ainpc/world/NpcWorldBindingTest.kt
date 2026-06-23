package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class NpcWorldBindingTest {
    @Test
    fun fromResolvedAnchorsBuildsBindingFromSharedHelper() {
        val homePlace = WorldPlaceInfo(
            "demo_sat:house_1",
            "demo_sat",
            "Casa 1",
            "world",
            PlaceType.HOUSE,
            10, 60, 10,
            20, 70, 20
            ,
            listOf("home"),
            "npc_1",
            true,
            mapOf("purpose" to "home")
        )
        val socialPlace = WorldPlaceInfo(
            "demo_sat:piata",
            "demo_sat",
            "Piata",
            "world",
            PlaceType.MARKET,
            30, 60, 30,
            40, 70, 40
            ,
            listOf("social"),
            "",
            true,
            mapOf("purpose" to "social")
        )
        val homeNode = WorldNodeInfo(
            "demo_sat:house_1:bed_1",
            "demo_sat",
            "demo_sat:house_1",
            WorldNodeType.BED.id,
            "world",
            12.5,
            61.0,
            12.5,
            2.0,
            mapOf("role" to "bed")
        )
        val socialNode = WorldNodeInfo(
            "demo_sat:piata:meeting_point_1",
            "demo_sat",
            "demo_sat:piata",
            WorldNodeType.MEETING_POINT.id,
            "world",
            33.5,
            61.0,
            33.5,
            3.0,
            mapOf("role" to "meeting")
        )

        val binding = NpcWorldBinding.fromResolvedAnchors(
            42,
            "uuid-42",
            "Ion",
            homePlace,
            null,
            socialPlace,
            homeNode,
            null,
            socialNode,
            "profile_backfill"
        )

        assertEquals(42, binding?.npcId())
        assertEquals("uuid-42", binding?.npcUuid())
        assertEquals("Ion", binding?.npcName())
        assertEquals("demo_sat:house_1", binding?.homePlaceId())
        assertEquals("demo_sat:piata", binding?.socialPlaceId())
        assertEquals("demo_sat:house_1:bed_1", binding?.homeNodeId())
        assertEquals("demo_sat:piata:meeting_point_1", binding?.socialNodeId())
        assertEquals("profile_backfill", binding?.source())
    }

    @Test
    fun fromResolvedAnchorsReturnsNullWhenNoPlacesExist() {
        val binding = NpcWorldBinding.fromResolvedAnchors(
            42,
            "uuid-42",
            "Ion",
            null,
            null,
            null,
            null,
            null,
            null,
            "profile_backfill"
        )

        assertNull(binding)
    }
}
