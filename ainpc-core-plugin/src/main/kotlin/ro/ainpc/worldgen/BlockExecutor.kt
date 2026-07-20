package ro.ainpc.worldgen

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.World
import java.io.File
import java.util.UUID

interface BlockExecutor {
    val executorId: String
    val displayName: String

    fun isAvailable(): Boolean
    fun getDescription(): String

    fun execute(operations: List<BlockOperation>): ExecutionResult
    fun executeWithPreview(operations: List<BlockOperation>): PreviewSession
    fun undo(sessionId: UUID): UndoResult
    fun saveSchematic(sessionId: UUID, file: File): Boolean
    fun loadSchematic(file: File): List<BlockOperation>
}

data class BlockOperation(
    val worldName: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val material: Material,
    val blockData: BlockData? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    fun toLocation(world: World) = org.bukkit.Location(world, x.toDouble(), y.toDouble(), z.toDouble())
}

data class ExecutionResult(
    val success: Boolean,
    val sessionId: UUID?,
    val blocksPlaced: Int,
    val blocksFailed: Int,
    val errors: List<String>,
    val warnings: List<String> = emptyList()
) {
    companion object {
        fun success(sessionId: UUID, placed: Int): ExecutionResult = ExecutionResult(true, sessionId, placed, 0, emptyList())
        fun failure(errors: List<String>): ExecutionResult = ExecutionResult(false, null, 0, 0, errors)
        fun partial(sessionId: UUID, placed: Int, failed: Int, errors: List<String>, warnings: List<String> = emptyList()): ExecutionResult =
            ExecutionResult(true, sessionId, placed, failed, errors, warnings)
    }
}

interface PreviewSession {
    val id: UUID
    val operations: List<BlockOperation>
    val executorId: String
    val createdAt: Long

    fun getPreviewBlocks(): List<BlockSnapshot>
    fun isValid(): Boolean
    fun getEstimatedCost(): Long
    fun getBoundingBox(): BoundingBox?
}

data class BlockSnapshot(
    val worldName: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val material: Material,
    val blockData: BlockData? = null
)

data class BoundingBox(
    val minX: Int, val minY: Int, val minZ: Int,
    val maxX: Int, val maxY: Int, val maxZ: Int
) {
    fun contains(x: Int, y: Int, z: Int): Boolean =
        x in minX..maxX && y in minY..maxY && z in minZ..maxZ
    fun expand(amount: Int): BoundingBox = BoundingBox(minX - amount, minY - amount, minZ - amount, maxX + amount, maxY + amount, maxZ + amount)
    fun volume(): Long = (maxX - minX + 1L) * (maxY - minY + 1L) * (maxZ - minZ + 1L)
}

data class UndoResult(
    val success: Boolean,
    val blocksRestored: Int,
    val errors: List<String>
)

interface BlockExecutorProvider {
    fun createExecutor(): BlockExecutor
    fun getExecutorId(): String
    fun isAvailable(): Boolean
    fun getDescription(): String
}

object BlockExecutorRegistry {
    private val providers = mutableMapOf<String, BlockExecutorProvider>()
    private val executors = mutableMapOf<String, BlockExecutor>()

    fun register(provider: BlockExecutorProvider) {
        providers[provider.executorId] = provider
    }

    fun unregister(executorId: String) {
        providers.remove(executorId)
        executors.remove(executorId)
    }

    fun getExecutor(executorId: String): BlockExecutor? {
        return executors.getOrPut(executorId) {
            providers[executorId]?.createExecutor()
        }
    }

    fun getAvailableExecutors(): List<BlockExecutor> {
        return providers.values.filter { it.isAvailable() }.map { it.createExecutor() }
    }

    fun getDefaultExecutor(): BlockExecutor? {
        return getAvailableExecutors().firstOrNull()
    }

    fun getExecutorByPriority(preferred: List<String>): BlockExecutor? {
        for (id in preferred) {
            val executor = getExecutor(id)
            if (executor != null && executor.isAvailable()) return executor
        }
        return getDefaultExecutor()
    }
}
