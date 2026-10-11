package ro.ainpc.story

import java.util.Locale

internal object StructureQuestCooldown {
    fun categoryForEvent(eventType: String): String {
        val normalized = eventType.trim().lowercase(Locale.ROOT).replace('-', '_').replace(' ', '_')
        return when (normalized) {
            "structure_node_work" -> "profession"
            "structure_node_social" -> "leisure"
            "structure_node_home" -> "residential"
            else -> when {
                normalized.startsWith("structure_visit_") -> normalized.removePrefix("structure_visit_")
                normalized.startsWith("structure_node_") -> normalized.removePrefix("structure_node_")
                else -> "unknown"
            }
        }
    }

    fun durationForCategory(category: String): Long = when (category) {
        "residential" -> 10_000L
        "profession" -> 22_000L
        "utility" -> 12_000L
        "leisure" -> 6_000L
        "public" -> 14_000L
        "exterior" -> 30_000L
        "quest" -> 45_000L
        else -> 15_000L
    }

    fun durationForEvent(eventType: String): Long = durationForCategory(categoryForEvent(eventType))
}
