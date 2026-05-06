package ro.ainpc.gui;

import java.util.Locale;
import java.util.Optional;

public enum GuiKey {
    MAIN("main", "Hub AINPC"),
    QUEST("quest", "Questuri"),
    QUEST_DETAIL("quest_detail", "Detalii quest"),
    WORLD("world", "World"),
    STATS("stats", "Statistici"),
    INTERACT("interact", "Interactiune NPC"),
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
            case "quests", "questuri", "log" -> "quest";
            case "questdetail", "quest_details", "quest_detalii", "detalii_quest" -> "quest_detail";
            case "map", "lume" -> "world";
            case "stat", "statistics", "statistici" -> "stats";
            case "npc", "interaction", "interactiune", "nearest" -> "interact";
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
