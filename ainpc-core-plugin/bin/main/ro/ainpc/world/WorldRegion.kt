package ro.ainpc.world

class WorldRegion(
    val id: String,
    val name: String,
    val worldName: String,
    val type: RegionType,
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
    var storyState: StoryState = StoryState(StoryMode.EVOLUTIVE, "default")

    fun getTags(): List<String> = tags.toList()

    fun setTags(tags: List<String>?) {
        this.tags.clear()
        if (tags != null) {
            this.tags.addAll(tags)
        }
    }

    fun contains(worldName: String?, x: Int, y: Int, z: Int): Boolean {
        return this.worldName.equals(worldName, ignoreCase = true) &&
            x >= minX && x <= maxX &&
            y >= minY && y <= maxY &&
            z >= minZ && z <= maxZ
    }
}
