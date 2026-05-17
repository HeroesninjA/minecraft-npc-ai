package ro.ainpc.spawn

import ro.ainpc.api.WorldAdminApi
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import java.util.Locale

class HouseAllocationValidator {

    fun validate(allocation: HouseAllocation?, worldAdmin: WorldAdminApi?): HouseAllocationValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (allocation == null) {
            errors.add("HouseAllocation este null.")
            return HouseAllocationValidationResult(false, errors, warnings)
        }

        validateStructure(allocation, errors, warnings)
        val house = resolveHouse(allocation, worldAdmin, errors, warnings)
        validateResidents(allocation, worldAdmin, house, errors, warnings)

        return HouseAllocationValidationResult(errors.isEmpty(), errors, warnings)
    }

    private fun validateStructure(allocation: HouseAllocation, errors: MutableList<String>, warnings: MutableList<String>) {
        if (allocation.placeId().isBlank()) {
            errors.add("HouseAllocation nu are placeId.")
        }
        if (allocation.maxResidents() <= 0) {
            errors.add("HouseAllocation pentru ${allocation.placeId()} are maxResidents invalid.")
        }
        if (allocation.residentPlans().isEmpty()) {
            errors.add("HouseAllocation pentru ${allocation.placeId()} nu are rezidenti.")
        }
        if (allocation.residentPlans().isNotEmpty() && allocation.residentPlans().size > allocation.maxResidents()) {
            errors.add(
                "HouseAllocation pentru ${allocation.placeId()} are ${allocation.residentPlans().size} " +
                    "rezidenti peste maxResidents=${allocation.maxResidents()}."
            )
        }
        if (allocation.residentPlans().size > 1 && allocation.familyId().isBlank()) {
            warnings.add(
                "HouseAllocation pentru ${allocation.placeId()} are mai multi rezidenti, " +
                    "dar nu are familyId/householdId."
            )
        }
    }

    private fun resolveHouse(
        allocation: HouseAllocation,
        worldAdmin: WorldAdminApi?,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ): WorldPlaceInfo? {
        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            errors.add("WorldAdmin este dezactivat sau indisponibil pentru HouseAllocation.")
            return null
        }

        val house = resolvePlace(worldAdmin, allocation.placeId(), "placeId", errors) ?: return null

        if (!isHousePlace(house)) {
            errors.add("HouseAllocation placeId '${allocation.placeId()}' nu indica o casa/place home.")
        }

        val metadataMaxResidents = parsePositiveIntMetadata(house, "max_residents", "maxResidents", "capacity")
        if (metadataMaxResidents == null) {
            warnings.add("Casa ${house.id()} nu are metadata.max_residents/capacity sincronizat.")
        } else {
            if (allocation.residentPlans().size > metadataMaxResidents) {
                errors.add(
                    "Casa ${house.id()} are capacitate metadata=$metadataMaxResidents, " +
                        "dar HouseAllocation cere ${allocation.residentPlans().size} rezidenti."
                )
            }
            if (allocation.maxResidents() > metadataMaxResidents) {
                warnings.add(
                    "HouseAllocation maxResidents=${allocation.maxResidents()} este peste " +
                        "metadata.max_residents=$metadataMaxResidents pentru ${house.id()}."
                )
            }
        }

        validateOwnerMetadata(allocation, house, warnings, errors)
        validateResidentsMetadata(allocation, house, warnings)
        return house
    }

    private fun validateOwnerMetadata(
        allocation: HouseAllocation,
        house: WorldPlaceInfo,
        warnings: MutableList<String>,
        errors: MutableList<String>
    ) {
        val residentKeys = normalizedResidentKeys(allocation)
        val primaryOwner = normalizeToken(allocation.primaryOwnerNpcKey())
        if (primaryOwner.isBlank()) {
            warnings.add("HouseAllocation pentru ${house.id()} nu are primaryOwnerNpcKey.")
        } else if (!residentKeys.contains(primaryOwner)) {
            errors.add(
                "primaryOwnerNpcKey '${allocation.primaryOwnerNpcKey()}' nu exista in residentPlans pentru ${house.id()}."
            )
        }

        if (house.ownerNpcId().isBlank()) {
            warnings.add("Casa ${house.id()} nu are owner_npc_id sincronizat.")
            return
        }
        if (primaryOwner.isNotBlank() && !selectorMatches(house.ownerNpcId(), allocation.primaryOwnerNpcKey())) {
            warnings.add(
                "Casa ${house.id()} are owner_npc_id=${house.ownerNpcId()}, " +
                    "dar HouseAllocation primaryOwnerNpcKey=${allocation.primaryOwnerNpcKey()}."
            )
        }
    }

    private fun validateResidentsMetadata(allocation: HouseAllocation, house: WorldPlaceInfo, warnings: MutableList<String>) {
        val configuredResidents = parseResidents(house)
        if (configuredResidents.isEmpty()) {
            warnings.add("Casa ${house.id()} nu are metadata.residents sincronizat.")
            return
        }

        val plannedResidents = normalizedResidentKeys(allocation)
        for (plannedResident in plannedResidents) {
            if (!configuredResidents.contains(plannedResident)) {
                warnings.add("Casa ${house.id()} nu contine in metadata.residents rezidentul planificat: $plannedResident.")
            }
        }
        for (configuredResident in configuredResidents) {
            if (!plannedResidents.contains(configuredResident)) {
                warnings.add("Casa ${house.id()} are in metadata.residents un rezident neplanificat: $configuredResident.")
            }
        }
    }

    private fun validateResidents(
        allocation: HouseAllocation,
        worldAdmin: WorldAdminApi?,
        house: WorldPlaceInfo?,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val npcKeys = mutableSetOf<String>()
        val logicalNames = mutableSetOf<String>()
        val exclusiveNodeOwners = mutableMapOf<String, String>()

        for (resident in allocation.residentPlans()) {
            val label = residentLabel(resident)
            val normalizedKey = normalizeToken(resident.npcKey())
            if (normalizedKey.isBlank()) {
                errors.add("ResidentPlan fara npcKey in ${allocation.placeId()}.")
            } else if (!npcKeys.add(normalizedKey)) {
                errors.add("ResidentPlan duplicat dupa npcKey: ${resident.npcKey()}.")
            }

            val logicalName = normalizeToken(if (resident.name().isNotBlank()) resident.name() else resident.npcKey())
            if (logicalName.isNotBlank() && !logicalNames.add(logicalName)) {
                errors.add("Nume logic duplicat in HouseAllocation ${allocation.placeId()}: ${resident.name()}.")
            }

            if (allocation.familyId().isNotBlank() && resident.relationRole().isBlank()) {
                warnings.add("$label nu are relationRole pentru familyId=${allocation.familyId()}.")
            }

            validateResidentHomeNodes(resident, worldAdmin, house, exclusiveNodeOwners, errors)
            validateResidentWorkNodes(resident, worldAdmin, errors, warnings)
            validateResidentSocialNodes(resident, worldAdmin, errors, warnings)
        }
    }

    private fun validateResidentHomeNodes(
        resident: HouseAllocation.ResidentPlan,
        worldAdmin: WorldAdminApi?,
        house: WorldPlaceInfo?,
        exclusiveNodeOwners: MutableMap<String, String>,
        errors: MutableList<String>
    ) {
        val label = residentLabel(resident)
        if (resident.spawnNodeId().isBlank()) {
            errors.add("$label nu are spawnNodeId.")
        } else {
            val spawnNode = resolveNode(worldAdmin, resident.spawnNodeId(), "$label spawnNodeId", errors)
            validateNodeInPlace(spawnNode, house, "$label spawnNodeId", errors)
            validateNodeSemantics(spawnNode, "$label spawnNodeId", errors, "npc_spawn", "spawn")
            registerExclusiveNode(exclusiveNodeOwners, resident.spawnNodeId(), resident.npcKey(), label, errors)
        }

        val homeNodeId = resident.effectiveHomeNodeId()
        if (homeNodeId.isBlank()) {
            errors.add("$label nu are homeNodeId sau bedNodeId.")
        } else {
            val homeNode = resolveNode(worldAdmin, homeNodeId, "$label home/bed node", errors)
            validateNodeInPlace(homeNode, house, "$label home/bed node", errors)
            validateNodeSemantics(homeNode, "$label home/bed node", errors, "bed", "home", "npc_spawn", "interaction")
            registerExclusiveNode(exclusiveNodeOwners, homeNodeId, resident.npcKey(), label, errors)
        }
    }

    private fun validateResidentWorkNodes(
        resident: HouseAllocation.ResidentPlan,
        worldAdmin: WorldAdminApi?,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val label = residentLabel(resident)
        if (requiresWorkAnchor(resident.occupation()) && resident.workPlaceId().isBlank() && resident.workNodeId().isBlank()) {
            errors.add("$label are ocupatia '${resident.occupation()}', dar nu are workPlaceId sau workNodeId.")
            return
        }

        var workPlace: WorldPlaceInfo? = null
        if (resident.workPlaceId().isNotBlank()) {
            workPlace = resolvePlace(worldAdmin, resident.workPlaceId(), "$label workPlaceId", errors)
            if (workPlace != null && !isWorkplace(workPlace)) {
                errors.add("$label workPlaceId '${resident.workPlaceId()}' nu indica un workplace compatibil.")
            }
        }

        if (resident.workNodeId().isNotBlank()) {
            val workNode = resolveNode(worldAdmin, resident.workNodeId(), "$label workNodeId", errors)
            if (workPlace != null) {
                validateNodeInPlace(workNode, workPlace, "$label workNodeId", errors)
            } else if (workNode != null && workNode.placeId().isNotBlank()) {
                val nodePlace = resolvePlace(worldAdmin, workNode.placeId(), "$label workNode place", errors)
                if (nodePlace != null && !isWorkplace(nodePlace)) {
                    errors.add("$label workNodeId '${resident.workNodeId()}' apartine unui place care nu este workplace.")
                }
            } else if (workNode != null) {
                warnings.add("$label workNodeId '${resident.workNodeId()}' este node de regiune, nu node sub workplace.")
            }
            validateNodeSemantics(workNode, "$label workNodeId", errors, "workstation", "work", "npc_spawn", "interaction")
        }
    }

    private fun validateResidentSocialNodes(
        resident: HouseAllocation.ResidentPlan,
        worldAdmin: WorldAdminApi?,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val label = residentLabel(resident)
        var socialPlace: WorldPlaceInfo? = null
        if (resident.socialPlaceId().isNotBlank()) {
            socialPlace = resolvePlace(worldAdmin, resident.socialPlaceId(), "$label socialPlaceId", errors)
        }

        if (resident.socialNodeId().isNotBlank()) {
            val socialNode = resolveNode(worldAdmin, resident.socialNodeId(), "$label socialNodeId", errors)
            if (socialPlace != null) {
                validateNodeInPlace(socialNode, socialPlace, "$label socialNodeId", errors)
            } else if (socialNode != null && socialNode.placeId().isBlank()) {
                warnings.add(
                    "$label socialNodeId '${resident.socialNodeId()}' este node de regiune; " +
                        "valid pentru piata/clopot, dar nu are socialPlaceId."
                )
            }
            validateNodeSemantics(socialNode, "$label socialNodeId", errors, "social", "meeting_point", "interaction", "npc_spawn")
        }
    }

    private fun resolvePlace(
        worldAdmin: WorldAdminApi?,
        placeId: String?,
        label: String,
        errors: MutableList<String>
    ): WorldPlaceInfo? {
        if (worldAdmin == null || placeId.isNullOrBlank()) {
            return null
        }

        val matches = worldAdmin.getPlaces().asSequence()
            .filter { place -> idMatches(place.id(), placeId) }
            .sortedBy { it.id() }
            .toList()

        if (matches.size == 1) {
            return matches[0]
        }
        if (matches.isEmpty()) {
            errors.add("$label '$placeId' nu exista in WorldAdmin.")
        } else {
            errors.add("$label '$placeId' este ambiguu: ${matches.map { it.id() }}")
        }
        return null
    }

    private fun resolveNode(
        worldAdmin: WorldAdminApi?,
        nodeId: String?,
        label: String,
        errors: MutableList<String>
    ): WorldNodeInfo? {
        if (worldAdmin == null || nodeId.isNullOrBlank()) {
            return null
        }

        val matches = worldAdmin.getNodes().asSequence()
            .filter { node -> idMatches(node.id(), nodeId) }
            .sortedBy { it.id() }
            .toList()

        if (matches.size == 1) {
            return matches[0]
        }
        if (matches.isEmpty()) {
            errors.add("$label '$nodeId' nu exista in WorldAdmin.")
        } else {
            errors.add("$label '$nodeId' este ambiguu: ${matches.map { it.id() }}")
        }
        return null
    }

    private fun validateNodeInPlace(node: WorldNodeInfo?, place: WorldPlaceInfo?, label: String, errors: MutableList<String>) {
        if (node == null || place == null) {
            return
        }
        if (!place.id().equals(node.placeId(), ignoreCase = true)) {
            errors.add("$label '${node.id()}' nu apartine place-ului ${place.id()}.")
            return
        }
        if (!pointInsidePlace(node, place)) {
            errors.add("$label '${node.id()}' nu este in interiorul place-ului ${place.id()}.")
        }
    }

    private fun validateNodeSemantics(node: WorldNodeInfo?, label: String, errors: MutableList<String>, vararg expectedTokens: String) {
        if (node != null && !nodeMatchesAny(node, *expectedTokens)) {
            errors.add("$label '${node.id()}' nu are tip/metadata compatibil: ${expectedTokens.joinToString(", ")}.")
        }
    }

    private fun registerExclusiveNode(
        owners: MutableMap<String, String>,
        nodeId: String?,
        npcKey: String?,
        label: String,
        errors: MutableList<String>
    ) {
        val normalizedNodeId = normalizeToken(nodeId)
        if (normalizedNodeId.isBlank()) {
            return
        }

        val normalizedNpcKey = normalizeToken(npcKey)
        val existingOwner = owners.putIfAbsent(normalizedNodeId, normalizedNpcKey)
        if (existingOwner != null && existingOwner != normalizedNpcKey) {
            errors.add("$label foloseste node-ul deja alocat altui rezident: $nodeId.")
        }
    }

    private fun isHousePlace(place: WorldPlaceInfo): Boolean =
        place.placeType() == PlaceType.HOUSE ||
            place.hasTag("home") ||
            place.hasTag("house") ||
            "home".equals(place.metadata()["role"], ignoreCase = true) ||
            "home".equals(place.metadata()["purpose"], ignoreCase = true)

    private fun isWorkplace(place: WorldPlaceInfo): Boolean =
        place.hasTag("work") ||
            place.hasTag("workplace") ||
            "work".equals(place.metadata()["role"], ignoreCase = true) ||
            "work".equals(place.metadata()["purpose"], ignoreCase = true) ||
            when (place.placeType()) {
                PlaceType.FORGE, PlaceType.SHOP, PlaceType.FARM, PlaceType.MARKET, PlaceType.TAVERN -> true
                else -> false
            }

    private fun parseResidents(place: WorldPlaceInfo): Set<String> {
        val rawResidents = firstNonBlank(
            place.metadata()["residents"],
            place.metadata()["resident_npc_ids"],
            place.metadata()["resident_ids"]
        )
        if (rawResidents.isBlank()) {
            return emptySet()
        }

        val residents = mutableSetOf<String>()
        rawResidents.split(Regex("[,;]")).forEach { part ->
            val resident = normalizeToken(part)
            if (resident.isNotBlank()) {
                residents.add(resident)
            }
        }
        return residents
    }

    private fun normalizedResidentKeys(allocation: HouseAllocation): Set<String> {
        val residentKeys = mutableSetOf<String>()
        allocation.residentPlans().forEach { resident ->
            val key = normalizeToken(resident.npcKey())
            if (key.isNotBlank()) {
                residentKeys.add(key)
            }
        }
        return residentKeys
    }

    private fun parsePositiveIntMetadata(place: WorldPlaceInfo, vararg keys: String): Int? {
        val rawValue = firstNonBlankFromMap(place.metadata(), *keys)
        if (rawValue.isBlank()) {
            return null
        }
        return try {
            val value = rawValue.trim().toInt()
            if (value > 0) value else null
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun firstNonBlankFromMap(values: Map<String, String>, vararg keys: String): String {
        for (key in keys) {
            val value = values[key]
            if (!value.isNullOrBlank()) {
                return value
            }
        }
        return ""
    }

    private fun firstNonBlank(vararg values: String?): String {
        for (value in values) {
            if (!value.isNullOrBlank()) {
                return value
            }
        }
        return ""
    }

    private fun idMatches(actualId: String?, selector: String?): Boolean {
        if (actualId == null || selector == null) {
            return false
        }
        val actual = actualId.trim().lowercase(Locale.ROOT)
        val expected = selector.trim().lowercase(Locale.ROOT)
        return actual == expected || actual.endsWith(":$expected")
    }

    private fun selectorMatches(left: String?, right: String?): Boolean {
        val normalizedLeft = normalizeToken(left)
        val normalizedRight = normalizeToken(right)
        return normalizedLeft == normalizedRight ||
            normalizedLeft == "npc_$normalizedRight" ||
            normalizedRight == "npc_$normalizedLeft"
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

    private fun requiresWorkAnchor(occupation: String?): Boolean {
        val normalized = normalizeToken(occupation)
        return normalized.isNotBlank() &&
            normalized != "locuitor" &&
            normalized != "localnic" &&
            normalized != "villager" &&
            normalized != "resident"
    }

    private fun residentLabel(resident: HouseAllocation.ResidentPlan): String {
        val name = if (resident.name().isNotBlank()) resident.name() else resident.npcKey()
        return "ResidentPlan ${if (name.isBlank()) "<fara nume>" else name}"
    }

    private fun normalizeToken(rawValue: String?): String =
        rawValue?.trim()?.lowercase(Locale.ROOT)?.replace(' ', '_')?.replace('-', '_') ?: ""

    private fun pointInsidePlace(node: WorldNodeInfo, place: WorldPlaceInfo): Boolean =
        node.worldName().equals(place.worldName(), ignoreCase = true) &&
            node.x() >= place.minX() &&
            node.x() <= place.maxX() &&
            node.y() >= place.minY() &&
            node.y() <= place.maxY() &&
            node.z() >= place.minZ() &&
            node.z() <= place.maxZ()
}
