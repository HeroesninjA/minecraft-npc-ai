package ro.ainpc.worldgen

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.block.data.BlockData
import org.bukkit.plugin.Plugin
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Nativeblockexecutor.
 */
class NativeBlockExecutor internal constructor(
    private val worldProvider: (String) -> World?,
    private val primaryThread: () -> Boolean,
) : BlockExecutor {
    constructor(plugin: Plugin) : this({ Bukkit.getWorld(it) }, { Bukkit.isPrimaryThread() })
    override val executorId: String = "native"
    override val displayName: String = "Native Bukkit API"

    private val activeSessions = ConcurrentHashMap<UUID, SessionData>()

    override fun isAvailable(): Boolean = true

    override fun getDescription(): String = "Direct Bukkit API block placement with read-only preview and in-memory snapshot undo."

    override fun execute(operations: List<BlockOperation>): ExecutionResult {
        if (!primaryThread()) return ExecutionResult.failure(listOf("Block placement requires the server primary thread"))
        val sessionId = UUID.randomUUID()
        var placed = 0
        var failed = 0
        val errors = mutableListOf<String>()
        val sessionOps = mutableListOf<BlockOperation>()
        val snapshots = linkedMapOf<BlockPosition, BlockState>()

        for (op in operations) {
            val world = worldProvider(op.worldName) ?: run {
                errors.add("World ${op.worldName} not found")
                failed++
                continue
            }

            try {
                val block = world.getBlockAt(op.x, op.y, op.z)
                val key = BlockPosition(op.worldName, op.x, op.y, op.z)
                if (key !in snapshots) snapshots[key] = block.state
                if (op.blockData != null) {
                    block.setBlockData(op.blockData, false)
                } else {
                    block.setType(op.material, false)
                }
                placed++
                sessionOps.add(op)
            } catch (e: Exception) {
                failed++
                errors.add("Failed to place ${op.material} at ${op.x},${op.y},${op.z}: ${e.message}")
            }
        }

        val sessionData = SessionData(sessionId, sessionOps.toList(), snapshots.values.toMutableList())
        activeSessions[sessionId] = sessionData

        return if (failed == 0) {
            ExecutionResult.success(sessionId, placed)
        } else if (placed > 0) {
            ExecutionResult(false, sessionId, placed, failed, errors, listOf("$failed blocks failed to place; undo session retained"))
        } else {
            ExecutionResult(false, sessionId, placed, failed, errors)
        }
    }

    override fun executeWithPreview(operations: List<BlockOperation>): PreviewSession {
        val sessionId = UUID.randomUUID()
        return NativePreviewSession(sessionId, operations.toList(), this)
    }

    override fun undo(sessionId: UUID): UndoResult {
        if (!primaryThread()) return UndoResult(false, 0, listOf("Block undo requires the server primary thread"))
        val session = activeSessions[sessionId] ?: return UndoResult(false, 0, listOf("Executed session not found"))
        var restored = 0
        val errors = mutableListOf<String>()

        for (snapshot in session.snapshots.toList().asReversed()) {
            try {
                val world = worldProvider(snapshot.world.name)
                check(world != null && world.uid == snapshot.world.uid) { "Original world is unavailable" }
                check(snapshot.update(true, false)) { "Snapshot restoration refused" }
                session.snapshots.remove(snapshot)
                restored++
            } catch (e: Exception) {
                errors.add("Failed to restore block snapshot: ${e.message}")
            }
        }

        if (session.snapshots.isEmpty()) activeSessions.remove(sessionId)

        return UndoResult(errors.isEmpty(), restored, errors)
    }

    override fun saveSchematic(sessionId: UUID, file: File): Boolean {
        val session = activeSessions[sessionId] ?: return false
        file.parentFile?.mkdirs()
        return try {
            FileOutputStream(file).use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(session.operations)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun loadSchematic(file: File): List<BlockOperation> {
        return try {
            FileInputStream(file).use { fis ->
                ObjectInputStream(fis).use { ois ->
                    ois.readObject() as List<BlockOperation>
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private data class BlockPosition(val worldName: String, val x: Int, val y: Int, val z: Int)

    private data class SessionData(
        val id: UUID,
        val operations: List<BlockOperation>,
        val snapshots: MutableList<BlockState>,
    )
}

/**
 * Nativepreviewsession.
 */
class NativePreviewSession(
    override val id: UUID,
    override val operations: List<BlockOperation>,
    private val executor: NativeBlockExecutor
) : PreviewSession {
    override val executorId: String = "native"
    override val createdAt: Long = System.currentTimeMillis()

    override fun getPreviewBlocks(): List<BlockSnapshot> {
        return operations.map { op ->
            BlockSnapshot(op.worldName, op.x, op.y, op.z, op.material, op.blockData)
        }
    }

    override fun isValid(): Boolean = operations.isNotEmpty()

    override fun getEstimatedCost(): Long = operations.size.toLong()

    override fun getBoundingBox(): BoundingBox? {
        if (operations.isEmpty()) return null
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var minZ = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var maxZ = Int.MIN_VALUE

        for (op in operations) {
            minX = minOf(minX, op.x)
            minY = minOf(minY, op.y)
            minZ = minOf(minZ, op.z)
            maxX = maxOf(maxX, op.x)
            maxY = maxOf(maxY, op.y)
            maxZ = maxOf(maxZ, op.z)
        }
        return BoundingBox(minX, minY, minZ, maxX, maxY, maxZ)
    }
}

/**
 * Nativeblockexecutorprovider.
 */
class NativeBlockExecutorProvider(private val plugin: Plugin) : BlockExecutorProvider {
    override fun createExecutor(): BlockExecutor = NativeBlockExecutor(plugin)
    override fun getExecutorId(): String = "native"
    override fun isAvailable(): Boolean = true
    override fun getDescription(): String = "Native Bukkit API block placement (fallback)"
}

fun NativeBlockExecutorProvider.register(plugin: Plugin) {
    BlockExecutorRegistry.register(NativeBlockExecutorProvider(plugin))
}
