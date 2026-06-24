package ro.ainpc.api.settlement

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettlementGenerationPlanTest {

    @Test
    fun buildingPlacementPlanDefaults() {
        val plan = BuildingPlacementPlan(
            buildingKey = "house_01",
            templateId = "house_small",
            placeId = "demo_sat:house_01",
            placeType = "house",
            centerX = 0, centerY = 64, centerZ = 0
        )
        assertEquals("house_01", plan.buildingKey)
        assertEquals(0, plan.rotation)
        assertTrue(plan.variantId.isEmpty())
    }

    @Test
    fun roadPlacementPlanDefaults() {
        val road = RoadPlacementPlan(
            roadId = "road_main",
            fromX = 0, fromZ = 0, toX = 50, toZ = 0
        )
        assertEquals(3, road.width)
        assertEquals("STONE_BRICKS", road.material)
    }

    @Test
    fun settlementGenerationPlanEmpty() {
        val plan = SettlementGenerationPlan(
            planId = "test_plan",
            settlementId = "demo_sat"
        )
        assertEquals(0, plan.buildingCount())
        assertEquals(0, plan.roadCount())
    }

    @Test
    fun settlementGenerationPlanWithData() {
        val buildings = listOf(
            BuildingPlacementPlan("house_01", "house_small", "", "demo_sat:house_01", "house", 0, 64, 0),
            BuildingPlacementPlan("forge_01", "forge", "", "demo_sat:forge_01", "forge", 30, 64, -20)
        )
        val roads = listOf(
            RoadPlacementPlan("road_1", 0, 0, 30, -20, 3)
        )
        val plan = SettlementGenerationPlan("plan_1", "demo_sat", buildings, roads)
        assertEquals(2, plan.buildingCount())
        assertEquals(1, plan.roadCount())
    }
}
