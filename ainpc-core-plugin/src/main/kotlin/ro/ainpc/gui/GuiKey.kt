package ro.ainpc.gui

import java.util.Locale
import java.util.Optional

enum class GuiKey(
    private val idValue: String,
    private val displayNameValue: String
) {
    MAIN("main", "Hub AINPC"),
    PLAYER_HUB("player_hub", "Jucator"),
    QUEST("quest", "Progresii"),
    QUEST_DETAIL("quest_detail", "Detalii progresie"),
    STORY("story", "Story"),
    AUTHORING("authoring", "Authoring"),
    WORLD("world", "World"),
    PLACE("place", "Place"),
    REGION("region", "Regiune"),
    STATS("stats", "Statistici"),
    INTERACT("interact", "Interactiune NPC"),
    ROUTINE("routine", "Rutine NPC"),
    SHOP("shop", "Shop NPC"),
    MANAGER("manager", "Manager NPC"),
    AUDIT("audit", "Audit"),
    DEBUG("debug", "Debug"),
    ADMIN_MAPPING("admin_mapping", "Admin Mapping"),
    ADMIN_QUEST("admin_quest", "Admin Quest"),
    ADMIN_HUB("admin_hub", "Admin Hub"),
    CREATOR_HUB("creator_hub", "Creator Hub"),
    QUEST_MAP("quest_map", "Quest Map"),
    CREATOR_QUEST("creator_quest", "Creator Quest"),
    CREATOR_QUEST_DEFS("creator_quest_defs", "Definitii"),
    CREATOR_QUEST_TEST("creator_quest_test", "Test Quest"),
    QUEST_EDIT("quest_edit", "Editeaza Quest"),
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
                "authoring", "quest_authoring", "progress_authoring", "authoring_view" -> "authoring"
                "map", "lume" -> "world"
                "place_detail", "world_place", "loc", "place_details" -> "place"
                "region_detail", "world_region", "regiune" -> "region"
                "stat", "statistics", "statistici" -> "stats"
                "npc", "interaction", "interactiune", "nearest" -> "interact"
                "routines", "rutine", "program", "schedule" -> "routine"
                "admin", "npc_manager", "manager_npc" -> "manager"
                "debugdump", "dump" -> "debug"
                "adminmapping", "admin_mapping", "mapping_admin", "edit_mapping" -> "admin_mapping"
                "adminquest", "admin_quest", "quest_admin", "edit_quest" -> "admin_quest"
                "playerhub", "player_hub", "jucator", "player" -> "player_hub"
                "adminhub", "admin_hub", "control_panel", "panel" -> "admin_hub"
                "creatorhub", "creator_hub", "creator", "designer" -> "creator_hub"
                "questmap", "quest_map", "map_quest", "quest_mapping" -> "quest_map"
                "creatorquest", "creator_quest", "quest_creator" -> "creator_quest"
                "creatorquestdefs", "creator_quest_defs", "defs", "definitions" -> "creator_quest_defs"
                "creatorquesttest", "creator_quest_test", "quest_test", "test_quest" -> "creator_quest_test"
                "questedit", "quest_edit", "edit_quest", "editor" -> "quest_edit"
                "questmap", "quest_map", "mapping", "create_quest", "edit_quest_map" -> "quest_map"
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
