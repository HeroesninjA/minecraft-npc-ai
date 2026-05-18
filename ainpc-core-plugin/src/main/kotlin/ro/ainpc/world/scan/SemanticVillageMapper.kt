package ro.ainpc.world.scan

import ro.ainpc.world.PlaceType
import ro.ainpc.world.RegionType
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.WorldNode
import ro.ainpc.world.WorldNodeType
import ro.ainpc.world.WorldPlace
import ro.ainpc.world.WorldRegion
import java.util.LinkedHashMap
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class SemanticVillageMapper {

    fun importScan(
        worldAdmin: WorldAdminService,
        scan: VanillaVillageScanResult,
        requestedRegionId: String?
    ): SemanticVillageImportResult {
        val warnings = scan.warnings().toMutableList()
        val errors = mutableListOf<String>()
        val createdPlaceIds = mutableListOf<String>()
        val createdNodeIds = mutableListOf<String>()
        if (!scan.hasVillageSignals()) {
            errors.add("Scanarea nu contine clopot, pat sau workstation. Importul a fost oprit.")
            return SemanticVillageImportResult("", createdPlaceIds, createdNodeIds, warnings, errors)
        }

        val regionId = normalizeId(
            if (requestedRegionId.isNullOrBlank()) "vanilla_village_${scan.centerX()}_${scan.centerZ()}" else requestedRegionId
        )
        if (worldAdmin.getRegion(regionId) != null) {
            errors.add("Exista deja o regiune cu ID-ul $regionId.")
            return SemanticVillageImportResult(regionId, createdPlaceIds, createdNodeIds, warnings, errors)
        }

        val anchorFeatures = scan.features().filter { it.type() != VanillaVillageFeatureType.DOOR }
        val scanBox = Box.around(anchorFeatures).expand(24, 8, 24).clampY(scan.minY(), scan.maxY())

        try {
            val region: WorldRegion = worldAdmin.createRegion(
                regionId,
                "Vanilla Village ${scan.centerX()} ${scan.centerZ()}",
                scan.worldName(),
                RegionType.SETTLEMENT,
                scanBox.minX,
                scanBox.minY,
                scanBox.minZ,
                scanBox.maxX,
                scanBox.maxY,
                scanBox.maxZ
            )
            region.setTags(listOf("vanilla", "village", "scanned", "ainpc_phase_5"))

            createMeetingNodes(worldAdmin, scan, regionId, createdNodeIds, warnings)
            val placeBoxes = mutableListOf<CreatedPlaceBox>()
            val houseBoxes = createHousePlaces(worldAdmin, scan, regionId, scanBox, createdPlaceIds, createdNodeIds, placeBoxes, warnings)
            createFarmPlace(worldAdmin, scan, regionId, scanBox, createdPlaceIds, createdNodeIds, placeBoxes, warnings)
            createStandaloneWorkplaces(
                worldAdmin,
                scan,
                regionId,
                scanBox,
                houseBoxes,
                createdPlaceIds,
                createdNodeIds,
                placeBoxes,
                warnings
            )
        } catch (exception: IllegalArgumentException) {
            errors.add(exception.message ?: "")
        }

        return SemanticVillageImportResult(regionId, createdPlaceIds, createdNodeIds, warnings, errors)
    }

    private fun createMeetingNodes(
        worldAdmin: WorldAdminService,
        scan: VanillaVillageScanResult,
        regionId: String,
        createdNodeIds: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val bells = scan.bells()
        if (bells.isEmpty()) {
            val fallback = worldAdmin.createNode(
                regionId,
                null,
                "meeting_point_1",
                WorldNodeType.MEETING_POINT,
                scan.worldName(),
                scan.centerX().toDouble(),
                scan.centerY().toDouble(),
                scan.centerZ().toDouble(),
                4.0
            )
            tagNode(fallback, "vanilla_scan", "fallback_center", "meeting_point")
            createdNodeIds.add(fallback.id)
            warnings.add("A fost creat meeting_point fallback la centrul scanarii.")
            return
        }

        var index = 1
        for (bell in bells) {
            val node = worldAdmin.createNode(
                regionId,
                null,
                "bell_$index",
                WorldNodeType.MEETING_POINT,
                scan.worldName(),
                bell.x().toDouble(),
                bell.y().toDouble(),
                bell.z().toDouble(),
                5.0
            )
            tagNode(node, "vanilla_scan", bell.material(), "meeting_point")
            createdNodeIds.add(node.id)
            index++
        }
    }

    private fun createHousePlaces(
        worldAdmin: WorldAdminService,
        scan: VanillaVillageScanResult,
        regionId: String,
        regionBox: Box,
        createdPlaceIds: MutableList<String>,
        createdNodeIds: MutableList<String>,
        placeBoxes: MutableList<CreatedPlaceBox>,
        warnings: MutableList<String>
    ): Map<String, Box> {
        val houseBoxes = linkedMapOf<String, Box>()
        val clusters = mergeOverlappingClusters(clusterByDistance(scan.beds(), HOUSE_CLUSTER_DISTANCE), 4, 2)
        if (clusters.isEmpty()) {
            warnings.add("Nu au fost gasite paturi. Nu s-au creat case.")
            return houseBoxes
        }

        var houseIndex = 1
        for (cluster in clusters) {
            val box = cluster.box().expand(4, 2, 4).clamp(regionBox)
            val house: WorldPlace = worldAdmin.createPlace(
                regionId,
                "house_$houseIndex",
                "House $houseIndex",
                scan.worldName(),
                PlaceType.HOUSE,
                box.minX,
                box.minY,
                box.minZ,
                box.maxX,
                box.maxY,
                box.maxZ
            )
            house.setTags(listOf("vanilla", "house", "residential"))
            house.isPublicAccess = false
            house.putMetadata("source", "vanilla_scan")
            house.putMetadata("max_residents", cluster.features.size.toString())
            house.putMetadata("vanilla_beds", cluster.features.size.toString())
            createdPlaceIds.add(house.id)
            placeBoxes.add(CreatedPlaceBox(house.id, box))
            houseBoxes[house.id] = box

            var bedIndex = 1
            for (bed in cluster.features) {
                val bedNode = worldAdmin.createNode(
                    regionId,
                    house.id,
                    "bed_$bedIndex",
                    WorldNodeType.BED,
                    scan.worldName(),
                    bed.x().toDouble(),
                    bed.y().toDouble(),
                    bed.z().toDouble(),
                    1.5
                )
                tagNode(bedNode, "vanilla_scan", bed.material(), "bed")
                createdNodeIds.add(bedNode.id)
                bedIndex++
            }

            val homeBed = cluster.features[0]
            val homeNode = worldAdmin.createNode(
                regionId,
                house.id,
                "home_1",
                WorldNodeType.HOME,
                scan.worldName(),
                homeBed.x().toDouble(),
                homeBed.y().toDouble(),
                homeBed.z().toDouble(),
                2.5
            )
            tagNode(homeNode, "vanilla_scan", homeBed.material(), "home_anchor")
            createdNodeIds.add(homeNode.id)

            addEntranceNode(worldAdmin, scan, regionId, house.id, box, createdNodeIds)
            addContainedWorkstationNodes(worldAdmin, scan, regionId, house.id, box, createdNodeIds)
            houseIndex++
        }

        return houseBoxes
    }

    private fun createFarmPlace(
        worldAdmin: WorldAdminService,
        scan: VanillaVillageScanResult,
        regionId: String,
        regionBox: Box,
        createdPlaceIds: MutableList<String>,
        createdNodeIds: MutableList<String>,
        placeBoxes: MutableList<CreatedPlaceBox>,
        warnings: MutableList<String>
    ) {
        if (scan.farmlands().size < 9) {
            return
        }

        val farmBox = Box.around(scan.farmlands()).expand(1, 1, 1).clamp(regionBox)
        if (intersectsAny(placeBoxes, farmBox)) {
            warnings.add("Ferma vanilla detectata se suprapune cu o casa/workplace si a fost lasata doar ca semnal scanat.")
            return
        }

        val farm = worldAdmin.createPlace(
            regionId,
            "farm_1",
            "Farm 1",
            scan.worldName(),
            PlaceType.FARM,
            farmBox.minX,
            farmBox.minY,
            farmBox.minZ,
            farmBox.maxX,
            farmBox.maxY,
            farmBox.maxZ
        )
        farm.setTags(listOf("vanilla", "farm", "workplace"))
        farm.putMetadata("source", "vanilla_scan")
        farm.putMetadata("farmland_blocks", scan.farmlands().size.toString())
        createdPlaceIds.add(farm.id)
        placeBoxes.add(CreatedPlaceBox(farm.id, farmBox))

        val workNode = worldAdmin.createNode(
            regionId,
            farm.id,
            "work_1",
            WorldNodeType.WORK,
            scan.worldName(),
            farmBox.centerX().toDouble(),
            farmBox.centerY().toDouble(),
            farmBox.centerZ().toDouble(),
            4.0
        )
        tagNode(workNode, "vanilla_scan", "FARMLAND", "farm_work")
        createdNodeIds.add(workNode.id)
        addContainedWorkstationNodes(worldAdmin, scan, regionId, farm.id, farmBox, createdNodeIds)
    }

    private fun createStandaloneWorkplaces(
        worldAdmin: WorldAdminService,
        scan: VanillaVillageScanResult,
        regionId: String,
        regionBox: Box,
        houseBoxes: Map<String, Box>,
        createdPlaceIds: MutableList<String>,
        createdNodeIds: MutableList<String>,
        placeBoxes: MutableList<CreatedPlaceBox>,
        warnings: MutableList<String>
    ) {
        val standaloneWorkstations = scan.workstations().asSequence()
            .filter { feature -> houseBoxes.values.none { box -> box.contains(feature) } }
            .filter { feature -> placeBoxes.none { placeBox -> placeBox.box.contains(feature) } }
            .toList()
        val clusters = mergeOverlappingClusters(clusterByDistance(standaloneWorkstations, WORK_CLUSTER_DISTANCE), 2, 1)
        val prefixCounters = HashMap<String, Int>()
        for (cluster in clusters) {
            val box = cluster.box().expand(2, 1, 2).clamp(regionBox)
            if (intersectsAny(placeBoxes, box)) {
                warnings.add("Workstation vanilla la ${cluster.features[0].material()} se suprapune cu alt place si a fost sarit.")
                continue
            }

            val placeType = resolveWorkplaceType(cluster.features)
            val prefix = workplacePrefix(placeType, cluster.features)
            val index = prefixCounters.merge(prefix, 1, Int::plus) ?: 1
            val place = worldAdmin.createPlace(
                regionId,
                "${prefix}_$index",
                null,
                scan.worldName(),
                placeType,
                box.minX,
                box.minY,
                box.minZ,
                box.maxX,
                box.maxY,
                box.maxZ
            )
            place.setTags(listOf("vanilla", "workplace", prefix))
            place.putMetadata("source", "vanilla_scan")
            place.putMetadata("vanilla_materials", materialSummary(cluster.features))
            createdPlaceIds.add(place.id)
            placeBoxes.add(CreatedPlaceBox(place.id, box))

            var nodeIndex = 1
            for (workstation in cluster.features) {
                val workstationNode = worldAdmin.createNode(
                    regionId,
                    place.id,
                    "workstation_$nodeIndex",
                    WorldNodeType.WORKSTATION,
                    scan.worldName(),
                    workstation.x().toDouble(),
                    workstation.y().toDouble(),
                    workstation.z().toDouble(),
                    2.0
                )
                tagNode(workstationNode, "vanilla_scan", workstation.material(), "workstation")
                createdNodeIds.add(workstationNode.id)
                nodeIndex++
            }

            val anchor = cluster.features[0]
            val workNode = worldAdmin.createNode(
                regionId,
                place.id,
                "work_1",
                WorldNodeType.WORK,
                scan.worldName(),
                anchor.x().toDouble(),
                anchor.y().toDouble(),
                anchor.z().toDouble(),
                3.0
            )
            tagNode(workNode, "vanilla_scan", anchor.material(), "work_anchor")
            createdNodeIds.add(workNode.id)
        }
    }

    private fun addEntranceNode(
        worldAdmin: WorldAdminService,
        scan: VanillaVillageScanResult,
        regionId: String,
        placeId: String,
        houseBox: Box,
        createdNodeIds: MutableList<String>
    ) {
        scan.doors().asSequence()
            .filter { houseBox.contains(it) }
            .minByOrNull { door -> distanceToCenterSquared(houseBox, door) }
            ?.let { door ->
                val node = worldAdmin.createNode(
                    regionId,
                    placeId,
                    "entrance_1",
                    WorldNodeType.ENTRANCE,
                    scan.worldName(),
                    door.x().toDouble(),
                    door.y().toDouble(),
                    door.z().toDouble(),
                    2.0
                )
                tagNode(node, "vanilla_scan", door.material(), "entrance")
                createdNodeIds.add(node.id)
            }
    }

    private fun addContainedWorkstationNodes(
        worldAdmin: WorldAdminService,
        scan: VanillaVillageScanResult,
        regionId: String,
        placeId: String,
        box: Box,
        createdNodeIds: MutableList<String>
    ) {
        var index = 1
        val seen = HashSet<String>()
        for (workstation in scan.workstations()) {
            val key = "${workstation.x()}:${workstation.y()}:${workstation.z()}"
            if (!box.contains(workstation) || !seen.add(key)) {
                continue
            }
            val node = worldAdmin.createNode(
                regionId,
                placeId,
                "workstation_$index",
                WorldNodeType.WORKSTATION,
                scan.worldName(),
                workstation.x().toDouble(),
                workstation.y().toDouble(),
                workstation.z().toDouble(),
                2.0
            )
            tagNode(node, "vanilla_scan", workstation.material(), "workstation")
            createdNodeIds.add(node.id)
            index++
        }
    }

    private fun clusterByDistance(features: List<VanillaVillageFeature>, maxDistance: Int): List<FeatureCluster> {
        val clusters = mutableListOf<FeatureCluster>()
        val maxDistanceSquared = maxDistance * maxDistance
        for (feature in features) {
            var nearest: FeatureCluster? = null
            var nearestDistance = Int.MAX_VALUE
            for (cluster in clusters) {
                val distance = cluster.distanceToCenterSquared(feature)
                if (distance <= maxDistanceSquared && distance < nearestDistance) {
                    nearest = cluster
                    nearestDistance = distance
                }
            }
            if (nearest == null) {
                nearest = FeatureCluster()
                clusters.add(nearest)
            }
            nearest.add(feature)
        }
        return clusters
    }

    private fun mergeOverlappingClusters(clusters: List<FeatureCluster>, expandXz: Int, expandY: Int): List<FeatureCluster> {
        val merged = clusters.toMutableList()
        var changed: Boolean
        do {
            changed = false
            outer@ for (i in merged.indices) {
                for (j in i + 1 until merged.size) {
                    if (merged[i].box().expand(expandXz, expandY, expandXz)
                            .intersects(merged[j].box().expand(expandXz, expandY, expandXz))
                    ) {
                        merged[i].features.addAll(merged[j].features)
                        merged.removeAt(j)
                        changed = true
                        break@outer
                    }
                }
            }
        } while (changed)
        return merged
    }

    private fun intersectsAny(placeBoxes: List<CreatedPlaceBox>, candidate: Box): Boolean =
        placeBoxes.any { existing -> existing.box.intersects(candidate) }

    private fun resolveWorkplaceType(features: List<VanillaVillageFeature>): PlaceType {
        val material = dominantMaterial(features)
        return when (material) {
            "BLAST_FURNACE", "GRINDSTONE", "SMITHING_TABLE", "STONECUTTER" -> PlaceType.FORGE
            "COMPOSTER" -> PlaceType.FARM
            "SMOKER", "BARREL", "LECTERN", "CARTOGRAPHY_TABLE", "LOOM", "FLETCHING_TABLE", "BREWING_STAND", "CAULDRON" -> PlaceType.SHOP
            else -> PlaceType.CUSTOM
        }
    }

    private fun workplacePrefix(placeType: PlaceType, features: List<VanillaVillageFeature>): String =
        when (placeType) {
            PlaceType.FORGE -> "forge"
            PlaceType.FARM -> "farm_work"
            PlaceType.SHOP -> "shop"
            else -> normalizeId(dominantMaterial(features).lowercase(Locale.ROOT))
        }

    private fun dominantMaterial(features: List<VanillaVillageFeature>): String =
        features.groupingBy { it.material() }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: "CUSTOM"

    private fun materialSummary(features: List<VanillaVillageFeature>): String {
        val counts = LinkedHashMap<String, Int>()
        for (feature in features) {
            counts.merge(feature.material(), 1, Int::plus)
        }
        return counts.entries.joinToString(",") { entry -> "${entry.key}=${entry.value}" }
    }

    private fun tagNode(node: WorldNode, source: String, material: String, semantic: String) {
        node.putMetadata("source", source)
        node.putMetadata("vanilla_material", material)
        node.putMetadata("semantic", semantic)
    }

    private fun normalizeId(raw: String?): String {
        val normalized = (raw ?: "").trim().lowercase(Locale.ROOT)
            .replace(' ', '_')
            .replace(Regex("[^a-z0-9_\\-]"), "_")
            .replace(Regex("_+"), "_")
        return if (normalized.isBlank()) "vanilla_village" else normalized
    }

    private fun distanceToCenterSquared(box: Box, feature: VanillaVillageFeature): Int {
        val dx = box.centerX() - feature.x()
        val dz = box.centerZ() - feature.z()
        return dx * dx + dz * dz
    }

    private class FeatureCluster {
        val features: MutableList<VanillaVillageFeature> = mutableListOf()

        fun add(feature: VanillaVillageFeature) {
            features.add(feature)
        }

        private fun centerX(): Int = features.map { it.x() }.average().roundToInt()
        private fun centerZ(): Int = features.map { it.z() }.average().roundToInt()

        fun distanceToCenterSquared(feature: VanillaVillageFeature): Int {
            val dx = centerX() - feature.x()
            val dz = centerZ() - feature.z()
            return dx * dx + dz * dz
        }

        fun box(): Box = Box.around(features)
    }

    private data class CreatedPlaceBox(val placeId: String, val box: Box)

    private class Box(
        minX: Int,
        minY: Int,
        minZ: Int,
        maxX: Int,
        maxY: Int,
        maxZ: Int
    ) {
        val minX: Int = min(minX, maxX)
        val minY: Int = min(minY, maxY)
        val minZ: Int = min(minZ, maxZ)
        val maxX: Int = max(minX, maxX)
        val maxY: Int = max(minY, maxY)
        val maxZ: Int = max(minZ, maxZ)

        fun expand(x: Int, y: Int, z: Int): Box = Box(minX - x, minY - y, minZ - z, maxX + x, maxY + y, maxZ + z)

        fun clamp(outer: Box): Box = Box(
            max(minX, outer.minX),
            max(minY, outer.minY),
            max(minZ, outer.minZ),
            min(maxX, outer.maxX),
            min(maxY, outer.maxY),
            min(maxZ, outer.maxZ)
        )

        fun clampY(minAllowedY: Int, maxAllowedY: Int): Box =
            Box(minX, max(minY, minAllowedY), minZ, maxX, min(maxY, maxAllowedY), maxZ)

        fun intersects(other: Box): Boolean =
            maxX >= other.minX && other.maxX >= minX &&
                maxY >= other.minY && other.maxY >= minY &&
                maxZ >= other.minZ && other.maxZ >= minZ

        fun contains(feature: VanillaVillageFeature): Boolean =
            feature.x() >= minX && feature.x() <= maxX &&
                feature.y() >= minY && feature.y() <= maxY &&
                feature.z() >= minZ && feature.z() <= maxZ

        fun centerX(): Int = (minX + maxX) / 2
        fun centerY(): Int = (minY + maxY) / 2
        fun centerZ(): Int = (minZ + maxZ) / 2

        companion object {
            fun around(features: List<VanillaVillageFeature>): Box {
                if (features.isEmpty()) {
                    return Box(0, 0, 0, 0, 0, 0)
                }
                val minX = features.minOf { it.x() }
                val minY = features.minOf { it.y() }
                val minZ = features.minOf { it.z() }
                val maxX = features.maxOf { it.x() }
                val maxY = features.maxOf { it.y() }
                val maxZ = features.maxOf { it.z() }
                return Box(minX, minY, minZ, maxX, maxY, maxZ)
            }
        }
    }

    companion object {
        private const val HOUSE_CLUSTER_DISTANCE = 9
        private const val WORK_CLUSTER_DISTANCE = 6
    }
}
