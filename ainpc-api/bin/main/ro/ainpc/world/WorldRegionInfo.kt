package ro.ainpc.world

class WorldRegionInfo(
    private val id: String,
    private val name: String,
    private val worldName: String,
    typeId: String?,
    private val minX: Int,
    private val minY: Int,
    private val minZ: Int,
    private val maxX: Int,
    private val maxY: Int,
    private val maxZ: Int,
    tags: List<String>?,
    storyMode: StoryMode?,
    storyStateKey: String?,
    storyPool: List<String>?
) {
    private val tags: List<String> = (tags ?: emptyList()).toList()
    private val storyMode: StoryMode = storyMode ?: StoryMode.EVOLUTIVE
    private val storyStateKey: String = storyStateKey ?: "default"
    private val storyPool: List<String> = (storyPool ?: emptyList()).toList()
    private val typeId: String = typeId ?: "custom"

    fun id(): String = id
    fun name(): String = name
    fun worldName(): String = worldName
    fun typeId(): String = typeId
    fun minX(): Int = minX
    fun minY(): Int = minY
    fun minZ(): Int = minZ
    fun maxX(): Int = maxX
    fun maxY(): Int = maxY
    fun maxZ(): Int = maxZ
    fun tags(): List<String> = tags
    fun storyMode(): StoryMode = storyMode
    fun storyStateKey(): String = storyStateKey
    fun storyPool(): List<String> = storyPool

    fun contains(worldName: String, x: Int, y: Int, z: Int): Boolean {
        return this.worldName.equals(worldName, ignoreCase = true)
            && x >= minX && x <= maxX
            && y >= minY && y <= maxY
            && z >= minZ && z <= maxZ
    }
}
