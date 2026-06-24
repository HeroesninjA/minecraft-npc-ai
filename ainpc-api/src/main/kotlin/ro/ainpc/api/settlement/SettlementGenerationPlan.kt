package ro.ainpc.api.settlement

data class BuildingPlacementPlan @JvmOverloads constructor(
    val buildingKey: String,
    val templateId: String,
    val variantId: String = "",
    val placeId: String,
    val placeType: String,
    val centerX: Int,
    val centerY: Int,
    val centerZ: Int,
    val rotation: Int = 0,
    val metadata: Map<String, String> = emptyMap()
)

data class RoadPlacementPlan @JvmOverloads constructor(
    val roadId: String,
    val fromX: Int,
    val fromZ: Int,
    val toX: Int,
    val toZ: Int,
    val width: Int = 3,
    val material: String = "STONE_BRICKS",
    val metadata: Map<String, String> = emptyMap()
)

data class SettlementGenerationPlan @JvmOverloads constructor(
    val planId: String,
    val settlementId: String,
    val buildingPlans: List<BuildingPlacementPlan> = emptyList(),
    val roadPlans: List<RoadPlacementPlan> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) {
    fun buildingCount(): Int = buildingPlans.size
    fun roadCount(): Int = roadPlans.size
}
