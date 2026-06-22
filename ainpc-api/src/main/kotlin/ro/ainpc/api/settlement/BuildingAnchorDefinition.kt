package ro.ainpc.api.settlement

data class BuildingAnchorDefinition(
    val anchorId: String,
    val nodeType: String,
    val offsetX: Int,
    val offsetY: Int,
    val offsetZ: Int,
    val radius: Double = 2.0,
    val role: String = "",
    val tags: Set<String> = emptySet(),
    val metadata: Map<String, String> = emptyMap()
)
