package ro.ainpc.world.patch

import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.WorldNodeType
import ro.ainpc.world.patch.PatchValidationStatus

class VillagePatchApplier {

    private val PLACE_SIZE = 12
    private val NODE_RADIUS = 2.0
    private val DEFAULT_Y = 64
    private val PLACE_HEIGHT = 8

    fun apply(
        worldAdmin: WorldAdminService,
        plan: PatchPlan,
        regionIdOverride: String? = null
    ): VillagePatchApplyResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val createdPlaceIds = mutableListOf<String>()
        val createdNodeIds = mutableListOf<String>()

        if (plan.errors().isNotEmpty()) {
            errors.add("Planul ${plan.patchId()} are erori de validare si nu poate fi aplicat.")
            return result(plan, createdPlaceIds, createdNodeIds, errors, warnings)
        }
        if (plan.validationStatus() == PatchValidationStatus.BLOCKED) {
            warnings.add("Planul ${plan.patchId()} este BLOCKED (capabilitati lipsa). " +
                "Se aplica doar partea de mapping semantic (place-uri si node-uri).")
        }

        val regionId = regionIdOverride?.takeIf { it.isNotBlank() } ?: plan.targetRegionId()
        val region = worldAdmin.getRegion(regionId)
        if (region == null) {
            errors.add("Regiunea $regionId nu exista.")
            return result(plan, createdPlaceIds, createdNodeIds, errors, warnings)
        }

        val worldName = region.worldName()
        val centerX = (region.minX() + region.maxX()) / 2
        val centerZ = (region.minZ() + region.maxZ()) / 2
        val minY = maxOf(region.minY(), DEFAULT_Y - 4)
        val maxY = minOf(region.maxY(), DEFAULT_Y + PLACE_HEIGHT + 4)

        val existingPlaceCount = worldAdmin.getPlaces(regionId).size

        when (plan.buildMode()) {
            PatchBuildMode.SEMANTIC_ONLY -> applySemanticOnly(
                worldAdmin, plan, regionId, worldName, centerX, centerZ,
                createdNodeIds, errors, warnings
            )
            PatchBuildMode.NATIVE_PATCH -> applyNativePatch(
                worldAdmin, plan, regionId, worldName,
                centerX, centerZ, minY, maxY, existingPlaceCount,
                createdPlaceIds, createdNodeIds, errors, warnings
            )
            PatchBuildMode.WORLDEDIT_TEMPLATE -> {
                warnings.add("WorldEdit templates nu sunt suportate pentru apply direct. " +
                    "Patch-ul ${plan.patchId()} necesita WorldEdit.")
            }
        }

