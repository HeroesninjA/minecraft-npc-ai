package ro.ainpc.engine

import org.bukkit.Material
import ro.ainpc.npc.AINPC
import java.util.Locale

internal fun applyQuestFallbackPlaceholders(
    value: String?,
    npc: AINPC?,
    professionName: String?,
    objectiveText: String?,
    rewardText: String?,
): String {
    if (value.isNullOrBlank()) {
        return value ?: ""
    }

    return value
        .replace("{npc}", npc?.name ?: "NPC")
        .replace("{profession}", professionName?.takeIf { it.isNotBlank() } ?: "localnic")
        .replace("{objective}", objectiveText ?: "materiale")
        .replace("{reward}", rewardText ?: "o recompensa")
}

internal fun sanitizeConfigKey(value: String?): String =
    normalizeScenarioToken(value)
        .replace('-', '_')
        .replace(' ', '_')
        .replace(Regex("[^a-z0-9_]"), "")

fun capitalizeProgressionLabel(label: String?): String {
    if (label.isNullOrBlank()) {
        return "Progresie"
    }

    val trimmed = label.trim()
    return if (trimmed.length == 1) {
        trimmed.uppercase(Locale.ROOT)
    } else {
        trimmed.substring(0, 1).uppercase(Locale.ROOT) + trimmed.substring(1)
    }
}

fun resolveQuestTitle(template: ScenarioEngine.ScenarioTemplate?): String {
    if (template == null) {
        return ""
    }

    return if (template.questCode.isBlank()) {
        template.displayName
    } else {
        "${template.questCode} - ${template.displayName}"
    }
}

fun formatStageCompletionMode(completionMode: String?): String =
    when (normalizeStageCompletionMode(completionMode)) {
        "any_objective" -> "Orice obiectiv"
        "manual_turn_in" -> "Returnare manuala"
        else -> "Toate obiectivele"
    }

fun valueOrFallback(value: String?, fallback: String): String =
    if (value.isNullOrBlank()) fallback else value

fun formatDuration(durationMillis: Long): String {
    val totalSeconds = ((durationMillis + 999L) / 1000L).coerceAtLeast(1L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    if (hours > 0L) {
        return "${hours}h ${minutes}m"
    }
    if (minutes > 0L) {
        return "${minutes}m ${seconds}s"
    }
    return "${seconds}s"
}

fun formatQuestDebugTime(epochMillis: Long): String =
    if (epochMillis > 0L) epochMillis.toString() else "<gol>"

fun formatOptional(value: String?): String =
    if (value.isNullOrBlank()) "<gol>" else value

fun formatQuestDebugMap(values: Map<String, *>?, limit: Int): List<String> {
    if (values.isNullOrEmpty()) {
        return listOf("&7- &f<gol>")
    }

    val maxRows = limit.coerceAtLeast(1)
    val lines = ArrayList<String>()
    values.entries
        .sortedBy { it.key }
        .take(maxRows)
        .forEach { entry -> lines.add("&7- &f${entry.key} &7= &f${entry.value}") }
    if (values.size > maxRows) {
        lines.add("&7- &f... inca ${values.size - maxRows} valori")
    }
    return lines
}

fun formatQuestLogMechanicCounts(mechanicCounts: Map<String, Int>?): String {
    if (mechanicCounts.isNullOrEmpty()) {
        return "<gol>"
    }

    return mechanicCounts.entries
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.key })
        .joinToString("&7, &f") { "${it.key}=${it.value}" }
}

internal fun resolveQuestMaterial(entry: FeaturePackLoader.QuestEntryDefinition?): Material? {
    if (entry == null || entry.itemId.isBlank()) {
        return null
    }

    return Material.matchMaterial(entry.itemId)
}

fun formatObjectiveProgressLabel(objective: FeaturePackLoader.QuestEntryDefinition?): String {
    if (objective == null) {
        return "obiectiv"
    }

    val objectiveType = normalizeObjectiveType(objective.type)
    val material = resolveQuestMaterial(objective)
    return when (objectiveType) {
        "collect_item",
        "deliver_to_npc",
        -> material?.name?.let(::humanizeItemId) ?: humanizeItemId(objective.itemId)

        "talk_to_npc" -> "vorbeste cu " + formatObjectiveTargetLabel(objective, "npc-ul")
        "visit_region" -> "viziteaza " + formatObjectiveTargetLabel(objective, "regiunea")
        "visit_place" -> "viziteaza " + formatObjectiveTargetLabel(objective, "locul")
        "inspect_node" -> "inspecteaza " + formatObjectiveTargetLabel(objective, "punctul")
        "kill_mob" -> "ucide " + formatObjectiveTargetLabel(objective, "inamicul")
        else -> objective.description.takeIf { it.isNotBlank() } ?: humanizeItemId(objective.itemId)
    }
}

