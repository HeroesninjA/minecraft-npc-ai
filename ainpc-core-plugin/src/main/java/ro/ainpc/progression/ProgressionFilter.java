package ro.ainpc.progression;

import java.util.Locale;

final class ProgressionFilter {

    private ProgressionFilter() {
    }

    static boolean isAllFilter(String filter) {
        String normalized = normalize(filter);
        return normalized.isBlank()
            || "all".equals(normalized)
            || "toate".equals(normalized);
    }

    static boolean matchesDefinition(ProgressionDefinition definition, String filter) {
        if (definition == null) {
            return false;
        }
        if (isAllFilter(filter)) {
            return true;
        }

        FieldFilter fieldFilter = FieldFilter.parse(filter);
        if (fieldFilter != null) {
            return matchesDefinitionField(definition, fieldFilter);
        }

        return matchesValue(definition.progressionId(), filter)
            || matchesValue(definition.packId(), filter)
            || matchesValue(definition.mechanicId(), filter)
            || matchesValue(definition.kind(), filter)
            || matchesValue(definition.category(), filter)
            || matchesValue(definition.scenarioKind(), filter)
            || matchesValue(definition.baseType(), filter)
            || matchesValue(definition.definitionId(), filter)
            || matchesValue(definition.templateId(), filter)
            || matchesValue(definition.code(), filter)
            || matchesValue(definition.displayName(), filter)
            || matchesValue(definition.description(), filter)
            || matchesValue(definition.label(), filter)
            || matchesValue(definition.singularLabel(), filter)
            || matchesValue(definition.pluralLabel(), filter);
    }

    static boolean matchesStored(StoredProgression progression, String filter) {
        if (progression == null) {
            return false;
        }
        if (isAllFilter(filter)) {
            return true;
        }

        String normalized = normalize(filter);
        switch (normalized) {
            case "current", "curent" -> {
                return progression.current();
            }
            case "active", "activ" -> {
                return "active".equalsIgnoreCase(progression.status());
            }
            case "offered", "oferta", "oferit" -> {
                return "offered".equalsIgnoreCase(progression.status());
            }
            case "completed", "complete", "completat" -> {
                return "completed".equalsIgnoreCase(progression.status());
            }
            case "failed", "abandoned", "abandonat" -> {
                return "failed".equalsIgnoreCase(progression.status());
            }
            case "archived", "arhivat" -> {
                return progression.archived();
            }
            case "tracked", "urmarit" -> {
                return progression.tracked();
            }
            case "resolved", "definition_resolved" -> {
                return progression.definitionResolved();
            }
            case "unresolved", "missing_definition" -> {
                return !progression.definitionResolved();
            }
            default -> {
            }
        }

        FieldFilter fieldFilter = FieldFilter.parse(filter);
        if (fieldFilter != null) {
            return matchesStoredField(progression, fieldFilter);
        }

        return matchesValue(progression.playerUuid(), filter)
            || matchesValue(progression.progressionId(), filter)
            || matchesValue(progression.packId(), filter)
            || matchesValue(progression.mechanicId(), filter)
            || matchesValue(progression.kind(), filter)
            || matchesValue(progression.category(), filter)
            || matchesValue(progression.scenarioKind(), filter)
            || matchesValue(progression.baseType(), filter)
            || matchesValue(progression.definitionId(), filter)
            || matchesValue(progression.templateId(), filter)
            || matchesValue(progression.code(), filter)
            || matchesValue(progression.status(), filter)
            || matchesValue(progression.currentPhase(), filter)
            || matchesValue(progression.currentStageId(), filter)
            || matchesValue(progression.mechanicLabel(), filter)
            || matchesValue(progression.singularLabel(), filter)
            || matchesValue(progression.pluralLabel(), filter)
            || matchesValue(progression.compatibilitySource(), filter);
    }

    private static boolean matchesDefinitionField(ProgressionDefinition definition, FieldFilter filter) {
        return switch (filter.field()) {
            case "progression", "progression_id" -> matchesValue(definition.progressionId(), filter.value());
            case "pack", "pack_id", "addon" -> matchesValue(definition.packId(), filter.value());
            case "mechanic", "mechanic_id", "mecanica" -> matchesValue(definition.mechanicId(), filter.value());
            case "kind", "progression_kind", "progress_kind", "type", "tip" -> matchesValue(definition.kind(), filter.value());
            case "category", "categorie", "quest_category" -> matchesValue(definition.category(), filter.value());
            case "scenario", "scenario_kind", "quest_kind" -> matchesValue(definition.scenarioKind(), filter.value());
            case "base", "base_type", "scenario_type" -> matchesValue(definition.baseType(), filter.value());
            case "definition", "definition_id", "id" -> matchesValue(definition.definitionId(), filter.value());
            case "template", "template_id" -> matchesValue(definition.templateId(), filter.value());
            case "code", "quest_code" -> matchesValue(definition.code(), filter.value());
            case "name", "display", "display_name", "title" -> matchesValue(definition.displayName(), filter.value());
            case "description", "desc" -> matchesValue(definition.description(), filter.value());
            case "label", "mechanic_label" -> matchesValue(definition.label(), filter.value());
            case "singular", "singular_label" -> matchesValue(definition.singularLabel(), filter.value());
            case "plural", "plural_label" -> matchesValue(definition.pluralLabel(), filter.value());
            case "enabled" -> matchesBoolean(definition.enabled(), filter.value());
            case "repeatable" -> matchesBoolean(definition.repeatable(), filter.value());
            default -> false;
        };
    }

