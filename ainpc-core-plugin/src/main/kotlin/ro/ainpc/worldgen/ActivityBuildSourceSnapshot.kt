package ro.ainpc.worldgen

import ro.ainpc.api.WorldAdminApi
import ro.ainpc.api.settlement.BuildingTemplateDefinition

internal data class ActivityBuildSourceSnapshot(
    private val template: BuildingTemplateDefinition,
    private val mapping: List<Any?>,
) {
    companion object {
        fun capture(worldAdmin: WorldAdminApi, definition: BuildingTemplateDefinition, placeId: String): ActivityBuildSourceSnapshot {
            val place = requireNotNull(worldAdmin.getPlace(placeId)) { "Mapped place no longer exists" }
            val region = requireNotNull(worldAdmin.getRegion(place.regionId())) { "Mapped region no longer exists" }
            require(place.placeType().id == definition.placeType && place.worldName() == region.worldName()) { "Mapped place type or world differs from template" }
            require(place.maxX().toLong() - place.minX() == definition.footprintWidth.toLong() &&
                place.maxY().toLong() - place.minY() == definition.footprintHeight.toLong() &&
                place.maxZ().toLong() - place.minZ() == definition.footprintDepth.toLong()) { "Mapped dimensions differ from template" }
            val nodes = worldAdmin.getNodesForPlace(place.id()).sortedBy { it.id() }
            for (anchor in definition.anchors) {
                val node = requireNotNull(nodes.singleOrNull { it.id() == "${place.id()}:${anchor.anchorId}" }) { "Mapped anchor ${anchor.anchorId} is missing or ambiguous" }
                require(node.typeId() == anchor.nodeType && node.worldName() == place.worldName() &&
                    node.x() == place.minX().toDouble() + anchor.offsetX &&
                    node.y() == place.minY().toDouble() + anchor.offsetY &&
                    node.z() == place.minZ().toDouble() + anchor.offsetZ && node.radius() == anchor.radius) {
                    "Mapped anchor ${anchor.anchorId} differs from template"
                }
            }
            val templateCopy = definition.copy(
                anchors = definition.anchors.map { it.copy(tags = it.tags.toSet(), metadata = it.metadata.toMap()) },
                variants = definition.variants.map { it.copy(materialOverrides = it.materialOverrides.toMap(), biomeFilter = it.biomeFilter.toList(), metadata = it.metadata.toMap()) },
                supportedRotations = definition.supportedRotations.toList(),
                requiredCapabilities = definition.requiredCapabilities.toList(),
                tags = definition.tags.toSet(), metadata = definition.metadata.toMap(),
            )
            return ActivityBuildSourceSnapshot(templateCopy, listOf(
                place.id(), place.regionId(), place.worldName(), place.displayName(), place.placeType(),
                place.minX(), place.minY(), place.minZ(), place.maxX(), place.maxY(), place.maxZ(),
                place.tags().sorted(), place.ownerNpcId(), place.publicAccess(), place.metadata().toMap(),
                region.id(), region.worldName(), region.minX(), region.minY(), region.minZ(), region.maxX(), region.maxY(), region.maxZ(),
                nodes.map { listOf(it.id(), it.regionId(), it.placeId(), it.typeId(), it.worldName(), it.x(), it.y(), it.z(), it.radius(), it.metadata().toMap()) },
            ))
        }
    }
}