internal fun formatMissingObjective(
    objective: FeaturePackLoader.QuestEntryDefinition?,
    currentAmount: Int,
    requiredAmount: Int,
): String {
    if (objective == null) {
        return "obiectiv necunoscut"
    }

    val missingAmount = (requiredAmount - currentAmount).coerceAtLeast(0)
    val objectiveType = normalizeObjectiveType(objective.type)
    val material = resolveQuestMaterial(objective)
    return when (objectiveType) {
        "collect_item",
        "deliver_to_npc",
        -> material?.let { formatQuestAmount(missingAmount.coerceAtLeast(1), it) } ?: formatQuestEntry(objective)

        "talk_to_npc" -> "vorbeste cu " + formatObjectiveTargetLabel(objective, "npc-ul tintit")
        "visit_region" -> "viziteaza " + formatObjectiveTargetLabel(objective, "regiunea tintita")
        "visit_place" -> "viziteaza " + formatObjectiveTargetLabel(objective, "locul tintit")
        "inspect_node" -> "inspecteaza " + formatObjectiveTargetLabel(objective, "punctul tintit")
        "kill_mob" -> "ucide " + if (missingAmount > 1) {
            "${missingAmount}x " + formatObjectiveTargetLabel(objective, "inamicul tintit")
        } else {
            formatObjectiveTargetLabel(objective, "inamicul tintit")
        }

        else -> formatQuestEntry(objective)
    }
}

private fun formatObjectiveTargetLabel(
    objective: FeaturePackLoader.QuestEntryDefinition?,
    fallback: String,
): String {
    if (objective == null || objective.itemId.isBlank()) {
        return fallback
    }

    return humanizeItemId(stripObjectivePrefix(objective.itemId))
}

fun formatQuestEntry(entry: FeaturePackLoader.QuestEntryDefinition?): String {
    if (entry == null) {
        return ""
    }

    if (entry.description.isNotBlank()) {
        return entry.description
    }

    val objectiveType = normalizeObjectiveType(entry.type)
    val material = resolveQuestMaterial(entry)
    return when (objectiveType) {
        "collect_item",
        "deliver_to_npc",
        -> material?.let { formatQuestAmount(entry.amount, it) }
            ?: if (entry.amount > 1) "${entry.amount}x ${humanizeItemId(entry.itemId)}" else humanizeItemId(entry.itemId)

        "talk_to_npc" -> "Vorbeste cu " + formatObjectiveTargetLabel(entry, "NPC-ul tintit")
        "visit_region" -> "Viziteaza " + formatObjectiveTargetLabel(entry, "regiunea tintita")
        "visit_place" -> "Viziteaza " + formatObjectiveTargetLabel(entry, "locul tintit")
        "inspect_node" -> "Inspecteaza " + formatObjectiveTargetLabel(entry, "punctul tintit")
        "kill_mob" -> "Ucide " + if (entry.amount > 1) {
            "${entry.amount}x " + formatObjectiveTargetLabel(entry, "inamicul tintit")
        } else {
            formatObjectiveTargetLabel(entry, "inamicul tintit")
        }

        "set_story_state" -> {
            val stateKey = firstNonBlank(getQuestEntryMetadata(entry, "state_key", "state", "flag", "value"), entry.itemId)
            "Actualizeaza story state" + if (stateKey.isNotBlank()) ": $stateKey" else ""
        }

        "record_story_event" -> {
            val eventType = firstNonBlank(getQuestEntryMetadata(entry, "event_type", "type_id"), "quest_completed")
            "Inregistreaza story event: $eventType"
        }

        else -> {
            val itemName = humanizeItemId(entry.itemId)
            if (entry.amount > 1) "${entry.amount}x $itemName" else itemName
        }
    }
}

fun formatQuestStatus(status: QuestStatus?): String =
    when (status) {
        null -> "Necunoscut"
        QuestStatus.NOT_STARTED -> "Disponibil"
        QuestStatus.OFFERED -> "Oferit, asteapta acceptarea"
        QuestStatus.ACTIVE -> "Activ"
        QuestStatus.COMPLETED -> "Completat"
        QuestStatus.FAILED -> "Esuat"
    }

internal fun describeQuestProgress(progress: PlayerQuestProgress?): String {
    if (progress == null) {
        return "necunoscut"
    }

    val questCode = progress.questCode()
    return if (!questCode.isNullOrBlank()) {
        "$questCode (${formatQuestStatus(progress.status())})"
    } else {
        "${progress.templateId()} (${formatQuestStatus(progress.status())})"
    }
}

internal fun formatQuestAmount(amount: Int, material: Material?): String {
    val itemName = material?.name?.let(::humanizeItemId) ?: "item"
    return if (amount > 1) "${amount}x $itemName" else itemName
}

internal fun joinNaturally(parts: List<String>?): String {
    if (parts.isNullOrEmpty()) {
        return ""
    }

    if (parts.size == 1) {
        return parts[0]
    }

    if (parts.size == 2) {
        return "${parts[0]} si ${parts[1]}"
    }

    val last = parts.last()
    return parts.dropLast(1).joinToString(", ") + " si " + last
}

internal fun humanizeItemId(itemId: String?): String =
    if (itemId.isNullOrBlank()) {
        "item"
    } else {
        itemId.lowercase(Locale.ROOT).replace('_', ' ')
    }
