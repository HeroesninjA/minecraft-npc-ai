package ro.ainpc.story

import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import java.util.LinkedHashSet
import java.util.Locale

class StoryStructureSignalResolver {
    fun collectPlaceMetadataSignals(metadata: Map<String, String>?): List<Pair<String, String>> {
        if (metadata.isNullOrEmpty()) {
            return emptyList()
        }

        val signals = mutableListOf<Pair<String, String>>()
        for (key in listOf("story_state", "state", "tension", "danger", "danger_level", "event", "conflict", "quest_hook")) {
            metadata[key]?.takeIf { it.isNotBlank() }?.let { value ->
                signals.add("place_$key" to value)
            }
        }
        return signals
    }

    fun collectPlaceStructureSignals(place: WorldPlaceInfo?): List<Pair<String, String>> {
        if (place == null) {
            return emptyList()
        }

        val placeTypeId = normalizeStructureValue(place.placeType().id)
        if (placeTypeId.isBlank()) {
            return emptyList()
        }

        val signals = mutableListOf<Pair<String, String>>()
        signals.add("structure_type" to place.placeType().id)
        signals.add("structure_category" to structureCategoryForPlaceType(placeTypeId))

        val questHooks = LinkedHashSet<String>()
        questHooks.addAll(questHooksForPlaceType(placeTypeId))
        questHooks.addAll(questHooksForPlaceTags(place.tags()))
        questHooks.forEach { hook ->
            signals.add("quest_hook" to hook)
        }
        return signals
    }

    fun addStructureQuestCooldownSignals(
        place: WorldPlaceInfo?,
        recentStoryEvents: List<StoryEvent>?
    ): List<Pair<String, String>> {
        if (recentStoryEvents.isNullOrEmpty()) {
            return emptyList()
        }

        val structureEvents = recentStoryEvents.filter { event -> event.eventType().startsWith("structure_") }
        if (structureEvents.isEmpty()) {
            return emptyList()
        }

        val signals = mutableListOf<Pair<String, String>>()
        signals.add("recent_structure_event_count" to structureEvents.size.toString())
        signals.add("recent_structure_event_types" to collectEventTypes(structureEvents).joinToString(","))
        signals.add("last_structure_event_type" to structureEvents.first().eventType())
        signals.add("last_structure_event_key" to structureEvents.first().eventKey())
        signals.add("last_structure_event_place_id" to structureEvents.first().placeId())

        val currentPlaceId = place?.id().orEmpty()
        if (currentPlaceId.isBlank()) {
            return signals
        }

        val now = System.currentTimeMillis()
        val recentSamePlaceEvent = structureEvents.firstOrNull { event ->
            event.placeId() == currentPlaceId && now - event.createdAt() <= 30_000L
        } ?: return signals

        val structureCategory = structureCategoryFromEventType(recentSamePlaceEvent.eventType())
        val cooldownMs = structureCooldownMs(structureCategory)
        val elapsedMs = now - recentSamePlaceEvent.createdAt()
        if (elapsedMs >= cooldownMs) {
            return signals
        }

        signals.add("quest_generation_cooldown" to "structure_recent")
        signals.add("quest_generation_cooldown_place_id" to currentPlaceId)
        signals.add("quest_generation_cooldown_category" to structureCategory)
        signals.add("quest_generation_cooldown_ms" to cooldownMs.toString())
        signals.add("quest_generation_cooldown_elapsed_ms" to elapsedMs.toString())
        signals.add("quest_generation_cooldown_remaining_ms" to (cooldownMs - elapsedMs).coerceAtLeast(0L).toString())
        signals.add("quest_generation_cooldown_reason" to buildQuestGenerationCooldownReason(structureCategory, recentSamePlaceEvent.eventType()))
        signals.add("quest_generation_cooldown_event_type" to recentSamePlaceEvent.eventType())
        signals.add("quest_generation_cooldown_event_key" to recentSamePlaceEvent.eventKey())
        return signals
    }

    fun isStoryRelevantNode(node: WorldNodeInfo): Boolean {
        val type = node.typeId().lowercase(Locale.ROOT)
        if (type.contains("quest") || type.contains("inspect") || type.contains("event")) {
            return true
        }

        val metadata = node.metadata()
        return metadata.containsKey("quest")
            || metadata.containsKey("story")
            || metadata.containsKey("event")
            || metadata.containsKey("interaction")
    }

