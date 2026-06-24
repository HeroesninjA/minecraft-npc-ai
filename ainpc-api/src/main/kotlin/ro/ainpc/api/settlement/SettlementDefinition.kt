package ro.ainpc.api.settlement

data class SettlementDefinition @JvmOverloads constructor(
    val id: String,
    val worldName: String,
    val centerX: Int,
    val centerY: Int,
    val centerZ: Int,
    val radius: Int,
    val profileId: String = "compact",
    val regionType: String = "village",
    val themeId: String = "medieval",
    val displayName: String = "",
    val tags: Set<String> = emptySet(),
    val metadata: Map<String, String> = emptyMap()
) {
    fun resolvedProfile(): SettlementLayoutProfile {
        return when (profileId.lowercase()) {
            "spacious" -> SettlementLayoutProfile.spacious()
            "rural" -> SettlementLayoutProfile.rural()
            "fortified" -> SettlementLayoutProfile.fortified()
            else -> SettlementLayoutProfile.compact()
        }
    }
}
