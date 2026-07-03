package ro.ainpc.story

import ro.ainpc.world.WorldNode
import ro.ainpc.world.WorldPlace
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Locale

class StructureStoryEventPlanner {
    fun planPlaceVisit(regionId: String?, place: WorldPlace?, node: WorldNode?): PlannedStructureStoryEvent? {
        if (place == null) {
            return null
        }

        val placeTypeId = normalize(place.placeType.id)
        val structureCategory = structureCategoryForPlaceType(placeTypeId)
        if (structureCategory == "unknown") {
            return null
        }

        val questHooks = collectQuestHooks(place, node)
        val eventType = structureVisitEventType(structureCategory) ?: return null
        val eventKey = firstNonBlank(
            metadataValue(place.getMetadata(), listOf("quest_hook", "event", "conflict", "danger", "danger_level")),
            questHooks.firstOrNull(),
            node?.let { normalize(it.type.id) },
            placeTypeId,
            structureCategory
        ).ifBlank { "structure_visit" }

        return PlannedStructureStoryEvent(
            scopeType = "place",
            scopeId = place.id,
            regionId = regionId.orEmpty(),
            placeId = place.id,
            nodeId = node?.id.orEmpty(),
            eventType = eventType,
            eventKey = eventKey,
            title = "Vizită la ${place.displayName}",
            description = "Loc structural ${place.displayName} (${structureCategory}).",
            payload = buildPayload(
                interaction = "place_visit",
                regionId = regionId,
                place = place,
                node = node,
                structureCategory = structureCategory,
                questHooks = questHooks
            )
        )
    }

    fun planNodeInteraction(regionId: String?, place: WorldPlace?, node: WorldNode?): PlannedStructureStoryEvent? {
        if (place == null || node == null) {
            return null
        }

        val nodeTypeId = normalize(node.type.id)
        if (!isInterestingNode(node) && !hasRelevantNodeMetadata(node)) {
            return null
        }

        val placeTypeId = normalize(place.placeType.id)
        val structureCategory = structureCategoryForPlaceType(placeTypeId)
        val eventType = structureNodeEventType(nodeTypeId, structureCategory) ?: return null
        val questHooks = collectQuestHooks(place, node)
        val eventKey = firstNonBlank(
            metadataValue(node.getMetadata(), listOf("quest_hook", "event", "conflict", "story", "quest")),
            metadataValue(place.getMetadata(), listOf("quest_hook", "event", "conflict", "danger", "danger_level")),
            questHooks.firstOrNull(),
            nodeTypeId,
            placeTypeId
        ).ifBlank { "node_interaction" }

        return PlannedStructureStoryEvent(
            scopeType = "place",
            scopeId = place.id,
            regionId = regionId.orEmpty(),
            placeId = place.id,
            nodeId = node.id,
            eventType = eventType,
            eventKey = eventKey,
            title = "Interacțiune la ${node.type.displayName}",
            description = "Interacțiune cu nodul ${node.id} din ${place.displayName}.",
            payload = buildPayload(
                interaction = "node_interaction",
                regionId = regionId,
                place = place,
                node = node,
                structureCategory = structureCategory,
                questHooks = questHooks
            )
        )
    }

    private fun buildPayload(
        interaction: String,
        regionId: String?,
        place: WorldPlace,
        node: WorldNode?,
        structureCategory: String,
        questHooks: List<String>
    ): Map<String, String> {
        val payload = LinkedHashMap<String, String>()
        payload["interaction"] = interaction
        payload["region_id"] = regionId.orEmpty()
        payload["place_id"] = place.id
        payload["place_type"] = place.placeType.id
        payload["structure_category"] = structureCategory
        payload["structure_type"] = place.placeType.id
        payload["structure_display_name"] = place.displayName
        payload["place_tags"] = place.getTags().joinToString(",")
        payload["quest_hooks"] = questHooks.joinToString(",")
        payload["node_id"] = node?.id.orEmpty()
        payload["node_type"] = node?.type?.id.orEmpty()

        addMetadataSignals(payload, "place", place.getMetadata())
        if (node != null) {
            addMetadataSignals(payload, "node", node.getMetadata())
        }
        return payload
    }

