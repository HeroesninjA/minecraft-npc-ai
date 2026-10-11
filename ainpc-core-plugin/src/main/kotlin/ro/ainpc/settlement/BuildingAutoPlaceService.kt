package ro.ainpc.settlement

import ro.ainpc.AINPCPlugin
import ro.ainpc.api.settlement.BuildingTemplateDefinition
import ro.ainpc.story.StructureStoryCategory
import ro.ainpc.story.StructureStoryHooks
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldNode
import ro.ainpc.world.WorldNodeType
import ro.ainpc.world.WorldPlace
import ro.ainpc.world.WorldRegion
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.WorldMappingCompensator

/**
 * Buildingautoplaceservice.
 */
class BuildingAutoPlaceService private constructor(
    private val worldAdminProvider: () -> WorldAdminService,
) {
    constructor(plugin: AINPCPlugin) : this({ plugin.platform.worldAdminService })

    internal constructor(worldAdminService: WorldAdminService) : this({ worldAdminService })

/**
 * Autoplaceresult.
 */
    data class AutoPlaceResult(
        val templateId: String,
        val regionId: String,
        val placed: MutableList<String> = mutableListOf(),
        val warnings: MutableList<String> = mutableListOf(),
        val errors: MutableList<String> = mutableListOf(),
        var compensated: Boolean = false,
    ) {
        fun success(): Boolean = errors.isEmpty()
    }

    fun autoPlace(templateId: String, regionId: String): AutoPlaceResult {
        val worldAdmin = worldAdminProvider()
        val region = worldAdmin.getRegionModels().find { it.id == regionId }
            ?: return AutoPlaceResult(templateId, regionId, warnings = mutableListOf("Regiunea $regionId nu exista."))

        BuildingTemplateRegistry.loadDefaults()
        val template = BuildingTemplateRegistry.get(templateId)
            ?: return AutoPlaceResult(templateId, regionId, warnings = mutableListOf("Template-ul $templateId nu exista."))

        val result = AutoPlaceResult(templateId, regionId)
        template.metadata["story_category"]?.let { category ->
            if (!StructureStoryCategory.isValidOverride(template.placeType, category)) {
                result.errors.add("Template-ul $templateId are story_category invalid; este disponibil doar pentru custom cu o categorie story cunoscuta.")
                return result
            }
        }
        template.metadata["quest_hooks"]?.let { hooks ->
            if (StructureStoryHooks.parseConfigured(hooks) == null) {
                result.errors.add("Template-ul $templateId are quest_hooks invalid; foloseste ID-uri separate prin virgula.")
                return result
            }
        }
        if (0 !in template.supportedRotations) {
            result.errors.add("Template-ul $templateId nu permite rotatia 0 folosita de auto-place.")
            return result
        }
        if (template.footprintWidth <= 0 || template.footprintDepth <= 0 || template.footprintHeight <= 0 ||
            template.footprintWidth.toLong() * template.footprintDepth > Int.MAX_VALUE
        ) {
            result.errors.add("Template-ul $templateId are dimensiuni de footprint invalide.")
            return result
        }
        val invalidAnchor = template.anchors.firstOrNull { anchor ->
            anchor.offsetX !in 0..template.footprintWidth ||
                anchor.offsetY !in 0..template.footprintHeight ||
                anchor.offsetZ !in 0..template.footprintDepth ||
                !anchor.radius.isFinite() || anchor.radius <= 0.0
        }
        if (invalidAnchor != null) {
            result.errors.add("Ancora ${invalidAnchor.anchorId} din template-ul $templateId depaseste footprint-ul sau are raza invalida.")
            return result
        }
        val workerCapacity = template.metadata["max_workers"]?.toIntOrNull()
        if ("work" in template.tags && !template.metadata.containsKey("max_workers")) {
            result.errors.add("Template-ul $templateId cu tag work necesita max_workers.")
            return result
        }
        if (template.metadata.containsKey("max_workers") && (workerCapacity == null || workerCapacity < 0)) {
            result.errors.add("Template-ul $templateId are max_workers invalid; este necesar un intreg nenegativ.")
            return result
        }
        if ("work" in template.tags && workerCapacity != null && workerCapacity > 0 &&
            template.anchors.none { anchor ->
                anchor.nodeType in setOf("workstation", "interaction") || anchor.metadata["semantic"] == "work_anchor"
            }
        ) {
            result.errors.add("Template-ul $templateId cu posturi de munca necesita o ancora de lucru.")
            return result
        }
        if (template.metadata.containsKey("max_social_associations") &&
            template.metadata["max_social_associations"]?.toIntOrNull()?.let { it >= 0 } != true
        ) {
            result.errors.add("Template-ul $templateId are max_social_associations invalid; este necesar un intreg nenegativ.")
            return result
        }
        val residentCapacity = template.metadata["max_residents"]?.toIntOrNull()
        if (template.metadata.containsKey("max_residents") && (residentCapacity == null || residentCapacity <= 0)) {
            result.errors.add("Template-ul $templateId are max_residents invalid; este necesar un intreg pozitiv.")
            return result
        }
        if (template.placeType == "house" && residentCapacity != null) {
            val spawnCount = template.anchors.count { it.nodeType == "npc_spawn" }
            val homeCount = template.anchors.count { it.nodeType in setOf("bed", "home") }
            if (residentCapacity > minOf(spawnCount, homeCount)) {
                result.errors.add("Template-ul $templateId are max_residents=$residentCapacity fara perechi npc_spawn si bed/home distincte.")
                return result
            }
        }
        val createdPlaceIds = mutableListOf<String>()
        val createdNodeIds = mutableListOf<String>()
        val existingPlaces = worldAdmin.getPlaceModels(regionId)
        val offset = findFreeOffset(region, template, existingPlaces)

        if (offset == null) {
            result.warnings.add("Nu s-a gasit spatiu liber in regiunea $regionId pentru template-ul $templateId.")
            return result
        }

        var suffix = existingPlaces.size + 1
        while (worldAdmin.getPlace("$regionId:${templateId}_$suffix") != null) suffix++
        val placeId = "${templateId}_$suffix"
        val placeName = "${template.displayName} #$suffix"
        val placeType = PlaceType.fromId(template.placeType)

        try {
            val minX = offset.first
            val minZ = offset.second
            val maxX = minX + template.footprintWidth
            val maxZ = minZ + template.footprintDepth
            val y = region.minY + 1

            val place = worldAdmin.createPlace(
                regionId, placeId, placeName, region.worldName, placeType,
                minX, y, minZ, maxX, y + template.footprintHeight, maxZ
            )
            createdPlaceIds.add(place.id)
            place.setTags((template.tags + setOf("auto_placed", templateId)).toList())
            template.metadata.forEach { (key, value) -> place.putMetadata(key, value) }
            place.putMetadata("template_id", templateId)
            place.putMetadata("source", "auto_place")
            result.placed.add(place.id)

            for (anchor in template.anchors) {
                val nodeType = WorldNodeType.fromId(anchor.nodeType)
                val nx = (minX + anchor.offsetX).toDouble()
                val ny = (y + anchor.offsetY).toDouble()
                val nz = (minZ + anchor.offsetZ).toDouble()
                val radius = anchor.radius
                try {
                    val node = worldAdmin.createNode(
                        regionId, place.id, anchor.anchorId, nodeType,
                        region.worldName, nx, ny, nz, radius
                    )
                    createdNodeIds.add(node.id)
                    anchor.metadata.forEach { (key, value) -> node.putMetadata(key, value) }
                    if (anchor.tags.isNotEmpty()) node.putMetadata("tags", anchor.tags.joinToString(","))
                    node.putMetadata("source", "auto_place")
                    node.putMetadata("anchor_role", anchor.role)
                    result.placed.add(node.id)
                } catch (e: IllegalArgumentException) {
                    result.errors.add("Nu s-a putut crea nodul ${anchor.anchorId}: ${e.message}")
                }
            }
        } catch (e: IllegalArgumentException) {
            result.errors.add("Eroare la plasare: ${e.message}")
        }

        if (result.errors.isNotEmpty() && (createdPlaceIds.isNotEmpty() || createdNodeIds.isNotEmpty())) {
            val compensation = WorldMappingCompensator.rollback(
                worldAdmin,
                mutableListOf(),
                createdPlaceIds,
                createdNodeIds,
            )
            result.placed.clear()
            result.placed.addAll(createdPlaceIds)
            result.placed.addAll(createdNodeIds)
            result.compensated = compensation.success()
            result.warnings.add(compensation.summary())
            result.errors.addAll(compensation.failures)
        }

        return result
    }

    private fun findFreeOffset(
        region: WorldRegion,
        template: BuildingTemplateDefinition,
        existingPlaces: List<WorldPlace>,
    ): Pair<Int, Int>? {
        val regionWidth = region.maxX - region.minX
        val regionDepth = region.maxZ - region.minZ
        val tplW = template.footprintWidth
        val tplD = template.footprintDepth

        if (tplW > regionWidth || tplD > regionDepth) {
            return null
        }

        val step = 4
        val zStart = region.minZ + 2
        val zEnd = region.maxZ - tplD - 2
        val xStart = region.minX + 2
        val xEnd = region.maxX - tplW - 2

        var oz = zStart
        while (oz <= zEnd) {
            var ox = xStart
            while (ox <= xEnd) {
                var overlaps = false
                for (existing in existingPlaces) {
                    if (rectanglesOverlap(ox, oz, ox + tplW, oz + tplD, existing.minX, existing.minZ, existing.maxX, existing.maxZ)) {
                        overlaps = true
                        break
                    }
                }
                if (!overlaps) {
                    return ox to oz
                }
                ox += step
            }
            oz += step
        }
        return null
    }

    private fun rectanglesOverlap(
        ax1: Int, az1: Int, ax2: Int, az2: Int,
        bx1: Int, bz1: Int, bx2: Int, bz2: Int,
    ): Boolean {
        return ax1 <= bx2 && ax2 >= bx1 && az1 <= bz2 && az2 >= bz1
    }
}
