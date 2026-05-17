package ro.ainpc.world

class WorldPlaceInfo(
    private val id: String,
    private val regionId: String,
    private val displayName: String,
    private val worldName: String,
    placeType: PlaceType?,
    private val minX: Int,
    private val minY: Int,
    private val minZ: Int,
    private val maxX: Int,
    private val maxY: Int,
    private val maxZ: Int,
    tags: List<String>?,
    ownerNpcId: String?,
    private val publicAccess: Boolean,
    metadata: Map<String, String>?
) {
    private val placeType: PlaceType = placeType ?: PlaceType.CUSTOM
    private val tags: List<String> = (tags ?: emptyList()).toList()
    private val ownerNpcId: String = ownerNpcId ?: ""
    private val metadata: Map<String, String> = (metadata ?: emptyMap()).toMap()

    fun id(): String = id
    fun regionId(): String = regionId
    fun displayName(): String = displayName
    fun worldName(): String = worldName
    fun placeType(): PlaceType = placeType
    fun minX(): Int = minX
    fun minY(): Int = minY
    fun minZ(): Int = minZ
    fun maxX(): Int = maxX
    fun maxY(): Int = maxY
    fun maxZ(): Int = maxZ
    fun tags(): List<String> = tags
    fun ownerNpcId(): String = ownerNpcId
    fun publicAccess(): Boolean = publicAccess
    fun metadata(): Map<String, String> = metadata

    fun contains(worldName: String, x: Int, y: Int, z: Int): Boolean {
        return this.worldName.equals(worldName, ignoreCase = true)
            && x >= minX && x <= maxX
            && y >= minY && y <= maxY
            && z >= minZ && z <= maxZ
    }

    fun hasTag(tag: String?): Boolean {
        return tag != null && tags.any { existing -> existing.equals(tag, ignoreCase = true) }
    }
}
