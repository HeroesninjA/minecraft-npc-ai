package ro.ainpc.progression

import java.util.Locale

object ProgressionFilter {
    @JvmStatic
    fun isAllFilter(filter: String?): Boolean {
        val normalized = normalize(filter)
        return normalized.isBlank() || normalized == "all" || normalized == "toate"
    }

    @JvmStatic
    fun matchesDefinition(definition: ProgressionDefinition?, filter: String?): Boolean {
        if (definition == null) {
            return false
        }
        if (isAllFilter(filter)) {
            return true
        }

        val fieldFilter = FieldFilter.parse(filter)
        if (fieldFilter != null) {
            return matchesDefinitionField(definition, fieldFilter)
        }

        return matchesValue(definition.progressionId(), filter) ||
            matchesValue(definition.packId(), filter) ||
            matchesValue(definition.mechanicId(), filter) ||
            matchesValue(definition.kind(), filter) ||
            matchesValue(definition.category(), filter) ||
            matchesValue(definition.scenarioKind(), filter) ||
            matchesValue(definition.baseType(), filter) ||
            matchesValue(definition.definitionId(), filter) ||
            matchesValue(definition.templateId(), filter) ||
            matchesValue(definition.code(), filter) ||
            matchesValue(definition.displayName(), filter) ||
            matchesValue(definition.description(), filter) ||
            matchesValue(definition.label(), filter) ||
            matchesValue(definition.singularLabel(), filter) ||
            matchesValue(definition.pluralLabel(), filter)
    }

    @JvmStatic
    fun matchesStored(progression: StoredProgression?, filter: String?): Boolean {
        if (progression == null) {
            return false
        }
        if (isAllFilter(filter)) {
            return true
        }

        when (normalize(filter)) {
            "current", "curent" -> return progression.current()
            "active", "activ" -> return "active".equals(progression.status(), ignoreCase = true)
            "offered", "oferta", "oferit" -> return "offered".equals(progression.status(), ignoreCase = true)
            "completed", "complete", "completat" -> return "completed".equals(progression.status(), ignoreCase = true)
            "failed", "abandoned", "abandonat" -> return "failed".equals(progression.status(), ignoreCase = true)
            "archived", "arhivat" -> return progression.archived()
            "tracked", "urmarit" -> return progression.tracked()
            "resolved", "definition_resolved" -> return progression.definitionResolved()
            "unresolved", "missing_definition" -> return !progression.definitionResolved()
        }

        val fieldFilter = FieldFilter.parse(filter)
        if (fieldFilter != null) {
            return matchesStoredField(progression, fieldFilter)
        }

        return matchesValue(progression.playerUuid(), filter) ||
            matchesValue(progression.progressionId(), filter) ||
            matchesValue(progression.packId(), filter) ||
            matchesValue(progression.mechanicId(), filter) ||
            matchesValue(progression.kind(), filter) ||
            matchesValue(progression.category(), filter) ||
            matchesValue(progression.scenarioKind(), filter) ||
            matchesValue(progression.baseType(), filter) ||
            matchesValue(progression.definitionId(), filter) ||
            matchesValue(progression.templateId(), filter) ||
            matchesValue(progression.code(), filter) ||
            matchesValue(progression.status(), filter) ||
            matchesValue(progression.currentPhase(), filter) ||
            matchesValue(progression.currentStageId(), filter) ||
            matchesValue(progression.mechanicLabel(), filter) ||
            matchesValue(progression.singularLabel(), filter) ||
            matchesValue(progression.pluralLabel(), filter) ||
            matchesValue(progression.compatibilitySource(), filter)
    }

    private fun matchesDefinitionField(definition: ProgressionDefinition, filter: FieldFilter): Boolean {
        return when (filter.field) {
            "progression", "progression_id" -> matchesValue(definition.progressionId(), filter.value)
            "pack", "pack_id", "addon" -> matchesValue(definition.packId(), filter.value)
            "mechanic", "mechanic_id", "mecanica" -> matchesValue(definition.mechanicId(), filter.value)
            "kind", "progression_kind", "progress_kind", "type", "tip" -> matchesValue(definition.kind(), filter.value)
            "category", "categorie", "quest_category" -> matchesValue(definition.category(), filter.value)
            "scenario", "scenario_kind", "quest_kind" -> matchesValue(definition.scenarioKind(), filter.value)
            "base", "base_type", "scenario_type" -> matchesValue(definition.baseType(), filter.value)
            "definition", "definition_id", "id" -> matchesValue(definition.definitionId(), filter.value)
            "template", "template_id" -> matchesValue(definition.templateId(), filter.value)
            "code", "quest_code" -> matchesValue(definition.code(), filter.value)
            "name", "display", "display_name", "title" -> matchesValue(definition.displayName(), filter.value)
            "description", "desc" -> matchesValue(definition.description(), filter.value)
            "label", "mechanic_label" -> matchesValue(definition.label(), filter.value)
            "singular", "singular_label" -> matchesValue(definition.singularLabel(), filter.value)
            "plural", "plural_label" -> matchesValue(definition.pluralLabel(), filter.value)
            "enabled" -> matchesBoolean(definition.enabled(), filter.value)
            "repeatable" -> matchesBoolean(definition.repeatable(), filter.value)
            else -> false
        }
    }

