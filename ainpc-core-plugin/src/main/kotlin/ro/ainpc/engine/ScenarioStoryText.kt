package ro.ainpc.engine

import java.util.Locale

fun isQuestStoryAction(entry: FeaturePackLoader.QuestEntryDefinition?): Boolean =
    normalizeStoryActionType(entry).isNotBlank()

fun normalizeStoryActionType(entry: FeaturePackLoader.QuestEntryDefinition?): String =
    when (normalizeReference(entry?.type)) {
        "set_story_state",
        "setstorystate",
        "story_state",
        "set_story_flag",
        "story_flag",
        "set_flag",
        -> "set_story_state"

        "record_story_event",
        "recordstoryevent",
        "story_event",
        "record_event",
        "event",
        -> "record_story_event"

        else -> ""
    }

fun getQuestEntryMetadata(entry: FeaturePackLoader.QuestEntryDefinition?, vararg keys: String?): String {
    if (entry == null || keys.isEmpty()) {
        return ""
    }

    val metadata = entry.metadata
    for (key in keys) {
        if (key.isNullOrBlank()) {
            continue
        }

        val directValue = metadata[key]
        if (!directValue.isNullOrBlank()) {
            return directValue
        }

        val normalizedKey = normalizeReference(key)
        for ((metadataKey, metadataValue) in metadata) {
            if (normalizeReference(metadataKey) == normalizedKey && !metadataValue.isNullOrBlank()) {
                return metadataValue
            }
        }
    }

    return ""
}

fun stripObjectivePrefix(reference: String?): String {
    if (reference.isNullOrBlank()) {
        return ""
    }

    val trimmed = reference.trim()
    val prefixSeparator = trimmed.indexOf(':')
    if (prefixSeparator <= 0) {
        return trimmed
    }

    return when (normalizeReference(trimmed.substring(0, prefixSeparator))) {
        "npc",
        "name",
        "profession",
        "region",
        "place",
        "node",
        "tag",
        "type",
        "mob",
        "entity",
        -> trimmed.substring(prefixSeparator + 1)

        else -> trimmed
    }
}

fun normalizeReference(value: String?): String =
    if (value.isNullOrBlank()) {
        ""
    } else {
        value
            .lowercase(Locale.ROOT)
            .replace("minecraft:", "")
            .replace(Regex("[^\\p{L}\\p{Nd}]+"), "_")
            .replace(Regex("^_+|_+$"), "")
            .replace(Regex("_+"), "_")
    }

fun normalizeStoryScope(scope: String?): String =
    when (normalizeReference(scope)) {
        "region", "regional" -> "region"
        "place", "local", "location" -> "place"
        else -> ""
    }

fun detectStoryTargetScope(targetValue: String?): String {
    if (targetValue.isNullOrBlank()) {
        return ""
    }

    val separator = targetValue.indexOf(':')
    if (separator <= 0) {
        return ""
    }

    val prefix = normalizeReference(targetValue.substring(0, separator))
    return if (prefix == "region" || prefix == "place") prefix else ""
}

fun cleanStoryId(value: String?): String {
    if (value.isNullOrBlank()) {
        return ""
    }

    val trimmed = value.trim()
    val targetScope = detectStoryTargetScope(trimmed)
    return if (targetScope.isNotBlank()) {
        trimmed.substring(trimmed.indexOf(':') + 1).trim()
    } else {
        trimmed
    }
}

fun parseStoryList(value: String?): List<String> {
    if (value.isNullOrBlank()) {
        return emptyList()
    }

    return value
        .split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

fun firstNonBlank(vararg values: String?): String {
    for (value in values) {
        if (!value.isNullOrBlank()) {
            return value
        }
    }
    return ""
}