    private fun addMetadataSignals(target: MutableMap<String, String>, prefix: String, metadata: Map<String, String>) {
        for (key in listOf("story_state", "state", "tension", "danger", "danger_level", "event", "conflict", "quest_hook")) {
            val value = metadata[key].orEmpty().trim()
            if (value.isNotBlank()) {
                target["${prefix}_$key"] = value
            }
        }
    }

    private fun collectQuestHooks(place: WorldPlace, node: WorldNode?): List<String> {
        val hooks = LinkedHashSet<String>()
        hooks.addAll(questHooksForPlaceType(normalize(place.placeType.id)))
        hooks.addAll(questHooksForPlaceTags(place.getTags()))
        hooks.addAll(questHooksForMetadata(place.getMetadata()))
        if (node != null) {
            hooks.addAll(questHooksForNode(node))
        }
        return hooks.toList()
    }

    private fun questHooksForPlaceType(placeTypeId: String): List<String> {
        return when (placeTypeId) {
            "house", "home", "apartment", "farmhouse", "shared_house", "inn_room" -> listOf("home_life", "household_request", "family_help")
            "farm" -> listOf("harvest", "field_work", "delivery")
            "forge" -> listOf("forge_order", "repair_tool", "metal_supply")
            "shop" -> listOf("market_supply", "customer_request", "shortage")
            "tavern" -> listOf("social_event", "rumor", "missing_person")
            "market" -> listOf("market_supply", "community_event", "trade")
            "castle_room" -> listOf("civic_request", "public_event", "lost_record")
            "cave_room" -> listOf("exploration", "danger", "recover_artifact")
            "camp" -> listOf("exploration", "route_report", "danger")
            else -> emptyList()
        }
    }

    private fun questHooksForPlaceTags(tags: List<String>): List<String> {
        if (tags.isEmpty()) {
            return emptyList()
        }

        val normalizedTags = tags.asSequence()
            .map { normalize(it) }
            .filter { it.isNotBlank() }
            .toSet()

        val hooks = LinkedHashSet<String>()
        if (normalizedTags.any { it == "market" || it == "trade" || it == "merchant" }) {
            hooks.add("market_supply")
        }
        if (normalizedTags.any { it == "farming" || it == "farm" || it == "harvest" }) {
            hooks.add("harvest")
        }
        if (normalizedTags.any { it == "social" || it == "culture" || it == "public" }) {
            hooks.add("community_event")
        }
        if (normalizedTags.any { it == "danger" || it == "dungeon" || it == "ambush" }) {
            hooks.add("danger")
        }
        if (normalizedTags.any { it == "quest" || it == "story" }) {
            hooks.add("quest_hook")
        }
        if (normalizedTags.any { it == "admin" || it == "civil" || it == "civic" }) {
            hooks.add("civic_request")
        }
        if (normalizedTags.any { it == "route" || it == "watch" || it == "border" }) {
            hooks.add("route_report")
        }
        if (normalizedTags.any { it == "resource" || it == "wilderness" }) {
            hooks.add("exploration")
        }
        return hooks.toList()
    }

    private fun questHooksForMetadata(metadata: Map<String, String>): List<String> {
        if (metadata.isEmpty()) {
            return emptyList()
        }

        val hooks = LinkedHashSet<String>()
        metadata["quest_hook"]?.takeIf { it.isNotBlank() }?.let { hooks.add(normalize(it)) }
        metadata["event"]?.takeIf { it.isNotBlank() }?.let { hooks.add(normalize(it)) }
        metadata["conflict"]?.takeIf { it.isNotBlank() }?.let { hooks.add(normalize(it)) }
        metadata["danger"]?.takeIf { it.isNotBlank() }?.let { hooks.add(normalize(it)) }
        metadata["danger_level"]?.takeIf { it.isNotBlank() }?.let { hooks.add(normalize(it)) }
        return hooks.filter { it.isNotBlank() }
    }

