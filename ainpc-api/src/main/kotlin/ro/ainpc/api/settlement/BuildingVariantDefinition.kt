package ro.ainpc.api.settlement

/**
 * Buildingvariantdefinition.
 */
data class BuildingVariantDefinition(
    val variantId: String,
    val displayName: String = "",
    val materialOverrides: Map<String, String> = emptyMap(),
    val minDifficulty: String = "easy",
    val biomeFilter: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)
