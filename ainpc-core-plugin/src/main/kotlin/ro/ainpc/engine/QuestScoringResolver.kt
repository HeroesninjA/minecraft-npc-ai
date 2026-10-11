package ro.ainpc.engine

import ro.ainpc.progression.ProgressionDefinition
import ro.ainpc.story.StoryContextSnapshot
import ro.ainpc.story.StructureQuestCooldown
import java.text.Normalizer
import java.util.LinkedHashSet
import java.util.Locale

/**
 * Questscoringresolver.
 */
class QuestScoringResolver(private val currentTimeMillis: () -> Long = System::currentTimeMillis) {
    fun score(
        definition: ProgressionDefinition,
        storyDemandSignals: List<String>,
        preferredMechanicId: String
    ): CandidateScore {
        var score = 0
        val matchedSignals = mutableListOf<String>()
        if (preferredMechanicId.isNotBlank()
            && normalize(preferredMechanicId) == normalize(definition.mechanicId())
        ) {
            score += 8
            matchedSignals.add("preferred_mechanic=" + definition.mechanicId())
        }

        val definitionTokens = definitionTokens(definition)
        for (signal in storyDemandSignals) {
            val signalTokens = tokens(signal)
            for (token in signalTokens) {
                if (definitionTokens.contains(token)) {
                    score += 2
                    matchedSignals.add(signal)
                    break
                }
            }
        }

        if (score > 0 && definition.objectiveCount() > 0) {
            score += 1
        }
        return CandidateScore(definition, score, matchedSignals)
    }

    fun storyDemandSignals(context: StoryContextSnapshot?): List<String> {
        if (context == null || context.isEmpty()) {
            return listOf()
        }

        val demand = LinkedHashSet<String>()
        for (signal in context.storySignals()) {
            val normalizedSignal = normalize(signal)
            if (isDemandSignal(normalizedSignal)) {
                demand.add(signal)
            }
        }

        val persistentRegionState = context.persistentRegionState()
        if (persistentRegionState != null
            && !persistentRegionState.stateKey().equals("default", ignoreCase = true)
        ) {
            demand.add("persistent_region_state=" + persistentRegionState.stateKey())
        }
        val persistentPlaceState = context.persistentPlaceState()
        if (persistentPlaceState != null
            && !persistentPlaceState.stateKey().equals("default", ignoreCase = true)
        ) {
            demand.add("persistent_place_state=" + persistentPlaceState.stateKey())
        }
        for (event in context.recentStoryEvents()) {
            if (event.eventType().isNotBlank()) {
                demand.add("recent_story_event_type=" + event.eventType())
            }
            if (event.eventKey().isNotBlank()) {
                demand.add("recent_story_event_key=" + event.eventKey())
            }
        }
        return demand.toList()
    }

    fun hasQuestGenerationCooldown(context: StoryContextSnapshot?, storyDemandSignals: List<String>): Boolean {
        if (context == null || context.isEmpty()) {
            return false
        }

        val signals = context.storySignals()
        val cooldownCategory = signalValue(signals, "quest_generation_cooldown_category")
        val currentPlaceId = signalValue(signals, "place_id")
        val cooldownPlaceId = signalValue(signals, "quest_generation_cooldown_place_id")
        if ((cooldownPlaceId.isBlank() || cooldownPlaceId == currentPlaceId)
            && isCooldownRelevantToDemand(cooldownCategory, storyDemandSignals, signals)
            && signals.any { it.startsWith("quest_generation_cooldown=") }
        ) {
            return true
        }

        if (currentPlaceId.isBlank()) {
            return false
        }

        val now = currentTimeMillis()
        return context.recentStoryEvents().any { event ->
            val elapsed = now - event.createdAt()
            event.eventType().startsWith("structure_")
                && event.placeId() == currentPlaceId
                && event.createdAt() <= now
                && elapsed >= 0L
                && elapsed < StructureQuestCooldown.durationForEvent(event.eventType())
                && isCooldownRelevantToDemand(
                    StructureQuestCooldown.categoryForEvent(event.eventType()), storyDemandSignals, signals
                )
        }
    }

    private fun isDemandSignal(normalizedSignal: String): Boolean {
        if (normalizedSignal.isBlank()) {
            return false
        }
        if (normalizedSignal.startsWith("npc_home_place_id_") ||
            normalizedSignal.startsWith("npc_work_place_id_") ||
            normalizedSignal.startsWith("npc_social_place_id_")
        ) {
            return false
        }
        return normalizedSignal.contains("persistent")
            || normalizedSignal.contains("recent_story_event")
            || normalizedSignal.contains("structure_type")
            || normalizedSignal.contains("structure_category")
            || normalizedSignal.contains("place_type")
            || normalizedSignal.contains("place_tags")
            || normalizedSignal.contains("quest_hook")
            || normalizedSignal.startsWith("npc_quest_role_")
            || normalizedSignal.startsWith("npc_social_role_")
            || normalizedSignal.contains("danger")
            || normalizedSignal.contains("tension")
            || normalizedSignal.contains("conflict")
            || normalizedSignal.contains("event")
            || normalizedSignal.contains("relevant_nodes")
    }

