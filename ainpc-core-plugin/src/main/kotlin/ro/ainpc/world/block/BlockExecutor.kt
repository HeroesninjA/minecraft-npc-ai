package ro.ainpc.world.block

import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.data.BlockData
import java.util.UUID

interface BlockExecutor {
    fun placeBlock(world: World, x: Int, y: Int, z: Int, material: Material): BlockOperationResult
    fun placeBlock(world: World, x: Int, y: Int, z: Int, blockData: BlockData): BlockOperationResult
    fun placeBlocks(world: World, operations: List<BlockOperation>): BlockBatchResult
    fun getBlock(world: World, x: Int, y: Int, z: Int): BlockSnapshot?
    fun supportsPreview(): Boolean
    fun createPreview(operations: List<BlockOperation>): PreviewSession?
    fun commitPreview(session: PreviewSession): BlockBatchResult
    fun rollbackPreview(session: PreviewSession): Boolean
    fun getExecutorId(): String
}

data class BlockOperation(
    val x: Int,
    val y: Int,
    val z: Int,
    val material: Material,
    val blockData: BlockData? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    fun toSnapshot(worldName: String): BlockSnapshot {
        return BlockSnapshot(
            worldName = worldName,
            x = x,
            y = y,
            z = z,
            material = material,
            blockData = blockData?.clone(),
            metadata = metadata
        )
    }
}

data class BlockSnapshot(
    val worldName: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val material: Material,
    val blockData: BlockData? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    fun toOperation(): BlockOperation {
        return BlockOperation(x, y, z, material, blockData, metadata)
    }
}

data class BlockOperationResult(
    val success: Boolean,
    val snapshot: BlockSnapshot? = null,
    val error: String? = null
) {
    companion object {
        fun success(snapshot: BlockSnapshot): BlockOperationResult = BlockOperationResult(true, snapshot)
        fun failure(error: String): BlockOperationResult = BlockOperationResult(false, error = error)
    }
}

data class BlockBatchResult(
    val successful: List<BlockSnapshot>,
    val failed: List<Pair<BlockOperation, String>>,
    val operationId: UUID = UUID.randomUUID()
) {
    val success: Boolean get() = failed.isEmpty()
    val totalAttempted: Int get() = successful.size + failed.size
    val successCount: Int get() = successful.size
    val failureCount: Int get() = failed.size
}

interface PreviewSession {
    val operations: List<BlockOperation>
    val executorId: String
    val createdAt: Long
    fun getPreviewBlocks(): List<BlockSnapshot>
    fun isValid(): Boolean
}
