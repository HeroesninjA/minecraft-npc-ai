package ro.ainpc.gui;

import java.util.Locale;
import java.util.Optional;

public enum GuiKey {
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

    private final String id;
    private final String displayName;

    GuiKey(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<GuiKey> fromId(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.of(MAIN);
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        normalized = switch (normalized) {
            case "hub", "home", "principal" -> "main";
            case "quests", "questuri", "progression", "progressions", "progresii", "progresie", "log" -> "quest";
            case "questdetail", "quest_details", "quest_detalii", "detalii_quest",
                 "progression_detail", "progression_details", "progresie_detalii", "detalii_progresie" -> "quest_detail";
            case "poveste", "story_state", "story_context", "narativ" -> "story";
            case "map", "lume" -> "world";
            case "stat", "statistics", "statistici" -> "stats";
            case "npc", "interaction", "interactiune", "nearest" -> "interact";
            case "routines", "rutine", "program", "schedule" -> "routine";
            case "admin", "npc_manager", "manager_npc" -> "manager";
            case "debugdump", "dump" -> "debug";
            default -> normalized;
        };

        for (GuiKey key : values()) {
            if (key.id.equals(normalized) || key.name().equalsIgnoreCase(normalized)) {
                return Optional.of(key);
            }
        }
        return Optional.empty();
    }
}