        return result(plan, createdPlaceIds, createdNodeIds, errors, warnings)
    }

    private fun applySemanticOnly(
        worldAdmin: WorldAdminService,
        plan: PatchPlan,
        regionId: String,
        worldName: String,
        centerX: Int,
        centerZ: Int,
        createdNodeIds: MutableList<String>,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val placeId = plan.targetPlaceId().takeIf { it.isNotBlank() }
        for (nodeId in plan.plannedNodes()) {
            val localNodeId = nodeId.substringAfterLast(':').ifEmpty { nodeId }
            try {
                val offsetX = (localNodeId.hashCode() % 7) - 3
                val offsetZ = ((localNodeId.hashCode() / 13) % 7) - 3
                val node = worldAdmin.createNode(
                    regionId, placeId, localNodeId,
                    resolveNodeType(localNodeId),
                    worldName,
                    (centerX + offsetX).toDouble(),
                    DEFAULT_Y.toDouble(),
                    (centerZ + offsetZ).toDouble(),
                    NODE_RADIUS
                )
                createdNodeIds.add(node.id)
            } catch (ex: IllegalArgumentException) {
                errors.add("Nu pot crea node-ul $nodeId: ${ex.message}")
            }
        }
    }

    private fun applyNativePatch(
        worldAdmin: WorldAdminService,
        plan: PatchPlan,
        regionId: String,
        worldName: String,
        centerX: Int,
        centerZ: Int,
        minY: Int,
        maxY: Int,
        existingPlaceCount: Int,
        createdPlaceIds: MutableList<String>,
        createdNodeIds: MutableList<String>,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        for ((index, plannedPlaceId) in plan.plannedPlaces().withIndex()) {
            val localPlaceId = plannedPlaceId.substringAfterLast(':').ifEmpty { plannedPlaceId }
            if (worldAdmin.getPlace("$regionId:$localPlaceId") != null) {
                warnings.add("Place-ul $regionId:$localPlaceId exista deja.")
                continue
            }

            val offset = (existingPlaceCount + index) * 14
            val placeCenterX = centerX - 30 + (offset % 40)
            val placeCenterZ = centerZ - 20 + (offset / 40) * 14

            val placeType = placeTypeFor(plan.type())
            try {
                val place = worldAdmin.createPlace(
                    regionId, localPlaceId, null, worldName, placeType,
                    placeCenterX - PLACE_SIZE / 2, minY, placeCenterZ - PLACE_SIZE / 2,
                    placeCenterX + PLACE_SIZE / 2, maxY, placeCenterZ + PLACE_SIZE / 2
                )
                if (plan.type() == PatchType.ADD_HOUSE) {
                    place.putMetadata("role", "home")
                    place.setTags(listOf("home", "house"))
                } else if (plan.type() == PatchType.ADD_WORKPLACE) {
                    place.putMetadata("role", "workplace")
                    place.setTags(listOf("workplace", "workshop"))
                } else if (plan.type() == PatchType.ADD_SOCIAL_PLACE) {
                    place.putMetadata("role", "social")
                    place.setTags(listOf("social", "hub"))
                }
                createdPlaceIds.add(place.id)
            } catch (ex: IllegalArgumentException) {
                errors.add("Nu pot crea place-ul $localPlaceId: ${ex.message}")
                continue
            }

            val placeNodeIds = plan.plannedNodes().filter { it.startsWith(plannedPlaceId) || it.startsWith(localPlaceId) }
            if (placeNodeIds.isEmpty()) {
                val defaultNodeId = localPlaceId + ":node"
                try {
                    val node = worldAdmin.createNode(
                        regionId, "$regionId:$localPlaceId", "node",
                        resolveNodeType(localPlaceId),
                        worldName,
                        placeCenterX.toDouble(),
                        DEFAULT_Y.toDouble(),
                        placeCenterZ.toDouble(),
                        NODE_RADIUS
                    )
                    createdNodeIds.add(node.id)
                } catch (ex: IllegalArgumentException) {
                    warnings.add("Nu pot crea node-ul implicit pentru $localPlaceId: ${ex.message}")
                }
            } else {
                for (nodeFullId in placeNodeIds) {
                    val localNodeId = nodeFullId.substringAfterLast(':').ifEmpty { nodeFullId }
                    try {
                        val offsetX = (localNodeId.hashCode() % 5) - 2
                        val offsetZ = ((localNodeId.hashCode() / 7) % 5) - 2
                        val node = worldAdmin.createNode(
                            regionId, "$regionId:$localPlaceId", localNodeId,
                            resolveNodeType(localNodeId),
                            worldName,
                            (placeCenterX + offsetX).toDouble(),
                            DEFAULT_Y.toDouble(),
                            (placeCenterZ + offsetZ).toDouble(),
                            NODE_RADIUS
                        )
                        createdNodeIds.add(node.id)
                    } catch (ex: IllegalArgumentException) {
                        warnings.add("Nu pot crea node-ul $nodeFullId: ${ex.message}")
                    }
                }
            }
        }
    }

    private fun placeTypeFor(patchType: PatchType): PlaceType = when (patchType) {
        PatchType.ADD_HOUSE -> PlaceType.HOUSE
        PatchType.ADD_WORKPLACE -> PlaceType.SHOP
        PatchType.ADD_SOCIAL_PLACE -> PlaceType.TAVERN
        else -> PlaceType.CUSTOM
    }

    private fun resolveNodeType(nodeId: String): WorldNodeType = when {
        nodeId.contains("bed", ignoreCase = true) || nodeId.contains("home", ignoreCase = true) -> WorldNodeType.BED
        nodeId.contains("entrance", ignoreCase = true) || nodeId.contains("entry", ignoreCase = true) -> WorldNodeType.ENTRANCE
        nodeId.contains("spawn", ignoreCase = true) -> WorldNodeType.NPC_SPAWN
        nodeId.contains("workstation", ignoreCase = true) || nodeId.contains("work", ignoreCase = true) -> WorldNodeType.WORKSTATION
        nodeId.contains("social", ignoreCase = true) || nodeId.contains("hub", ignoreCase = true) -> WorldNodeType.SOCIAL
        nodeId.contains("meeting", ignoreCase = true) -> WorldNodeType.MEETING_POINT
        nodeId.contains("quest", ignoreCase = true) || nodeId.contains("board", ignoreCase = true) -> WorldNodeType.QUEST_TRIGGER
        nodeId.contains("inspect", ignoreCase = true) || nodeId.contains("interact", ignoreCase = true) -> WorldNodeType.INTERACTION
        nodeId.contains("node", ignoreCase = true) -> WorldNodeType.INTERACTION
        else -> WorldNodeType.CUSTOM
    }

    private fun result(
        plan: PatchPlan,
        createdPlaceIds: List<String>,
        createdNodeIds: List<String>,
        errors: List<String>,
        warnings: List<String>
    ): VillagePatchApplyResult = VillagePatchApplyResult(
        plan.patchId(),
        listOf(plan.patchId()),
        createdPlaceIds,
        createdNodeIds,
        errors,
        warnings
    )
}
