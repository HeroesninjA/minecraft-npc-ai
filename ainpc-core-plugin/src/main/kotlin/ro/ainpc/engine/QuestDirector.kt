package ro.ainpc.engine

import ro.ainpc.progression.ProgressionDefinition
import ro.ainpc.story.StoryContextSnapshot
import ro.ainpc.story.StoryEvent
import java.text.Normalizer
import java.util.LinkedHashSet
import java.util.Locale

class QuestDirector {
    private val scoringResolver = QuestScoringResolver()

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

        val storyDemandSignals = scoringResolver.storyDemandSignals(context)
        if (scoringResolver.hasQuestGenerationCooldown(context, storyDemandSignals)) {
            return QuestDirectorDecision.noAction("structure_quest_generation_cooldown", warnings)
        }
        if (storyDemandSignals.isEmpty() && request.preferredMechanicId().isBlank()) {
            return QuestDirectorDecision.noAction("story_context_does_not_request_quest", warnings)
        }

        val scores: List<QuestScoringResolver.CandidateScore> = request.definitions().asSequence()
            .filter { it.enabled() }
            .map { definition -> scoringResolver.score(definition, storyDemandSignals, request.preferredMechanicId()) }
            .filter { candidate -> candidate.score > 0 }
            .sortedWith(
                compareByDescending<QuestScoringResolver.CandidateScore> { it.score }
                    .thenBy { it.definition.progressionId() }
            )
            .toList()

        if (scores.isNotEmpty()) {
            val best = scores.first()
            return QuestDirectorDecision.candidateFound(
                best.definition,
                best.matchedSignals,
                scores.asSequence()
                    .map { candidate -> candidate.definition.templateId() }
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

}
