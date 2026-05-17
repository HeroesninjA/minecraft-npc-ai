package ro.ainpc.world.patch

import ro.ainpc.api.WorldAdminApi
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Locale

class VillageGapAnalyzer {
    fun analyze(worldAdmin: WorldAdminApi?, regionSelector: String?, options: PatchPlannerOptions?): GapReport {
        val safeOptions = options ?: PatchPlannerOptions.forTargetPopulation(0)
        val errors = ArrayList<String>()
        val warnings = ArrayList<String>()
        val gaps = ArrayList<VillageGap>()
        val missingWorkplaces = LinkedHashSet<String>()
        val missingNodes = LinkedHashSet<String>()
        val capacityByHouse = LinkedHashMap<String, Int>()

        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            errors.add("WorldAdmin este dezactivat sau indisponibil.")
            return emptyReport("", safeOptions, errors, warnings)
        }

        val region = resolveRegion(worldAdmin, regionSelector, errors) ?: return emptyReport("", safeOptions, errors, warnings)

        val places = worldAdmin.getPlaces(region.id())
            .sortedBy { place -> place.id() }
        val nodes = worldAdmin.getNodes(region.id())
            .sortedBy { node -> node.id() }
        val houses = places.filter(::isHousePlace)
        val workplaces = places.filter(::isWorkplace)
        val socialPlaces = places.filter(::isSocialPlace)

        var currentCapacity = 0
        for (house in houses) {
            val capacity = houseCapacity(worldAdmin, house)
            capacityByHouse[house.id()] = capacity
            currentCapacity += capacity
            if (capacity <= 0) {
                warnings.add("Casa " + house.id() + " nu are capacitate clara pentru rezidenti.")
                gaps.add(
                    VillageGap(
                        PatchGapType.HOUSE_TOO_SMALL_FOR_FAMILY,
                        1,
                        region.id(),
                        house.id(),
                        "capacity",
                        6,
                        "Casa exista, dar nu are max_residents sau bed/home nodes."
                    )
                )
            }
            if (!hasNode(worldAdmin.getNodesForPlace(house.id()), "entrance")) {
                missingNodes.add("entrance:" + house.id())
                gaps.add(
                    VillageGap(
                        PatchGapType.MISSING_ENTRANCE_NODE,
                        1,
                        region.id(),
                        house.id(),
                        "entrance",
                        3,
                        "Casa nu are node semantic de intrare."
                    )
                )
            }
        }

        val requiredCapacity = maxOf(safeOptions.targetPopulation(), currentCapacity)
        val missingCapacity = maxOf(0, requiredCapacity - currentCapacity)
        val missingHomes = if (missingCapacity <= 0) {
            maxOf(0, safeOptions.minHouseCount() - houses.size)
        } else {
            maxOf(1, kotlin.math.ceil(missingCapacity / DEFAULT_HOUSE_PATCH_CAPACITY.toDouble()).toInt())
        }
        if (missingCapacity > 0) {
            gaps.add(
                VillageGap(
                    PatchGapType.MISSING_HOUSE_CAPACITY,
                    missingCapacity,
                    region.id(),
                    "",
                    "population",
                    10,
                    "Capacitatea curenta $currentCapacity este sub tinta $requiredCapacity."
                )
            )
            gaps.add(
                VillageGap(
                    PatchGapType.MISSING_BEDS,
                    missingCapacity,
                    region.id(),
                    "",
                    "bed",
                    9,
                    "Lipsesc bed/home anchors pentru populatia tinta."
                )
            )
        } else if (missingHomes > 0) {
            gaps.add(
                VillageGap(
                    PatchGapType.MISSING_HOUSE_CAPACITY,
                    missingHomes,
                    region.id(),
                    "",
                    "min_house_count",
                    7,
                    "Regiunea are mai putine case decat minimul cerut."
                )
            )
        }

