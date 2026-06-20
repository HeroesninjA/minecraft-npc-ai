package ro.ainpc.world.exterior

import ro.ainpc.api.WorldAdminApi
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.util.Locale

class ExteriorStructureAnalyzer {
    fun analyze(worldAdmin: WorldAdminApi, region: WorldRegionInfo): ExteriorStructureReport {
        val places = worldAdmin.getPlaces(region.id()).sortedBy { place -> place.id() }
        val nodes = worldAdmin.getNodes(region.id()).sortedBy { node -> node.id() }
        val type = detectType(region, places, nodes)
        val warnings = ArrayList<String>()
        val errors = ArrayList<String>()
        val entryNodes = nodes.filter { node -> isEntryNode(node) }.map { node -> node.id() }
        val interactionNodes = nodes.filter { node -> isInteractionNode(node) }.map { node -> node.id() }

        validateCommon(region, places, nodes, type, entryNodes, warnings)
        validateByType(type, region, places, nodes, entryNodes, warnings, errors)

        return ExteriorStructureReport(
            region.id(),
            region.name(),
            type,
            places.map { place -> place.id() },
            nodes.map { node -> node.id() },
            entryNodes,
            interactionNodes,
            warnings,
            errors
        )
    }

    private fun detectType(
        region: WorldRegionInfo,
        places: List<WorldPlaceInfo>,
        nodes: List<WorldNodeInfo>
    ): ExteriorStructureType {
        val regionType = normalize(region.typeId())
        when (regionType) {
            "castle" -> return ExteriorStructureType.CASTLE
            "dungeon" -> return ExteriorStructureType.DUNGEON
            "cave" -> return ExteriorStructureType.CAVE_OR_MINE
            "wilderness" -> {
                if (hasAnyKeyword(region, places, nodes, ExteriorStructureType.FOREST.keywords())) {
                    return ExteriorStructureType.FOREST
                }
                return ExteriorStructureType.FOREST
            }
        }

        for (type in DETECTION_ORDER) {
            if (hasAnyKeyword(region, places, nodes, type.keywords())) {
                return type
            }
        }

        if (regionType == "settlement" && places.any { place -> place.placeType() == PlaceType.CAMP }) {
            return ExteriorStructureType.FACTION_SETTLEMENT
        }

        return ExteriorStructureType.CUSTOM
    }

    private fun validateCommon(
        region: WorldRegionInfo,
        places: List<WorldPlaceInfo>,
        nodes: List<WorldNodeInfo>,
        type: ExteriorStructureType,
        entryNodes: List<String>,
        warnings: MutableList<String>
    ) {
        if (type == ExteriorStructureType.CUSTOM) {
            warnings.add("Regiunea nu are un tip exterior canonic detectabil; foloseste tag/metadata exterior_structure_type.")
        }
        if (region.tags().isEmpty()) {
            warnings.add("Regiunea nu are tag-uri semantice; quest/story/AI vor avea context slab.")
        }
        if (places.isEmpty()) {
            warnings.add("Regiunea nu are places; structurile exterioare mature trebuie impartite semantic.")
        }
        if (nodes.isEmpty()) {
            warnings.add("Regiunea nu are nodes; interactiunile trebuie ancorate in WorldNode.")
        }
        if (entryNodes.isEmpty() && type != ExteriorStructureType.CUSTOM) {
            warnings.add("Nu exista node de intrare/poarta/drum; pathing-ul si questurile pot fi ambigue.")
        }
        if (requiresDangerMarker(type) && !hasDangerMarker(region, places, nodes)) {
            warnings.add("Lipseste marker/tag de risc pentru o structura cu pericol potential.")
        }
    }

