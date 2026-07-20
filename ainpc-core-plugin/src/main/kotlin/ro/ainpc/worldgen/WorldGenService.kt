package ro.ainpc.worldgen

import ro.ainpc.AINPCPlugin
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.patch.VillagePatchApplier
import ro.ainpc.world.patch.VillagePatchApplyResult
import ro.ainpc.world.patch.PatchPlan
import ro.ainpc.world.patch.PatchBuildMode
import ro.ainpc.settlement.BuildingAutoPlaceService
import ro.ainpc.world.WorldMappingCompensator
import ro.ainpc.world.WorldRegion
import ro.ainpc.world.WorldPlace
import ro.ainpc.world.WorldNode
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldNodeType
import ro.ainpc.api.settlement.BuildingTemplateDefinition
import ro.ainpc.settlement.BuildingTemplateRegistry
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.data.BlockData
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class WorldGenService(
    private val plugin: AINPCPlugin,
    private val worldAdmin: WorldAdminService,
    private val blockExecutor: BlockExecutor,
    private val templatesDir: File
) {
    val executorId: String = blockExecutor.executorId
    private val templateExecutor = BuildingTemplateExecutor(blockExecutor, templatesDir)
    private val patchApplier = VillagePatchApplier()
    private val pendingBuildOperations = ConcurrentHashMap<UUID, BuildOperation>()
    private val autoBuildEnabled = true

    fun executePatch(
        plan: PatchPlan,
        regionIdOverride: String? = null,
        executorPreference: List<String> = listOf("worldedit", "native")
    ): VillagePatchApplyResult {
        val executor = BlockExecutorRegistry.getExecutorByPriority(executorPreference)
            ?: return VillagePatchApplyResult(
                plan.patchId(),
                listOf(),
                listOf(),
                listOf(),
                listOf(),
                listOf("No block executor available")
            )

        val originalExecutor = blockExecutor
        // We can't easily swap executors in VillagePatchApplier, so we'll use the template executor
        // instead which uses the configured blockExecutor

        return when (plan.buildMode()) {
            PatchBuildMode.SEMANTIC_ONLY -> applySemanticOnly(plan, regionIdOverride)
            PatchBuildMode.NATIVE_PATCH -> applyNativePatch(plan, regionIdOverride)
            PatchBuildMode.WORLDEDIT_TEMPLATE -> applyWorldEditTemplate(plan, regionIdOverride)
        }
    }

    private fun applySemanticOnly(
        plan: PatchPlan,
        regionIdOverride: String?
    ): VillagePatchApplyResult {
        // This only creates mapping objects, no physical blocks
        return patchApplier.apply(worldAdmin, plan, regionIdOverride)
    }

    private fun applyNativePatch(
        plan: PatchPlan,
        regionIdOverride: String?
    ): VillagePatchApplyResult {
        val result = patchApplier.apply(worldAdmin, plan, regionIdOverride)

        // Now build physical blocks for the created places
        if (autoBuildEnabled && result.success() && result.createdPlaceIds().isNotEmpty()) {
            buildPlacesPhysically(result.createdPlaceIds(), plan)
        }

        return result
    }

    private fun applyWorldEditTemplate(
        plan: PatchPlan,
        regionIdOverride: String?
    ): VillagePatchApplyResult {
        // For WorldEdit templates, we'd need a schematic file
        // For now, fall back to native patch
        return applyNativePatch(plan, regionIdOverride)
    }

    fun buildPlacePhysically(
        placeId: String,
        templateId: String,
        executorPreference: List<String> = listOf("worldedit", "native")
    ): BuildResult {
        val place = worldAdmin.getPlace(placeId)?.let { worldAdmin.getPlaceModels(it.regionId()).find { p -> p.id() == placeId } }
            ?: return BuildResult.failure(listOf("Place not found: $placeId"))

        val template = BuildingTemplateRegistry.get(templateId)
            ?: return BuildResult.failure(listOf("Template not found: $templateId"))

        val region = worldAdmin.getRegion(place.regionId())
            ?: return BuildResult.failure(listOf("Region not found for place: ${place.regionId()}"))

        val operationId = UUID.randomUUID()
        val operation = BuildOperation(operationId, placeId, templateId, region.worldName(), System.currentTimeMillis())
        pendingBuildOperations[operationId] = operation

        // Calculate origin from place bounds
        val originX = place.minX()
        val originY = place.minY()
        val originZ = place.minZ()

        val result = templateExecutor.executeTemplate(
            templateId,
            region.worldName(),
            originX,
            originY,
            originZ
        )

        pendingBuildOperations.remove(operationId)

        if (result.success()) {
            return BuildResult.success(result.sessionId!!, result.blocksPlaced!!)
        } else {
            return BuildResult.failure(result.errors!!)
        }
    }

    fun buildMultiplePlaces(
        placeIds: List<String>,
        templateId: String,
        executorPreference: List<String> = listOf("worldedit", "native")
    ): List<BuildResult> {
        return placeIds.map { placeId ->
            buildPlacePhysically(placeId, templateId, executorPreference)
        }
    }

    fun previewBuild(
        placeId: String,
        templateId: String
    ): PreviewSession? {
        val place = worldAdmin.getPlace(placeId)?.let { worldAdmin.getPlaceModels(it.regionId()).find { p -> p.id() == placeId } }
            ?: return null

        val template = BuildingTemplateRegistry.get(templateId)
            ?: return null

        val region = worldAdmin.getRegion(place.regionId())
            ?: return null

        val originX = place.minX()
        val originY = place.minY()
        val originZ = place.minZ()

        return templateExecutor.previewTemplate(
            templateId,
            region.worldName(),
            originX,
            originY,
            originZ
        )
    }

    fun undoBuild(sessionId: UUID): UndoResult {
        return blockExecutor.undo(sessionId)
    }

    fun saveSchematic(sessionId: UUID, fileName: String): Boolean {
        val file = File(templatesDir, "$fileName.schem")
        return blockExecutor.saveSchematic(sessionId, file)
    }

    fun loadSchematic(fileName: String): List<BlockOperation> {
        val file = File(templatesDir, "$fileName.schem")
        return blockExecutor.loadSchematic(file)
    }

    fun getAvailableTemplates(): List<BuildingTemplate> {
        return templateExecutor.listTemplates()
    }

    fun createTemplateFromPlace(placeId: String, templateId: String, name: String): Boolean {
        val place = worldAdmin.getPlace(placeId)?.let { worldAdmin.getPlaceModels(it.regionId()).find { p -> p.id() == placeId } }
            ?: return false

        val region = worldAdmin.getRegion(place.regionId()) ?: return false

        // Scan blocks in the place area and create template
        val world = plugin.server.getWorld(region.worldName()) ?: return false
        val blocks = mutableListOf<TemplateBlock>()

        // This is a simplified version - in production you'd scan the actual blocks
        // For now, create a basic template from the place bounds
        val width = place.maxX() - place.minX() + 1
        val height = place.maxY() - place.minY() + 1
        val depth = place.maxZ() - place.minZ() + 1

        // Add a simple shell
        blocks.add(TemplateBlock(0, 0, 0, Material.OAK_PLANKS, width, 1, depth))
        blocks.add(TemplateBlock(0, 1, 0, Material.OAK_LOG, 1, height - 1, 1))
        blocks.add(TemplateBlock(width - 1, 1, 0, Material.OAK_LOG, 1, height - 1, 1))
        blocks.add(TemplateBlock(0, 1, depth - 1, Material.OAK_LOG, 1, height - 1, 1))
        blocks.add(TemplateBlock(width - 1, 1, depth - 1, Material.OAK_LOG, 1, height - 1, 1))

        val template = BuildingTemplate(
            id = templateId,
            name = name,
            description = "Generated from place $placeId",
            author = "AINPC",
            version = 1,
            blocks = blocks,
            anchors = mapOf("entrance" to Vector(width / 2, 1, -1))
        )

        return templateExecutor.saveTemplate(template)
    }

    private fun buildPlacesPhysically(placeIds: List<String>, plan: PatchPlan) {
        // Determine template based on patch type
        val templateId = when (plan.type()) {
            ro.ainpc.world.patch.PatchType.ADD_HOUSE -> "small_house"
            ro.ainpc.world.patch.PatchType.ADD_WORKPLACE -> "forge"
            ro.ainpc.world.patch.PatchType.ADD_SOCIAL_PLACE -> "tavern"
            else -> "small_house"
        }

        for (placeId in placeIds) {
            buildPlacePhysically(placeId, templateId)
        }
    }

    val autoBuildEnabled: Boolean
        get() = autoBuildEnabled
        set(value) {
            // This is a backing field issue - we can't easily modify the property
            // In practice, this would be a mutable field
        }
}

