package ro.ainpc.routine

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Waterlogged
import org.bukkit.entity.Entity
import org.bukkit.util.BoundingBox

internal object RoutineTeleport {
    private const val EPSILON = 0.000001
    private const val MAX_CELLS = 512L
    private val hazards = setOf(
        Material.FIRE, Material.SOUL_FIRE, Material.LAVA, Material.WATER, Material.BUBBLE_COLUMN,
        Material.CACTUS, Material.MAGMA_BLOCK, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
        Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE, Material.POWDER_SNOW,
        Material.NETHER_PORTAL, Material.END_PORTAL, Material.END_GATEWAY,
    )

    fun tryTeleport(entity: Entity?, target: Location): Boolean {
        if (entity == null || !entity.isValid || !validLocation(target)) return false
        val current = entity.location
        if (!validLocation(current)) return false
        val originalBody = entity.boundingBox
        if (!validBox(originalBody)) return false
        val body = originalBody.clone().shift(target.x - current.x, target.y - current.y, target.z - current.z)
        if (!validBox(body)) return false
        val world = target.world ?: return false
        val minX = floor(body.minX).toInt()
        val maxX = ceil(body.maxX).toInt() - 1
        val minZ = floor(body.minZ).toInt()
        val maxZ = ceil(body.maxZ).toInt() - 1
        val groundY = floor(body.minY - EPSILON).toInt()
        val maxY = ceil(body.maxY).toInt() - 1
        if (groundY < world.minHeight || body.maxY > world.maxHeight) return false
        val cells = (maxX - minX + 1L) * (maxZ - minZ + 1L) * (maxY - groundY + 1L)
        if (cells !in 1L..MAX_CELLS) return false
        for (chunkX in ((minX - 1) shr 4)..((maxX + 1) shr 4)) {
            for (chunkZ in ((minZ - 1) shr 4)..((maxZ + 1) shr 4)) {
                if (!world.isChunkLoaded(chunkX, chunkZ)) return false
            }
        }
        for (blockX in minX..maxX) {
            for (blockZ in minZ..maxZ) {
                for (blockY in groundY..maxY) {
                    val block = world.getBlockAt(blockX, blockY, blockZ)
                    if (unsafeBlock(block)) return false
                    if (blockY == groundY && !supports(block, blockX, groundY, blockZ, body)) return false
                }
            }
        }
        if (world.hasCollisionsIn(body)) return false
        return entity.teleport(target)
    }

    private fun supports(block: Block, blockX: Int, blockY: Int, blockZ: Int, body: BoundingBox): Boolean =
        block.collisionShape.boundingBoxes.any { shape ->
            shape.height > 0.0 && abs(blockY + shape.maxY - body.minY) <= EPSILON &&
                blockX + shape.minX <= maxOf(body.minX, blockX.toDouble()) + EPSILON &&
                blockX + shape.maxX >= minOf(body.maxX, blockX + 1.0) - EPSILON &&
                blockZ + shape.minZ <= maxOf(body.minZ, blockZ.toDouble()) + EPSILON &&
                blockZ + shape.maxZ >= minOf(body.maxZ, blockZ + 1.0) - EPSILON
        }

    private fun unsafeBlock(block: Block): Boolean =
        block.isLiquid || block.type in hazards || (block.blockData as? Waterlogged)?.isWaterlogged == true

    private fun validLocation(location: Location): Boolean =
        location.world != null && listOf(location.x, location.y, location.z).all {
            it.isFinite() && it > Int.MIN_VALUE + 2.0 && it < Int.MAX_VALUE - 2.0
        } &&
            location.yaw.isFinite() && location.pitch.isFinite()

    private fun validBox(body: BoundingBox): Boolean =
        listOf(body.minX, body.minY, body.minZ, body.maxX, body.maxY, body.maxZ).all {
            it.isFinite() && it > Int.MIN_VALUE + 2.0 && it < Int.MAX_VALUE - 2.0
        } && body.widthX > 0.0 && body.widthX <= 16.0 && body.widthZ > 0.0 && body.widthZ <= 16.0 &&
            body.height > 0.0 && body.height <= 16.0
}
