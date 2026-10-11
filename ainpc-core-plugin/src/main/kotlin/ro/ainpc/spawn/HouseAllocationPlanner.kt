package ro.ainpc.spawn

import ro.ainpc.api.WorldAdminApi
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorkplaceOccupancy
import ro.ainpc.world.SocialAssociationOccupancy
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.util.Locale
import kotlin.math.abs

/**
 * Houseallocationplanner.
 */
class HouseAllocationPlanner {

    fun plan(worldAdmin: WorldAdminApi?, houseSelector: String?, requestedResidents: Int): PlanningResult =
        plan(worldAdmin, houseSelector, requestedResidents, emptyMap())

    private fun plan(
        worldAdmin: WorldAdminApi?,
        houseSelector: String?,
        requestedResidents: Int,
        reservedWorkplaces: Map<String, Int>,
        reservedSocialPlaces: Map<String, Int> = emptyMap(),
    ): PlanningResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (worldAdmin == null || !worldAdmin.isEnabled) {
            errors.add("WorldAdmin este dezactivat sau indisponibil.")
            return PlanningResult.failed(errors, warnings)
        }

        val house = resolveHouse(worldAdmin, houseSelector, errors) ?: return PlanningResult.failed(errors, warnings)
        if (hasMappedResidents(house)) {
            errors.add("Casa ${house.id()} are deja rezidenti in mapping; inspecteaza household-ul existent inainte de replanificare.")
            return PlanningResult.failed(errors, warnings)
        }

        val houseNodes = worldAdmin.getNodesForPlace(house.id()).sortedBy { it.id() }
        val spawnNodes = nodesMatching(houseNodes, "npc_spawn", "spawn")
        val homeNodes = nodesMatching(houseNodes, "bed", "home")
        if (spawnNodes.isEmpty()) {
            errors.add("Casa ${house.id()} nu are node npc_spawn/spawn pentru spawn plan.")
        }
        if (homeNodes.isEmpty()) {
            errors.add("Casa ${house.id()} nu are node bed/home pentru spawn plan.")
        }

        val metadataCapacity = SpawnSemanticRules.parsePositiveIntMetadata(house, "max_residents", "maxResidents", "capacity")
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
            .filter { SpawnSemanticRules.isWorkplace(it) }
            .sortedWith(compareBy<WorldPlaceInfo> { SpawnSemanticRules.workplacePriority(it) }.thenBy { it.id() })
            .toList()
        val socialPlaces = worldAdmin.getPlaces(house.regionId()).asSequence()
            .filter { !it.id().equals(house.id(), ignoreCase = true) }
            .filter { SpawnSemanticRules.isSocialPlace(it) }
            .sortedWith(compareBy<WorldPlaceInfo> { SpawnSemanticRules.socialPriority(it) }.thenBy { it.id() })
            .toList()

        val workSlots = mutableListOf<WorldPlaceInfo>()
        for (workplace in workplaces) {
            val configuredCapacity = workplace.metadata()["max_workers"]
            val capacity = if (configuredCapacity == null) 1 else configuredCapacity.trim().toIntOrNull()
            if (capacity == null || capacity < 0) {
                errors.add("Workplace-ul ${workplace.id()} are max_workers invalid; este necesar un intreg nenegativ.")
                continue
            }
            val existingWorkerCount = WorkplaceOccupancy.workerIds(workplace.metadata()).size
            val regionalReservation = reservedWorkplaces[workplace.id()] ?: 0
            if (existingWorkerCount > 0 || regionalReservation > 0) {
                warnings.add(
                    "Workplace-ul ${workplace.id()} are $existingWorkerCount angajati existenti si " +
                        "$regionalReservation posturi rezervate in plan; max_workers=$capacity."
                )
            }
            val available = WorkplaceOccupancy.availableSlots(capacity, workplace.metadata(), regionalReservation)
            repeat(minOf(available, residentCount - workSlots.size)) { workSlots.add(workplace) }
        }
        if (errors.isNotEmpty()) {
            return PlanningResult.failed(errors, warnings)
        }
        val socialCapacities = mutableMapOf<String, Int?>()
        for (place in socialPlaces) {
            try {
                socialCapacities[place.id()] = SocialAssociationOccupancy.capacity(place.metadata())
            } catch (exception: IllegalArgumentException) {
                errors.add("Locul social ${place.id()} are ${exception.message}")
            }
        }
        if (errors.isNotEmpty()) return PlanningResult.failed(errors, warnings)
        if (workSlots.size < residentCount && workplaces.isNotEmpty()) {
            warnings.add("Capacitate workplace insuficienta in regiunea ${house.regionId()}; ${residentCount - workSlots.size} rezidenti primesc ocupatia locuitor.")
        }

        val builder = HouseAllocation.builder(house.id()).maxResidents(maxResidents)
        if (residentCount > 1) {
            builder.familyId("family_${SpawnSemanticRules.normalizeId(SpawnSemanticRules.localId(house.id()))}")
        }