    private fun isCooldownRelevantToDemand(
        cooldownCategory: String,
        storyDemandSignals: List<String>,
        contextSignals: List<String>
    ): Boolean {
        val normalizedCategory = normalize(cooldownCategory)
        if (normalizedCategory.isBlank()) {
            return false
        }

        if (storyDemandSignals.any { signalValue(listOf(it), "structure_category") == normalizedCategory }) {
            return true
        }
        if (contextSignals.any { signalValue(listOf(it), "structure_category") == normalizedCategory }) {
            return true
        }

        val roleSignals = storyDemandSignals.asSequence().plus(contextSignals.asSequence())
        return when (normalizedCategory) {
            "residential" -> roleSignals.any { signal ->
                val normalized = normalize(signal)
                normalized.contains("home_life") || normalized.contains("household_request") || normalized.contains("family_help")
            }
            "profession" -> roleSignals.any { signal ->
                val normalized = normalize(signal)
                normalized.contains("repair_tool")
                    || normalized.contains("forge_order")
                    || normalized.contains("market_supply")
                    || normalized.contains("customer_request")
                    || normalized.contains("delivery")
                    || normalized.contains("craft_order")
                    || normalized.contains("material_supply")
                    || normalized.contains("harvest")
                    || normalized.contains("field_work")
                    || normalized.contains("work_event")
            }
            "utility" -> roleSignals.any { signal ->
                val normalized = normalize(signal)
                normalized.contains("route_report") || normalized.contains("patrol_request") || normalized.contains("observe_tracks")
            }
            "leisure" -> roleSignals.any { signal ->
                val normalized = normalize(signal)
                normalized.contains("community_event") || normalized.contains("social_event") || normalized.contains("festival") || normalized.contains("rumor")
            }
            "public" -> roleSignals.any { signal ->
                val normalized = normalize(signal)
                normalized.contains("civic_request") || normalized.contains("public_event") || normalized.contains("lost_record")
            }
            "exterior" -> roleSignals.any { signal ->
                val normalized = normalize(signal)
                normalized.contains("exploration") || normalized.contains("danger") || normalized.contains("route_report")
            }
            "quest" -> roleSignals.any { signal ->
                val normalized = normalize(signal)
                normalized.contains("quest_hook") || normalized.contains("quest_trigger") || normalized.contains("recover_artifact") || normalized.contains("investigation")
            }
            else -> false
        }
    }

    private fun definitionTokens(definition: ProgressionDefinition): Set<String> {
        val tokens = LinkedHashSet<String>()
        addTokens(tokens, definition.progressionId())
        addTokens(tokens, definition.packId())
        addTokens(tokens, definition.mechanicId())
        addTokens(tokens, definition.kind())
        addTokens(tokens, definition.definitionId())
        addTokens(tokens, definition.templateId())
        addTokens(tokens, definition.code())
        addTokens(tokens, definition.displayName())
        addTokens(tokens, definition.description())
        addTokens(tokens, definition.category())
        addTokens(tokens, definition.scenarioKind())
        addTokens(tokens, definition.baseType())
        addTokens(tokens, definition.label())
        return tokens
    }

    private fun addTokens(target: MutableSet<String>, value: String?) {
        target.addAll(tokens(value))
    }

    private fun tokens(value: String?): List<String> {
        val normalized = normalize(value)
        if (normalized.isBlank()) {
            return listOf()
        }
        val result = mutableListOf<String>()
        for (token in normalized.split("_")) {
            if (token.length >= 3) {
                result.add(token)
            }
        }
        return result
    }

    private fun signalValue(signals: List<String>, prefix: String): String {
        return signals.firstOrNull { it.startsWith("$prefix=") }
            ?.substringAfter("=")
            .orEmpty()
    }

    private fun normalize(value: String?): String {
        if (value.isNullOrBlank()) {
            return ""
        }
        val withoutDiacritics = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
        return withoutDiacritics.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{Nd}]+"), "_")
            .replace(Regex("^_+|_+$"), "")
            .replace(Regex("_+"), "_")
    }

/**
 * Candidatescore.
 */
    data class CandidateScore(
        val definition: ProgressionDefinition,
        val score: Int,
        val matchedSignals: List<String>
    )
}
