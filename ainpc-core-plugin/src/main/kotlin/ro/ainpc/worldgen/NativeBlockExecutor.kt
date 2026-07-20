package ro.ainpc.worldgen

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.plugin.Plugin
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class NativeBlockExecutor(private val plugin: Plugin) : BlockExecutor {
    override val executorId: String = "native"
    override val displayName: String = "Native Bukkit API"

    private val activeSessions = ConcurrentHashMap<UUID, SessionData>()

    override fun isAvailable(): Boolean = true

    override fun getDescription(): String = "Direct Bukkit API block placement. No external dependencies. No preview/undo support."

    override fun execute(operations: List<BlockOperation>): ExecutionResult {
        val sessionId = UUID.randomUUID()
        var placed = 0
        var failed = 0
        val errors = mutableListOf<String>()
        val sessionOps = mutableListOf<BlockOperation>()

        for (op in operations) {
            val world = Bukkit.getWorld(op.worldName) ?: run {
                errors.add("World ${op.worldName} not found")
                failed++
                continue
            }

            val block = world.getBlockAt(op.x, op.y, op.z)
            try {
                if (op.blockData != null) {
                    block.blockData = op.blockData
                } else {
                    block.type = op.material
                }
                placed++
                sessionOps.add(op)
            } catch (e: Exception) {
                failed++
                errors.add("Failed to place ${op.material} at ${op.x},${op.y},${op.z}: ${e.message}")
            }
        }

        val sessionData = SessionData(sessionId, sessionOps.toList())
        activeSessions[sessionId] = sessionData

        return if (failed == 0) {
            ExecutionResult.success(sessionId, placed)
        } else if (placed > 0) {
            ExecutionResult.partial(sessionId, placed, failed, errors, listOf("$failed blocks failed to place"))
        } else {
            ExecutionResult.failure(errors)
        }
    }

    override fun executeWithPreview(operations: List<BlockOperation>): PreviewSession {
        val sessionId = UUID.randomUUID()
        val sessionData = SessionData(sessionId, operations)
        activeSessions[sessionId] = sessionData
        return NativePreviewSession(sessionId, operations, this)
    }

    override fun undo(sessionId: UUID): UndoResult {
        val session = activeSessions.remove(sessionId) ?: return UndoResult(false, 0, listOf("Session not found"))
        var restored = 0
        val errors = mutableListOf<String>()

        for (op in session.operations.asReversed()) {
            val world = Bukkit.getWorld(op.worldName) ?: run {
                errors.add("World ${op.worldName} not found")
                continue
            }
            val block = world.getBlockAt(op.x, op.y, op.z)
            try {
                block.type = Material.AIR
                restored++
            } catch (e: Exception) {
                errors.add("Failed to undo at ${op.x},${op.y},${op.z}: ${e.message}")
            }
        }

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

    private data class SessionData(
        val id: UUID,
        val operations: List<BlockOperation>
    )
}

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

class NativeBlockExecutorProvider(private val plugin: Plugin) : BlockExecutorProvider {
    override fun createExecutor(): BlockExecutor = NativeBlockExecutor(plugin)
    override fun getExecutorId(): String = "native"
    override fun isAvailable(): Boolean = true
    override fun getDescription(): String = "Native Bukkit API block placement (fallback)"
}

fun NativeBlockExecutorProvider.register(plugin: Plugin) {
    BlockExecutorRegistry.register(NativeBlockExecutorProvider(plugin))
}
