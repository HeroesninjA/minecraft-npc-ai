package ro.ainpc.world.exterior

class ExteriorStructureReport(
    regionId: String?,
    regionName: String?,
    type: ExteriorStructureType?,
    places: List<String>?,
    nodes: List<String>?,
    entryNodes: List<String>?,
    interactionNodes: List<String>?,
    warnings: List<String>?,
    errors: List<String>?
) {
    private val regionId: String = regionId?.trim().orEmpty()
    private val regionName: String = regionName?.trim().orEmpty()
    private val type: ExteriorStructureType = type ?: ExteriorStructureType.CUSTOM
    private val places: List<String> = (places ?: emptyList()).toList()
    private val nodes: List<String> = (nodes ?: emptyList()).toList()
    private val entryNodes: List<String> = (entryNodes ?: emptyList()).toList()
    private val interactionNodes: List<String> = (interactionNodes ?: emptyList()).toList()
    private val warnings: List<String> = (warnings ?: emptyList()).toList()
    private val errors: List<String> = (errors ?: emptyList()).toList()

    fun regionId(): String = regionId

    fun regionName(): String = regionName

    fun type(): ExteriorStructureType = type

    fun typeId(): String = type.id()

    fun places(): List<String> = places

    fun nodes(): List<String> = nodes

    fun entryNodes(): List<String> = entryNodes

    fun interactionNodes(): List<String> = interactionNodes

    fun warnings(): List<String> = warnings

    fun errors(): List<String> = errors

    fun success(): Boolean = errors.isEmpty()
}
