package ro.ainpc.api.settlement

data class BuildingTemplateDefinition(
    val templateId: String,
    val displayName: String = "",
    val placeType: String = "house",
    val footprintWidth: Int = 5,
    val footprintDepth: Int = 5,
    val footprintHeight: Int = 4,
    val anchors: List<BuildingAnchorDefinition> = emptyList(),
    val variants: List<BuildingVariantDefinition> = emptyList(),
    val supportedRotations: List<Int> = listOf(0),
    val requiredCapabilities: List<String> = emptyList(),
    val tags: Set<String> = emptySet(),
    val metadata: Map<String, String> = emptyMap()
) {
    fun anchorCount(): Int = anchors.size
    fun variantCount(): Int = variants.size
    fun footprintArea(): Int = footprintWidth * footprintDepth
}
