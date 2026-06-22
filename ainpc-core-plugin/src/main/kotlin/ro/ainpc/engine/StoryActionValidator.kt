package ro.ainpc.engine

data class StoryActionValidationResult(
    val valid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
) {
    companion object {
        fun valid(): StoryActionValidationResult = StoryActionValidationResult(true, emptyList(), emptyList())

        fun withErrors(errors: List<String>): StoryActionValidationResult =
            StoryActionValidationResult(false, errors, emptyList())

        fun withWarnings(warnings: List<String>): StoryActionValidationResult =
            StoryActionValidationResult(true, emptyList(), warnings)
    }
}

object StoryActionValidator {
    private val VALID_SCOPES = setOf("region", "place")
    private val VALID_TYPES = setOf("set_story_state", "record_story_event")
    private val TARGET_KEYS = setOf(
        "target", "scope_id", "target_id", "id",
        "place_id", "region_id", "target_place", "target_region", "place", "region"
    )
    private val STATE_KEYS = setOf("state_key", "state", "flag", "value", "item")
    private val EVENT_TYPE_KEYS = setOf("event_type", "type_id")
    private val EVENT_KEY_KEYS = setOf("event_key", "key")
    private val SEMANTIC_PAYLOAD_KEYS = setOf(
        "quest", "outcome", "result", "reason", "state", "mechanic", "tag", "quest_template", "quest_code"
    )

    fun validate(entry: FeaturePackLoader.QuestEntryDefinition): StoryActionValidationResult {
        val normalizedType = normalizeStoryActionType(entry)
        if (normalizedType.isBlank()) {
            return StoryActionValidationResult.withErrors(
                listOf("Tip story action necunoscut: '${entry.type}'. Tipuri acceptate: set_story_state, record_story_event.")
            )
        }
        return validateAction(normalizedType, entry)
    }

    fun validateType(rawType: String?): StoryActionValidationResult {
        val normalized = when {
            rawType.isNullOrBlank() -> ""
            else -> normalizeReference(rawType)
        }
        val canonical = when (normalized) {
            "set_story_state", "set-flag", "story_state", "set_flag", "setstate" -> "set_story_state"
            "record_story_event", "record_event", "record event", "event", "recordstoryevent" -> "record_story_event"
            else -> ""
        }
        if (canonical.isBlank()) {
            return StoryActionValidationResult.withErrors(
                listOf("Tip story action necunoscut: '$rawType'. Tipuri acceptate: set_story_state, record_story_event.")
            )
        }
        if (canonical !in VALID_TYPES) {
            return StoryActionValidationResult.withErrors(
                listOf("Tip story action invalid: '$canonical'. Tipuri acceptate: set_story_state, record_story_event.")
            )
        }
        return StoryActionValidationResult.valid()
    }

    private fun validateAction(type: String, entry: FeaturePackLoader.QuestEntryDefinition): StoryActionValidationResult {
        val metadata = entry.metadata
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val label = "Recompensa '${entry.type}'"

        val rawScope = metadata.getOrDefault("scope", "")
        val normalizedScope = normalizeReference(rawScope).replace('-', '_')
        val scope = when (normalizedScope) {
            "region", "world_region", "village", "settlement" -> "region"
            "place", "world_place", "location" -> "place"
            else -> normalizedScope
        }

        if (scope.isBlank()) {
            errors.add("$label nu are metadata.scope pentru story action.")
        } else if (scope !in VALID_SCOPES) {
            errors.add("$label are metadata.scope invalid: '${metadata.getOrDefault("scope", "")}'. Valori acceptate: region, place.")
        }

        val hasTarget = TARGET_KEYS.any { key ->
            metadata.containsKey(key) && metadata.getOrDefault(key, "").isNotBlank()
        }
        if (!hasTarget) {
            errors.add("$label nu are metadata.target pentru story action.")
        }

        if (type == "set_story_state") {
            val hasStateKey = STATE_KEYS.any { key ->
                metadata.containsKey(key) && metadata.getOrDefault(key, "").isNotBlank()
            }
            if (!hasStateKey) {
                errors.add("$label nu are metadata.state pentru set_story_state.")
            }
            if (entry.variables.isEmpty()) {
                warnings.add("$label nu are variables pentru set_story_state; va scrie doar state_key.")
            }
        }

        if (type == "record_story_event") {
            val hasEventType = EVENT_TYPE_KEYS.any { key ->
                metadata.containsKey(key) && metadata.getOrDefault(key, "").isNotBlank()
            }
            if (!hasEventType) {
                errors.add("$label nu are metadata.event_type pentru record_story_event.")
            }
            val hasEventKey = EVENT_KEY_KEYS.any { key ->
                metadata.containsKey(key) && metadata.getOrDefault(key, "").isNotBlank()
            }
            if (!hasEventKey) {
                errors.add("$label nu are metadata.event_key pentru record_story_event.")
            }
            if (entry.payload.isEmpty()) {
                errors.add("$label nu are payload minim pentru record_story_event.")
            } else {
                val hasSemanticKey = SEMANTIC_PAYLOAD_KEYS.any { entry.payload.containsKey(it) }
                if (!hasSemanticKey) {
                    warnings.add("$label are payload record_story_event, dar fara cheie semantica uzuala (quest/outcome/result/reason/state/mechanic/tag).")
                }
            }
        }

        return StoryActionValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
}