    private fun matchesStoredField(progression: StoredProgression, filter: FieldFilter): Boolean {
        return when (filter.field) {
            "player", "player_uuid" -> matchesValue(progression.playerUuid(), filter.value)
            "progression", "progression_id" -> matchesValue(progression.progressionId(), filter.value)
            "pack", "pack_id", "addon" -> matchesValue(progression.packId(), filter.value)
            "mechanic", "mechanic_id", "mecanica" -> matchesValue(progression.mechanicId(), filter.value)
            "kind", "progression_kind", "progress_kind", "type", "tip" -> matchesValue(progression.kind(), filter.value)
            "category", "categorie", "quest_category" -> matchesValue(progression.category(), filter.value)
            "scenario", "scenario_kind", "quest_kind" -> matchesValue(progression.scenarioKind(), filter.value)
            "base", "base_type", "scenario_type" -> matchesValue(progression.baseType(), filter.value)
            "definition", "definition_id", "id" -> matchesValue(progression.definitionId(), filter.value)
            "template", "template_id" -> matchesValue(progression.templateId(), filter.value)
            "code", "quest_code" -> matchesValue(progression.code(), filter.value)
            "status" -> matchesValue(progression.status(), filter.value)
            "phase", "current_phase" -> matchesValue(progression.currentPhase(), filter.value)
            "stage", "current_stage" -> matchesValue(progression.currentStageId(), filter.value)
            "label", "mechanic_label" -> matchesValue(progression.mechanicLabel(), filter.value)
            "singular", "singular_label" -> matchesValue(progression.singularLabel(), filter.value)
            "plural", "plural_label" -> matchesValue(progression.pluralLabel(), filter.value)
            "source", "compatibility_source" -> matchesValue(progression.compatibilitySource(), filter.value)
            "tracked" -> matchesBoolean(progression.tracked(), filter.value)
            "resolved", "definition_resolved" -> matchesBoolean(progression.definitionResolved(), filter.value)
            else -> false
        }
    }

    private fun matchesValue(value: String?, filter: String?): Boolean {
        val safeValue = value?.trim().orEmpty()
        val safeFilter = filter?.trim().orEmpty()
        if (safeValue.isBlank() || safeFilter.isBlank()) {
            return false
        }

        val lowerValue = safeValue.lowercase(Locale.ROOT)
        val lowerFilter = safeFilter.lowercase(Locale.ROOT)
        return lowerValue.contains(lowerFilter) ||
            normalizeComparable(safeValue).contains(normalizeComparable(safeFilter))
    }

    private fun matchesBoolean(value: Boolean, filter: String?): Boolean {
        return when (normalize(filter)) {
            "true", "yes", "da", "1" -> value
            "false", "no", "nu", "0" -> !value
            else -> false
        }
    }

    private fun normalize(value: String?): String = value?.trim()?.lowercase(Locale.ROOT).orEmpty()

    private fun normalizeComparable(value: String?): String =
        normalize(value).replace('-', '_').replace(' ', '_')

    private data class FieldFilter(val field: String, val value: String) {
        companion object {
            fun parse(rawFilter: String?): FieldFilter? {
                if (rawFilter.isNullOrBlank()) {
                    return null
                }

                val trimmed = rawFilter.trim()
                val separator = separatorIndex(trimmed)
                if (separator <= 0 || separator >= trimmed.length - 1) {
                    return null
                }

                val field = normalize(trimmed.substring(0, separator))
                if (!isKnownField(field)) {
                    return null
                }
                return FieldFilter(field, trimmed.substring(separator + 1).trim())
            }

            private fun separatorIndex(value: String): Int {
                val colon = value.indexOf(':')
                val equals = value.indexOf('=')
                if (colon < 0) return equals
                if (equals < 0) return colon
                return minOf(colon, equals)
            }

            private fun isKnownField(field: String): Boolean {
                return when (field) {
                    "player", "player_uuid",
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
                    "enabled", "repeatable", "tracked", "resolved", "definition_resolved" -> true
                    else -> false
                }
            }
        }
    }
}
