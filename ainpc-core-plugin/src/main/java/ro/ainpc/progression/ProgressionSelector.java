package ro.ainpc.progression;

import java.util.Locale;

public record ProgressionSelector(
    String raw,
    String normalized,
    String packId,
    String mechanicOrKind,
    String definitionId,
    boolean trackedAlias
) {
    public ProgressionSelector {
        raw = raw == null ? "" : raw.trim();
        normalized = normalized == null ? "" : normalized.trim();
        packId = packId == null ? "" : packId.trim();
        mechanicOrKind = mechanicOrKind == null ? "" : mechanicOrKind.trim();
        definitionId = definitionId == null ? "" : definitionId.trim();
    }

    public static ProgressionSelector parse(String selector) {
        String raw = selector == null ? "" : selector.trim();
        if (raw.isBlank()) {
            return new ProgressionSelector("", "", "", "", "", false);
        }

        String lower = raw.toLowerCase(Locale.ROOT);
        if (isTrackedAlias(lower)) {
            return new ProgressionSelector(raw, "tracked", "", "", "tracked", true);
        }

        String[] parts = raw.split(":", -1);
        if (parts.length == 2) {
            String mechanicOrKind = parts[0].trim();
            String definitionId = parts[1].trim();
            return new ProgressionSelector(raw, mechanicOrKind + ":" + definitionId, "", mechanicOrKind, definitionId, false);
        }
        if (parts.length == 3) {
            String packId = parts[0].trim();
            String mechanicOrKind = parts[1].trim();
            String definitionId = parts[2].trim();
            return new ProgressionSelector(raw, packId + ":" + mechanicOrKind + ":" + definitionId, packId, mechanicOrKind, definitionId, false);
        }

        return new ProgressionSelector(raw, raw, "", "", raw, false);
    }

    public static ProgressionSelector forContractAlias(String selector) {
        return forKindAlias(selector, "contract");
    }

    public static ProgressionSelector forKindAlias(String selector, String kind) {
        ProgressionSelector parsed = parse(selector);
        if (parsed.isEmpty()
            || parsed.hasNamespace()
            || parsed.isTrackedAlias()
            || "nearest".equalsIgnoreCase(parsed.normalized())) {
            return parsed;
        }

        String safeKind = kind == null ? "" : kind.trim();
        if (safeKind.isBlank()) {
            return parsed;
        }
        return parse(safeKind + ":" + parsed.normalized());
    }

    public static boolean isTrackedAlias(String selector) {
        String normalized = selector == null ? "" : selector.trim().toLowerCase(Locale.ROOT);
        return "tracked".equals(normalized)
            || "current".equals(normalized)
            || "curent".equals(normalized)
            || "urmarit".equals(normalized);
    }

    public boolean isEmpty() {
        return normalized.isBlank();
    }

    public boolean hasNamespace() {
        return !mechanicOrKind.isBlank() || !packId.isBlank();
    }

    public boolean isTrackedAlias() {
        return trackedAlias;
    }

    public String commandSelector() {
        return normalized;
    }
}
