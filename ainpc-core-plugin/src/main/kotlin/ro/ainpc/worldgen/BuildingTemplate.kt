package ro.ainpc.worldgen

import kotlinx.serialization.Serializable
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.util.Vector
import java.util.UUID

@Serializable
data class BuildingTemplate(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val version: Int,
    val blocks: List<TemplateBlock>,
    val anchors: Map<String, Vector> = emptyMap(),
    val metadata: Map<String, String> = emptyMap()
) {
    fun toBlockOperations(
        worldName: String,
        originX: Int,
        originY: Int,
        originZ: Int,
        rotation: Rotation = Rotation.NONE,
        mirror: Mirror = Mirror.NONE
    ): List<BlockOperation> {
        val operations = mutableListOf<BlockOperation>()

        for (block in blocks) {
            val rotatedBlocks = block.generateBlocks(originX, originY, originZ, rotation, mirror)
            for ((x, y, z, material, blockData) in rotatedBlocks) {
                operations.add(BlockOperation(
                    worldName = worldName,
                    x = x,
                    y = y,
                    z = z,
                    material = material,
                    blockData = blockData
                ))
            }
        }

        return operations
    }

    fun getAnchor(name: String, originX: Int, originY: Int, originZ: Int, rotation: Rotation = Rotation.NONE): Vector? {
        return anchors[name]?.let { anchor ->
            val rotated = rotation.apply(anchor)
            Vector(originX + rotated.x.toInt(), originY + rotated.y.toInt(), originZ + rotated.z.toInt())
        }
    }

    fun getBoundingBox(rotation: Rotation = Rotation.NONE): BoundingBox {
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var minZ = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var maxZ = Int.MIN_VALUE

        for (block in blocks) {
            val rotatedBlocks = block.generateBlocks(0, 0, 0, rotation, Mirror.NONE)
            for ((x, y, z, _, _) in rotatedBlocks) {
                minX = minOf(minX, x)
                minY = minOf(minY, y)
                minZ = minOf(minZ, z)
                maxX = maxOf(maxX, x)
                maxY = maxOf(maxY, y)
                maxZ = maxOf(maxZ, z)
            }
        }

        return BoundingBox(minX, minY, minZ, maxX, maxY, maxZ)
    }

    val blockCount: Int
        get() = blocks.sumOf { it.width * it.height * it.depth }
}

@Serializable
data class TemplateBlock(
    val x: Int,
    val y: Int,
    val z: Int,
    val material: Material,
    val width: Int = 1,
    val height: Int = 1,
    val depth: Int = 1,
    val blockData: BlockData? = null,
    val probability: Float = 1.0f,
    val conditions: List<String> = emptyList()
) {
    fun generateBlocks(
        originX: Int,
        originY: Int,
        originZ: Int,
        rotation: Rotation,
        mirror: Mirror
    ): List<Tuple5<Int, Int, Int, Material, BlockData?>> {
        val blocks = mutableListOf<Tuple5<Int, Int, Int, Material, BlockData?>>()
        val random = Random(originX.toLong() * 31 + originY.toLong() * 17 + originZ.toLong() * 13 + material.ordinal.toLong())

        for (dx in 0 until width) {
            for (dy in 0 until height) {
                for (dz in 0 until depth) {
                    if (probability < 1.0f && random.nextFloat() > probability) continue

                    var rx = x + dx
                    var ry = y + dy
                    var rz = z + dz

                    val rotated = rotation.apply(Vector(rx, ry, rz))
                    rx = rotated.x.toInt()
                    ry = rotated.y.toInt()
                    rz = rotated.z.toInt()

                    val mirrored = mirror.apply(Vector(rx, ry, rz))
                    rx = mirrored.x.toInt()
                    ry = mirrored.y.toInt()
                    rz = mirrored.z.toInt()

                    blocks.add(Tuple5(
                        originX + rx,
                        originY + ry,
                        originZ + rz,
                        material,
                        blockData
                    ))
                }
            }
        }
        return blocks
    }
}

@Serializable
enum class Rotation {
    NONE,
    CLOCKWISE_90,
    CLOCKWISE_180,
    CLOCKWISE_270
} {
    fun apply(vector: Vector): Vector {
        return when (this) {
            NONE -> vector
            CLOCKWISE_90 -> Vector(-vector.z, vector.y, vector.x)
            CLOCKWISE_180 -> Vector(-vector.x, vector.y, -vector.z)
            CLOCKWISE_270 -> Vector(vector.z, vector.y, -vector.x)
        }
    }
}

@Serializable
enum class Mirror {
    NONE,
    X,
    Z,
    XZ
} {
    fun apply(vector: Vector): Vector {
        return when (this) {
            NONE -> vector
            X -> Vector(-vector.x, vector.y, vector.z)
            Z -> Vector(vector.x, vector.y, -vector.z)
            XZ -> Vector(-vector.x, vector.y, -vector.z)
        }
    }
}

typealias Tuple5<A, B, C, D, E> = Tuple5<A, B, C, D, E>

@Serializable
data class Tuple5<out A, out B, out C, out D, out E>(
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E
)

import java.util.Random
