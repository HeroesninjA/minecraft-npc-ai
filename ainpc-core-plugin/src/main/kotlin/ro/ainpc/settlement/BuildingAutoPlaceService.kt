package ro.ainpc.settlement

import ro.ainpc.AINPCPlugin
import ro.ainpc.api.settlement.BuildingTemplateDefinition
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldNode
import ro.ainpc.world.WorldNodeType
import ro.ainpc.world.WorldPlace
import ro.ainpc.world.WorldRegion
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.WorldMappingCompensator

class BuildingAutoPlaceService private constructor(
    private val worldAdminProvider: () -> WorldAdminService,
) {
    constructor(plugin: AINPCPlugin) : this({ plugin.platform.worldAdminService })

    internal constructor(worldAdminService: WorldAdminService) : this({ worldAdminService })

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
        val createdPlaceIds = mutableListOf<String>()
        val createdNodeIds = mutableListOf<String>()
        val existingPlaces = worldAdmin.getPlaceModels(regionId)
        val offset = findFreeOffset(region, template, existingPlaces)

        if (offset == null) {
            result.warnings.add("Nu s-a gasit spatiu liber in regiunea $regionId pentru template-ul $templateId.")
            return result
        }

        val placeId = "${templateId}_${existingPlaces.size + 1}"
        val placeName = "${template.displayName} #${existingPlaces.size + 1}"
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
            place.setTags(listOf("auto_placed", templateId))
            place.putMetadata("template_id", templateId)
            place.putMetadata("source", "auto_place")
            worldAdmin.registerPlace(place)
            result.placed.add(place.id)

            for (anchor in template.anchors) {
                val nodeType = WorldNodeType.fromId(anchor.nodeType)
                val nx = (minX + anchor.offsetX).toDouble()
                val ny = (y + anchor.offsetY).toDouble()
                val nz = (minZ + anchor.offsetZ).toDouble()
                val radius = maxOf(anchor.radius, 1.0)
                try {
                    val node = worldAdmin.createNode(
                        regionId, place.id, anchor.anchorId, nodeType,
                        region.worldName, nx, ny, nz, radius
                    )
                    createdNodeIds.add(node.id)
                    node.putMetadata("source", "auto_place")
                    node.putMetadata("anchor_role", anchor.role)
                    worldAdmin.registerNode(node)
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
        return ax1 < bx2 && ax2 > bx1 && az1 < bz2 && az2 > bz1
    }
}
