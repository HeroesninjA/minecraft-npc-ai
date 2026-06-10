package ro.ainpc.world

import kotlin.math.max

class WorldNode(
    val id: String,
    val regionId: String,
    val placeId: String?,
    val type: WorldNodeType,
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val radius: Double
) {
    private val metadata: MutableMap<String, String> = LinkedHashMap()

    fun getMetadata(): Map<String, String> = metadata.toMap()

    fun putMetadata(key: String, value: String) {
        metadata[key] = value
    }

    fun contains(worldName: String?, x: Double, y: Double, z: Double): Boolean {
        return isNear(worldName, x, y, z, radius)
    }

    fun isNear(worldName: String?, x: Double, y: Double, z: Double, searchRadius: Double): Boolean {
        if (worldName == null || !this.worldName.equals(worldName, ignoreCase = true)) {
            return false
        }

        val effectiveRadius = max(0.0, searchRadius)
        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        return dx * dx + dy * dy + dz * dz <= effectiveRadius * effectiveRadius
    }

    fun distanceSquared(worldName: String?, x: Double, y: Double, z: Double): Double {
        if (worldName == null || !this.worldName.equals(worldName, ignoreCase = true)) {
            return Double.MAX_VALUE
        }

        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        return dx * dx + dy * dy + dz * dz
    }
}
