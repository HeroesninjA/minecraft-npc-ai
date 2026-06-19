package ro.ainpc.world.exterior

class ExteriorStructurePlan(
    type: ExteriorStructureType?,
    regionId: String?,
    displayName: String?,
    regionTypeHint: String?,
    tags: List<String>?,
    plannedPlaces: List<String>?,
    plannedNodes: List<String>?,
    warnings: List<String>?
) {
    private val type: ExteriorStructureType = type ?: ExteriorStructureType.CUSTOM
    private val regionId: String = regionId?.trim().orEmpty()
    private val displayName: String = displayName?.trim().orEmpty()
    private val regionTypeHint: String = regionTypeHint?.trim().orEmpty()
    private val tags: List<String> = (tags ?: emptyList()).toList()
    private val plannedPlaces: List<String> = (plannedPlaces ?: emptyList()).toList()
    private val plannedNodes: List<String> = (plannedNodes ?: emptyList()).toList()
    private val warnings: List<String> = (warnings ?: emptyList()).toList()

    fun type(): ExteriorStructureType = type

    fun typeId(): String = type.id()

    fun regionId(): String = regionId

    fun displayName(): String = displayName

    fun regionTypeHint(): String = regionTypeHint

    fun tags(): List<String> = tags

    fun plannedPlaces(): List<String> = plannedPlaces

    fun plannedNodes(): List<String> = plannedNodes

    fun warnings(): List<String> = warnings
}
