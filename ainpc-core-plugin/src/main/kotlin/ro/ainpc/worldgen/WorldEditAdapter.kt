package ro.ainpc.worldgen

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.reflect.Method
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class WorldEditAdapter(
    private val plugin: Plugin,
    private val schematicsDir: File,
    private val enableUndo: Boolean = true,
    private val enablePersistence: Boolean = true,
    private val maxPreviewBlocks: Int = 50000
) : BlockExecutor {
    override val executorId: String = "worldedit"
    override val displayName: String = "WorldEdit"

    private val activeSessions = ConcurrentHashMap<UUID, SessionData>()
    private val weClasses = WorldEditClasses()

    override fun isAvailable(): Boolean {
        return weClasses.isAvailable()
    }

    override fun getDescription(): String = "WorldEdit integration with preview, undo, and schematic persistence (runtime reflection)"

    override fun execute(operations: List<BlockOperation>): ExecutionResult {
        if (operations.isEmpty()) return ExecutionResult.success(UUID.randomUUID(), 0)

        val worldName = operations.first().worldName
        val world = Bukkit.getWorld(worldName) ?: return ExecutionResult.failure(listOf("World $worldName not found"))
        
        val editSession = createEditSession(world)
        if (editSession == null) return ExecutionResult.failure(listOf("Failed to create WorldEdit session"))

        val sessionId = UUID.randomUUID()
        var placed = 0
        var failed = 0
        val errors = mutableListOf<String>()
        val successfulOps = mutableListOf<BlockOperation>()

        try {
            for (op in operations) {
                if (placed + failed > maxPreviewBlocks) {
                    errors.add("Max blocks per operation ($maxPreviewBlocks) exceeded")
                    failed += operations.size - placed - failed
                    break
                }
                try {
                    val pos = createBlockVector(op.x, op.y, op.z)
                    val blockState = when (op.blockData) {
                        null -> createBlockData(op.material)
                        else -> op.blockData
                    }
                    callSetBlock(editSession!!, pos!!, blockState)
                    placed++
                    successfulOps.add(op)
                } catch (e: Exception) {
                    failed++
                    errors.add("Failed at ${op.x},${op.y},${op.z}: ${e.message}")
                }
            }
            callFlush(editSession)
        } catch (e: Exception) {
            errors.add("Batch failed: ${e.message}")
            failed = operations.size
            placed = 0
        } finally {
            callClose(editSession)
        }

        val sessionData = SessionData(sessionId, successfulOps.toList(), worldName)
        activeSessions[sessionId] = sessionData

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
        val session = WorldEditPreviewSession(sessionId, operations, this, plugin)
        activeSessions[sessionId] = SessionData(sessionId, operations, operations.firstOrNull()?.worldName ?: "")
        return session
    }

    override fun undo(sessionId: UUID): UndoResult {
        val session = activeSessions.remove(sessionId) ?: return UndoResult(false, 0, listOf("Session not found"))
        if (!enableUndo) return UndoResult(false, 0, listOf("Undo disabled"))

        val world = Bukkit.getWorld(session.worldName) ?: return UndoResult(false, 0, listOf("World not found"))
        val editSession = createEditSession(world)
        if (editSession == null) return UndoResult(false, 0, listOf("Failed to create edit session"))

        var restored = 0
        val errors = mutableListOf<String>()

        try {
            for (op in session.operations.asReversed()) {
                val pos = createBlockVector(op.x, op.y, op.z)
                try {
                    callSetBlock(editSession!!, pos!!, createBlockData(Material.AIR))
                    restored++
                } catch (e: Exception) {
                    errors.add("Failed to undo ${op.x},${op.y},${op.z}: ${e.message}")
                }
            }
            callFlush(editSession)
        } catch (e: Exception) {
            errors.add("Undo batch failed: ${e.message}")
        } finally {
            callClose(editSession)
        }

        return UndoResult(errors.isEmpty(), restored, errors)
    }

    override fun saveSchematic(sessionId: UUID, file: File): Boolean {
        val session = activeSessions[sessionId] ?: return false
        if (session.operations.isEmpty()) return false

        file.parentFile?.mkdirs()

        return try {
            val world = Bukkit.getWorld(session.worldName) ?: return false
            val weWorld = adaptWorld(world)
            if (weWorld == null) return false
            
            val region = createRegionFromOperations(weWorld, session.operations)
            val clipboard = callForwardExtentCopy(region, weWorld, callGetMinimumPoint(region)!!)!!
            
            val format = findClipboardFormat("schem") ?: findClipboardFormat("schematic")
            format?.let { callWriteClipboard(it, clipboard, FileOutputStream(file)) }
            true
        } catch (e: Exception) {
            plugin.logger.warning("Failed to save schematic for $sessionId: ${e.message}")
            false
        }
    }

    override fun loadSchematic(file: File): List<BlockOperation> {
        try {
            val format = findClipboardFormatByFile(file) ?: return emptyList()
            val clipboard = callReadClipboard(format, FileInputStream(file))
            
            val region = callGetRegion(clipboard!!)
            val operations = mutableListOf<BlockOperation>()
            for (pos in callGetAllPositions(region!!)) {
                val block = callGetFullBlock(clipboard!!, pos)
                val material = adaptMaterial(callGetBlockType(block!!))
                if (material != Material.AIR) {
                    operations.add(BlockOperation(
                        worldName = "",
                        x = callGetBlockVectorX(pos),
                        y = callGetBlockVectorY(pos),
                        z = callGetBlockVectorZ(pos),
                        material = material,
                        blockData = callGetBlockData(block!!)
                    ))
                }
            }
            return operations
        } catch (e: Exception) {
            plugin.logger.warning("Failed to load schematic ${file.name}: ${e.message}")
            return emptyList()
        }
    }

    fun pasteSchematic(
        world: World,
        fileName: String,
        originX: Int,
        originY: Int,
        originZ: Int,
        rotation: Any? = null
    ): ExecutionResult {
        val file = File(schematicsDir, fileName)
        if (!file.exists()) return ExecutionResult.failure(listOf("Schematic not found: $fileName"))

        val weWorld = adaptWorld(world)
        if (weWorld == null) return ExecutionResult.failure(listOf("Failed to adapt world"))
        
        val editSession = createEditSession(world)
        if (editSession == null) return ExecutionResult.failure(listOf("Failed to create edit session"))
        val origin = createBlockVector(originX, originY, originZ)

        try {
            val format = findClipboardFormatByFile(file) ?: return ExecutionResult.failure(listOf("Unknown schematic format"))
            val weClipboard = callReadClipboard(format!!, FileInputStream(file))
            val operation = callPasteClipboard(weClipboard!!, weWorld!!, origin!!, rotation)
            callCompleteOperation(operation!!)
            callFlush(editSession!!)
            val area = callGetRegionArea(weClipboard)
            return ExecutionResult.success(UUID.randomUUID(), area)
        } catch (e: Exception) {
            return ExecutionResult.failure(listOf("Paste failed: ${e.message}"))
        } finally {
            callClose(editSession!!)
        }
    }

    // WorldEdit reflection helpers
    private class WorldEditClasses {
        val worldEditClass: Class<*>? = try { Class.forName("com.sk89q.worldedit.WorldEdit") } catch (e: Exception) { null }
        val editSessionClass: Class<*>? = try { Class.forName("com.sk89q.worldedit.EditSession") } catch (e: Exception) { null }
        val bukkitAdapterClass: Class<*>? = try { Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter") } catch (e: Exception) { null }
        val blockVector3Class: Class<*>? = try { Class.forName("com.sk89q.worldedit.math.BlockVector3") } catch (e: Exception) { null }
        val regionClass: Class<*>? = try { Class.forName("com.sk89q.worldedit.regions.Region") } catch (e: Exception) { null }
        val cuboidRegionClass: Class<*>? = try { Class.forName("com.sk89q.worldedit.regions.CuboidRegion") } catch (e: Exception) { null }
        val forwardExtentCopyClass: Class<*>? = try { Class.forName("com.sk89q.worldedit.function.operation.ForwardExtentCopy") } catch (e: Exception) { null }
        val operationsClass: Class<*>? = try { Class.forName("com.sk89q.worldedit.function.operation.Operations") } catch (e: Exception) { null }
        val clipboardClass: Class<*>? = try { Class.forName("com.sk89q.worldedit.extent.clipboard.Clipboard") } catch (e: Exception) { null }
        val clipboardFormatsClass: Class<*>? = try { Class.forName("com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats") } catch (e: Exception) { null }
        val localSessionClass: Class<*>? = try { Class.forName("com.sk89q.worldedit.session.LocalSession") } catch (e: Exception) { null }
        val clipboardHolderClass: Class<*>? = try { Class.forName("com.sk89q.worldedit.session.ClipboardHolder") } catch (e: Exception) { null }

        fun isAvailable(): Boolean {
            return worldEditClass != null &&
                Bukkit.getPluginManager().getPlugin("WorldEdit") != null
        }
    }

    private fun createEditSession(world: World): Any? {
        val weWorld = adaptWorld(world) ?: return null
        val builder = callNewEditSessionBuilder(weWorld)
            ?: return null
        val fastMode = callFastMode(builder!!, true) ?: return null
        val allowedRegions = callAllowedRegionsEverywhere(fastMode!!) ?: return null
        val limitUnlimited = callLimitUnlimited(allowedRegions!!) ?: return null
        return callBuild(limitUnlimited!!)
    }

    private fun adaptWorld(world: World): Any? {
        return weClasses.bukkitAdapterClass?.getMethod("adapt", World::class.java)?.invoke(null, world)
    }

    private fun createBlockVector(x: Int, y: Int, z: Int): Any? {
        return weClasses.blockVector3Class?.getMethod("at", Int::class.java, Int::class.java, Int::class.java)?.invoke(null, x, y, z)
    }

    private fun createBlockData(material: Material): BlockData {
        return material.createBlockData()
    }

    private fun callSetBlock(editSession: Any, pos: Any, blockData: BlockData) {
        weClasses.editSessionClass?.getMethod("setBlock", weClasses.blockVector3Class, org.bukkit.block.data.BlockData::class.java)?.invoke(editSession, pos, blockData)
    }

    private fun callFlush(editSession: Any) {
        weClasses.editSessionClass?.getMethod("flush")?.invoke(editSession)
    }

    private fun callClose(editSession: Any) {
        weClasses.editSessionClass?.getMethod("close")?.invoke(editSession)
    }

    private fun callNewEditSessionBuilder(world: Any): Any? {
        val instance = weClasses.worldEditClass?.getMethod("getInstance")?.invoke(null) ?: return null
        val builder = instance::class.java.getMethod("newEditSessionBuilder")?.invoke(instance) ?: return null
        val worldInterface = weClasses.worldEditClass?.interfaces?.firstOrNull() ?: world::class.java
        return builder::class.java.getMethod("world", worldInterface)?.invoke(builder, world) ?: builder
    }

    private fun callFastMode(builder: Any, enabled: Boolean): Any? {
        return builder.javaClass.getMethod("fastMode", Boolean::class.java)?.invoke(builder, enabled)
    }

    private fun callAllowedRegionsEverywhere(builder: Any): Any? {
        return builder.javaClass.getMethod("allowedRegionsEverywhere")?.invoke(builder)
    }

    private fun callLimitUnlimited(builder: Any): Any? {
        return builder.javaClass.getMethod("limitUnlimited")?.invoke(builder)
    }

    private fun callBuild(builder: Any): Any? {
        return builder.javaClass.getMethod("build")?.invoke(builder)
    }

    private fun callGetMinimumPoint(region: Any): Any? {
        return region.javaClass.getMethod("getMinimumPoint")?.invoke(region)
    }

    private fun callForwardExtentCopy(region: Any, world: Any, origin: Any): Any? {
        val worldInterface = weClasses.worldEditClass?.interfaces?.firstOrNull() ?: world::class.java
        return weClasses.forwardExtentCopyClass?.getDeclaredConstructor(weClasses.regionClass, worldInterface, weClasses.blockVector3Class)?.newInstance(region, world, origin)
    }

    private fun callCompleteOperation(operation: Any) {
        weClasses.operationsClass?.getMethod("complete", operation.javaClass)?.invoke(null, operation)
    }

    private fun findClipboardFormat(alias: String): Any? {
        return weClasses.clipboardFormatsClass?.getMethod("findByAlias", String::class.java)?.invoke(null, alias)
    }

    private fun findClipboardFormatByFile(file: File): Any? {
        return weClasses.clipboardFormatsClass?.getMethod("findByFile", File::class.java)?.invoke(null, file)
    }

    private fun callWriteClipboard(format: Any, clipboard: Any, outputStream: FileOutputStream) {
        format.javaClass.getMethod("write", weClasses.clipboardClass, FileOutputStream::class.java)?.invoke(format, clipboard, outputStream)
    }

    private fun callReadClipboard(format: Any, inputStream: FileInputStream): Any? {
        return format.javaClass.getMethod("read", FileInputStream::class.java)?.invoke(format, inputStream)
    }

    private fun callGetRegion(clipboard: Any): Any? {
        return clipboard.javaClass.getMethod("getRegion")?.invoke(clipboard)
    }

    private fun callGetAllPositions(region: Any): List<Any> {
        val iterable = region as? Iterable<*> ?: return emptyList()
        return iterable.filterNotNull()
    }

    private fun callGetBlockVectorX(vector: Any): Int {
        return (vector::class.java.getMethod("getX").invoke(vector) as? Int) ?: 0
    }

    private fun callGetBlockVectorY(vector: Any): Int {
        return (vector::class.java.getMethod("getY").invoke(vector) as? Int) ?: 0
    }

    private fun callGetBlockVectorZ(vector: Any): Int {
        return (vector::class.java.getMethod("getZ").invoke(vector) as? Int) ?: 0
    }

    private fun callGetFullBlock(clipboard: Any, pos: Any): Any? {
        return clipboard.javaClass.getMethod("getFullBlock", weClasses.blockVector3Class)?.invoke(clipboard, pos)
    }

    private fun callGetBlockType(block: Any): Any? {
        return block.javaClass.getMethod("getBlockType")?.invoke(block)
    }

    private fun adaptMaterial(blockType: Any?): Material {
        val adapted = weClasses.bukkitAdapterClass?.getMethod("adapt", blockType?.let { it::class.java })?.invoke(null, blockType)
        return (adapted?.let { it::class.java.getMethod("toMaterial").invoke(it) } as? Material) ?: Material.AIR
    }

    private fun callGetBlockData(block: Any): BlockData? {
        val blockState = block.javaClass.getMethod("getBlockState")?.invoke(block)
        return blockState?.let { it::class.java.getMethod("getBlockData").invoke(it) } as? BlockData
    }

    private fun callPasteClipboard(clipboard: Any, world: Any, origin: Any, rotation: Any?): Any? {
        return clipboard.javaClass.getMethod("paste", world.javaClass, weClasses.blockVector3Class, Boolean::class.java, Boolean::class.java, rotation?.javaClass)?.invoke(clipboard, world, origin, false, true, rotation)
    }

    private fun callGetRegionArea(clipboard: Any): Int {
        val region = callGetRegion(clipboard)
        return (region?.javaClass?.getMethod("getArea")?.invoke(region) as? Int) ?: 0
    }

    private fun createRegionFromOperations(world: Any, operations: List<BlockOperation>): Any {
        if (operations.isEmpty()) return createCuboidRegion(world, createBlockVector(0, 0, 0)!!, createBlockVector(0, 0, 0)!!)
        
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

        return createCuboidRegion(world, createBlockVector(minX, minY, minZ)!!, createBlockVector(maxX, maxY, maxZ)!!)
    }

    private fun createCuboidRegion(world: Any, min: Any, max: Any): Any {
        val worldInterface = weClasses.worldEditClass?.interfaces?.firstOrNull() ?: world::class.java
        return weClasses.cuboidRegionClass?.getDeclaredConstructor(worldInterface, weClasses.blockVector3Class, weClasses.blockVector3Class)?.newInstance(world, min, max)!!
    }

    private fun persistSchematic(sessionId: UUID, operations: List<BlockOperation>) {
        if (!enablePersistence || operations.isEmpty()) return
        try {
            val world = Bukkit.getWorld(operations.first().worldName) ?: return
            val weWorld = adaptWorld(world) ?: return
            val region = createRegionFromOperations(weWorld, operations)
            val clipboard = callForwardExtentCopy(region, weWorld, callGetMinimumPoint(region)!!)!!
            val file = File(schematicsDir, "ainpc_$sessionId.schem")
            val format = findClipboardFormat("schem") ?: findClipboardFormat("schematic")
            format?.let { callWriteClipboard(it, clipboard, FileOutputStream(file)) }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to persist schematic for $sessionId: ${e.message}")
        }
    }

    data class SessionData(
        val id: UUID,
        val operations: List<BlockOperation>,
        val worldName: String
    )

    class WorldEditPreviewSession(
        override val id: UUID,
        override val operations: List<BlockOperation>,
        private val executor: WorldEditAdapter,
        private val plugin: Plugin
    ) : PreviewSession {
        override val executorId: String = "worldedit"
        override val createdAt: Long = System.currentTimeMillis()

        override fun getPreviewBlocks(): List<BlockSnapshot> {
            return operations.map { op ->
                BlockSnapshot(op.worldName, op.x, op.y, op.z, op.material, op.blockData ?: op.material.createBlockData())
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

        fun applyToPlayer(player: Player): Boolean {
            val world = player.world
            val editSession = executor.createEditSession(world)
            if (editSession == null) return false

            try {
                for (op in operations) {
                    val pos = executor.createBlockVector(op.x, op.y, op.z)
                    val blockData = op.blockData ?: op.material.createBlockData()
                    executor.callSetBlock(editSession, pos!!, blockData)
                }
                executor.callFlush(editSession)

                val localSessionClass = Class.forName("com.sk89q.worldedit.session.LocalSession")
                val sessionManager = Class.forName("com.sk89q.worldedit.WorldEdit").getMethod("getInstance").invoke(null)
                    .javaClass.getMethod("getSessionManager").invoke(null)
                val localSession = localSessionClass.getDeclaredConstructor(sessionManager.javaClass).newInstance(sessionManager)
                
                val clipboardHolderClass = Class.forName("com.sk89q.worldedit.session.ClipboardHolder")
                val holder = clipboardHolderClass.getDeclaredConstructor(List::class.java).newInstance(operations)
                localSession.javaClass.getMethod("remember", clipboardHolderClass).invoke(localSession, holder)
                return true
            } catch (e: Exception) {
                plugin.logger.warning("Failed to apply preview: ${e.message}")
                return false
            } finally {
                executor.callClose(editSession)
            }
        }
    }

    class WorldEditAdapterProvider(private val plugin: Plugin) : BlockExecutorProvider {
        private var adapter: WorldEditAdapter? = null

        override fun createExecutor(): BlockExecutor {
            adapter = WorldEditAdapter(plugin, File(plugin.dataFolder, "schematics"))
            return adapter!!
        }

        override fun getExecutorId(): String = "worldedit"
        override fun isAvailable(): Boolean = createExecutor().isAvailable()
        override fun getDescription(): String = "WorldEdit integration with preview, undo, and schematic persistence (runtime reflection)"
    }

    fun WorldEditAdapterProvider.register(plugin: Plugin) {
        BlockExecutorRegistry.register(WorldEditAdapterProvider(plugin))
    }
}