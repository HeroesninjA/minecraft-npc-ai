package ro.ainpc.spawn

import ro.ainpc.api.WorldAdminApi
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.util.Locale
import kotlin.math.abs

class HouseAllocationPlanner {

    fun plan(worldAdmin: WorldAdminApi?, houseSelector: String?, requestedResidents: Int): PlanningResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (worldAdmin == null || !worldAdmin.isEnabled) {
            errors.add("WorldAdmin este dezactivat sau indisponibil.")
            return PlanningResult.failed(errors, warnings)
        }

        val house = resolveHouse(worldAdmin, houseSelector, errors) ?: return PlanningResult.failed(errors, warnings)

        val houseNodes = worldAdmin.getNodesForPlace(house.id()).sortedBy { it.id() }
        val spawnNodes = nodesMatching(houseNodes, "npc_spawn", "spawn")
        val homeNodes = nodesMatching(houseNodes, "bed", "home")
        if (spawnNodes.isEmpty()) {
            errors.add("Casa ${house.id()} nu are node npc_spawn/spawn pentru spawn plan.")
        }
        if (homeNodes.isEmpty()) {
            errors.add("Casa ${house.id()} nu are node bed/home pentru spawn plan.")
        }

        val metadataCapacity = parsePositiveIntMetadata(house, "max_residents", "maxResidents", "capacity")
        val nodeCapacity = minOf(spawnNodes.size, homeNodes.size)
        val maxResidents = if (metadataCapacity > 0) metadataCapacity else maxOf(1, nodeCapacity)
        var residentCount = if (requestedResidents > 0) requestedResidents else minOf(maxResidents, nodeCapacity)
        if (residentCount > maxResidents) {
            warnings.add(
                "Au fost ceruti $residentCount rezidenti, dar casa are maxResidents=$maxResidents. Folosesc capacitatea casei."
            )
            residentCount = maxResidents
        }
        if (nodeCapacity > 0 && residentCount > nodeCapacity) {
            warnings.add(
                "Casa ${house.id()} are doar $nodeCapacity perechi spawn/home disponibile. Folosesc $nodeCapacity rezidenti."
            )
            residentCount = nodeCapacity
        }
        if (residentCount <= 0) {
            errors.add("Nu exista capacitate valida pentru rezidenti in casa ${house.id()}.")
        }
        if (errors.isNotEmpty()) {
            return PlanningResult.failed(errors, warnings)
        }

        val workplaces = worldAdmin.getPlaces(house.regionId()).asSequence()
            .filter { !it.id().equals(house.id(), ignoreCase = true) }
            .filter { isWorkplace(it) }
            .sortedWith(compareBy<WorldPlaceInfo> { workplacePriority(it) }.thenBy { it.id() })
            .toList()
        val socialPlaces = worldAdmin.getPlaces(house.regionId()).asSequence()
            .filter { !it.id().equals(house.id(), ignoreCase = true) }
            .filter { isSocialPlace(it) }
            .sortedWith(compareBy<WorldPlaceInfo> { socialPriority(it) }.thenBy { it.id() })
            .toList()

        val builder = HouseAllocation.builder(house.id()).maxResidents(maxResidents)
        if (residentCount > 1) {
            builder.familyId("family_${normalizeId(localId(house.id()))}")
        }

