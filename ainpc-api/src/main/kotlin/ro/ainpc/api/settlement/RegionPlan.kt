package ro.ainpc.api.settlement

data class RegionPlan @JvmOverloads constructor(
    val regionId: String,
    val displayName: String,
    val worldName: String,
    val regionType: String,
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int,
    val centerX: Double,
    val centerY: Double,
    val centerZ: Double,
    val tags: Set<String> = emptySet(),
    val storySeed: String = "",
    val metadata: Map<String, String> = emptyMap()
)