        for (workplace in workplaces) {
            if (!hasNode(worldAdmin.getNodesForPlace(workplace.id()), "work", "workstation", "work_anchor")) {
                missingNodes.add("work:" + workplace.id())
                gaps.add(
                    VillageGap(
                        PatchGapType.WORKPLACE_WITHOUT_WORK_NODE,
                        1,
                        region.id(),
                        workplace.id(),
                        "work",
                        5,
                        "Workplace-ul nu are node work/workstation."
                    )
                )
            }
        }

        for (profession in safeOptions.normalizedRequiredProfessions()) {
            if (workplaces.none { place -> supportsProfession(place, profession) }) {
                missingWorkplaces.add(profession)
                gaps.add(
                    VillageGap(
                        PatchGapType.MISSING_WORKPLACE_FOR_PROFESSION,
                        1,
                        region.id(),
                        "",
                        profession,
                        8,
                        "Nu exista workplace pentru profesia $profession."
                    )
                )
            }
        }

        var missingSocialPlaces = 0
        if (safeOptions.requireSocialHub() && socialPlaces.isEmpty()
            && nodes.none { node -> nodeMatchesAny(node, "social", "meeting_point", "meeting") }
        ) {
            missingSocialPlaces = 1
            gaps.add(
                VillageGap(
                    PatchGapType.MISSING_SOCIAL_HUB,
                    1,
                    region.id(),
                    "",
                    "social",
                    7,
                    "Regiunea nu are loc social sau meeting point."
                )
            )
        }

        if (safeOptions.requireQuestTriggerNode()
            && nodes.none { node -> nodeMatchesAny(node, "quest_trigger", "quest_board", "notice_board") }
        ) {
            missingNodes.add("quest_trigger")
            gaps.add(
                VillageGap(
                    PatchGapType.MISSING_QUEST_TRIGGER_NODE,
                    1,
                    region.id(),
                    "",
                    "quest_trigger",
                    6,
                    "Regiunea nu are node pentru quest/story trigger."
                )
            )
        }

        if (places.isEmpty()) {
            warnings.add("Regiunea " + region.id() + " nu are places mapate.")
        }