    private fun validateByType(
        type: ExteriorStructureType,
        region: WorldRegionInfo,
        places: List<WorldPlaceInfo>,
        nodes: List<WorldNodeInfo>,
        entryNodes: List<String>,
        warnings: MutableList<String>,
        errors: MutableList<String>
    ) {
        when (type) {
            ExteriorStructureType.CASTLE -> validateCastle(places, nodes, entryNodes, warnings, errors)
            ExteriorStructureType.FOREST -> validateForest(region, places, nodes, warnings)
            ExteriorStructureType.FOUNTAIN -> validateFountain(places, nodes, errors)
            ExteriorStructureType.ISOLATED_HOUSE -> validateIsolatedHouse(places, nodes, warnings, errors)
            ExteriorStructureType.HAMLET -> validateHamlet(places, nodes, warnings, errors)
            ExteriorStructureType.FACTION_SETTLEMENT -> validateFactionSettlement(
                region,
                places,
                nodes,
                warnings,
                errors
            )

            ExteriorStructureType.DUNGEON -> validateDungeon(nodes, entryNodes, warnings, errors)
            ExteriorStructureType.CAMP -> validateCamp(places, nodes, warnings)
            ExteriorStructureType.RUINS -> validateRuins(places, nodes, warnings)
            ExteriorStructureType.CAVE_OR_MINE -> validateCaveOrMine(nodes, entryNodes, warnings, errors)
            ExteriorStructureType.SHRINE -> validateShrine(places, nodes, warnings, errors)
            ExteriorStructureType.WATCHTOWER -> validateWatchtower(places, nodes, warnings, errors)
            ExteriorStructureType.CUSTOM -> Unit
        }
    }

    private fun validateCastle(
        places: List<WorldPlaceInfo>,
        nodes: List<WorldNodeInfo>,
        entryNodes: List<String>,
        warnings: MutableList<String>,
        errors: MutableList<String>
    ) {
        if (entryNodes.isEmpty() && !hasKeywordInPlaces(places, "gate", "poarta", "entrance", "intrare")) {
            errors.add("Castelul trebuie sa aiba poarta sau intrare marcata semantic.")
        }
        if (!hasKeywordInPlaces(places, "throne", "sala_tronului", "court", "royal")) {
            warnings.add("Castelul nu are inca sala de autoritate/tron/court marcata.")
        }
        if (!hasKeywordInPlaces(places, "guard", "barracks", "armory") && !hasKeywordInNodes(
                nodes,
                "guard",
                "patrol"
            )
        ) {
            warnings.add("Castelul nu are ancora militara: guard_post, barracks sau armory.")
        }
    }

    private fun validateForest(
        region: WorldRegionInfo,
        places: List<WorldPlaceInfo>,
        nodes: List<WorldNodeInfo>,
        warnings: MutableList<String>
    ) {
        if (!hasKeywordInPlaces(places, "clearing", "luminis") && !hasKeywordInNodes(
                nodes,
                "clearing",
                "trail",
                "path"
            )
        ) {
            warnings.add("Padurea nu are luminis/trail marker pentru orientare.")
        }
        if (!hasKeyword(region, "resource") && !hasKeywordInPlaces(
                places,
                "resource",
                "herb",
                "grove"
            ) && !hasKeywordInNodes(
                nodes,
                "resource",
                "herb"
            )
        ) {
            warnings.add("Padurea nu are resource_node sau tag de resursa.")
        }
    }

    private fun validateFountain(
        places: List<WorldPlaceInfo>,
        nodes: List<WorldNodeInfo>,
        errors: MutableList<String>
    ) {
        if (!hasKeywordInPlaces(places, "fountain", "fantana", "well") && !hasKeywordInNodes(
                nodes,
                "fountain",
                "fantana",
                "well",
                "water_source"
            )
        ) {
            errors.add("Fantana trebuie sa existe ca place sau node semantic.")
        }
    }

