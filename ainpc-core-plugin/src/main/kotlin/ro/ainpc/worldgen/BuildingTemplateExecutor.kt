package ro.ainpc.worldgen

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.util.Vector
import java.io.File
import java.util.Random
import java.util.UUID

private val templateJson = Json {
    serializersModule = SerializersModule {
        contextual(Vector::class, VectorSerializer)
    }
}

class BuildingTemplateExecutor(
    private val blockExecutor: BlockExecutor,
    private val templatesDir: File,
    private val enablePreview: Boolean = true,
    private val maxPreviewBlocks: Int = 10000
) {
    fun executeTemplate(
        templateId: String,
        worldName: String,
        originX: Int,
        originY: Int,
        originZ: Int,
        rotation: Rotation = Rotation.NONE,
        mirror: Mirror = Mirror.NONE
    ): ExecutionResult {
        val template = loadTemplate(templateId) ?: return ExecutionResult.failure(listOf("Template not found: $templateId"))
        val operations = template.toBlockOperations(worldName, originX, originY, originZ, rotation, mirror)
        return if (operations.size > maxPreviewBlocks) {
            ExecutionResult.failure(listOf("Template too large: ${operations.size} blocks (max $maxPreviewBlocks)"))
        } else {
            blockExecutor.execute(operations)
        }
    }

    fun previewTemplate(
        templateId: String,
        worldName: String,
        originX: Int,
        originY: Int,
        originZ: Int,
        rotation: Rotation = Rotation.NONE,
        mirror: Mirror = Mirror.NONE
    ): PreviewSession? {
        val template = loadTemplate(templateId) ?: return null
        val operations = template.toBlockOperations(worldName, originX, originY, originZ, rotation, mirror)
        return if (operations.size > maxPreviewBlocks) {
            null
        } else {
            blockExecutor.executeWithPreview(operations)
        }
    }

    fun undoLast(sessionId: UUID): UndoResult = blockExecutor.undo(sessionId)

    fun saveTemplate(template: BuildingTemplate): Boolean {
        templatesDir.mkdirs()
        val file = File(templatesDir, "${template.id}.json")
        return try {
            file.writeText(templateJson.encodeToString(BuildingTemplate.serializer(), template))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun loadTemplate(templateId: String): BuildingTemplate? {
        val file = File(templatesDir, "$templateId.json")
        if (!file.exists()) return null
        return try {
            templateJson.decodeFromString<BuildingTemplate>(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    fun listTemplates(): List<BuildingTemplate> {
        return templatesDir.listFiles()?.filter { it.extension == "json" }?.mapNotNull { loadTemplate(it.nameWithoutExtension) } ?: emptyList()
    }

    companion object {
        fun createDefaultTemplates(dir: File): List<BuildingTemplate> {
            val templates = mutableListOf<BuildingTemplate>()
            dir.mkdirs()

            val smallHouse = BuildingTemplate(
                id = "small_house",
                name = "Small House",
                description = "A simple 7x7 house with door and windows",
                author = "AINPC",
                version = 1,
                blocks = listOf(
                    // Foundation
                    TemplateBlock(0, 0, 0, Material.OAK_PLANKS, 7, 1, 7),
                    // Walls
                    TemplateBlock(0, 1, 0, Material.OAK_LOG, 1, 4, 1),
                    TemplateBlock(6, 1, 0, Material.OAK_LOG, 1, 4, 1),
                    TemplateBlock(0, 1, 6, Material.OAK_LOG, 1, 4, 1),
                    TemplateBlock(6, 1, 6, Material.OAK_LOG, 1, 4, 1),
                    // Walls fill
                    TemplateBlock(1, 1, 0, Material.OAK_PLANKS, 5, 3, 1),
                    TemplateBlock(1, 1, 6, Material.OAK_PLANKS, 5, 3, 1),
                    TemplateBlock(0, 1, 1, Material.OAK_PLANKS, 1, 3, 5),
                    TemplateBlock(6, 1, 1, Material.OAK_PLANKS, 1, 3, 5),
                    // Door opening
                    TemplateBlock(3, 1, 0, Material.AIR, 1, 2, 1),
                    // Windows
                    TemplateBlock(1, 2, 0, Material.GLASS_PANE, 1, 1, 1),
                    TemplateBlock(5, 2, 0, Material.GLASS_PANE, 1, 1, 1),
                    TemplateBlock(0, 2, 1, Material.GLASS_PANE, 1, 1, 1),
                    TemplateBlock(0, 2, 5, Material.GLASS_PANE, 1, 1, 1),
                    TemplateBlock(6, 2, 1, Material.GLASS_PANE, 1, 1, 1),
                    TemplateBlock(6, 2, 5, Material.GLASS_PANE, 1, 1, 1),
                    // Roof
                    TemplateBlock(0, 4, 0, Material.OAK_STAIRS, 7, 1, 7),
                    // Interior
                    TemplateBlock(1, 1, 1, Material.CRAFTING_TABLE, 1, 1, 1),
                    TemplateBlock(2, 1, 1, Material.FURNACE, 1, 1, 1),
                    TemplateBlock(4, 1, 1, Material.CHEST, 1, 1, 1),
                    TemplateBlock(5, 1, 1, Material.RED_BED, 1, 1, 1)
                ),
                anchors = mapOf(
                    "entrance" to Vector(3, 1, -1),
                    "bed" to Vector(5, 1, 1),
                    "crafting" to Vector(1, 1, 1)
                )
            )
            templates.add(smallHouse)

            val forge = BuildingTemplate(
                id = "forge",
                name = "Blacksmith Forge",
                description = "A forge with furnace, anvil, and work area",
                author = "AINPC",
                version = 1,
                blocks = listOf(
                    TemplateBlock(0, 0, 0, Material.STONE_BRICKS, 9, 1, 7),
                    TemplateBlock(0, 1, 0, Material.STONE_BRICKS, 1, 3, 7),
                    TemplateBlock(8, 1, 0, Material.STONE_BRICKS, 1, 3, 7),
                    TemplateBlock(0, 1, 6, Material.STONE_BRICKS, 9, 3, 1),
                    TemplateBlock(1, 1, 0, Material.STONE_BRICKS, 7, 3, 1),
                    TemplateBlock(3, 1, 0, Material.AIR, 1, 2, 1),
                    TemplateBlock(4, 1, 1, Material.BLAST_FURNACE, 1, 1, 1),
                    TemplateBlock(4, 1, 2, Material.ANVIL, 1, 1, 1),
                    TemplateBlock(5, 1, 1, Material.SMITHING_TABLE, 1, 1, 1),
                    TemplateBlock(3, 1, 3, Material.GRINDSTONE, 1, 1, 1),
                    TemplateBlock(6, 1, 3, Material.CAULDRON, 1, 1, 1),
                    TemplateBlock(0, 4, 0, Material.STONE_BRICK_STAIRS, 9, 1, 7)
                ),
                anchors = mapOf(
                    "entrance" to Vector(4, 1, -1),
                    "forge" to Vector(4, 1, 1),
                    "anvil" to Vector(4, 1, 2)
                )
            )
            templates.add(forge)

            val farm = BuildingTemplate(
                id = "farm",
                name = "Crop Farm",
                description = "A 9x9 farmland with water center and fence",
                author = "AINPC",
                version = 1,
                blocks = listOf(
                    TemplateBlock(0, 0, 0, Material.FARMLAND, 9, 1, 9),
                    TemplateBlock(4, 0, 4, Material.WATER, 1, 1, 1),
                    TemplateBlock(0, 1, 0, Material.OAK_FENCE, 1, 1, 9),
                    TemplateBlock(8, 1, 0, Material.OAK_FENCE, 1, 1, 9),
                    TemplateBlock(0, 1, 0, Material.OAK_FENCE, 9, 1, 1),
                    TemplateBlock(0, 1, 8, Material.OAK_FENCE, 9, 1, 1),
                    TemplateBlock(4, 1, 0, Material.OAK_FENCE_GATE, 1, 1, 1),
                    TemplateBlock(1, 1, 1, Material.WHEAT, 7, 1, 7)
                ),
                anchors = mapOf(
                    "entrance" to Vector(4, 1, -1),
                    "center" to Vector(4, 1, 4)
                )
            )
            templates.add(farm)

            val tavern = BuildingTemplate(
                id = "tavern",
                name = "Tavern",
                description = "A two-story tavern with bar, tables, and rooms",
                author = "AINPC",
                version = 1,
                blocks = listOf(
                    // Ground floor
                    TemplateBlock(0, 0, 0, Material.SPRUCE_PLANKS, 11, 1, 11),
                    TemplateBlock(0, 1, 0, Material.SPRUCE_LOG, 1, 4, 1),
                    TemplateBlock(10, 1, 0, Material.SPRUCE_LOG, 1, 4, 1),
                    TemplateBlock(0, 1, 10, Material.SPRUCE_LOG, 1, 4, 1),
                    TemplateBlock(10, 1, 10, Material.SPRUCE_LOG, 1, 4, 1),
                    TemplateBlock(1, 1, 0, Material.SPRUCE_PLANKS, 9, 3, 1),
                    TemplateBlock(1, 1, 10, Material.SPRUCE_PLANKS, 9, 3, 1),
                    TemplateBlock(0, 1, 1, Material.SPRUCE_PLANKS, 1, 3, 9),
                    TemplateBlock(10, 1, 1, Material.SPRUCE_PLANKS, 1, 3, 9),
                    TemplateBlock(5, 1, 0, Material.SPRUCE_DOOR, 1, 2, 1),
                    TemplateBlock(2, 2, 0, Material.GLASS_PANE, 1, 1, 1),
                    TemplateBlock(8, 2, 0, Material.GLASS_PANE, 1, 1, 1),
                    TemplateBlock(0, 2, 2, Material.GLASS_PANE, 1, 1, 1),
                    TemplateBlock(0, 2, 8, Material.GLASS_PANE, 1, 1, 1),
                    TemplateBlock(10, 2, 2, Material.GLASS_PANE, 1, 1, 1),
                    TemplateBlock(10, 2, 8, Material.GLASS_PANE, 1, 1, 1),
                    // Bar
                    TemplateBlock(2, 1, 3, Material.BARREL, 3, 1, 1),
                    TemplateBlock(2, 2, 3, Material.BARREL, 3, 1, 1),
                    TemplateBlock(2, 1, 6, Material.BREWING_STAND, 1, 1, 1),
                    TemplateBlock(3, 1, 7, Material.CAULDRON, 1, 1, 1),
                    // Tables
                    TemplateBlock(5, 1, 3, Material.OAK_FENCE, 1, 1, 1),
                    TemplateBlock(5, 2, 3, Material.OAK_PRESSURE_PLATE, 1, 1, 1),
                    TemplateBlock(7, 1, 3, Material.OAK_FENCE, 1, 1, 1),
                    TemplateBlock(7, 2, 3, Material.OAK_PRESSURE_PLATE, 1, 1, 1),
                    TemplateBlock(5, 1, 7, Material.OAK_FENCE, 1, 1, 1),
                    TemplateBlock(5, 2, 7, Material.OAK_PRESSURE_PLATE, 1, 1, 1),
                    TemplateBlock(7, 1, 7, Material.OAK_FENCE, 1, 1, 1),
                    TemplateBlock(7, 2, 7, Material.OAK_PRESSURE_PLATE, 1, 1, 1),
                    // Second floor
                    TemplateBlock(0, 5, 0, Material.SPRUCE_PLANKS, 11, 1, 11),
                    TemplateBlock(0, 6, 0, Material.SPRUCE_LOG, 1, 3, 1),
                    TemplateBlock(10, 6, 0, Material.SPRUCE_LOG, 1, 3, 1),
                    TemplateBlock(0, 6, 10, Material.SPRUCE_LOG, 1, 3, 1),
                    TemplateBlock(10, 6, 10, Material.SPRUCE_LOG, 1, 3, 1),
                    TemplateBlock(1, 6, 0, Material.SPRUCE_PLANKS, 9, 3, 1),
                    TemplateBlock(1, 6, 10, Material.SPRUCE_PLANKS, 9, 3, 1),
                    TemplateBlock(0, 6, 1, Material.SPRUCE_PLANKS, 1, 3, 9),
                    TemplateBlock(10, 6, 1, Material.SPRUCE_PLANKS, 1, 3, 9),
                    TemplateBlock(2, 6, 2, Material.RED_BED, 3, 1, 1),
                    TemplateBlock(6, 6, 2, Material.RED_BED, 3, 1, 1),
                    TemplateBlock(0, 9, 0, Material.SPRUCE_STAIRS, 11, 1, 11)
                ),
                anchors = mapOf(
                    "entrance" to Vector(5, 1, -1),
                    "bar" to Vector(3, 1, 4),
                    "room1" to Vector(2, 6, 2),
                    "room2" to Vector(6, 6, 2)
                )
            )
            templates.add(tavern)

            return templates
        }
    }
}