        var firstNpcKey = ""
        for (index in 0 until residentCount) {
            val npcKey = "${normalizeId(house.id())}_resident_${index + 1}"
            val npcName = buildNpcName(house, index)
            val relationRole = relationRole(index, residentCount)
            val workPlace = workplaces.getOrNull(index)
            val workNode = if (workPlace != null) findBestNodeForPlace(worldAdmin, workPlace, "work") else null
            val socialPlace = if (socialPlaces.isEmpty()) null else socialPlaces[index % socialPlaces.size]
            val socialNode = if (socialPlace != null) findBestNodeForPlace(worldAdmin, socialPlace, "social") else null

            val residentBuilder = HouseAllocation.ResidentPlan.builder(npcKey, npcName)
                .relationRole(relationRole)
                .occupation(if (workPlace != null) occupationForWorkplace(workPlace) else "locuitor")
                .age(defaultAge(index, residentCount))
                .gender(defaultGender(index))
                .archetype(if (workPlace != null) archetypeForWorkplace(workPlace) else "caregiver")
                .spawnNodeId(spawnNodes[index].id())
                .homeNodeId(homeNodes[index].id())
                .bedNodeId(homeNodes[index].id())

            if (workPlace != null) {
                residentBuilder.workPlaceId(workPlace.id())
                if (workNode != null) {
                    residentBuilder.workNodeId(workNode.id())
                } else {
                    warnings.add(
                        "Workplace-ul ${workPlace.id()} nu are node work/workstation; orchestratorul va folosi centrul place-ului."
                    )
                }
            }
            if (socialPlace != null) {
                residentBuilder.socialPlaceId(socialPlace.id())
                if (socialNode != null) {
                    residentBuilder.socialNodeId(socialNode.id())
                }
            }

            if (firstNpcKey.isBlank()) {
                firstNpcKey = npcKey
            }
            builder.addResident(residentBuilder.build())
        }
        builder.primaryOwnerNpcKey(firstNpcKey)

        val allocation = builder.build()
        if (workplaces.isEmpty()) {
            warnings.add("Nu exista workplace in regiunea ${house.regionId()}; rezidentii generati primesc ocupatia locuitor.")
        }
        if (socialPlaces.isEmpty()) {
            warnings.add("Nu exista loc social in regiunea ${house.regionId()}; planul nu seteaza socialPlaceId.")
        }