    private fun validateIsolatedHouse(
        places: List<WorldPlaceInfo>,
        nodes: List<WorldNodeInfo>,
        warnings: MutableList<String>,
        errors: MutableList<String>
    ) {
        if (places.none { place ->
                place.placeType() == PlaceType.HOUSE || hasKeyword(
                    place,
                    "house",
                    "casa",
                    "shelter"
                )
            }) {
            errors.add("Casa izolata trebuie sa contina un place de tip house sau tag house.")
        }
        if (!hasNodeType(nodes, "bed", "home")) {
            warnings.add("Casa izolata nu are bed/home node pentru rezident sau rutina.")
        }
        if (!hasKeywordInNodes(nodes, "door", "entrance", "intrare")) {
            warnings.add("Casa izolata nu are usa/intrare marcata ca node.")
        }
    }

    private fun validateHamlet(
        places: List<WorldPlaceInfo>,
        nodes: List<WorldNodeInfo>,
        warnings: MutableList<String>,
        errors: MutableList<String>
    ) {
        val houseCount =
            places.count { place -> place.placeType() == PlaceType.HOUSE || hasKeyword(place, "house", "casa") }
        if (houseCount == 0) {
            errors.add("Mini-satul trebuie sa aiba cel putin o casa mapata.")
        }
        if (!hasKeywordInPlaces(places, "well", "fantana", "center", "centru", "market") &&
            !hasKeywordInNodes(nodes, "well", "fantana", "meeting_point", "center", "centru")
        ) {
            warnings.add("Mini-satul nu are centru/fantana/meeting point.")
        }
        if (!hasKeywordInNodes(nodes, "road_link", "path", "trail", "entrance")) {
            warnings.add("Mini-satul nu are legatura de drum marcata semantic.")
        }
    }

    private fun validateFactionSettlement(
        region: WorldRegionInfo,
        places: List<WorldPlaceInfo>,
        nodes: List<WorldNodeInfo>,
        warnings: MutableList<String>,
        errors: MutableList<String>
    ) {
        if (!hasKeyword(region, "faction") && !hasKeywordInPlaces(
                places,
                "faction",
                "tribal",
                "barbarian",
                "barbari"
            )
        ) {
            warnings.add("Asezarea de factiune nu are tag de factiune; foloseste faction sau faction:<id>.")
        }
        if (!hasKeywordInPlaces(places, "campfire", "center", "chieftain", "leader", "hut") &&
            !hasKeywordInNodes(nodes, "campfire", "center", "leader", "chieftain")
        ) {
            errors.add("Asezarea de factiune trebuie sa aiba centru/campfire/leader anchor.")
        }
        if (hasKeyword(region, "always_hostile")) {
            warnings.add("Evita always_hostile in core; prefera hostile_optional sau reguli de scenariu/addon.")
        }
        if (!hasKeywordInPlaces(places, "gate", "training", "storage", "prison") &&
            !hasKeywordInNodes(nodes, "gate", "training", "loot", "storage", "prison")
        ) {
            warnings.add("Asezarea de factiune nu are inca gate/training/storage/prison anchors.")
        }
    }

    private fun validateDungeon(
        nodes: List<WorldNodeInfo>,
        entryNodes: List<String>,
        warnings: MutableList<String>,
        errors: MutableList<String>
    ) {
        if (entryNodes.isEmpty()) {
            errors.add("Dungeon-ul trebuie sa aiba intrare marcata.")
        }
        if (!hasKeywordInNodes(nodes, "exit", "iesire")) {
            errors.add("Dungeon-ul trebuie sa aiba iesire marcata separat.")
        }
        if (!hasKeywordInNodes(nodes, "loot", "chest", "treasure", "boss", "checkpoint")) {
            warnings.add("Dungeon-ul nu are loot/boss/checkpoint node.")
        }
    }

    private fun validateCamp(
        places: List<WorldPlaceInfo>,
        nodes: List<WorldNodeInfo>,
        warnings: MutableList<String>
    ) {
        if (!hasKeywordInPlaces(places, "campfire", "tent") && !hasKeywordInNodes(
                nodes,
                "campfire",
                "tent",
                "npc_spawn"
            )
        ) {
            warnings.add("Tabara nu are campfire/tent/npc_spawn anchor.")
        }
    }

