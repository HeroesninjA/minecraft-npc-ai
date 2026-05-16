package ro.ainpc.world

import java.util.LinkedHashSet
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

internal class MappingIndex {
    private val regionsByChunk: MutableMap<ChunkKey, MutableList<WorldRegion>> = HashMap()
    private val placesByChunk: MutableMap<ChunkKey, MutableList<WorldPlace>> = HashMap()
    private val nodesByChunk: MutableMap<ChunkKey, MutableList<WorldNode>> = HashMap()

    fun clear() {
        regionsByChunk.clear()
        placesByChunk.clear()
        nodesByChunk.clear()
    }

    fun indexRegion(region: WorldRegion) {
        forEachChunk(region.worldName, region.minX, region.minZ, region.maxX, region.maxZ) { key ->
            regionsByChunk.computeIfAbsent(key) { ArrayList() }.add(region)
        }
    }

    fun indexPlace(place: WorldPlace) {
        forEachChunk(place.worldName, place.minX, place.minZ, place.maxX, place.maxZ) { key ->
            placesByChunk.computeIfAbsent(key) { ArrayList() }.add(place)
        }
    }

    fun indexNode(node: WorldNode) {
        val minX = floor(node.x - node.radius).toInt()
        val minZ = floor(node.z - node.radius).toInt()
        val maxX = ceil(node.x + node.radius).toInt()
        val maxZ = ceil(node.z + node.radius).toInt()
        forEachChunk(node.worldName, minX, minZ, maxX, maxZ) { key ->
            nodesByChunk.computeIfAbsent(key) { ArrayList() }.add(node)
        }
    }

    fun findRegion(worldName: String?, x: Int, y: Int, z: Int): WorldRegion? {
        for (region in regionsByChunk[keyAt(worldName, x, z)] ?: emptyList()) {
            if (region.contains(worldName, x, y, z)) {
                return region
            }
        }
        return null
    }

    fun findPlace(worldName: String?, x: Int, y: Int, z: Int): WorldPlace? {
        for (place in placesByChunk[keyAt(worldName, x, z)] ?: emptyList()) {
            if (place.contains(worldName, x, y, z)) {
                return place
            }
        }
        return null
    }

    fun findNode(worldName: String?, x: Double, y: Double, z: Double): WorldNode? {
        val key = keyAt(worldName, floor(x).toInt(), floor(z).toInt())
        for (node in nodesByChunk[key] ?: emptyList()) {
            if (node.contains(worldName, x, y, z)) {
                return node
            }
        }
        return null
    }

    fun findNodesNear(worldName: String?, x: Double, y: Double, z: Double, radius: Double, limit: Int): List<WorldNode> {
        if (worldName == null || radius < 0.0 || limit == 0) {
            return emptyList()
        }

        val candidates = LinkedHashSet<WorldNode>()
        val minX = floor(x - radius).toInt()
        val minZ = floor(z - radius).toInt()
        val maxX = ceil(x + radius).toInt()
        val maxZ = ceil(z + radius).toInt()
        forEachChunk(worldName, minX, minZ, maxX, maxZ) { key ->
            candidates.addAll(nodesByChunk[key] ?: emptyList())
        }

        return candidates.asSequence()
            .filter { node -> node.isNear(worldName, x, y, z, radius + node.radius) }
            .sortedBy { node -> node.distanceSquared(worldName, x, y, z) }
            .let { sequence -> if (limit > 0) sequence.take(limit) else sequence }
            .toList()
    }

    fun indexedRegionChunks(): Int = regionsByChunk.size

    fun indexedPlaceChunks(): Int = placesByChunk.size

    fun indexedNodeChunks(): Int = nodesByChunk.size

    private fun forEachChunk(
        worldName: String?,
        minX: Int,
        minZ: Int,
        maxX: Int,
        maxZ: Int,
        consumer: (ChunkKey) -> Unit
    ) {
        val minChunkX = toChunk(minX)
        val maxChunkX = toChunk(maxX)
        val minChunkZ = toChunk(minZ)
        val maxChunkZ = toChunk(maxZ)

        for (chunkX in minChunkX..maxChunkX) {
            for (chunkZ in minChunkZ..maxChunkZ) {
                consumer(ChunkKey(normalizeWorld(worldName), chunkX, chunkZ))
            }
        }
    }

    private fun keyAt(worldName: String?, x: Int, z: Int): ChunkKey {
        return ChunkKey(normalizeWorld(worldName), toChunk(x), toChunk(z))
    }

    private fun toChunk(blockCoordinate: Int): Int = Math.floorDiv(blockCoordinate, 16)

    private fun normalizeWorld(worldName: String?): String {
        return worldName?.lowercase(Locale.ROOT).orEmpty()
    }

    private data class ChunkKey(
        val worldName: String,
        val chunkX: Int,
        val chunkZ: Int
    )
}