    private static boolean matchesStoredField(StoredProgression progression, FieldFilter filter) {
        return switch (filter.field()) {
            case "player", "player_uuid" -> matchesValue(progression.playerUuid(), filter.value());
            case "progression", "progression_id" -> matchesValue(progression.progressionId(), filter.value());
            case "pack", "pack_id", "addon" -> matchesValue(progression.packId(), filter.value());
            case "mechanic", "mechanic_id", "mecanica" -> matchesValue(progression.mechanicId(), filter.value());
            case "kind", "progression_kind", "progress_kind", "type", "tip" -> matchesValue(progression.kind(), filter.value());
            case "category", "categorie", "quest_category" -> matchesValue(progression.category(), filter.value());
            case "scenario", "scenario_kind", "quest_kind" -> matchesValue(progression.scenarioKind(), filter.value());
            case "base", "base_type", "scenario_type" -> matchesValue(progression.baseType(), filter.value());
            case "definition", "definition_id", "id" -> matchesValue(progression.definitionId(), filter.value());
            case "template", "template_id" -> matchesValue(progression.templateId(), filter.value());
            case "code", "quest_code" -> matchesValue(progression.code(), filter.value());
            case "status" -> matchesValue(progression.status(), filter.value());
            case "phase", "current_phase" -> matchesValue(progression.currentPhase(), filter.value());
            case "stage", "current_stage" -> matchesValue(progression.currentStageId(), filter.value());
            case "label", "mechanic_label" -> matchesValue(progression.mechanicLabel(), filter.value());
            case "singular", "singular_label" -> matchesValue(progression.singularLabel(), filter.value());
            case "plural", "plural_label" -> matchesValue(progression.pluralLabel(), filter.value());
            case "source", "compatibility_source" -> matchesValue(progression.compatibilitySource(), filter.value());
            case "tracked" -> matchesBoolean(progression.tracked(), filter.value());
            case "resolved", "definition_resolved" -> matchesBoolean(progression.definitionResolved(), filter.value());
            default -> false;
        };
    }

    private static boolean matchesValue(String value, String filter) {
        String safeValue = value == null ? "" : value.trim();
        String safeFilter = filter == null ? "" : filter.trim();
        if (safeValue.isBlank() || safeFilter.isBlank()) {
            return false;
        }

        String lowerValue = safeValue.toLowerCase(Locale.ROOT);
        String lowerFilter = safeFilter.toLowerCase(Locale.ROOT);
        return lowerValue.contains(lowerFilter)
            || normalizeComparable(safeValue).contains(normalizeComparable(safeFilter));
    }

    private static boolean matchesBoolean(boolean value, String filter) {
        return switch (normalize(filter)) {
            case "true", "yes", "da", "1" -> value;
            case "false", "no", "nu", "0" -> !value;
            default -> false;
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeComparable(String value) {
        return normalize(value)
            .replace('-', '_')
            .replace(' ', '_');
    }

    private record FieldFilter(String field, String value) {

        private static FieldFilter parse(String rawFilter) {
            if (rawFilter == null || rawFilter.isBlank()) {
                return null;
            }

            String trimmed = rawFilter.trim();
            int separator = separatorIndex(trimmed);
            if (separator <= 0 || separator >= trimmed.length() - 1) {
                return null;
            }

            String field = normalize(trimmed.substring(0, separator));
            if (!isKnownField(field)) {
                return null;
            }

            return new FieldFilter(field, trimmed.substring(separator + 1).trim());
        }

        private static int separatorIndex(String value) {
            int colon = value.indexOf(':');
            int equals = value.indexOf('=');
            if (colon < 0) {
                return equals;
            }
            if (equals < 0) {
                return colon;
            }
            return Math.min(colon, equals);
        }

        private static boolean isKnownField(String field) {
            return switch (field) {
                case "player", "player_uuid",
                     "progression", "progression_id",
                     "pack", "pack_id", "addon",
                     "mechanic", "mechanic_id", "mecanica",
                     "kind", "progression_kind", "progress_kind", "type", "tip",
                     "category", "categorie", "quest_category",
                     "scenario", "scenario_kind", "quest_kind",
                     "base", "base_type", "scenario_type",
                     "definition", "definition_id", "id",
                     "template", "template_id",
                     "code", "quest_code",
                     "name", "display", "display_name", "title",
                     "description", "desc",
                     "status",
                     "phase", "current_phase",
                     "stage", "current_stage",
                     "label", "mechanic_label",
                     "singular", "singular_label",
                     "plural", "plural_label",
                     "source", "compatibility_source",
                     "enabled", "repeatable", "tracked", "resolved", "definition_resolved" -> true;
                default -> false;
            };
        }
    }
}