    private fun validateRuins(
        places: List<WorldPlaceInfo>,
        nodes: List<WorldNodeInfo>,
        warnings: MutableList<String>
    ) {
        if (!hasKeywordInPlaces(places, "inscription", "hidden", "altar") && !hasKeywordInNodes(
                nodes,
                "inspect",
                "inscription",
                "loot"
            )
        ) {
            warnings.add("Ruinele nu au inspect/inscription/loot anchor pentru gameplay.")
        }
    }

    private fun validateCaveOrMine(
        nodes: List<WorldNodeInfo>,
        entryNodes: List<String>,
        warnings: MutableList<String>,
        errors: MutableList<String>
    ) {
        if (entryNodes.isEmpty()) {
            errors.add("Pestera/mina trebuie sa aiba intrare marcata.")
        }
        if (!hasKeywordInNodes(nodes, "resource", "ore", "danger", "exit")) {
            warnings.add("Pestera/mina nu are resource/danger/exit nodes.")
        }
    }

    private fun validateShrine(
        places: List<WorldPlaceInfo>,
        nodes: List<WorldNodeInfo>,
        warnings: MutableList<String>,
        errors: MutableList<String>
    ) {
        if (!hasKeywordInPlaces(places, "altar", "shrine", "sanctuary") && !hasKeywordInNodes(
                nodes,
                "altar",
                "offering",
                "ritual"
            )
        ) {
            errors.add("Shrine-ul trebuie sa aiba altar/offering/ritual node.")
        }
        if (!hasKeywordInNodes(nodes, "inspect", "quest", "offering")) {
            warnings.add("Shrine-ul nu are inspect/quest/offering anchor.")
        }
    }

    private fun validateWatchtower(
        places: List<WorldPlaceInfo>,
        nodes: List<WorldNodeInfo>,
        warnings: MutableList<String>,
        errors: MutableList<String>
    ) {
        if (!hasKeywordInPlaces(places, "tower", "watchtower", "lookout", "turn") && !hasKeywordInNodes(
                nodes,
                "lookout",
                "signal",
                "guard"
            )
        ) {
            errors.add("Turnul de paza trebuie sa aiba lookout/signal/guard anchor.")
        }
        if (!hasKeywordInNodes(nodes, "entrance", "ladder", "stairs", "guard")) {
            warnings.add("Turnul de paza nu are acces sau guard node.")
        }
    }

    private fun requiresDangerMarker(type: ExteriorStructureType): Boolean =
        type == ExteriorStructureType.FOREST ||
            type == ExteriorStructureType.FACTION_SETTLEMENT ||
            type == ExteriorStructureType.DUNGEON ||
            type == ExteriorStructureType.CAVE_OR_MINE ||
            type == ExteriorStructureType.RUINS

    private fun hasDangerMarker(
        region: WorldRegionInfo,
        places: List<WorldPlaceInfo>,
        nodes: List<WorldNodeInfo>
    ): Boolean =
        hasKeyword(region, "danger", "risk", "safe", "hostile_optional", "combat", "boss") ||
            hasKeywordInPlaces(places, "danger", "risk", "safe", "hostile_optional", "combat", "boss") ||
            hasKeywordInNodes(nodes, "danger", "risk", "safe", "hostile_optional", "combat", "boss")

    private fun isEntryNode(node: WorldNodeInfo): Boolean =
        hasNodeType(listOf(node), "entrance") ||
            hasKeyword(node, "entrance", "entry", "intrare", "gate", "poarta", "road_link", "path", "trail")

    private fun isInteractionNode(node: WorldNodeInfo): Boolean =
        hasNodeType(
            listOf(node),
            "interaction",
            "quest_trigger",
            "meeting_point",
            "npc_spawn",
            "boss",
            "progression"
        ) ||
            hasKeyword(node, "inspect", "loot", "altar", "offering", "storage", "resource")