    private fun structureCooldownMs(structureCategory: String): Long {
        return when (structureCategory) {
            "residential" -> 10_000L
            "profession" -> 22_000L
            "utility" -> 12_000L
            "leisure" -> 6_000L
            "public" -> 14_000L
            "exterior" -> 30_000L
            "quest" -> 45_000L
            else -> 15_000L
        }
    }

    private fun structureCategoryFromEventType(eventType: String): String {
        val normalized = normalizeStructureValue(eventType)
        return when {
            normalized.startsWith("structure_visit_") -> normalized.removePrefix("structure_visit_")
            normalized.startsWith("structure_node_") -> normalized.removePrefix("structure_node_")
            else -> "unknown"
        }
    }

    private fun buildQuestGenerationCooldownReason(structureCategory: String, eventType: String): String {
        return "structure_category=$structureCategory,event_type=$eventType"
    }

    private fun questHooksForPlaceType(placeTypeId: String): List<String> {
        return when (placeTypeId) {
            "house", "home", "apartment", "farmhouse", "shared_house", "inn_room" -> listOf("home_life", "household_request", "family_help")
            "farm" -> listOf("harvest", "field_work", "delivery")
            "smithy" -> listOf("forge_order", "repair_tool", "metal_supply")
            "shop" -> listOf("market_supply", "customer_request", "shortage")
            "bakery" -> listOf("bread_order", "grain_supply", "delivery")
            "mill" -> listOf("grain_supply", "machine_repair", "delivery")
            "workshop", "carpentry", "tailor_shop" -> listOf("craft_order", "repair_order", "material_supply")
            "tavern" -> listOf("social_event", "rumor", "missing_person")
            "town_hall", "administration", "court", "library", "church", "civic_hall" -> listOf("civic_request", "public_event", "lost_record")
            "park", "market_square", "plaza", "public_garden", "campfire_area", "performance_area" -> listOf("community_event", "social_meeting", "festival")
            "watchtower", "outpost" -> listOf("route_report", "observe_tracks", "patrol_request")
            "forest", "ruin", "cave", "fort", "shrine", "caravan_stop", "hidden_camp", "border_watch" -> listOf("exploration", "route_report", "danger")
            "quest_board", "ancient_altar", "sealed_door", "investigation_house", "delivery_point", "secret_cellar", "bandit_camp", "abandoned_tower", "crypt" -> listOf("quest_hook", "investigation", "recover_artifact")
            else -> emptyList()
        }
    }

    private fun questHooksForPlaceTags(tags: List<String>?): List<String> {
        if (tags.isNullOrEmpty()) {
            return emptyList()
        }

        val normalizedTags = tags.asSequence()
            .map { normalizeStructureValue(it) }
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

    private fun structureCategoryForPlaceType(placeTypeId: String): String {
        return when (placeTypeId) {
            "house", "home", "apartment", "farmhouse", "shared_house", "inn_room" -> "residential"
            "farm", "smithy", "shop", "bakery", "mill", "workshop", "carpentry", "tailor_shop", "tavern", "town_hall", "temple" -> "profession"
            "well", "storage", "barn", "gate", "bridge", "path", "road", "stable", "collection_point", "lighting" -> "utility"
            "park", "market_square", "plaza", "bench_area", "public_garden", "campfire_area", "performance_area" -> "leisure"
            "administration", "court", "church", "library", "watchtower", "training_ground", "civic_hall" -> "public"
            "forest", "ruin", "cave", "outpost", "fort", "shrine", "caravan_stop", "hidden_camp", "border_watch" -> "exterior"
            "quest_board", "ancient_altar", "sealed_door", "investigation_house", "delivery_point", "secret_cellar", "bandit_camp", "abandoned_tower", "crypt" -> "quest"
            else -> "unknown"
        }
    }

    private fun normalizeStructureValue(value: String?): String {
        if (value.isNullOrBlank()) {
            return ""
        }

        return value.trim()
            .lowercase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_')
    }

    private fun collectEventTypes(events: List<StoryEvent>): List<String> {
        return events.asSequence()
            .map(StoryEvent::eventType)
            .filter { !it.isNullOrBlank() }
            .distinct()
            .take(5)
            .toList()
    }
}
