package ro.ainpc.worldgen

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.bukkit.BukkitWorld
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.extent.transform.AffineTransform
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.World as WEWorld
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class WorldEditBlockExecutor(
    private val plugin: Plugin,
    private val schematicsDir: File,
    private val enableUndo: Boolean = true,
    private val enablePersistence: Boolean = true
) : BlockExecutor {
    override val executorId: String = "worldedit"
    override val displayName: String = "WorldEdit"

    private val activePreviews = ConcurrentHashMap<UUID, WorldEditPreviewSession>()
    private val clipboardCache = ConcurrentHashMap<String, Clipboard>()

    override fun isAvailable(): Boolean {
        return try {
            plugin.server.pluginManager.getPlugin("WorldEdit") != null &&
            Class.forName("com.sk89q.worldedit.WorldEdit") != null
        } catch (e: Exception) {
            false
        }
    }

    override fun getDescription(): String = "WorldEdit integration with preview, undo, and schematic persistence"

    override fun execute(operations: List<BlockOperation>): ExecutionResult {
        if (operations.isEmpty()) return ExecutionResult.success(UUID.randomUUID(), 0)

        val worldName = operations.first().worldName
        val world = Bukkit.getWorld(worldName) ?: return ExecutionResult.failure(listOf("World $worldName not found"))
        val weWorld = BukkitAdapter.adapt(world)
        val session = createEditSession(weWorld)

        val sessionId = UUID.randomUUID()
        var placed = 0
        var failed = 0
        val errors = mutableListOf<String>()
        val successfulOps = mutableListOf<BlockOperation>()

        try {
            for (op in operations) {
                val pos = BlockVector3.at(op.x, op.y, op.z)
                try {
                    when (op.blockData) {
                        null -> session.setBlock(pos, BukkitAdapter.adapt(op.material.createBlockData()))
                        is org.bukkit.block.data.BlockData -> session.setBlock(pos, BukkitAdapter.adapt(op.blockData))
                    }
                    placed++
                    successfulOps.add(op)
                } catch (e: Exception) {
                    failed++
                    errors.add("Failed at ${op.x},${op.y},${op.z}: ${e.message}")
                }
            }
            session.flush()
        } catch (e: Exception) {
            errors.add("Batch failed: ${e.message}")
            failed = operations.size
            placed = 0
        } finally {
            session.close()
        }

        val sessionData = WorldEditSessionData(sessionId, successfulOps.toList())
        activePreviews[sessionId] = WorldEditPreviewSession(sessionId, successfulOps.toList(), this)

        if (enablePersistence && placed > 0) {
            persistSchematic(sessionId, successfulOps)
        }

        return if (failed == 0) {
            ExecutionResult.success(sessionId, placed)
        } else if (placed > 0) {
            ExecutionResult.partial(sessionId, placed, failed, errors, listOf("$failed blocks failed"))
        } else {
            ExecutionResult.failure(errors)
        }
    }

    override fun executeWithPreview(operations: List<BlockOperation>): PreviewSession {
        val sessionId = UUID.randomUUID()
        val preview = WorldEditPreviewSession(sessionId, operations, this)
        activePreviews[sessionId] = preview
        return preview
    }

    override fun undo(sessionId: UUID): UndoResult {
        val session = activePreviews.remove(sessionId) ?: return UndoResult(false, 0, listOf("Session not found"))
        if (!enableUndo) return UndoResult(false, 0, listOf("Undo disabled"))

        val world = Bukkit.getWorld(session.operations.firstOrNull()?.worldName ?: "") ?: return UndoResult(false, 0, listOf("World not found"))
        val weWorld = BukkitAdapter.adapt(world)
        val editSession = createEditSession(weWorld)
        var restored = 0
        val errors = mutableListOf<String>()

        try {
            for (op in session.operations.asReversed()) {
                val pos = BlockVector3.at(op.x, op.y, op.z)
                try {
                    editSession.setBlock(pos, BukkitAdapter.adapt(Material.AIR.createBlockData()))
                    restored++
                } catch (e: Exception) {
                    errors.add("Failed to undo ${op.x},${op.y},${op.z}: ${e.message}")
                }
            }
            editSession.flush()
        } catch (e: Exception) {
            errors.add("Undo failed: ${e.message}")
        } finally {
            editSession.close()
        }

        return UndoResult(errors.isEmpty(), restored, errors)
    }

    override fun saveSchematic(sessionId: UUID, file: File): Boolean {
        val session = activePreviews[sessionId] ?: return false
        return try {
            val world = Bukkit.getWorld(session.operations.first().worldName) ?: return false
            val weWorld = BukkitAdapter.adapt(world)
            val minPos = BlockVector3.at(
                session.operations.minByOrNull { it.x }?.x ?: 0,
                session.operations.minByOrNull { it.y }?.y ?: 0,
                session.operations.minByOrNull { it.z }?.z ?: 0
            )
            val maxPos = BlockVector3.at(
                session.operations.maxByOrNull { it.x }?.x ?: 0,
                session.operations.maxByOrNull { it.y }?.y ?: 0,
                session.operations.maxByOrNull { it.z }?.z ?: 0
            )
            val region = CuboidRegionSelector(weWorld).select(minPos, maxPos)
            val clipboard = ForwardExtentCopy(region, weWorld, minPos).copy()
            file.parentFile?.mkdirs()
            ClipboardFormats.findByAlias("schem")?.write(clipboard, FileOutputStream(file))
            true
        } catch (e: Exception) {
            plugin.logger.warning("Failed to save schematic: ${e.message}")
            false
        }
    }

    override fun loadSchematic(file: File): List<BlockOperation> {
        return clipboardCache.getOrPut(file.name) {
            try {
                ClipboardFormats.findByFile(file)?.read(FileInputStream(file))
            } catch (e: Exception) {
                plugin.logger.warning("Failed to load schematic ${file.name}: ${e.message}")
                null
            }
        }?.let { clipboard ->
            val weWorld = BukkitAdapter.adapt(Bukkit.getWorlds().firstOrNull()) ?: return emptyList()
            val origin = clipboard.origin
            clipboard.regions.flatMap { region ->
                region.allBlocks().map { pos ->
                    val relative = pos.subtract(origin)
                    val block = weWorld.getBlock(pos)
                    BlockOperation(
                        worldName = weWorld.name,
                        x = relative.x,
                        y = relative.y,
                        z = relative.z,
                        material = BukkitAdapter.adapt(block.blockData).material,
                        blockData = BukkitAdapter.adapt(block.blockData)
                    )
                }
            }
        } ?: emptyList()
    }

    fun pasteSchematic(world: World, fileName: String, originX: Int, originY: Int, originZ: Int, player: Player? = null): ExecutionResult {
        val clipboard = loadSchematic(File(schematicsDir, fileName)) ?: return ExecutionResult.failure(listOf("Schematic not found: $fileName"))

        return try {
            val weWorld = BukkitAdapter.adapt(world)
            val session = createEditSession(weWorld)
            val origin = BlockVector3.at(originX, originY, originZ)
            val operation = clipboard.paste(weWorld, origin, false, true, null)
            Operations.complete(operation)
            session.flush()
            ExecutionResult.success(UUID.randomUUID(), clipboard.regions.sumOf { it.count() })
        } catch (e: Exception) {
            ExecutionResult.failure(listOf("Paste failed: ${e.message}"))
        }
    }

    private fun createEditSession(world: WEWorld): EditSession {
        return WorldEdit.getInstance().newEditSessionBuilder(world).build()
    }

    private fun persistSchematic(sessionId: UUID, operations: List<BlockOperation>) {
        if (!enablePersistence) return
        try {
            val world = Bukkit.getWorld(operations.first().worldName) ?: return
            val weWorld = BukkitAdapter.adapt(world)
            val minPos = BlockVector3.at(
                operations.minByOrNull { it.x }?.x ?: 0,
                operations.minByOrNull { it.y }?.y ?: 0,
                operations.minByOrNull { it.z }?.z ?: 0
            )
            val maxPos = BlockVector3.at(
                operations.maxByOrNull { it.x }?.x ?: 0,
                operations.maxByOrNull { it.y }?.y ?: 0,
                operations.maxByOrNull { it.z }?.z ?: 0
            )
            val region = CuboidRegionSelector(weWorld).select(minPos, maxPos)
            val clipboard = ForwardExtentCopy(region, weWorld, minPos).copy()
            val file = File(schematicsDir, "ainpc_${sessionId}.schem")
            file.parentFile?.mkdirs()
            ClipboardFormats.findByAlias("schem")?.write(clipboard, FileOutputStream(file))
        } catch (e: Exception) {
            plugin.logger.warning("Failed to persist schematic for $sessionId: ${e.message}")
        }
    }

    private data class WorldEditSessionData(
        val id: UUID,
        val operations: List<BlockOperation>
    )
}