    private fun hasNodeType(nodes: List<WorldNodeInfo>, vararg typeIds: String): Boolean {
        val expected = typeIds.map { typeId -> normalize(typeId) }.toSet()
        return nodes.any { node -> expected.contains(normalize(node.typeId())) }
    }

    private fun hasAnyKeyword(
        region: WorldRegionInfo,
        places: List<WorldPlaceInfo>,
        nodes: List<WorldNodeInfo>,
        keywords: Set<String>
    ): Boolean =
        hasKeyword(region, *keywords.toTypedArray()) ||
            hasKeywordInPlaces(places, *keywords.toTypedArray()) ||
            hasKeywordInNodes(nodes, *keywords.toTypedArray())

    private fun hasKeyword(region: WorldRegionInfo, vararg keywords: String): Boolean =
        hasKeyword(regionValues(region), *keywords)

    private fun hasKeyword(place: WorldPlaceInfo, vararg keywords: String): Boolean =
        hasKeyword(placeValues(place), *keywords)

    private fun hasKeyword(node: WorldNodeInfo, vararg keywords: String): Boolean =
        hasKeyword(nodeValues(node), *keywords)

    private fun hasKeywordInPlaces(places: List<WorldPlaceInfo>, vararg keywords: String): Boolean =
        places.any { place -> hasKeyword(place, *keywords) }

    private fun hasKeywordInNodes(nodes: List<WorldNodeInfo>, vararg keywords: String): Boolean =
        nodes.any { node -> hasKeyword(node, *keywords) }

    private fun hasKeyword(values: Collection<String>, vararg keywords: String): Boolean {
        if (values.isEmpty() || keywords.isEmpty()) {
            return false
        }
        val normalizedValues = values.map { value -> normalize(value) }.filter { value -> value.isNotBlank() }
        val normalizedKeywords =
            keywords.map { keyword -> normalize(keyword) }.filter { keyword -> keyword.isNotBlank() }
        return normalizedKeywords.any { keyword ->
            normalizedValues.any { value -> value == keyword || value.contains(keyword) }
        }
    }

    private fun regionValues(region: WorldRegionInfo): List<String> {
        val values = ArrayList<String>()
        values.add(region.id())
        values.add(region.name())
        values.add(region.typeId())
        values.addAll(region.tags())
        return values
    }

    private fun placeValues(place: WorldPlaceInfo): List<String> {
        val values = ArrayList<String>()
        values.add(place.id())
        values.add(place.displayName())
        values.add(place.placeType().id)
        values.addAll(place.tags())
        values.addAll(place.metadata().keys)
        values.addAll(place.metadata().values)
        return values
    }

    private fun nodeValues(node: WorldNodeInfo): List<String> {
        val values = ArrayList<String>()
        values.add(node.id())
        values.add(node.typeId())
        values.addAll(node.metadata().keys)
        values.addAll(node.metadata().values)
        return values
    }

    private fun normalize(value: String?): String =
        value?.trim()?.lowercase(Locale.ROOT)
            ?.replace('-', '_')
            ?.replace(' ', '_')
            ?: ""

    private companion object {
        val DETECTION_ORDER = listOf(
            ExteriorStructureType.FACTION_SETTLEMENT,
            ExteriorStructureType.ISOLATED_HOUSE,
            ExteriorStructureType.HAMLET,
            ExteriorStructureType.FOUNTAIN,
            ExteriorStructureType.FOREST,
            ExteriorStructureType.CAMP,
            ExteriorStructureType.RUINS,
            ExteriorStructureType.CAVE_OR_MINE,
            ExteriorStructureType.SHRINE,
            ExteriorStructureType.WATCHTOWER,
            ExteriorStructureType.CASTLE,
            ExteriorStructureType.DUNGEON
        )
    }
}
