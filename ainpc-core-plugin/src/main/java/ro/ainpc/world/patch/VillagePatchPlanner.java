package ro.ainpc.world.patch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class VillagePatchPlanner {

    public PatchPlannerResult plan(GapReport report, PatchPlannerOptions options) {
        PatchPlannerOptions safeOptions = options != null
            ? options
            : PatchPlannerOptions.forTargetPopulation(report != null ? report.targetPopulation() : 0);
        if (report == null) {
            return new PatchPlannerResult(null, List.of(), List.of(), List.of(), List.of("GapReport lipsa."));
        }
        if (!report.errors().isEmpty()) {
            return new PatchPlannerResult(report, List.of(), List.of(), List.of(), report.errors());
        }

        List<PatchCandidate> candidates = report.gaps().stream()
            .map(gap -> toCandidate(report.regionId(), gap))
            .sorted(Comparator
                .comparingInt(PatchCandidate::priority).reversed()
                .thenComparingInt(PatchCandidate::risk)
                .thenComparing(PatchCandidate::candidateId))
            .limit(safeOptions.maxPatchCount())
            .toList();
        List<PatchPlan> plans = candidates.stream()
            .map(candidate -> toPlan(candidate, safeOptions))
            .toList();
        List<String> warnings = new ArrayList<>(report.warnings());
        if (report.gaps().size() > plans.size()) {
            warnings.add("Planul a fost limitat la " + plans.size()
                + " patch-uri din " + report.gaps().size() + " gap-uri.");
        }
        return new PatchPlannerResult(report, candidates, plans, warnings, List.of());
    }

    private PatchCandidate toCandidate(String regionId, VillageGap gap) {
        PatchType patchType = patchTypeFor(gap);
        int cost = costFor(patchType);
        int risk = riskFor(patchType);
        int priority = Math.max(1, gap.severity() * 10 - cost - risk);
        return new PatchCandidate(
            candidateId(regionId, gap),
            gap.type(),
            patchType,
            valueOrFallback(gap.targetRegionId(), regionId),
            gap.targetPlaceId(),
            priority,
            cost,
            risk,
            requiredCapabilities(patchType),
            gap.reason()
        );
    }

    private PatchPlan toPlan(PatchCandidate candidate, PatchPlannerOptions options) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (String capability : candidate.requiredCapabilities()) {
            if (!options.hasCapability(capability)) {
                errors.add("Lipseste capabilitatea " + capability + ".");
            }
        }

        PatchBuildMode buildMode = buildModeFor(candidate.patchType());
        List<String> plannedPlaces = plannedPlaces(candidate);
        List<String> plannedNodes = plannedNodes(candidate);
        if (plannedPlaces.isEmpty() && plannedNodes.isEmpty()) {
            errors.add("Patch-ul nu produce mapping semantic.");
        }
        PatchValidationStatus status = !errors.isEmpty()
            ? PatchValidationStatus.BLOCKED
            : warnings.isEmpty() ? PatchValidationStatus.VALID : PatchValidationStatus.WARNING;

        return new PatchPlan(
            candidate.candidateId().replace("candidate", "patch"),
            candidate.patchType(),
            buildMode,
            candidate.targetRegionId(),
            candidate.targetPlaceId(),
            templateFor(candidate),
            plannedPlaces,
            plannedNodes,
            candidate.requiredCapabilities(),
            status,
            warnings,
            errors,
            candidate.reason(),
            candidate.priority(),
            candidate.cost(),
            candidate.risk()
        );
    }

    private PatchType patchTypeFor(VillageGap gap) {
        return switch (gap.type()) {
            case MISSING_BEDS, MISSING_HOUSE_CAPACITY, HOUSE_TOO_SMALL_FOR_FAMILY -> PatchType.ADD_HOUSE;
            case MISSING_WORKPLACE_FOR_PROFESSION -> PatchType.ADD_WORKPLACE;
            case MISSING_SOCIAL_HUB -> PatchType.ADD_SOCIAL_PLACE;
            case MISSING_QUEST_TRIGGER_NODE, MISSING_ENTRANCE_NODE, WORKPLACE_WITHOUT_WORK_NODE -> PatchType.ADD_NODE;
        };
    }

    private PatchBuildMode buildModeFor(PatchType patchType) {
        return switch (patchType) {
            case ADD_NODE, MARK_EXISTING_PLACE -> PatchBuildMode.SEMANTIC_ONLY;
            case ADD_HOUSE, ADD_WORKPLACE, ADD_SOCIAL_PLACE, EXPAND_HOUSE, DECORATE_PLACE, CONNECT_PATH ->
                PatchBuildMode.NATIVE_PATCH;
        };
    }

    private List<String> requiredCapabilities(PatchType patchType) {
        return switch (patchType) {
            case ADD_NODE, MARK_EXISTING_PLACE -> List.of("semantic-place-mapping");
            case ADD_HOUSE, ADD_WORKPLACE, ADD_SOCIAL_PLACE, EXPAND_HOUSE, DECORATE_PLACE, CONNECT_PATH ->
                List.of("native-block-build", "semantic-place-mapping");
        };
    }

    private List<String> plannedPlaces(PatchCandidate candidate) {
        return switch (candidate.patchType()) {
            case ADD_HOUSE -> List.of(candidate.targetRegionId() + ":patch_house_01");
            case ADD_WORKPLACE -> List.of(candidate.targetRegionId() + ":patch_" + localReference(candidate));
            case ADD_SOCIAL_PLACE -> List.of(candidate.targetRegionId() + ":patch_social_hub");
            default -> List.of();
        };
    }

    private List<String> plannedNodes(PatchCandidate candidate) {
        return switch (candidate.gapType()) {
            case MISSING_QUEST_TRIGGER_NODE -> List.of(nodeScope(candidate) + ":patch_quest_board");
            case MISSING_ENTRANCE_NODE -> List.of(nodeScope(candidate) + ":patch_entrance");
            case WORKPLACE_WITHOUT_WORK_NODE -> List.of(nodeScope(candidate) + ":patch_work_anchor");
            case MISSING_SOCIAL_HUB -> List.of(candidate.targetRegionId() + ":patch_social_hub:meeting_point");
            case MISSING_BEDS, MISSING_HOUSE_CAPACITY, HOUSE_TOO_SMALL_FOR_FAMILY ->
                List.of(candidate.targetRegionId() + ":patch_house_01:bed_1",
                    candidate.targetRegionId() + ":patch_house_01:npc_spawn_1");
            case MISSING_WORKPLACE_FOR_PROFESSION ->
                List.of(candidate.targetRegionId() + ":patch_" + localReference(candidate) + ":work_1");
        };
    }

    private String templateFor(PatchCandidate candidate) {
        return switch (candidate.patchType()) {
            case ADD_HOUSE -> "small_house";
            case ADD_WORKPLACE -> "workplace_" + localReference(candidate);
            case ADD_SOCIAL_PLACE -> "social_hub";
            case ADD_NODE -> "";
            default -> candidate.patchType().id();
        };
    }

    private int costFor(PatchType patchType) {
        return switch (patchType) {
            case ADD_NODE, MARK_EXISTING_PLACE -> 1;
            case CONNECT_PATH, DECORATE_PLACE -> 2;
            case EXPAND_HOUSE -> 3;
            case ADD_HOUSE, ADD_SOCIAL_PLACE -> 4;
            case ADD_WORKPLACE -> 5;
        };
    }

    private int riskFor(PatchType patchType) {
        return switch (patchType) {
            case ADD_NODE, MARK_EXISTING_PLACE -> 1;
            case DECORATE_PLACE -> 2;
            case CONNECT_PATH, EXPAND_HOUSE -> 3;
            case ADD_HOUSE, ADD_SOCIAL_PLACE -> 4;
            case ADD_WORKPLACE -> 5;
        };
    }

    private String candidateId(String regionId, VillageGap gap) {
        return valueOrFallback(gap.targetRegionId(), regionId)
            + ":candidate:"
            + normalize(gap.type().name())
            + (gap.reference().isBlank() ? "" : ":" + normalize(gap.reference()));
    }

    private String nodeScope(PatchCandidate candidate) {
        return candidate.targetPlaceId().isBlank() ? candidate.targetRegionId() : candidate.targetPlaceId();
    }

    private String localReference(PatchCandidate candidate) {
        String raw = candidate.candidateId();
        int separator = raw.lastIndexOf(':');
        if (separator >= 0 && separator < raw.length() - 1) {
            return normalize(raw.substring(separator + 1));
        }
        return normalize(candidate.gapType().name());
    }

    private String valueOrFallback(String value, String fallback) {
        String safeValue = value == null ? "" : value.trim();
        return safeValue.isBlank() ? fallback : safeValue;
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return normalized.isBlank() ? "patch" : normalized;
    }
}