class WorldEditPreviewSession(
    override val id: UUID,
    override val operations: List<BlockOperation>,
    private val executor: WorldEditBlockExecutor
) : PreviewSession {
    override val executorId: String = "worldedit"
    override val createdAt: Long = System.currentTimeMillis()

    override fun getPreviewBlocks(): List<BlockSnapshot> {
        return operations.map { op ->
            BlockSnapshot(op.worldName, op.x, op.y, op.z, op.material, op.blockData)
        }
    }

    override fun isValid(): Boolean = operations.isNotEmpty()

    override fun getEstimatedCost(): Long {
        val world = Bukkit.getWorld(operations.firstOrNull()?.worldName ?: "") ?: return operations.size.toLong()
        var estimatedCost = 0L
        for (op in operations) {
            val block = world.getBlockAt(op.x, op.y, op.z)
            if (block.type != Material.AIR) estimatedCost++
        }
        return operations.size + estimatedCost
    }

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

class WorldEditBlockExecutorProvider(private val plugin: Plugin) : BlockExecutorProvider {
    override fun createExecutor(): BlockExecutor {
        val schematicsDir = File(plugin.dataFolder, "schematics")
        return WorldEditBlockExecutor(plugin, schematicsDir)
    }
    override fun getExecutorId(): String = "worldedit"
    override fun isAvailable(): Boolean {
        return try {
            plugin.server.pluginManager.getPlugin("WorldEdit") != null &&
            Class.forName("com.sk89q.worldedit.WorldEdit") != null
        } catch (e: Exception) {
            false
        }
    }
    override fun getDescription(): String = "WorldEdit integration with preview, undo, and schematic persistence"
}

fun WorldEditBlockExecutorProvider.register(plugin: Plugin) {
    BlockExecutorRegistry.register(WorldEditBlockExecutorProvider(plugin))
}
