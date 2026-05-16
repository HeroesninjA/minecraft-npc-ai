package ro.ainpc.engine;

import ro.ainpc.progression.ProgressionDefinition;
import ro.ainpc.story.StoryContextSnapshot;
import ro.ainpc.story.StoryEvent;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class QuestDirector {

    public QuestDirectorDecision decide(QuestDirectorRequest request) {
        if (request == null) {
            return QuestDirectorDecision.blocked(
                "invalid_request",
                List.of("QuestDirectorRequest lipsa."),
                List.of()
            );
        }

        StoryContextSnapshot context = request.storyContext();
        List<String> warnings = context.warnings();
        if (!request.blockingReasons().isEmpty()) {
            return QuestDirectorDecision.blocked("request_blocked", request.blockingReasons(), warnings);
        }

        List<String> storyDemandSignals = storyDemandSignals(context);
        if (storyDemandSignals.isEmpty() && request.preferredMechanicId().isBlank()) {
            return QuestDirectorDecision.noAction("story_context_does_not_request_quest", warnings);
        }

        List<CandidateScore> scores = request.definitions().stream()
            .filter(ProgressionDefinition::enabled)
            .map(definition -> score(definition, storyDemandSignals, request.preferredMechanicId()))
            .filter(candidate -> candidate.score() > 0)
            .sorted(Comparator
                .comparingInt(CandidateScore::score).reversed()
                .thenComparing(candidate -> candidate.definition().progressionId()))
            .toList();

        if (!scores.isEmpty()) {
            CandidateScore best = scores.getFirst();
            return QuestDirectorDecision.candidateFound(
                best.definition(),
                best.matchedSignals(),
                scores.stream()
                    .map(candidate -> candidate.definition().templateId())
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .limit(5)
                    .toList(),
                warnings
            );
        }

        if (request.questSeedAllowed()) {
            return QuestDirectorDecision.seedSuggested(
                "no_matching_template_but_seed_allowed",
                storyDemandSignals,
                warnings
            );
        }

        return QuestDirectorDecision.blocked(
            "no_matching_progression_definition",
            List.of("Nu exista template/progresie potrivita pentru story context."),
            warnings
        );
    }

    private CandidateScore score(ProgressionDefinition definition,
                                 List<String> storyDemandSignals,
                                 String preferredMechanicId) {
        int score = 0;
        List<String> matchedSignals = new ArrayList<>();
        if (!preferredMechanicId.isBlank()
            && normalize(preferredMechanicId).equals(normalize(definition.mechanicId()))) {
            score += 8;
            matchedSignals.add("preferred_mechanic=" + definition.mechanicId());
        }

        Set<String> definitionTokens = definitionTokens(definition);
        for (String signal : storyDemandSignals) {
            List<String> signalTokens = tokens(signal);
            for (String token : signalTokens) {
                if (definitionTokens.contains(token)) {
                    score += 2;
                    matchedSignals.add(signal);
                    break;
                }
            }
        }

        if (score > 0 && definition.objectiveCount() > 0) {
            score += 1;
        }
        return new CandidateScore(definition, score, matchedSignals);
    }

    private List<String> storyDemandSignals(StoryContextSnapshot context) {
        if (context == null || context.isEmpty()) {
            return List.of();
        }

        Set<String> demand = new LinkedHashSet<>();
        for (String signal : context.storySignals()) {
            String normalizedSignal = normalize(signal);
            if (isDemandSignal(normalizedSignal)) {
                demand.add(signal);
            }
        }

        if (context.persistentRegionState() != null
            && !"default".equalsIgnoreCase(context.persistentRegionState().stateKey())) {
            demand.add("persistent_region_state=" + context.persistentRegionState().stateKey());
        }
        if (context.persistentPlaceState() != null
            && !"default".equalsIgnoreCase(context.persistentPlaceState().stateKey())) {
            demand.add("persistent_place_state=" + context.persistentPlaceState().stateKey());
        }
        for (StoryEvent event : context.recentStoryEvents()) {
            if (!event.eventType().isBlank()) {
                demand.add("recent_story_event_type=" + event.eventType());
            }
            if (!event.eventKey().isBlank()) {
                demand.add("recent_story_event_key=" + event.eventKey());
            }
        }
        return List.copyOf(demand);
    }

    private boolean isDemandSignal(String normalizedSignal) {
        if (normalizedSignal.isBlank()) {
            return false;
        }
        return normalizedSignal.contains("persistent")
            || normalizedSignal.contains("recent_story_event")
            || normalizedSignal.contains("quest_hook")
            || normalizedSignal.contains("danger")
            || normalizedSignal.contains("tension")
            || normalizedSignal.contains("conflict")
            || normalizedSignal.contains("event")
            || normalizedSignal.contains("relevant_nodes");
    }

    private Set<String> definitionTokens(ProgressionDefinition definition) {
        Set<String> tokens = new LinkedHashSet<>();
        addTokens(tokens, definition.progressionId());
        addTokens(tokens, definition.packId());
        addTokens(tokens, definition.mechanicId());
        addTokens(tokens, definition.kind());
        addTokens(tokens, definition.definitionId());
        addTokens(tokens, definition.templateId());
        addTokens(tokens, definition.code());
        addTokens(tokens, definition.displayName());
        addTokens(tokens, definition.description());
        addTokens(tokens, definition.category());
        addTokens(tokens, definition.scenarioKind());
        addTokens(tokens, definition.baseType());
        addTokens(tokens, definition.label());
        return tokens;
    }

    private void addTokens(Set<String> target, String value) {
        target.addAll(tokens(value));
    }

    private List<String> tokens(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String token : normalized.split("_")) {
            if (token.length() >= 3) {
                result.add(token);
            }
        }
        return result;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String withoutDiacritics = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return withoutDiacritics.toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{Nd}]+", "_")
            .replaceAll("^_+|_+$", "")
            .replaceAll("_+", "_");
    }

    private record CandidateScore(
        ProgressionDefinition definition,
        int score,
        List<String> matchedSignals
    ) {
        private CandidateScore {
            matchedSignals = List.copyOf(matchedSignals != null ? matchedSignals : List.of());
        }
    }
}
