package ro.ainpc.world.fixture

import ro.ainpc.world.PlaceType
import ro.ainpc.world.RegionType
import ro.ainpc.world.WorldNodeType
import ro.ainpc.world.WorldAdminService

class ControlledTestWorldFixtureApplyResult(
    fixtureId: String?,
    regionIds: List<String>?,
    placeIds: List<String>?,
    nodeIds: List<String>?,
    errors: List<String>?,
    warnings: List<String>?
) {
    private val fixtureId: String = fixtureId?.trim().orEmpty()
    private val regionIds: List<String> = (regionIds ?: emptyList()).toList()
    private val placeIds: List<String> = (placeIds ?: emptyList()).toList()
    private val nodeIds: List<String> = (nodeIds ?: emptyList()).toList()
    private val errors: List<String> = (errors ?: emptyList()).toList()
    private val warnings: List<String> = (warnings ?: emptyList()).toList()

    fun fixtureId(): String = fixtureId
    fun regionIds(): List<String> = regionIds
    fun placeIds(): List<String> = placeIds
    fun nodeIds(): List<String> = nodeIds
    fun regionCount(): Int = regionIds.size
    fun placeCount(): Int = placeIds.size
    fun nodeCount(): Int = nodeIds.size
    fun errors(): List<String> = errors
    fun warnings(): List<String> = warnings
    fun success(): Boolean = errors.isEmpty()
}

class ControlledTestWorldFixtureApplier {

    private val EXTERIOR_REGION_SIZE = 80
    private val VILLAGE_REGION_SIZE = 144
    private val PLACE_SIZE = 16
    private val NODE_RADIUS = 2.0
    private val DEFAULT_Y = 64
    private val REGION_HEIGHT = 30

    fun apply(
        worldAdminService: WorldAdminService,
        plan: ControlledTestWorldFixturePlan,
        worldName: String?,
        centerX: Int,
        centerZ: Int
    ): ControlledTestWorldFixtureApplyResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val createdRegionIds = mutableListOf<String>()
        val createdPlaceIds = mutableListOf<String>()
        val createdNodeIds = mutableListOf<String>()

        val safeWorldName = worldName?.trim().orEmpty()
        if (safeWorldName.isBlank()) {
            errors.add("Numele lumii nu poate fi gol.")
            return result(plan, createdRegionIds, createdPlaceIds, createdNodeIds, errors, warnings)
        }

        for (regionPlan in listOf(plan.villageRegion()) + plan.exteriorRegions()) {
            val regionId = regionPlan.id()
            if (regionId.isBlank()) {
                warnings.add("Regiune cu ID gol, sar peste.")
                continue
            }

            if (worldAdminService.getRegion(regionId) != null) {
                warnings.add("Regiunea $regionId exista deja, se sare peste.")
                continue
            }

            val regionSize = regionSize(regionPlan)
            val offsetX = regionPlan.offsetX()
            val offsetZ = regionPlan.offsetZ()
            val regMinX = centerX + offsetX - regionSize / 2
            val regMaxX = regMinX + regionSize
            val regMinZ = centerZ + offsetZ - regionSize / 2
            val regMaxZ = regMinZ + regionSize
            val regionType = RegionType.fromId(regionPlan.type())

            try {
                val region = worldAdminService.createRegion(
                    regionId,
                    regionPlan.displayName(),
                    safeWorldName,
                    regionType,
                    regMinX, DEFAULT_Y - 4, regMinZ,
                    regMaxX, DEFAULT_Y + REGION_HEIGHT, regMaxZ
                )
                if (regionPlan.tags().isNotEmpty()) {
                    region.setTags(regionPlan.tags())
                }
                createdRegionIds.add(region.id)
            } catch (ex: IllegalArgumentException) {
                errors.add("Nu pot crea regiunea $regionId: ${ex.message}")
                continue
            }

            for (nodeId in regionPlan.requiredNodes()) {
                val node = createRegionNode(worldAdminService, regionId, safeWorldName, nodeId, centerX + offsetX, centerZ + offsetZ)
                if (node != null) {
                    createdNodeIds.add(node.id)
                } else {
                    warnings.add("Nu pot crea node-ul de regiune $regionId:$nodeId.")
                }
            }

            for ((placeIndex, placePlan) in regionPlan.plannedPlaces().withIndex()) {
                val placeId = placePlan.id()
                if (placeId.isBlank()) continue

                if (worldAdminService.getPlace(placeId) != null) {
                    warnings.add("Place-ul $placeId exista deja, se sare peste.")
                    continue
                }

                val placeType = PlaceType.fromId(placePlan.type())
                val placePos = assignPlacePosition(placeIndex, centerX + offsetX, centerZ + offsetZ)

                try {
                    val place = worldAdminService.createPlace(
                        regionId,
                        placeId.substringAfterLast(':').ifEmpty { placeId },
                        null,
                        safeWorldName,
                        placeType,
                        placePos.x - PLACE_SIZE / 2, DEFAULT_Y - 1, placePos.z - PLACE_SIZE / 2,
                        placePos.x + PLACE_SIZE / 2, DEFAULT_Y + 7, placePos.z + PLACE_SIZE / 2
                    )
                    place.putMetadata("role", placePlan.role())
                    if (placePlan.tags().isNotEmpty()) {
                        place.setTags(placePlan.tags())
                    }
                    createdPlaceIds.add(place.id)

                    for (nodeId in placePlan.requiredNodes()) {
                        val node = createPlaceNode(worldAdminService, regionId, placeId, safeWorldName, nodeId, placePos.x, placePos.z)
                        if (node != null) {
                            createdNodeIds.add(node.id)
                        } else {
                            warnings.add("Nu pot crea node-ul $placeId:$nodeId.")
                        }
                    }
                } catch (ex: IllegalArgumentException) {
                    errors.add("Nu pot crea place-ul $placeId: ${ex.message}")
                }
            }
        }

