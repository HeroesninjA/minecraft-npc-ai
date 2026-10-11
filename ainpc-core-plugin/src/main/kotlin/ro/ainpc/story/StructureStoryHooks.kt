package ro.ainpc.story

import java.util.LinkedHashSet
import java.util.Locale

internal object StructureStoryHooks {
    private val configuredHookPattern = Regex("[a-z][a-z0-9_]*")

    fun parseConfigured(value: String): List<String>? {
        val hooks = value.split(',').map(String::trim)
        if (hooks.any { !configuredHookPattern.matches(it) }) return null
        return hooks.distinct()
    }

    fun forPlaceMetadata(metadata: Map<String, String>): List<String> =
        metadata["quest_hooks"]?.let(::parseConfigured) ?: emptyList()

    fun forPlaceType(placeTypeId: String): List<String> = when (placeTypeId) {
        "house", "home", "apartment", "farmhouse", "shared_house", "inn_room" -> listOf("home_life", "household_request", "family_help")
        "farm" -> listOf("harvest", "field_work", "delivery")
        "forge", "smithy" -> listOf("forge_order", "repair_tool", "metal_supply")
        "shop" -> listOf("market_supply", "customer_request", "shortage")
        "market" -> listOf("market_supply", "community_event", "trade")
        "bakery" -> listOf("bread_order", "grain_supply", "delivery")
        "mill" -> listOf("grain_supply", "machine_repair", "delivery")
        "workshop", "carpentry", "tailor_shop" -> listOf("craft_order", "repair_order", "material_supply")
        "tavern" -> listOf("social_event", "rumor", "missing_person")
        "town_hall", "administration", "court", "library", "church", "civic_hall", "castle_room" -> listOf("civic_request", "public_event", "lost_record")
        "park", "market_square", "plaza", "public_garden", "campfire_area", "performance_area" -> listOf("community_event", "social_meeting", "festival")
        "watchtower", "outpost" -> listOf("route_report", "observe_tracks", "patrol_request")
        "cave_room" -> listOf("exploration", "danger", "recover_artifact")
        "forest", "ruin", "cave", "fort", "shrine", "caravan_stop", "hidden_camp", "border_watch", "camp" -> listOf("exploration", "route_report", "danger")
        "quest_board", "ancient_altar", "sealed_door", "investigation_house", "delivery_point", "secret_cellar", "bandit_camp", "abandoned_tower", "crypt" -> listOf("quest_hook", "investigation", "recover_artifact")
        else -> emptyList()
    }

    fun forPlaceTags(tags: List<String>): List<String> {
        val normalizedTags = tags.asSequence().map(::normalize).filter(String::isNotBlank).toSet()
        val hooks = LinkedHashSet<String>()
        if (normalizedTags.any { it == "market" || it == "trade" || it == "merchant" }) hooks.add("market_supply")
        if (normalizedTags.any { it == "farming" || it == "farm" || it == "harvest" }) hooks.add("harvest")
        if (normalizedTags.any { it == "social" || it == "culture" || it == "public" }) hooks.add("community_event")
        if (normalizedTags.any { it == "danger" || it == "dungeon" || it == "ambush" }) hooks.add("danger")
        if (normalizedTags.any { it == "quest" || it == "story" }) hooks.add("quest_hook")
        if (normalizedTags.any { it == "admin" || it == "civil" || it == "civic" }) hooks.add("civic_request")
        if (normalizedTags.any { it == "route" || it == "watch" || it == "border" }) hooks.add("route_report")
        if (normalizedTags.any { it == "resource" || it == "wilderness" }) hooks.add("exploration")
        return hooks.toList()
    }

    private fun normalize(value: String): String = value.trim()
        .lowercase(Locale.ROOT)
        .replace('-', '_')
        .replace(' ', '_')
}