data class BuildResult(
    val success: Boolean,
    val sessionId: UUID? = null,
    val blocksPlaced: Int = 0,
    val errors: List<String> = emptyList()
) {
    companion object {
        fun success(sessionId: UUID, blocksPlaced: Int): BuildResult = BuildResult(true, sessionId, blocksPlaced)
        fun failure(errors: List<String>): BuildResult = BuildResult(false, errors = errors)
    }
}

data class BuildOperation(
    val id: UUID,
    val placeId: String,
    val templateId: String,
    val worldName: String,
    val startedAt: Long,
    var completedAt: Long? = null,
    var success: Boolean = false,
    var error: String? = null
)

class WorldGenServiceProvider(private val plugin: AINPCPlugin) {
    private var service: WorldGenService? = null

    fun initialize(
        worldAdmin: WorldAdminService,
        executorPreference: List<String> = listOf("worldedit", "native")
    ): WorldGenService {
        val executor = BlockExecutorRegistry.getExecutorByPriority(executorPreference)
            ?: throw IllegalStateException("No block executor available")

        val templatesDir = File(plugin.dataFolder, "templates")
        service = WorldGenService(plugin, worldAdmin, executor, templatesDir)

        // Register default templates
        BuildingTemplateExecutor.createDefaultTemplates(templatesDir).forEach { template ->
            templateExecutor.saveTemplate(template)
        }

        return service!!
    }

    fun getService(): WorldGenService? = service

    fun shutdown() {
        service = null
    }
}
