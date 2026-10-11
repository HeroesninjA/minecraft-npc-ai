package ro.ainpc.story

import java.util.Locale

internal object StructureStoryCategory {
    private val configurableCategories = setOf("residential", "profession", "utility", "leisure", "public", "exterior", "quest")

    fun isValidOverride(placeTypeId: String, value: String): Boolean =
        placeTypeId == "custom" && normalize(value) in configurableCategories

    fun forPlaceType(placeTypeId: String, tags: List<String>, metadata: Map<String, String> = emptyMap()): String = when (placeTypeId) {
        "custom" -> metadata["story_category"]?.takeIf { isValidOverride(placeTypeId, it) }?.let(::normalize)
            ?: if (tags.any { normalize(it) == "leisure" }) "leisure" else "unknown"
        "house", "home", "apartment", "farmhouse", "shared_house", "inn_room" -> "residential"
        "farm", "forge", "smithy", "shop", "market", "bakery", "mill", "workshop", "carpentry", "tailor_shop", "tavern", "town_hall", "temple" -> "profession"
        "well", "storage", "barn", "gate", "bridge", "path", "road", "stable", "collection_point", "lighting" -> "utility"
        "park", "market_square", "plaza", "bench_area", "public_garden", "campfire_area", "performance_area" -> "leisure"
        "administration", "court", "church", "library", "watchtower", "training_ground", "civic_hall", "castle_room" -> "public"
        "forest", "ruin", "cave", "cave_room", "outpost", "fort", "shrine", "caravan_stop", "hidden_camp", "border_watch", "camp" -> "exterior"
        "quest_board", "ancient_altar", "sealed_door", "investigation_house", "delivery_point", "secret_cellar", "bandit_camp", "abandoned_tower", "crypt" -> "quest"
        else -> "unknown"
    }

    private fun normalize(value: String): String = value.trim()
        .lowercase(Locale.ROOT)
        .replace('-', '_')
        .replace(' ', '_')
}
