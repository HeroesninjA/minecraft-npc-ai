package ro.ainpc.api.settlement

data class BuildingPlan(
    val buildingKey: String,
    val placeId: String,
    val displayName: String,
    val placeType: String,
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int,
    val entranceX: Double = 0.0,
    val entranceY: Double = 0.0,
    val entranceZ: Double = 0.0,
    val capacity: Int = 1,
    val templateId: String = "",
    val buildMode: BuildMode = BuildMode.SEMANTIC_ONLY,
    val tags: Set<String> = emptySet(),
    val requiredNodes: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)
