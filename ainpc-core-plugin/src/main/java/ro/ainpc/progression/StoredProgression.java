package ro.ainpc.progression;

public record StoredProgression(
    String playerUuid,
    String progressionId,
    String packId,
    String mechanicId,
    String kind,
    String definitionId,
    String templateId,
    String code,
    String status,
    long startedAt,
    long completedAt,
    String currentPhase,
    String currentStageId,
    String objectiveProgressJson,
    String variablesJson,
    long updatedAt,
    boolean tracked,
    boolean definitionResolved,
    String mechanicLabel,
    String singularLabel,
    String pluralLabel,
    String compatibilitySource
) {
    public StoredProgression {
        playerUuid = valueOrEmpty(playerUuid);
        progressionId = valueOrEmpty(progressionId);
        packId = valueOrEmpty(packId);
        mechanicId = valueOrEmpty(mechanicId);
        kind = valueOrEmpty(kind);
        definitionId = valueOrEmpty(definitionId);
        templateId = valueOrEmpty(templateId);
        code = valueOrEmpty(code);
        status = valueOrEmpty(status);
        startedAt = Math.max(0L, startedAt);
        completedAt = Math.max(0L, completedAt);
        currentPhase = valueOrEmpty(currentPhase);
        currentStageId = valueOrEmpty(currentStageId);
        objectiveProgressJson = jsonOrEmptyObject(objectiveProgressJson);
        variablesJson = jsonOrEmptyObject(variablesJson);
        updatedAt = Math.max(0L, updatedAt);
        mechanicLabel = valueOrEmpty(mechanicLabel);
        singularLabel = valueOrEmpty(singularLabel);
        pluralLabel = valueOrEmpty(pluralLabel);
        compatibilitySource = valueOrFallback(compatibilitySource, "player_quests");
    }

    public boolean current() {
        return "active".equalsIgnoreCase(status) || "offered".equalsIgnoreCase(status);
    }

    public boolean archived() {
        return "completed".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status);
    }

    private static String jsonOrEmptyObject(String value) {
        String safeValue = valueOrEmpty(value);
        return safeValue.isBlank() ? "{}" : safeValue;
    }

    private static String valueOrFallback(String value, String fallback) {
        String safeValue = valueOrEmpty(value);
        return safeValue.isBlank() ? valueOrEmpty(fallback) : safeValue;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
