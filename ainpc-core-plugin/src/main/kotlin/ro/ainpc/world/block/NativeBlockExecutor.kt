package ro.ainpc.world.block

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import java.util.UUID

class NativeBlockExecutor : BlockExecutor {
    override fun placeBlock(world: World, x: Int, y: Int, z: Int, material: Material): BlockOperationResult {
        return placeBlock(world, x, y, z, material.createBlockData())
    }

    override fun placeBlock(world: World, x: Int, y: Int, z: Int, blockData: BlockData): BlockOperationResult {
        val block = world.getBlockAt(x, y, z)
        return try {
            block.setBlockData(blockData, true)
            val snapshot = BlockSnapshot(
                worldName = world.name,
                x = x,
                y = y,
                z = z,
                material = blockData.material,
                blockData = blockData.clone()
            )
            BlockOperationResult.success(snapshot)
        } catch (e: Exception) {
            BlockOperationResult.failure("Failed to place block at $x,$y,$z: ${e.message}")
        }
    }

    override fun placeBlocks(world: World, operations: List<BlockOperation>): BlockBatchResult {
        val successful = mutableListOf<BlockSnapshot>()
        val failed = mutableListOf<Pair<BlockOperation, String>>()

        for (op in operations) {
            val result = if (op.blockData == null) {
                placeBlock(world, op.x, op.y, op.z, op.material)
            } else {
                placeBlock(world, op.x, op.y, op.z, op.blockData!!)
            }
            if (result.success) {
                result.snapshot?.let { successful.add(it) }
            } else {
                failed.add(op to (result.error ?: "Unknown error"))
            }
        }

        return BlockBatchResult(successful, failed)
    }

    override fun getBlock(world: World, x: Int, y: Int, z: Int): BlockSnapshot? {
        val block = world.getBlockAt(x, y, z)
        return if (block.type != Material.AIR) {
            BlockSnapshot(
                worldName = world.name,
                x = x,
                y = y,
                z = z,
                material = block.type,
                blockData = block.blockData.clone()
            )
        } else null
    }

    override fun supportsPreview(): Boolean = false

    override fun createPreview(operations: List<BlockOperation>): PreviewSession? = null

    override fun commitPreview(session: PreviewSession): BlockBatchResult = BlockBatchResult(emptyList(), emptyList())

    override fun rollbackPreview(session: PreviewSession): Boolean = false

    override fun getExecutorId(): String = "native"
}

class NativeBlockExecutorProvider : BlockExecutorProvider {
    override fun createExecutor(): BlockExecutor = NativeBlockExecutor()
    override fun getExecutorId(): String = "native"
    override fun isAvailable(): Boolean = true
    override fun getDescription(): String = "Native Bukkit API block placement (no dependencies)"
}

interface BlockExecutorProvider {
    fun createExecutor(): BlockExecutor
    fun getExecutorId(): String
    fun isAvailable(): Boolean
    fun getDescription(): String
}

object BlockExecutorRegistry {
    private val providers = mutableMapOf<String, BlockExecutorProvider>()
    private var currentExecutor: BlockExecutor? = null
    private var currentExecutorId: String = "native"

    fun register(provider: BlockExecutorProvider) {
        providers[provider.getExecutorId()] = provider
    }

    fun getExecutor(executorId: String? = null): BlockExecutor {
        val id = executorId ?: currentExecutorId
        return currentExecutor
            ?: providers[id]?.createExecutor()
            ?: providers["native"]?.createExecutor()
            ?: NativeBlockExecutor()
    }

    fun setExecutor(executorId: String): Boolean {
        return providers[executorId]?.let {
            currentExecutor = it.createExecutor()
            currentExecutorId = executorId
            true
        } ?: false
    }

    fun getAvailableExecutors(): List<Pair<String, String>> {
        return providers.entries.map { it.key to it.value.getDescription() }
    }

    fun isExecutorAvailable(executorId: String): Boolean = providers[executorId]?.isAvailable() ?: false
}

fun BlockExecutorProvider.register() {
    BlockExecutorRegistry.register(this)
}
