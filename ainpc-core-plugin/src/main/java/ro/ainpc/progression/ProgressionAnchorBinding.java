package ro.ainpc.progression;

import java.util.Locale;

public record ProgressionAnchorBinding(
    String playerUuid,
    String templateId,
    String objectiveKey,
    String questCode,
    String objectiveType,
    String reference,
    String anchorType,
    String anchorId,
    String anchorLabel,
    long createdAt,
    long updatedAt,
    String status
) {
    public ProgressionAnchorBinding {
        playerUuid = valueOrEmpty(playerUuid);
        templateId = valueOrEmpty(templateId);
        objectiveKey = valueOrEmpty(objectiveKey);
        questCode = valueOrEmpty(questCode);
        objectiveType = valueOrEmpty(objectiveType);
        reference = valueOrEmpty(reference);
        anchorType = valueOrEmpty(anchorType);
        anchorId = valueOrEmpty(anchorId);
        anchorLabel = valueOrEmpty(anchorLabel);
        createdAt = Math.max(0L, createdAt);
        updatedAt = Math.max(0L, updatedAt);
        status = valueOrEmpty(status);
    }

    public boolean matchesAnchor(String type, String id) {
        String normalizedType = normalize(type);
        String normalizedId = normalize(id);
        return !normalizedType.isBlank()
            && !normalizedId.isBlank()
            && normalize(anchorType).equals(normalizedType)
            && normalize(anchorId).equals(normalizedId);
    }

    public String anchorSelector() {
        if (anchorType.isBlank() && anchorId.isBlank()) {
            return "";
        }
        return anchorType + ":" + anchorId;
    }

    public String displayLabel() {
        if (!anchorLabel.isBlank()) {
            return anchorLabel;
        }
        if (!reference.isBlank()) {
            return reference;
        }
        return anchorId;
    }

    private static String normalize(String value) {
        return valueOrEmpty(value).toLowerCase(Locale.ROOT);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
