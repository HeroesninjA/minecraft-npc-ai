package ro.ainpc.engine

import ro.ainpc.progression.ProgressionDefinition
import ro.ainpc.story.StoryContextSnapshot
import ro.ainpc.story.StoryEvent
import java.text.Normalizer
import java.util.LinkedHashSet
import java.util.Locale

class QuestDirector {
    fun decide(request: QuestDirectorRequest?): QuestDirectorDecision {
        if (request == null) {
            return QuestDirectorDecision.blocked(
                "invalid_request",
                listOf("QuestDirectorRequest lipsa."),
                listOf()
            )
        }

        val context = request.storyContext()
        val warnings = context.warnings()
        if (request.blockingReasons().isNotEmpty()) {
            return QuestDirectorDecision.blocked("request_blocked", request.blockingReasons(), warnings)
        }

        val storyDemandSignals = storyDemandSignals(context)
        if (storyDemandSignals.isEmpty() && request.preferredMechanicId().isBlank()) {
            return QuestDirectorDecision.noAction("story_context_does_not_request_quest", warnings)
        }

        val scores = request.definitions().asSequence()
            .filter { it.enabled() }
            .map { definition -> score(definition, storyDemandSignals, request.preferredMechanicId()) }
            .filter { candidate -> candidate.score() > 0 }
            .sortedWith(
                compareByDescending<CandidateScore> { it.score() }
                    .thenBy { it.definition().progressionId() }
            )
            .toList()

        if (scores.isNotEmpty()) {
            val best = scores.first()
            return QuestDirectorDecision.candidateFound(
                best.definition(),
                best.matchedSignals(),
                scores.asSequence()
                    .map { candidate -> candidate.definition().templateId() }
                    .filter { value -> !value.isNullOrBlank() }
                    .distinct()
                    .take(5)
                    .toList(),
                warnings
            )
        }

        if (request.questSeedAllowed()) {
            return QuestDirectorDecision.seedSuggested(
                "no_matching_template_but_seed_allowed",
                storyDemandSignals,
                warnings
            )
        }

        return QuestDirectorDecision.blocked(
            "no_matching_progression_definition",
            listOf("Nu exista template/progresie potrivita pentru story context."),
            warnings
        )
    }

    private fun score(
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

    private fun storyDemandSignals(context: StoryContextSnapshot?): List<String> {
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

    private fun isDemandSignal(normalizedSignal: String): Boolean {
        if (normalizedSignal.isBlank()) {
            return false
        }
        return normalizedSignal.contains("persistent")
            || normalizedSignal.contains("recent_story_event")
            || normalizedSignal.contains("quest_hook")
            || normalizedSignal.contains("danger")
            || normalizedSignal.contains("tension")
            || normalizedSignal.contains("conflict")
            || normalizedSignal.contains("event")
            || normalizedSignal.contains("relevant_nodes")
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

    private data class CandidateScore(
        val definition: ProgressionDefinition,
        val score: Int,
        val matchedSignals: List<String>
    ) {
        fun definition(): ProgressionDefinition = definition
        fun score(): Int = score
        fun matchedSignals(): List<String> = matchedSignals.toList()
    }
}