        return PlanningResult.success(allocation, warnings)
    }

    fun planSettlement(worldAdmin: WorldAdminApi?, regionSelector: String?, maxHouses: Int): SettlementPlanningResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val allocations = mutableListOf<HouseAllocation>()

        if (worldAdmin == null || !worldAdmin.isEnabled) {
            errors.add("WorldAdmin este dezactivat sau indisponibil.")
            return SettlementPlanningResult.failed("", allocations, errors, warnings)
        }

        val region = resolveRegion(worldAdmin, regionSelector, errors)
            ?: return SettlementPlanningResult.failed("", allocations, errors, warnings)

        val houses = worldAdmin.getPlaces(region.id()).asSequence()
            .filter { isHousePlace(it) }
            .sortedBy { it.id() }
            .toList()
        if (houses.isEmpty()) {
            errors.add("Regiunea ${region.id()} nu are case/place-uri home.")
            return SettlementPlanningResult.failed(region.id(), allocations, errors, warnings)
        }

        val limit = if (maxHouses > 0) minOf(maxHouses, houses.size) else houses.size
        for (house in houses.take(limit)) {
            val planning = plan(worldAdmin, house.id(), 0)
            if (planning.success()) {
                allocations.add(planning.allocation()!!)
            }
            planning.warnings().forEach { warning -> warnings.add("${house.id()}: $warning") }
            planning.errors().forEach { error -> errors.add("${house.id()}: $error") }
        }

        if (maxHouses > 0 && houses.size > maxHouses) {
            warnings.add("Regiunea ${region.id()} are ${houses.size} case; au fost planificate primele $maxHouses.")
        }
        if (allocations.isEmpty()) {
            errors.add("Nu a fost generat niciun HouseAllocation valid pentru regiunea ${region.id()}.")
        }

        return if (errors.isEmpty()) {
            SettlementPlanningResult.success(region.id(), allocations, warnings)
        } else {
            SettlementPlanningResult.failed(region.id(), allocations, errors, warnings)
        }
    }

    private fun resolveHouse(worldAdmin: WorldAdminApi, houseSelector: String?, errors: MutableList<String>): WorldPlaceInfo? {
        if (houseSelector.isNullOrBlank()) {
            errors.add("Trebuie specificat homePlaceId.")
            return null
        }

        val selector = houseSelector.trim()
        val idSuffix = ":${selector.lowercase(Locale.ROOT)}"
        val matches = worldAdmin.places.asSequence()
            .filter { place ->
                place.id().equals(selector, ignoreCase = true) ||
                    place.displayName().equals(selector, ignoreCase = true) ||
                    place.id().lowercase(Locale.ROOT).endsWith(idSuffix)
            }
            .sortedBy { it.id() }
            .toList()

        if (matches.size != 1) {
            if (matches.isEmpty()) {
                errors.add("Casa/place-ul $houseSelector nu a fost gasit.")
            } else {
                errors.add("Selectorul $houseSelector este ambiguu: ${matches.map { it.id() }}")
            }
            return null
        }

        val house = matches.first()
        if (!isHousePlace(house)) {
            errors.add("Place-ul ${house.id()} nu este marcat ca house/home.")
            return null
        }
        return house
    }

    private fun resolveRegion(worldAdmin: WorldAdminApi, regionSelector: String?, errors: MutableList<String>): WorldRegionInfo? {
        if (regionSelector.isNullOrBlank()) {
            errors.add("Trebuie specificat regionId.")
            return null
        }

        val selector = regionSelector.trim()
        val matches = worldAdmin.regions.asSequence()
            .filter { region -> region.id().equals(selector, ignoreCase = true) || region.name().equals(selector, ignoreCase = true) }
            .sortedBy { it.id() }
            .toList()

        if (matches.size == 1) {
            return matches.first()
        }
        if (matches.isEmpty()) {
            errors.add("Regiunea $regionSelector nu a fost gasita.")
        } else {
            errors.add("Selectorul $regionSelector este ambiguu: ${matches.map { it.id() }}")
        }
        return null
    }

    private fun nodesMatching(nodes: List<WorldNodeInfo>, vararg tokens: String): List<WorldNodeInfo> =
        nodes.asSequence()
            .filter { node -> nodeMatchesAny(node, *tokens) }
            .sortedWith(compareBy<WorldNodeInfo> { nodePriority(it, *tokens) }.thenBy { it.id() })
            .toList()

    private fun findBestNodeForPlace(worldAdmin: WorldAdminApi, place: WorldPlaceInfo, anchorRole: String): WorldNodeInfo? =
        worldAdmin.getNodesForPlace(place.id()).asSequence()
            .filter { node -> nodePriorityForAnchor(node, anchorRole) >= 0 }
            .sortedWith(
                compareBy<WorldNodeInfo> { nodePriorityForAnchor(it, anchorRole) }
                    .thenBy { distanceSquaredToPlaceCenter(place, it) }
                    .thenBy { it.id() }
            )
            .firstOrNull()

    private fun nodePriorityForAnchor(node: WorldNodeInfo, anchorRole: String): Int = when (anchorRole) {
        "work" -> {
            if (matchesAnyToken(node.typeId(), "work") || matchesAnyToken(node.metadata()["semantic"], "work_anchor")) {
                0
            } else if (nodeMatchesAny(node, "work", "workplace", "workstation", "job", "munca", "lucru")) {
                1
            } else if (nodeMatchesAny(node, "interaction", "counter", "desk", "npc_spawn", "spawn")) {
                2
            } else {
                -1
            }
        }
        "social" -> {
            if (matchesAnyToken(node.typeId(), "social", "meeting_point") ||
                matchesAnyToken(node.metadata()["semantic"], "social_anchor", "meeting_point")
            ) {
                0
            } else if (nodeMatchesAny(node, "social", "meeting_point", "meeting", "market", "well", "tavern", "piata")) {
                1
            } else if (nodeMatchesAny(node, "interaction", "npc_spawn", "spawn")) {
                2
            } else {
                -1
            }
        }
        else -> -1
    }

    private fun nodePriority(node: WorldNodeInfo, vararg tokens: String): Int =
        if (tokens.isNotEmpty() && matchesAnyToken(node.typeId(), *tokens)) 0 else 1

    private fun isHousePlace(place: WorldPlaceInfo): Boolean =
        place.placeType() == PlaceType.HOUSE ||
            place.hasTag("home") ||
            place.hasTag("house") ||
            metadataEquals(place, "role", "home") ||
            metadataEquals(place, "purpose", "home")

    private fun isWorkplace(place: WorldPlaceInfo): Boolean =
        place.hasTag("work") ||
            place.hasTag("workplace") ||
            place.hasTag("job") ||
            metadataEquals(place, "role", "work") ||
            metadataEquals(place, "purpose", "work") ||
            when (place.placeType()) {
                PlaceType.FORGE, PlaceType.SHOP, PlaceType.FARM, PlaceType.MARKET, PlaceType.TAVERN -> true
                else -> false
            }

    private fun isSocialPlace(place: WorldPlaceInfo): Boolean =
        place.placeType() == PlaceType.MARKET ||
            place.placeType() == PlaceType.TAVERN ||
            place.placeType() == PlaceType.CAMP ||
            place.hasTag("social") ||
            place.hasTag("public") ||
            place.hasTag("meeting") ||
            metadataEquals(place, "role", "social") ||
            metadataEquals(place, "purpose", "social")

    private fun workplacePriority(place: WorldPlaceInfo): Int = when (place.placeType()) {
        PlaceType.FORGE -> 0
        PlaceType.FARM -> 1
        PlaceType.MARKET -> 2
        PlaceType.TAVERN -> 3
        PlaceType.SHOP -> 4
        else -> 5
    }

    private fun socialPriority(place: WorldPlaceInfo): Int = when (place.placeType()) {
        PlaceType.MARKET -> 0
        PlaceType.TAVERN -> 1
        PlaceType.CAMP -> 2
        else -> 3
    }

    private fun occupationForWorkplace(place: WorldPlaceInfo): String {
        val configuredProfession = firstNonBlank(place.metadata()["profession"], place.metadata()["occupation"])
        if (configuredProfession.isNotBlank()) {
            return configuredProfession
        }
        return when (place.placeType()) {
            PlaceType.FORGE -> "fierar"
            PlaceType.FARM -> "fermier"
            PlaceType.MARKET, PlaceType.SHOP -> "negustor"
            PlaceType.TAVERN -> "hangiu"
            PlaceType.CAMP -> "paznic"
            else -> "locuitor"
        }
    }

    private fun archetypeForWorkplace(place: WorldPlaceInfo): String = when (place.placeType()) {
        PlaceType.FORGE, PlaceType.FARM, PlaceType.SHOP -> "creator"
        PlaceType.MARKET, PlaceType.TAVERN -> "merchant"
        PlaceType.CAMP -> "warrior"
        else -> "caregiver"
    }

    private fun relationRole(index: Int, residentCount: Int): String {
        if (residentCount <= 1) {
            return "resident"
        }
        return when (index) {
            0 -> "father"
            1 -> "mother"
            else -> "child"
        }
    }

    private fun defaultAge(index: Int, residentCount: Int): Int = if (residentCount <= 1) 30 else if (index < 2) 36 - index else 16

    private fun defaultGender(index: Int): String = if (index % 2 == 0) "male" else "female"

    private fun buildNpcName(house: WorldPlaceInfo, index: Int): String {
        val offset = abs(house.id().hashCode())
        val stem = NAME_STEMS[(offset + index) % NAME_STEMS.size]
        val suffix = normalizeId(localId(house.id())).replace("_", "")
        return "${stem}_${suffix}_${index + 1}"
    }

    private fun parsePositiveIntMetadata(place: WorldPlaceInfo, vararg keys: String): Int {
        for (key in keys) {
            val value = place.metadata()[key]
            if (value.isNullOrBlank()) {
                continue
            }
            try {
                val parsed = value.trim().toInt()
                if (parsed > 0) {
                    return parsed
                }
            } catch (_: NumberFormatException) {
                return 0
            }
        }
        return 0
    }

    private fun nodeMatchesAny(node: WorldNodeInfo, vararg expectedTokens: String): Boolean {
        if (matchesAnyToken(node.typeId(), *expectedTokens)) {
            return true
        }
        node.metadata().forEach { (key, value) ->
            if (matchesAnyToken(key, *expectedTokens) || matchesAnyToken(value, *expectedTokens)) {
                return true
            }
        }
        return false
    }

    private fun matchesAnyToken(rawValue: String?, vararg expectedTokens: String): Boolean {
        val value = normalizeToken(rawValue)
        if (value.isBlank()) {
            return false
        }
        for (expectedToken in expectedTokens) {
            if (value == normalizeToken(expectedToken)) {
                return true
            }
        }
        return false
    }

    private fun metadataEquals(place: WorldPlaceInfo, key: String, expectedValue: String): Boolean {
        val value = place.metadata()[key]
        return value != null && value.equals(expectedValue, ignoreCase = true)
    }

    private fun firstNonBlank(vararg values: String?): String {
        for (value in values) {
            if (!value.isNullOrBlank()) {
                return value.trim()
            }
        }
        return ""
    }

    private fun localId(qualifiedId: String?): String {
        if (qualifiedId == null) {
            return ""
        }
        val index = qualifiedId.lastIndexOf(':')
        return if (index >= 0) qualifiedId.substring(index + 1) else qualifiedId
    }

    private fun normalizeId(rawValue: String?): String {
        val value = rawValue?.trim()?.lowercase(Locale.ROOT) ?: ""
        val normalized = value.replace(Regex("[^a-z0-9]+"), "_").replace(Regex("^_+|_+$"), "")
        return if (normalized.isBlank()) "npc" else normalized
    }

    private fun normalizeToken(rawValue: String?): String =
        rawValue?.trim()?.lowercase(Locale.ROOT)?.replace(' ', '_')?.replace('-', '_') ?: ""

    private fun distanceSquaredToPlaceCenter(place: WorldPlaceInfo, node: WorldNodeInfo): Double {
        val dx = ((place.minX() + place.maxX()) / 2.0) - node.x()
        val dy = minOf(place.maxY().toDouble(), place.minY() + 1.0) - node.y()
        val dz = ((place.minZ() + place.maxZ()) / 2.0) - node.z()
        return dx * dx + dy * dy + dz * dz
    }

    class PlanningResult private constructor(
        private val success: Boolean,
        private val allocation: HouseAllocation?,
        errors: List<String>?,
        warnings: List<String>?
    ) {
        private val errors: List<String> = (errors ?: emptyList()).toList()
        private val warnings: List<String> = (warnings ?: emptyList()).toList()

        fun success(): Boolean = success
        fun allocation(): HouseAllocation? = allocation
        fun errors(): List<String> = errors
        fun warnings(): List<String> = warnings

        companion object {
            @JvmStatic
            fun success(allocation: HouseAllocation, warnings: List<String>?): PlanningResult =
                PlanningResult(true, allocation, emptyList(), warnings)

            @JvmStatic
            fun failed(errors: List<String>?, warnings: List<String>?): PlanningResult =
                PlanningResult(false, null, errors, warnings)
        }
    }

    class SettlementPlanningResult private constructor(
        private val success: Boolean,
        regionId: String?,
        allocations: List<HouseAllocation>?,
        errors: List<String>?,
        warnings: List<String>?
    ) {
        private val regionId: String = regionId?.trim() ?: ""
        private val allocations: List<HouseAllocation> = (allocations ?: emptyList()).toList()
        private val errors: List<String> = (errors ?: emptyList()).toList()
        private val warnings: List<String> = (warnings ?: emptyList()).toList()

        fun success(): Boolean = success
        fun regionId(): String = regionId
        fun allocations(): List<HouseAllocation> = allocations
        fun errors(): List<String> = errors
        fun warnings(): List<String> = warnings

        companion object {
            @JvmStatic
            fun success(
                regionId: String?,
                allocations: List<HouseAllocation>?,
                warnings: List<String>?
            ): SettlementPlanningResult = SettlementPlanningResult(true, regionId, allocations, emptyList(), warnings)

            @JvmStatic
            fun failed(
                regionId: String?,
                allocations: List<HouseAllocation>?,
                errors: List<String>?,
                warnings: List<String>?
            ): SettlementPlanningResult = SettlementPlanningResult(false, regionId, allocations, errors, warnings)
        }
    }

    companion object {
        private val NAME_STEMS: List<String> = listOf(
            "Ion", "Maria", "Andrei", "Elena", "Gabriel", "Madalina", "Vlad", "Ana",
            "Stefan", "Irina", "Radu", "Ioana"
        )
    }
}
