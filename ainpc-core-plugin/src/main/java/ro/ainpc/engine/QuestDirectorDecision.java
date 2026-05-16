package ro.ainpc.engine;

import ro.ainpc.progression.ProgressionDefinition;

import java.util.ArrayList;
import java.util.List;

public record QuestDirectorDecision(
    Status status,
    String reason,
    String selectedProgressionId,
    String selectedTemplateId,
    String selectedMechanicId,
    String selectedDefinitionId,
    List<String> matchedSignals,
    List<String> candidateTemplateIds,
    List<String> blockedReasons,
    List<String> warnings,
    boolean runtimeExecutable
) {
    public QuestDirectorDecision {
        status = status != null ? status : Status.NO_ACTION;
        reason = valueOrEmpty(reason);
        selectedProgressionId = valueOrEmpty(selectedProgressionId);
        selectedTemplateId = valueOrEmpty(selectedTemplateId);
        selectedMechanicId = valueOrEmpty(selectedMechanicId);
        selectedDefinitionId = valueOrEmpty(selectedDefinitionId);
        matchedSignals = sanitizeStrings(matchedSignals);
        candidateTemplateIds = sanitizeStrings(candidateTemplateIds);
        blockedReasons = sanitizeStrings(blockedReasons);
        warnings = sanitizeStrings(warnings);
        runtimeExecutable = false;
    }

    public static QuestDirectorDecision noAction(String reason, List<String> warnings) {
        return new QuestDirectorDecision(
            Status.NO_ACTION,
            reason,
            "",
            "",
            "",
            "",
            List.of(),
            List.of(),
            List.of(),
            warnings,
            false
        );
    }

    public static QuestDirectorDecision blocked(String reason,
                                                List<String> blockedReasons,
                                                List<String> warnings) {
        return new QuestDirectorDecision(
            Status.BLOCKED,
            reason,
            "",
            "",
            "",
            "",
            List.of(),
            List.of(),
            blockedReasons,
            warnings,
            false
        );
    }

    public static QuestDirectorDecision seedSuggested(String reason,
                                                      List<String> matchedSignals,
                                                      List<String> warnings) {
        return new QuestDirectorDecision(
            Status.SEED_SUGGESTED,
            reason,
            "",
            "",
            "",
            "",
            matchedSignals,
            List.of(),
            List.of(),
            warnings,
            false
        );
    }

    public static QuestDirectorDecision candidateFound(ProgressionDefinition definition,
                                                       List<String> matchedSignals,
                                                       List<String> candidateTemplateIds,
                                                       List<String> warnings) {
        ProgressionDefinition safeDefinition = definition != null
            ? definition
            : new ProgressionDefinition(
                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                0, 0, 0, 0, false, false
            );
        return new QuestDirectorDecision(
            Status.CANDIDATE_FOUND,
            "matching_progression_definition",
            safeDefinition.progressionId(),
            safeDefinition.templateId(),
            safeDefinition.mechanicId(),
            safeDefinition.definitionId(),
            matchedSignals,
            candidateTemplateIds,
            List.of(),
            warnings,
            false
        );
    }

    public enum Status {
        NO_ACTION("no_action"),
        CANDIDATE_FOUND("candidate_found"),
        SEED_SUGGESTED("seed_suggested"),
        BLOCKED("blocked");

        private final String id;

        Status(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    private static List<String> sanitizeStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        List<String> sanitized = new ArrayList<>();
        for (String value : values) {
            String safeValue = valueOrEmpty(value);
            if (!safeValue.isBlank()) {
                sanitized.add(safeValue);
            }
        }
        return List.copyOf(sanitized);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
