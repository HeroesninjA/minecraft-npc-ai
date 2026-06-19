package ro.ainpc.world.exterior

class ExteriorStructureBlueprint(
    type: ExteriorStructureType?,
    title: String?,
    summary: String?,
    regionTypeHint: String?,
    tags: List<String>?,
    requiredPlaces: List<String>?,
    recommendedPlaces: List<String>?,
    requiredNodes: List<String>?,
    recommendedNodes: List<String>?,
    rules: List<String>?
) {
    private val type: ExteriorStructureType = type ?: ExteriorStructureType.CUSTOM
    private val title: String = title?.trim().orEmpty()
    private val summary: String = summary?.trim().orEmpty()
    private val regionTypeHint: String = regionTypeHint?.trim().orEmpty()
    private val tags: List<String> = (tags ?: emptyList()).toList()
    private val requiredPlaces: List<String> = (requiredPlaces ?: emptyList()).toList()
    private val recommendedPlaces: List<String> = (recommendedPlaces ?: emptyList()).toList()
    private val requiredNodes: List<String> = (requiredNodes ?: emptyList()).toList()
    private val recommendedNodes: List<String> = (recommendedNodes ?: emptyList()).toList()
    private val rules: List<String> = (rules ?: emptyList()).toList()

    fun type(): ExteriorStructureType = type

    fun typeId(): String = type.id()

    fun title(): String = title

    fun summary(): String = summary

    fun regionTypeHint(): String = regionTypeHint

    fun tags(): List<String> = tags

    fun requiredPlaces(): List<String> = requiredPlaces

    fun recommendedPlaces(): List<String> = recommendedPlaces

    fun requiredNodes(): List<String> = requiredNodes

    fun recommendedNodes(): List<String> = recommendedNodes

    fun rules(): List<String> = rules
}
