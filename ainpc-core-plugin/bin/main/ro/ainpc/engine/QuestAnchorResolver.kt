package ro.ainpc.engine

import org.bukkit.Location
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.npc.AINPC
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.util.LinkedHashMap
import java.util.Locale

class QuestAnchorResolver(
    private val worldAdminApi: WorldAdminApi?,
    npcs: Collection<AINPC>?
) {
    private val npcs: Collection<AINPC> = npcs?.toList() ?: listOf()

    fun resolve(
        template: ScenarioEngine.ScenarioTemplate?,
        playerLocation: Location?,
        questGiver: AINPC?
    ): ResolvedQuestAnchors {
        if (template == null || template.objectives.isEmpty()) {
            return ResolvedQuestAnchors.valid(listOf())
        }

        val anchors = mutableListOf<ResolvedQuestAnchor>()
        val issues = mutableListOf<ResolutionIssue>()
        val objectives = template.objectives
        for (index in objectives.indices) {
            val objective = objectives[index]
            val objectiveType = normalizeObjectiveType(objective?.type ?: "")
            val objectiveKey = buildObjectiveKey(objective, index)
            val reference = objective?.itemId ?: ""

            when (objectiveType) {
                "visit_region" -> resolveRegionObjective(objectiveKey, reference, playerLocation, anchors, issues)
                "visit_place" -> resolvePlaceObjective(objectiveKey, reference, playerLocation, anchors, issues)
                "inspect_node" -> resolveNodeObjective(objectiveKey, reference, playerLocation, anchors, issues)
                "talk_to_npc" -> resolveNpcObjective(objectiveKey, reference, questGiver, anchors)
                else -> {
                    // Non-semantic objectives are validated by their own systems.
                }
            }
        }

        return ResolvedQuestAnchors(anchors.toList(), issues.toList())
    }

    private fun resolveRegionObjective(
        objectiveKey: String,
        reference: String,
        playerLocation: Location?,
        anchors: MutableList<ResolvedQuestAnchor>,
        issues: MutableList<ResolutionIssue>
    ) {
        if (!isWorldAdminReadyFor("region", objectiveKey, issues)) {
            return
        }

        var region: WorldRegionInfo? = null
        if (isBlank(reference)) {
            region = findCurrentRegion(playerLocation)
            if (region == null && worldAdminApi!!.regionCount == 1) {
                region = worldAdminApi.regions.first()
            }
            if (region != null) {
                anchors.add(anchor(objectiveKey, "visit_region", reference, "region", region.id(), region.name()))
            }
            return
        }

        region = worldAdminApi!!.regions.stream()
            .filter { candidate -> matchesRegion(reference, candidate) }
            .findFirst()
            .orElse(null)
        if (region == null) {
            issues.add(ResolutionIssue(objectiveKey, "region", reference, "Nu exista regiune/tag/tip pentru obiectiv."))
            return
        }

        anchors.add(anchor(objectiveKey, "visit_region", reference, "region", region.id(), region.name()))
    }

    private fun resolvePlaceObjective(
        objectiveKey: String,
        reference: String,
        playerLocation: Location?,
        anchors: MutableList<ResolvedQuestAnchor>,
        issues: MutableList<ResolutionIssue>
    ) {
        if (!isWorldAdminReadyFor("place", objectiveKey, issues)) {
            return
        }

        val place: WorldPlaceInfo?
        if (isBlank(reference)) {
            place = findCurrentPlace(playerLocation) ?: if (worldAdminApi!!.placeCount == 1) worldAdminApi.places.first() else null
            if (place != null) {
                anchors.add(anchor(objectiveKey, "visit_place", reference, "place", place.id(), place.displayName()))
            }
            return
        }

        place = orderedPlaces(playerLocation).stream()
            .filter { candidate -> matchesPlace(reference, candidate) }
            .findFirst()
            .orElse(null)
        if (place == null) {
            issues.add(ResolutionIssue(objectiveKey, "place", reference, "Nu exista place/tag/tip pentru obiectiv."))
            return
        }

        anchors.add(anchor(objectiveKey, "visit_place", reference, "place", place.id(), place.displayName()))
    }

    private fun resolveNodeObjective(
        objectiveKey: String,
        reference: String,
        playerLocation: Location?,
        anchors: MutableList<ResolvedQuestAnchor>,
        issues: MutableList<ResolutionIssue>
    ) {
        if (!isWorldAdminReadyFor("node", objectiveKey, issues)) {
            return
        }

        val node: WorldNodeInfo?
        if (isBlank(reference)) {
            node = findCurrentNode(playerLocation) ?: if (worldAdminApi!!.nodeCount == 1) worldAdminApi.nodes.first() else null
            if (node != null) {
                anchors.add(anchor(objectiveKey, "inspect_node", reference, "node", node.id(), node.typeId()))
            }
            return
        }

        node = orderedNodes(playerLocation).stream()
            .filter { candidate -> matchesNode(reference, candidate) }
            .findFirst()
            .orElse(null)
        if (node == null) {
            issues.add(ResolutionIssue(objectiveKey, "node", reference, "Nu exista node/tip/metadata pentru obiectiv."))
            return
        }

        anchors.add(anchor(objectiveKey, "inspect_node", reference, "node", node.id(), node.typeId()))
    }

    private fun resolveNpcObjective(
        objectiveKey: String,
        reference: String,
        questGiver: AINPC?,
        anchors: MutableList<ResolvedQuestAnchor>
    ) {
        val npc = findMatchingNpc(reference, questGiver) ?: return

        val anchorId = if (npc.uuid != null) npc.uuid.toString() else npc.databaseId.toString()
        anchors.add(anchor(objectiveKey, "talk_to_npc", reference, "npc", anchorId, npc.name))
    }

    private fun isWorldAdminReadyFor(anchorType: String, objectiveKey: String, issues: MutableList<ResolutionIssue>): Boolean {
        if (worldAdminApi == null || !worldAdminApi.isEnabled) {
            issues.add(ResolutionIssue(objectiveKey, anchorType, "", "World admin este dezactivat."))
            return false
        }

        val missing = when (anchorType) {
            "region" -> worldAdminApi.regionCount <= 0
            "place" -> worldAdminApi.placeCount <= 0
            "node" -> worldAdminApi.nodeCount <= 0
            else -> false
        }
        if (missing) {
            issues.add(ResolutionIssue(objectiveKey, anchorType, "", "Mapping-ul nu are ancore de tip $anchorType."))
            return false
        }

        return true
    }

    private fun findCurrentRegion(location: Location?): WorldRegionInfo? {
        if (location == null || location.world == null || worldAdminApi == null) {
            return null
        }
        return worldAdminApi.findRegion(location.world.name, location.blockX, location.blockY, location.blockZ)
    }

    private fun findCurrentPlace(location: Location?): WorldPlaceInfo? {
        if (location == null || location.world == null || worldAdminApi == null) {
            return null
        }
        return worldAdminApi.findPlace(location.world.name, location.blockX, location.blockY, location.blockZ)
    }

    private fun findCurrentNode(location: Location?): WorldNodeInfo? {
        if (location == null || location.world == null || worldAdminApi == null) {
            return null
        }
        return worldAdminApi.findNode(location.world.name, location.blockX, location.blockY, location.blockZ)
    }

    private fun orderedPlaces(playerLocation: Location?): List<WorldPlaceInfo> {
        val places = ArrayList(worldAdminApi!!.places)
        if (playerLocation == null || playerLocation.world == null) {
            return places
        }

        val currentRegion = findCurrentRegion(playerLocation)
        val currentRegionId = currentRegion?.id() ?: ""
        places.sortWith(
            compareBy<WorldPlaceInfo> { place -> !place.regionId().equals(currentRegionId, ignoreCase = true) }
                .thenBy { place -> distanceSquaredToPlaceCenter(playerLocation, place) }
        )
        return places
    }

    private fun orderedNodes(playerLocation: Location?): List<WorldNodeInfo> {
        val nodes = ArrayList(worldAdminApi!!.nodes)
        if (playerLocation == null || playerLocation.world == null) {
            return nodes
        }

        val currentPlace = findCurrentPlace(playerLocation)
        val currentPlaceId = currentPlace?.id() ?: ""
        nodes.sortWith(
            compareBy<WorldNodeInfo> { node -> !node.placeId().equals(currentPlaceId, ignoreCase = true) }
                .thenBy { node -> distanceSquaredToNode(playerLocation, node) }
        )
        return nodes
    }

    private fun findMatchingNpc(reference: String, questGiver: AINPC?): AINPC? {
        if (isBlank(reference)) {
            return questGiver
        }
        if (questGiver != null && matchesNpc(reference, questGiver)) {
            return questGiver
        }
        return npcs.stream()
            .filter { npc -> matchesNpc(reference, npc) }
            .findFirst()
            .orElse(null)
    }

    private fun matchesRegion(reference: String, region: WorldRegionInfo?): Boolean {
        if (region == null) return false
        val candidates = mutableListOf<String>()
        candidates.add(region.id())
        candidates.add(region.name())
        candidates.add(region.typeId())
        candidates.addAll(region.tags())
        candidates.add(region.storyStateKey())
        candidates.addAll(region.storyPool())
        return matchesReference(reference, candidates)
    }

    private fun matchesPlace(reference: String, place: WorldPlaceInfo?): Boolean {
        if (place == null) return false
        val candidates = mutableListOf<String>()
        candidates.add(place.id())
        candidates.add(place.displayName())
        candidates.add(place.regionId())
        candidates.add(place.placeType().name)
        candidates.add(place.placeType().id)
        candidates.addAll(place.tags())
        candidates.addAll(place.metadata().keys)
        candidates.addAll(place.metadata().values)
        return matchesReference(reference, candidates)
    }

    private fun matchesNode(reference: String, node: WorldNodeInfo?): Boolean {
        if (node == null) return false
        val candidates = mutableListOf<String>()
        candidates.add(node.id())
        candidates.add(node.regionId())
        candidates.add(node.placeId())
        candidates.add(node.typeId())
        candidates.addAll(node.metadata().keys)
        candidates.addAll(node.metadata().values)
        return matchesReference(reference, candidates)
    }

    private fun matchesNpc(reference: String, npc: AINPC?): Boolean {
        if (npc == null) return false
        val candidates = mutableListOf<String>()
        candidates.add(npc.name)
        candidates.add(npc.displayName ?: "")
        candidates.add(npc.occupation ?: "")
        if (npc.uuid != null) candidates.add(npc.uuid.toString())
        if (npc.databaseId > 0) candidates.add(npc.databaseId.toString())
        return matchesReference(reference, candidates)
    }

    private fun matchesReference(reference: String, candidates: Collection<String>): Boolean {
        val normalizedReference = normalizeReference(stripObjectivePrefix(reference))
        if (normalizedReference.isBlank()) {
            return false
        }
        for (candidate in candidates) {
            if (normalizedReference == normalizeReference(candidate)) {
                return true
            }
        }
        return false
    }

    private fun anchor(
        objectiveKey: String,
        objectiveType: String,
        reference: String?,
        anchorType: String,
        anchorId: String?,
        label: String?
    ): ResolvedQuestAnchor {
        return ResolvedQuestAnchor(
            objectiveKey,
            objectiveType,
            reference ?: "",
            anchorType,
            anchorId ?: "",
            label ?: ""
        )
    }

    private fun normalizeObjectiveType(type: String?): String {
        val normalized = normalizeReference(type)
        return when (normalized) {
            "", "item", "collect", "collectitem", "collect_item", "fetch", "gather" -> "collect_item"
            "deliver", "deliveritem", "deliver_item", "deliver_to_npc", "turnin", "turn_in" -> "deliver_to_npc"
            "talk", "speak", "conversation", "talk_to_npc", "speak_to_npc" -> "talk_to_npc"
            "visit", "travel", "go_to", "visit_region", "enter_region" -> "visit_region"
            "visitplace", "visit_place", "enterplace", "enter_place", "go_to_place", "place" -> "visit_place"
            "inspect", "inspectnode", "inspect_node", "interact_node", "node" -> "inspect_node"
            "kill", "slay", "defeat", "kill_mob" -> "kill_mob"
            else -> normalized
        }
    }

    private fun buildObjectiveKey(objective: FeaturePackLoader.QuestEntryDefinition?, index: Int): String {
        val entryId = if (objective != null) normalizeEntryId(objective.entryId) else ""
        if (entryId.isNotBlank()) {
            return entryId
        }

        val type = if (objective != null && !objective.type.isNullOrBlank()) normalize(objective.type) else "objective"
        val itemId = if (objective != null && !objective.itemId.isNullOrBlank()) normalize(objective.itemId) else "entry"
        return "$type:$itemId:$index"
    }

    private fun normalizeEntryId(entryId: String?): String = entryId?.trim() ?: ""

    private fun stripObjectivePrefix(reference: String?): String {
        if (reference.isNullOrBlank()) {
            return ""
        }

        val trimmed = reference.trim()
        val prefixSeparator = trimmed.indexOf(':')
        if (prefixSeparator <= 0) {
            return trimmed
        }

        val prefix = normalizeReference(trimmed.substring(0, prefixSeparator))
        return when (prefix) {
            "npc", "name", "profession", "region", "place", "node", "tag", "type", "mob", "entity" ->
                trimmed.substring(prefixSeparator + 1)
            else -> trimmed
        }
    }

    private fun normalizeReference(value: String?): String {
        if (value.isNullOrBlank()) {
            return ""
        }

        return value.lowercase(Locale.ROOT)
            .replace("minecraft:", "")
            .replace(Regex("[^\\p{L}\\p{Nd}]+"), "_")
            .replace(Regex("^_+|_+$"), "")
            .replace(Regex("_+"), "_")
    }

    private fun normalize(value: String?): String = value?.trim()?.lowercase(Locale.ROOT) ?: ""

    private fun isBlank(value: String?): Boolean = value.isNullOrBlank()

    private fun distanceSquaredToPlaceCenter(location: Location, place: WorldPlaceInfo): Double {
        val centerX = (place.minX() + place.maxX()) / 2.0
        val centerY = (place.minY() + place.maxY()) / 2.0
        val centerZ = (place.minZ() + place.maxZ()) / 2.0
        val dx = centerX - location.x
        val dy = centerY - location.y
        val dz = centerZ - location.z
        return dx * dx + dy * dy + dz * dz
    }

    private fun distanceSquaredToNode(location: Location, node: WorldNodeInfo): Double {
        val dx = node.x() - location.x
        val dy = node.y() - location.y
        val dz = node.z() - location.z
        return dx * dx + dy * dy + dz * dz
    }

    data class ResolvedQuestAnchors(
        val anchors: List<ResolvedQuestAnchor>?,
        val issues: List<ResolutionIssue>?
    ) {
        private val safeAnchors = anchors?.toList() ?: listOf()
        private val safeIssues = issues?.toList() ?: listOf()

        fun anchors(): List<ResolvedQuestAnchor> = safeAnchors.toList()
        fun issues(): List<ResolutionIssue> = safeIssues.toList()

        fun valid(): Boolean = safeIssues.isEmpty()

        fun toQuestVariables(): Map<String, String> {
            val variables = LinkedHashMap<String, String>()
            variables["quest_anchor_count"] = safeAnchors.size.toString()
            for (anchor in safeAnchors) {
                val prefix = "anchor." + anchor.objectiveKey()
                variables["$prefix.objective_type"] = anchor.objectiveType()
                variables["$prefix.reference"] = anchor.reference()
                variables["$prefix.type"] = anchor.anchorType()
                variables["$prefix.id"] = anchor.anchorId()
                variables["$prefix.label"] = anchor.label()
            }
            return variables
        }

        fun formatIssues(): List<String> {
            return safeIssues.stream()
                .map { issue -> issue.anchorType() + " `" + issue.reference() + "`: " + issue.message() }
                .toList()
        }

        companion object {
            @JvmStatic
            fun valid(anchors: List<ResolvedQuestAnchor>): ResolvedQuestAnchors {
                return ResolvedQuestAnchors(anchors, listOf())
            }
        }
    }

    data class ResolvedQuestAnchor(
        val objectiveKey: String,
        val objectiveType: String,
        val reference: String,
        val anchorType: String,
        val anchorId: String,
        val label: String
    ) {
        fun objectiveKey(): String = objectiveKey
        fun objectiveType(): String = objectiveType
        fun reference(): String = reference
        fun anchorType(): String = anchorType
        fun anchorId(): String = anchorId
        fun label(): String = label
    }

    data class ResolutionIssue(
        val objectiveKey: String,
        val anchorType: String,
        val reference: String,
        val message: String
    ) {
        fun objectiveKey(): String = objectiveKey
        fun anchorType(): String = anchorType
        fun reference(): String = reference
        fun message(): String = message
    }
}
