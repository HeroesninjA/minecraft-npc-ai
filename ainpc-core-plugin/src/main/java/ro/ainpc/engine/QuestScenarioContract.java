package ro.ainpc.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Contractul mecanicii comune de quest, independent de tema scenariului.
 */
public record QuestScenarioContract(
    Kind kind,
    AcceptanceMode acceptanceMode,
    CompletionMode completionMode,
    TrackingMode trackingMode,
    List<String> tags
) {
    public QuestScenarioContract {
        kind = kind != null ? kind : Kind.CUSTOM;
        acceptanceMode = acceptanceMode != null ? acceptanceMode : AcceptanceMode.EXPLICIT;
        completionMode = completionMode != null ? completionMode : CompletionMode.RETURN_TO_GIVER;
        trackingMode = trackingMode != null ? trackingMode : TrackingMode.NEXT_OBJECTIVE;
        tags = tags != null ? List.copyOf(tags) : List.of();
    }

    public static QuestScenarioContract defaultContract() {
        return new QuestScenarioContract(
            Kind.CUSTOM,
            AcceptanceMode.EXPLICIT,
            CompletionMode.RETURN_TO_GIVER,
            TrackingMode.NEXT_OBJECTIVE,
            List.of()
        );
    }

    public static QuestScenarioContract fromScenarioDefinition(FeaturePackLoader.ScenarioDefinition definition) {
        if (definition == null) {
            return defaultContract();
        }

        return fromQuestEntries(
            definition.getQuestScenarioKind(),
            definition.getQuestAcceptanceMode(),
            definition.getQuestCompletionMode(),
            definition.getQuestTrackingMode(),
            definition.getQuestTags(),
            definition.getObjectives()
        );
    }

    public static QuestScenarioContract fromQuestEntries(String kind,
                                                         String acceptanceMode,
                                                         String completionMode,
                                                         String trackingMode,
                                                         List<String> tags,
                                                         List<FeaturePackLoader.QuestEntryDefinition> objectives) {
        Kind resolvedKind = Kind.fromId(kind);
        if (resolvedKind == Kind.CUSTOM) {
            resolvedKind = inferKind(objectives);
        }

        return new QuestScenarioContract(
            resolvedKind,
            AcceptanceMode.fromId(acceptanceMode),
            CompletionMode.fromId(completionMode),
            TrackingMode.fromId(trackingMode),
            normalizeTags(tags)
        );
    }

    public String displayName() {
        return switch (kind) {
            case FETCH -> "adunare";
            case HUNT -> "vanatoare";
            case DELIVERY -> "livrare";
            case EXPLORATION -> "explorare";
            case SOCIAL -> "social";
            case CUSTOM -> "personalizat";
        };
    }

    public boolean autoAcceptOnOffer() {
        return acceptanceMode == AcceptanceMode.AUTO_ACCEPT;
    }

    private static Kind inferKind(List<FeaturePackLoader.QuestEntryDefinition> objectives) {
        if (objectives == null || objectives.isEmpty()) {
            return Kind.CUSTOM;
        }

        boolean hasHunt = false;
        boolean hasDelivery = false;
        boolean hasExploration = false;
        boolean hasSocial = false;
        boolean hasFetch = false;

        for (FeaturePackLoader.QuestEntryDefinition objective : objectives) {
            String type = normalize(objective != null ? objective.getType() : "");
            switch (type) {
                case "killmob", "kill_mob", "hunt" -> hasHunt = true;
                case "deliveritem", "deliver_item", "delivery" -> hasDelivery = true;
                case "visitregion", "visit_region", "visitplace", "visit_place", "inspectnode", "inspect_node", "explore" -> hasExploration = true;
                case "talktonpc", "talk_to_npc", "dialogue", "social" -> hasSocial = true;
                case "item", "collect", "collectitem", "collect_item", "fetch", "gather" -> hasFetch = true;
                default -> {
                    // Tipurile necunoscute raman custom daca nu exista semnale mai precise.
                }
            }
        }

        if (hasHunt) {
            return Kind.HUNT;
        }
        if (hasDelivery) {
            return Kind.DELIVERY;
        }
        if (hasExploration) {
            return Kind.EXPLORATION;
        }
        if (hasSocial) {
            return Kind.SOCIAL;
        }
        return hasFetch ? Kind.FETCH : Kind.CUSTOM;
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }

        List<String> normalizedTags = new ArrayList<>();
        for (String tag : tags) {
            String normalized = normalize(tag);
            if (!normalized.isBlank()) {
                normalizedTags.add(normalized);
            }
        }
        return normalizedTags;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(" ", "_");
    }

    public enum Kind {
        FETCH,
        HUNT,
        DELIVERY,
        EXPLORATION,
        SOCIAL,
        CUSTOM;

        public static Kind fromId(String value) {
            return switch (normalize(value)) {
                case "fetch", "collect", "collect_item", "gather" -> FETCH;
                case "hunt", "kill", "kill_mob", "combat" -> HUNT;
                case "delivery", "deliver", "deliver_item" -> DELIVERY;
                case "exploration", "explore", "visit", "inspect" -> EXPLORATION;
                case "social", "dialogue", "talk" -> SOCIAL;
                default -> CUSTOM;
            };
        }
    }

    public enum AcceptanceMode {
        EXPLICIT,
        AUTO_ACCEPT;

        public static AcceptanceMode fromId(String value) {
            return switch (normalize(value)) {
                case "auto", "auto_accept", "direct", "direct_accept" -> AUTO_ACCEPT;
                default -> EXPLICIT;
            };
        }
    }

    public enum CompletionMode {
        RETURN_TO_GIVER,
        AUTO,
        MANUAL;

        public static CompletionMode fromId(String value) {
            return switch (normalize(value)) {
                case "auto", "automatic" -> AUTO;
                case "manual", "admin" -> MANUAL;
                default -> RETURN_TO_GIVER;
            };
        }
    }

    public enum TrackingMode {
        NEXT_OBJECTIVE,
        QUEST_GIVER,
        DISABLED;

        public static TrackingMode fromId(String value) {
            return switch (normalize(value)) {
                case "giver", "quest_giver", "return_to_giver" -> QUEST_GIVER;
                case "none", "disabled", "off" -> DISABLED;
                default -> NEXT_OBJECTIVE;
            };
        }
    }
}