    private fun questHooksForNode(node: WorldNode): List<String> {
        val hooks = LinkedHashSet<String>()
        val nodeType = normalize(node.type.id)
        if (nodeType == "quest_trigger") {
            hooks.add("quest_trigger")
        }
        if (nodeType == "boss") {
            hooks.add("boss")
        }
        if (nodeType == "social" || nodeType == "meeting_point") {
            hooks.add("social_event")
        }
        if (nodeType == "work" || nodeType == "workstation") {
            hooks.add("work_event")
        }
        if (nodeType == "home" || nodeType == "bed") {
            hooks.add("home_event")
        }
        hooks.addAll(questHooksForMetadata(node.getMetadata()))
        return hooks.toList()
    }

    private fun structureVisitEventType(structureCategory: String): String? {
        return when (structureCategory) {
            "residential" -> "structure_visit_residential"
            "profession" -> "structure_visit_profession"
            "utility" -> "structure_visit_utility"
            "leisure" -> "structure_visit_leisure"
            "public" -> "structure_visit_public"
            "exterior" -> "structure_visit_exterior"
            "quest" -> "structure_visit_quest"
            else -> null
        }
    }

    private fun structureNodeEventType(nodeTypeId: String, structureCategory: String): String? {
        return when (nodeTypeId) {
            "quest_trigger" -> "structure_node_quest"
            "boss" -> "structure_node_boss"
            "social", "meeting_point" -> "structure_node_social"
            "work", "workstation" -> "structure_node_work"
            "home", "bed" -> "structure_node_home"
            "entrance", "interaction", "progression" -> "structure_node_interaction"
            else -> structureVisitEventType(structureCategory)?.replace("visit", "node")
        }
    }

    private fun structureCategoryForPlaceType(placeTypeId: String): String {
        return when (placeTypeId) {
            "house", "home", "apartment", "farmhouse", "shared_house", "inn_room" -> "residential"
            "farm", "forge", "shop", "tavern", "market" -> "profession"
            "well", "storage", "barn", "gate", "bridge", "path", "road", "stable", "collection_point", "lighting" -> "utility"
            "park", "market_square", "plaza", "bench_area", "public_garden", "campfire_area", "performance_area" -> "leisure"
            "administration", "court", "church", "library", "watchtower", "training_ground", "civic_hall", "castle_room" -> "public"
            "forest", "ruin", "cave", "outpost", "fort", "shrine", "caravan_stop", "hidden_camp", "border_watch", "camp" -> "exterior"
            "quest_board", "ancient_altar", "sealed_door", "investigation_house", "delivery_point", "secret_cellar", "bandit_camp", "abandoned_tower", "crypt" -> "quest"
            else -> "unknown"
        }
    }

    private fun isInterestingNode(node: WorldNode): Boolean {
        val nodeType = normalize(node.type.id)
        return nodeType == "quest_trigger" ||
            nodeType == "boss" ||
            nodeType == "social" ||
            nodeType == "meeting_point" ||
            nodeType == "work" ||
            nodeType == "workstation" ||
            nodeType == "home" ||
            nodeType == "bed" ||
            nodeType == "entrance" ||
            nodeType == "interaction" ||
            nodeType == "progression" ||
            nodeType == "custom"
    }

    private fun hasRelevantNodeMetadata(node: WorldNode): Boolean {
        val metadata = node.getMetadata()
        return metadata.containsKey("quest") ||
            metadata.containsKey("story") ||
            metadata.containsKey("event") ||
            metadata.containsKey("conflict") ||
            metadata.containsKey("danger") ||
            metadata.containsKey("danger_level")
    }

    private fun metadataValue(metadata: Map<String, String>, keys: List<String>): String {
        for (key in keys) {
            val value = metadata[key].orEmpty().trim()
            if (value.isNotBlank()) {
                return normalize(value)
            }
        }
        return ""
    }

    private fun firstNonBlank(vararg values: String?): String {
        for (value in values) {
            val safeValue = value?.trim().orEmpty()
            if (safeValue.isNotBlank()) {
                return safeValue
            }
        }
        return ""
    }

    private fun normalize(value: String?): String {
        if (value.isNullOrBlank()) {
            return ""
        }
        return value.trim()
            .lowercase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_')
    }
}

data class PlannedStructureStoryEvent(
    val scopeType: String,
    val scopeId: String,
    val regionId: String,
    val placeId: String,
    val nodeId: String,
    val eventType: String,
    val eventKey: String,
    val title: String,
    val description: String,
    val payload: Map<String, String>
)