        return GapReport(
            region.id(),
            safeOptions.targetPopulation(),
            currentCapacity,
            requiredCapacity,
            houses.size,
            missingHomes,
            missingWorkplaces.toList(),
            missingSocialPlaces,
            missingNodes.toList(),
            gaps,
            emptyList(),
            warnings,
            errors,
            capacityByHouse
        )
    }

    private fun emptyReport(
        regionId: String,
        options: PatchPlannerOptions,
        errors: List<String>,
        warnings: List<String>
    ): GapReport {
        return GapReport(
            regionId,
            options.targetPopulation(),
            0,
            options.targetPopulation(),
            0,
            0,
            emptyList(),
            0,
            emptyList(),
            emptyList(),
            emptyList(),
            warnings,
            errors,
            emptyMap()
        )
    }

    private fun resolveRegion(worldAdmin: WorldAdminApi, regionSelector: String?, errors: MutableList<String>): WorldRegionInfo? {
        if (regionSelector.isNullOrBlank()) {
            errors.add("Trebuie specificat regionId.")
            return null
        }

        val selector = regionSelector.trim()
        val matches = worldAdmin.getRegions()
            .filter { region ->
                region.id().equals(selector, ignoreCase = true) || region.name().equals(selector, ignoreCase = true)
            }
            .sortedBy { region -> region.id() }
        if (matches.size == 1) {
            return matches.first()
        }
        if (matches.isEmpty()) {
            errors.add("Regiunea $regionSelector nu a fost gasita.")
        } else {
            errors.add("Selectorul $regionSelector este ambiguu: " + matches.map { region -> region.id() })
        }
        return null
    }

    private fun houseCapacity(worldAdmin: WorldAdminApi, house: WorldPlaceInfo): Int {
        val metadataCapacity = parsePositiveIntMetadata(house, "max_residents", "maxResidents", "capacity")
        if (metadataCapacity > 0) {
            return metadataCapacity
        }

        val nodes = worldAdmin.getNodesForPlace(house.id()).toList()
        val bedCapacity = nodes.count { node -> nodeMatchesAny(node, "bed") }
        if (bedCapacity > 0) {
            return bedCapacity
        }
        return nodes.count { node -> nodeMatchesAny(node, "home") }
    }

    private fun isHousePlace(place: WorldPlaceInfo): Boolean {
        return place.placeType() == PlaceType.HOUSE ||
            place.hasTag("home") ||
            place.hasTag("house") ||
            metadataEquals(place, "role", "home") ||
            metadataEquals(place, "purpose", "home")
    }

    private fun isWorkplace(place: WorldPlaceInfo): Boolean {
        return place.hasTag("work") ||
            place.hasTag("workplace") ||
            place.hasTag("job") ||
            metadataEquals(place, "role", "work") ||
            metadataEquals(place, "purpose", "work") ||
            when (place.placeType()) {
                PlaceType.FORGE, PlaceType.SHOP, PlaceType.FARM, PlaceType.MARKET, PlaceType.TAVERN -> true
                else -> false
            }
    }

    private fun isSocialPlace(place: WorldPlaceInfo): Boolean {
        return place.placeType() == PlaceType.MARKET ||
            place.placeType() == PlaceType.TAVERN ||
            place.placeType() == PlaceType.CAMP ||
            place.hasTag("social") ||
            place.hasTag("public") ||
            place.hasTag("meeting") ||
            metadataEquals(place, "role", "social") ||
            metadataEquals(place, "purpose", "social")
    }

    private fun supportsProfession(place: WorldPlaceInfo, profession: String): Boolean {
        val normalizedProfession = normalizeToken(profession)
        if (normalizedProfession.isBlank()) {
            return false
        }
        if (matchesAnyToken(place.metadata()["profession"], normalizedProfession) ||
            matchesAnyToken(place.metadata()["occupation"], normalizedProfession) ||
            place.tags().any { tag -> matchesAnyToken(tag, normalizedProfession) }
        ) {
            return true
        }

        return when (normalizedProfession) {
            "blacksmith", "fierar", "armorer" -> place.placeType() == PlaceType.FORGE
            "farmer", "fermier" -> place.placeType() == PlaceType.FARM
            "merchant", "negustor" -> place.placeType() == PlaceType.MARKET || place.placeType() == PlaceType.SHOP
            "innkeeper", "hangiu" -> place.placeType() == PlaceType.TAVERN
            "guard", "garda", "soldat" -> place.hasTag("guard") || place.hasTag("barracks") || place.hasTag("watch")
            "priest", "preot" -> place.hasTag("shrine") || place.hasTag("altar") || place.hasTag("temple")
            else -> false
        }
    }

    private fun hasNode(nodes: Collection<WorldNodeInfo>, vararg tokens: String): Boolean {
        return nodes.any { node -> nodeMatchesAny(node, *tokens) }
    }

    private fun nodeMatchesAny(node: WorldNodeInfo, vararg expectedTokens: String): Boolean {
        if (matchesAnyToken(node.typeId(), *expectedTokens)) {
            return true
        }
        for ((key, value) in node.metadata()) {
            if (matchesAnyToken(key, *expectedTokens) || matchesAnyToken(value, *expectedTokens)) {
                return true
            }
        }
        return false
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

    private fun metadataEquals(place: WorldPlaceInfo, key: String, expectedValue: String): Boolean {
        val value = place.metadata()[key]
        return value != null && value.equals(expectedValue, ignoreCase = true)
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

    private fun normalizeToken(rawValue: String?): String {
        return rawValue?.trim()?.lowercase(Locale.ROOT)?.replace(' ', '_')?.replace('-', '_').orEmpty()
    }

    companion object {
        private const val DEFAULT_HOUSE_PATCH_CAPACITY = 2
    }
}
