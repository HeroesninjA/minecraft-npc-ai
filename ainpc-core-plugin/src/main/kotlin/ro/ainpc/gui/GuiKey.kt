package ro.ainpc.gui

import java.util.Locale
import java.util.Optional

enum class GuiKey(
    private val idValue: String,
    private val displayNameValue: String
) {
    MAIN("main", "Hub AINPC"),
    QUEST("quest", "Progresii"),
    QUEST_DETAIL("quest_detail", "Detalii progresie"),
    STORY("story", "Story"),
    WORLD("world", "World"),
    STATS("stats", "Statistici"),
    INTERACT("interact", "Interactiune NPC"),
    ROUTINE("routine", "Rutine NPC"),
    SHOP("shop", "Shop NPC"),
    MANAGER("manager", "Manager NPC"),
    AUDIT("audit", "Audit"),
    DEBUG("debug", "Debug"),
    CONFIRM("confirm", "Confirmare");

    fun id(): String = idValue

    fun displayName(): String = displayNameValue

    companion object {
        @JvmStatic
        fun fromId(rawValue: String?): Optional<GuiKey> {
            if (rawValue.isNullOrBlank()) {
                return Optional.of(MAIN)
            }

            val trimmed = rawValue.trim()
                .lowercase(Locale.ROOT)
                .replace('-', '_')
            val normalized = when (trimmed) {
                "hub", "home", "principal" -> "main"
                "quests", "questuri", "progression", "progressions", "progresii", "progresie", "log" -> "quest"
                "questdetail", "quest_details", "quest_detalii", "detalii_quest",
                "progression_detail", "progression_details", "progresie_detalii", "detalii_progresie" -> "quest_detail"
                "poveste", "story_state", "story_context", "narativ" -> "story"
                "map", "lume" -> "world"
                "stat", "statistics", "statistici" -> "stats"
                "npc", "interaction", "interactiune", "nearest" -> "interact"
                "routines", "rutine", "program", "schedule" -> "routine"
                "admin", "npc_manager", "manager_npc" -> "manager"
                "debugdump", "dump" -> "debug"
                else -> trimmed
            }

            for (key in values()) {
                if (key.idValue == normalized || key.name.equals(normalized, ignoreCase = true)) {
                    return Optional.of(key)
                }
            }
            return Optional.empty()
        }
    }
}
