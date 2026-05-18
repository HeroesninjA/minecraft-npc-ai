package ro.ainpc.world

class WorldPlace(
    val id: String,
    val regionId: String,
    val displayName: String,
    val worldName: String,
    val placeType: PlaceType,
    minX: Int,
    minY: Int,
    minZ: Int,
    maxX: Int,
    maxY: Int,
    maxZ: Int
) {
    val minX: Int = minOf(minX, maxX)
    val minY: Int = minOf(minY, maxY)
    val minZ: Int = minOf(minZ, maxZ)
    val maxX: Int = maxOf(minX, maxX)
    val maxY: Int = maxOf(minY, maxY)
    val maxZ: Int = maxOf(minZ, maxZ)

    private val tags: MutableList<String> = ArrayList()
    private val metadata: MutableMap<String, String> = LinkedHashMap()

    private var ownerNpcId: String = ""
    var isPublicAccess: Boolean = true

    fun getOwnerNpcId(): String = ownerNpcId

    fun setOwnerNpcId(ownerNpcId: String?) {
        this.ownerNpcId = ownerNpcId ?: ""
    }

    fun getTags(): List<String> = tags.toList()

    fun setTags(tags: List<String>?) {
        this.tags.clear()
        if (tags != null) {
            this.tags.addAll(tags)
        }
    }

    fun getMetadata(): Map<String, String> = metadata.toMap()

    fun putMetadata(key: String, value: String) {
        metadata[key] = value
    }

    fun contains(worldName: String?, x: Int, y: Int, z: Int): Boolean {
        return this.worldName.equals(worldName, ignoreCase = true) &&
            x >= minX && x <= maxX &&
            y >= minY && y <= maxY &&
            z >= minZ && z <= maxZ
    }

    fun hasTag(tag: String?): Boolean {
        return tag != null && tags.any { existing -> existing.equals(tag, ignoreCase = true) }
    }
}