        return result(plan, createdRegionIds, createdPlaceIds, createdNodeIds, errors, warnings)
    }

    private fun createRegionNode(
        worldAdminService: WorldAdminService,
        regionId: String,
        worldName: String,
        nodeId: String,
        centerX: Int,
        centerZ: Int
    ): ro.ainpc.world.WorldNode? {
        val nodeType = resolveNodeType(nodeId)
        return try {
            worldAdminService.createNode(
                regionId, null, nodeId, nodeType,
                worldName, centerX.toDouble(), DEFAULT_Y.toDouble(), centerZ.toDouble(), NODE_RADIUS
            )
        } catch (ex: IllegalArgumentException) {
            null
        }
    }

    private fun createPlaceNode(
        worldAdminService: WorldAdminService,
        regionId: String,
        placeId: String,
        worldName: String,
        nodeId: String,
        placeCenterX: Int,
        placeCenterZ: Int
    ): ro.ainpc.world.WorldNode? {
        val nodeType = resolveNodeType(nodeId)
        val offsetX = (nodeId.hashCode() % 5) - 2
        val offsetZ = ((nodeId.hashCode() / 7) % 5) - 2
        return try {
            worldAdminService.createNode(
                regionId, placeId, nodeId, nodeType,
                worldName, (placeCenterX + offsetX).toDouble(), DEFAULT_Y.toDouble(), (placeCenterZ + offsetZ).toDouble(), NODE_RADIUS
            )
        } catch (ex: IllegalArgumentException) {
            null
        }
    }

    private fun regionSize(regionPlan: ControlledFixtureRegionPlan): Int =
        if (regionPlan.type().equals("settlement", ignoreCase = true)) VILLAGE_REGION_SIZE else EXTERIOR_REGION_SIZE

    private fun resolveNodeType(nodeId: String): WorldNodeType = when {
        nodeId.contains("spawn", ignoreCase = true) -> WorldNodeType.NPC_SPAWN
        nodeId.contains("bed", ignoreCase = true) || nodeId.contains("home", ignoreCase = true) -> WorldNodeType.BED
        nodeId.contains("door", ignoreCase = true) || nodeId.contains("entry", ignoreCase = true) || nodeId.contains("entrance", ignoreCase = true) -> WorldNodeType.ENTRANCE
        nodeId.contains("workstation", ignoreCase = true) || nodeId.contains("work", ignoreCase = true) -> WorldNodeType.WORKSTATION
        nodeId.contains("social", ignoreCase = true) || nodeId.contains("conversation", ignoreCase = true) -> WorldNodeType.SOCIAL
        nodeId.contains("meeting", ignoreCase = true) -> WorldNodeType.MEETING_POINT
        nodeId.contains("quest", ignoreCase = true) || nodeId.contains("trigger", ignoreCase = true) -> WorldNodeType.QUEST_TRIGGER
        nodeId.contains("inspect", ignoreCase = true) || nodeId.contains("interaction", ignoreCase = true) -> WorldNodeType.INTERACTION
        nodeId.contains("ritual", ignoreCase = true) || nodeId.contains("progression", ignoreCase = true) -> WorldNodeType.PROGRESSION
        nodeId.contains("lookout", ignoreCase = true) || nodeId.contains("watch", ignoreCase = true) -> WorldNodeType.CUSTOM
        else -> WorldNodeType.INTERACTION
    }

    private fun assignPlacePosition(index: Int, regionCenterX: Int, regionCenterZ: Int): PlacePosition {
        val cols = 3
        val spacing = 18
        val col = index % cols
        val row = index / cols
        return PlacePosition(
            regionCenterX + (col - cols / 2) * spacing,
            regionCenterZ + (row - 1) * spacing
        )
    }

    private fun result(
        plan: ControlledTestWorldFixturePlan,
        regionIds: List<String>,
        placeIds: List<String>,
        nodeIds: List<String>,
        errors: List<String>,
        warnings: List<String>
    ): ControlledTestWorldFixtureApplyResult =
        ControlledTestWorldFixtureApplyResult(
            plan.fixtureId(), regionIds, placeIds, nodeIds, errors, warnings
        )

    private data class PlacePosition(val x: Int, val z: Int)
}
