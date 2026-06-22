package ro.ainpc.api.settlement

data class NodePlan(
    val nodeId: String,
    val placeId: String,
    val nodeType: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val radius: Double = 2.0,
    val role: String = "",
    val tags: Set<String> = emptySet(),
    val metadata: Map<String, String> = emptyMap()
)