        val workAssignments = workplaces.associate { place ->
            place.id() to WorkplaceOccupancy.workerIds(place.metadata()).size + (reservedWorkplaces[place.id()] ?: 0)
        }.toMutableMap()
        val workNodesByPlace = workplaces.associate { place ->
            val nodes = worldAdmin.getNodesForPlace(place.id()).toList()
            val preferred = SpawnSemanticRules.bestNodeForPlace(place, nodes, "work")
            val workNodes = SpawnSemanticRules.nodesMatching(nodes, "workstation", "work")
            place.id() to if (preferred == null) workNodes else listOf(preferred) + workNodes.filter { it.id() != preferred.id() }
        }
        val socialAssignments = socialPlaces.associate { place ->
            place.id() to SocialAssociationOccupancy.associatedIds(place.metadata()).size + (reservedSocialPlaces[place.id()] ?: 0)
        }.toMutableMap()
        val socialNodesByPlace = socialPlaces.associate { place ->
            val nodes = worldAdmin.getNodesForPlace(place.id()).toList()
            val preferred = SpawnSemanticRules.bestNodeForPlace(place, nodes, "social")
            val socialNodes = SpawnSemanticRules.nodesMatching(nodes, "social", "meeting_point")
            place.id() to if (preferred == null) socialNodes else listOf(preferred) + socialNodes.filter { it.id() != preferred.id() }
        }
        var firstNpcKey = ""
        var residentsWithoutSocial = 0
        for (index in 0 until residentCount) {
            val npcKey = "${SpawnSemanticRules.normalizeId(house.id())}_resident_${index + 1}"
            val npcName = buildNpcName(house, index)
            val relationRole = relationRole(index, residentCount)
            val workPlace = workSlots.getOrNull(index)
            val workAssignmentIndex = workPlace?.let { workAssignments.getValue(it.id()) } ?: 0
            if (workPlace != null) workAssignments[workPlace.id()] = workAssignmentIndex + 1
            val workNodes = workPlace?.let { workNodesByPlace.getValue(it.id()) }.orEmpty()
            val workNode = if (workNodes.isNotEmpty()) workNodes[workAssignmentIndex % workNodes.size] else null
            val socialPlace = socialPlaces.asSequence()
                .filter { place -> SocialAssociationOccupancy.hasAvailableSlot(socialCapacities[place.id()], socialAssignments.getValue(place.id())) }
                .minByOrNull { socialAssignments.getValue(it.id()) }
            val socialAssignmentIndex = socialPlace?.let { socialAssignments.getValue(it.id()) } ?: 0
            if (socialPlace != null) {
                socialAssignments[socialPlace.id()] = socialAssignmentIndex + 1
            } else if (socialPlaces.isNotEmpty()) {
                residentsWithoutSocial++
            }
            val socialNodes = socialPlace?.let { socialNodesByPlace.getValue(it.id()) }.orEmpty()
            val socialNode = if (socialNodes.isNotEmpty()) socialNodes[socialAssignmentIndex % socialNodes.size] else null

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
        } else if (residentsWithoutSocial > 0) {
            warnings.add("Capacitate sociala insuficienta in regiunea ${house.regionId()}; $residentsWithoutSocial rezidenti raman fara socialPlaceId.")
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
            .filter { SpawnSemanticRules.isHousePlace(it) }
            .sortedBy { it.id() }
            .toList()
        if (houses.isEmpty()) {
            errors.add("Regiunea ${region.id()} nu are case/place-uri home.")
            return SettlementPlanningResult.failed(region.id(), allocations, errors, warnings)
        }

        val availableHouses = houses.filter { house ->
            if (hasMappedResidents(house)) {
                warnings.add("${house.id()}: casa are deja rezidenti in mapping; este omisa din planul nou.")
                false
            } else true
        }
        if (availableHouses.isEmpty()) {
            errors.add("Regiunea ${region.id()} nu are case fara rezidenti in mapping pentru un plan nou.")
            return SettlementPlanningResult.failed(region.id(), allocations, errors, warnings)
        }
        val limit = if (maxHouses > 0) minOf(maxHouses, availableHouses.size) else availableHouses.size
        val reservedWorkplaces = mutableMapOf<String, Int>()
        val reservedSocialPlaces = mutableMapOf<String, Int>()
        for (house in availableHouses.take(limit)) {
            val planning = plan(worldAdmin, house.id(), 0, reservedWorkplaces, reservedSocialPlaces)
            if (planning.success()) {
                val allocation = planning.allocation() ?: continue
                allocations.add(allocation)
                allocation.residentPlans().map { it.workPlaceId() }.filter { it.isNotBlank() }.forEach { workplaceId ->
                    reservedWorkplaces[workplaceId] = (reservedWorkplaces[workplaceId] ?: 0) + 1
                }
                allocation.residentPlans().map { it.socialPlaceId() }.filter { it.isNotBlank() }.forEach { socialPlaceId ->
                    reservedSocialPlaces[socialPlaceId] = (reservedSocialPlaces[socialPlaceId] ?: 0) + 1
                }
            }
            planning.warnings().forEach { warning -> warnings.add("${house.id()}: $warning") }
            planning.errors().forEach { error -> errors.add("${house.id()}: $error") }
        }

        if (maxHouses > 0 && availableHouses.size > maxHouses) {
            warnings.add("Regiunea ${region.id()} are ${availableHouses.size} case disponibile; au fost planificate primele $maxHouses.")
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

    private fun hasMappedResidents(house: WorldPlaceInfo): Boolean =
        house.metadata()["resident_npc_ids"].orEmpty().split(Regex("[,;]")).any { it.isNotBlank() }

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
        if (!SpawnSemanticRules.isHousePlace(house)) {
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
            .filter { node -> SpawnSemanticRules.nodeMatchesAny(node, *tokens) }
            .sortedWith(compareBy<WorldNodeInfo> { nodePriority(it, *tokens) }.thenBy { it.id() })
            .toList()

    private fun nodePriorityForAnchor(node: WorldNodeInfo, anchorRole: String): Int = when (anchorRole) {
        "work" -> {
            if (SpawnSemanticRules.matchesAnyToken(node.typeId(), "work") || SpawnSemanticRules.matchesAnyToken(node.metadata()["semantic"], "work_anchor")) {
                0
            } else if (SpawnSemanticRules.nodeMatchesAny(node, "work", "workplace", "workstation", "job", "munca", "lucru")) {
                1
            } else if (SpawnSemanticRules.nodeMatchesAny(node, "interaction", "counter", "desk", "npc_spawn", "spawn")) {
                2
            } else {
                -1
            }
        }
        "social" -> {
            if (SpawnSemanticRules.matchesAnyToken(node.typeId(), "social", "meeting_point") ||
                SpawnSemanticRules.matchesAnyToken(node.metadata()["semantic"], "social_anchor", "meeting_point")
            ) {
                0
            } else if (SpawnSemanticRules.nodeMatchesAny(node, "social", "meeting_point", "meeting", "market", "well", "tavern", "piata")) {
                1
            } else if (SpawnSemanticRules.nodeMatchesAny(node, "interaction", "npc_spawn", "spawn")) {
                2
            } else {
                -1
            }
        }
        else -> -1
    }

    private fun nodePriority(node: WorldNodeInfo, vararg tokens: String): Int =
        if (tokens.isNotEmpty() && SpawnSemanticRules.matchesAnyToken(node.typeId(), *tokens)) 0 else 1

    private fun occupationForWorkplace(place: WorldPlaceInfo): String {
        val configuredProfession = SpawnSemanticRules.firstNonBlank(place.metadata()["profession"], place.metadata()["occupation"])
        if (configuredProfession.isNotBlank()) {
            return configuredProfession
        }
        return "Locuitor"
    }

    private fun archetypeForWorkplace(place: WorldPlaceInfo): String = when (place.placeType()) {
        PlaceType.FORGE, PlaceType.FARM, PlaceType.SHOP -> "creator"
        PlaceType.MARKET, PlaceType.TAVERN, PlaceType.CAMP -> "caregiver"
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
        val firstName = NAME_STEMS[(offset + index) % NAME_STEMS.size]
        val surname = SURNAME_STEMS[(offset + index + house.id().length) % SURNAME_STEMS.size]
        return "${firstName} ${surname}"
    }

    private fun distanceSquaredToPlaceCenter(place: WorldPlaceInfo, node: WorldNodeInfo): Double {
        val dx = ((place.minX() + place.maxX()) / 2.0) - node.x()
        val dy = minOf(place.maxY().toDouble(), place.minY() + 1.0) - node.y()
        val dz = ((place.minZ() + place.maxZ()) / 2.0) - node.z()
        return dx * dx + dy * dy + dz * dz
    }

/**
 * Planningresult.
 */
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

/**
 * Settlementplanningresult.
 */
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
            "Stefan", "Irina", "Radu", "Ioana", "Mihai", "Catalin", "Adriana", "Daniel",
            "Nicolae", "Florina", "Alexandru", "Cristina", "Vasile", "Laura", "Petru", "Alina",
            "Gheorghe", "Raluca", "Sorin", "Diana", "Mircea", "Oana"
        )
        private val SURNAME_STEMS: List<String> = listOf(
            "Popescu", "Ionescu", "Georgescu", "Dumitrescu", "Marinescu", "Constantinescu",
            "Radulescu", "Stefanescu", "Florescu", "Barbulescu", "Dobrescu", "Lupulescu"
        )
    }
}
