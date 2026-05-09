package ro.ainpc.gui;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum QuestLogGuiFilter {
    ALL("all", "Toate", "toate"),
    ACTIVE("active", "Active", "active"),
    QUEST("quest", "Questuri", "questuri"),
    CONTRACT("contract", "Contracte", "contracte"),
    DUTY("duty", "Sarcini", "sarcini"),
    BOUNTY("bounty", "Bounty", "bounty-uri"),
    EVENT("event", "Events", "evenimente"),
    TUTORIAL("tutorial", "Tutoriale", "tutoriale"),
    RITUAL("ritual", "Ritualuri", "ritualuri");

    private final String filter;
    private final String buttonLabel;
    private final String displayLabel;

    QuestLogGuiFilter(String filter, String buttonLabel, String displayLabel) {
        this.filter = filter;
        this.buttonLabel = buttonLabel;
        this.displayLabel = displayLabel;
    }

    public String filter() {
        return filter;
    }

    public String buttonLabel() {
        return buttonLabel;
    }

    public String displayLabel() {
        return displayLabel;
    }

    public boolean matches(String rawFilter) {
        return normalize(filter).equals(normalize(rawFilter));
    }

    public static List<QuestLogGuiFilter> primaryFilters() {
        return Arrays.asList(values());
    }

    public static String normalizeFilter(String rawFilter) {
        return fromId(rawFilter)
            .map(QuestLogGuiFilter::filter)
            .orElseGet(() -> rawFilter == null || rawFilter.isBlank() ? ALL.filter : rawFilter.trim());
    }

    public static Optional<QuestLogGuiFilter> fromId(String rawValue) {
        String normalized = normalize(rawValue);
        if (normalized.isBlank()) {
            return Optional.of(ALL);
        }

        String canonical = switch (normalized) {
            case "toate", "tot" -> "all";
            case "activ", "curente", "curent" -> "active";
            case "quests", "questuri" -> "quest";
            case "contracts", "contracte" -> "contract";
            case "duties", "sarcina", "sarcini" -> "duty";
            case "bounties", "recompensa", "recompense" -> "bounty";
            case "events", "eveniment", "evenimente" -> "event";
            case "tutorials", "onboarding", "indrumare" -> "tutorial";
            case "rituals", "ceremony", "ceremonies", "ceremonie", "ceremonii" -> "ritual";
            default -> normalized;
        };

        return Arrays.stream(values())
            .filter(filter -> filter.filter.equals(canonical) || normalize(filter.buttonLabel).equals(canonical))
            .findFirst();
    }

    private static String normalize(String value) {
        return value == null
            ? ""
            : value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replaceAll("[^\\p{L}\\p{Nd}]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
