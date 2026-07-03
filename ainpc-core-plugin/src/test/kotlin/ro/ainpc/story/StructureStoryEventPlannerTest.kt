package ro.ainpc.story

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldNode
import ro.ainpc.world.WorldNodeType
import ro.ainpc.world.WorldPlace

class StructureStoryEventPlannerTest {
    private val planner = StructureStoryEventPlanner()

    @Test
    fun placeVisitProducesProfessionEvent() {
        val place = WorldPlace(
            "smithy_north_01",
            "region_village",
            "Fieraria Nordului",
            "world",
            PlaceType.FORGE,
            0,
            0,
            0,
            10,
            10,
            10
        )
        place.putMetadata("quest_hook", "repair_tool")
        place.setTags(listOf("craft", "work"))

        val plan = planner.planPlaceVisit("region_village", place, null)

        assertNotNull(plan)
        assertEquals("place", plan!!.scopeType)
        assertEquals("smithy_north_01", plan.scopeId)
        assertEquals("structure_visit_profession", plan.eventType)
        assertEquals("repair_tool", plan.eventKey)
        assertEquals("Vizită la Fieraria Nordului", plan.title)
        assertEquals("profession", plan.payload["structure_category"])
        assertEquals("forge", plan.payload["place_type"])
        assertEquals("repair_tool", plan.payload["place_quest_hook"])
    }

    @Test
    fun nodeInteractionProducesQuestEvent() {
        val place = WorldPlace(
            "town_market",
            "region_village",
            "Piața",
            "world",
            PlaceType.MARKET,
            0,
            0,
            0,
            12,
            12,
            12
        )
        place.putMetadata("quest_hook", "local_notice")

        val node = WorldNode(
            "market_board",
            "region_village",
            "town_market",
            WorldNodeType.QUEST_TRIGGER,
            "world",
            5.0,
            1.0,
            5.0,
            1.5
        )
        node.putMetadata("quest_hook", "local_notice")

        val plan = planner.planNodeInteraction("region_village", place, node)

        assertNotNull(plan)
        assertEquals("place", plan!!.scopeType)
        assertEquals("town_market", plan.scopeId)
        assertEquals("structure_node_quest", plan.eventType)
        assertEquals("local_notice", plan.eventKey)
        assertTrue(plan.payload["interaction"] == "node_interaction")
        assertTrue(plan.payload["node_id"] == "market_board")
        assertTrue(plan.payload["node_type"] == "quest_trigger")
    }
}
