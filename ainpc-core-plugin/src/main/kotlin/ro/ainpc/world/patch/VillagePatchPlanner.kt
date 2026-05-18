package ro.ainpc.world.patch

import java.util.Locale

class VillagePatchPlanner {
    fun plan(report: GapReport?, options: PatchPlannerOptions?): PatchPlannerResult {
        val safeOptions = options ?: PatchPlannerOptions.forTargetPopulation(report?.targetPopulation() ?: 0)
        if (report == null) {
            return PatchPlannerResult(null, emptyList(), emptyList(), emptyList(), listOf("GapReport lipsa."))
        }
        if (report.errors().isNotEmpty()) {
            return PatchPlannerResult(report, emptyList(), emptyList(), emptyList(), report.errors())
        }

        val candidates = report.gaps().asSequence()
            .map { gap -> toCandidate(report.regionId(), gap) }
            .sortedWith(
                compareByDescending<PatchCandidate> { it.priority() }
                    .thenBy { it.risk() }
                    .thenBy { it.candidateId() }
            )
            .take(safeOptions.maxPatchCount())
            .toList()

        val plans = candidates.map { candidate -> toPlan(candidate, safeOptions) }
        val warnings = ArrayList(report.warnings())
        if (report.gaps().size > plans.size) {
            warnings.add(
                "Planul a fost limitat la " + plans.size +
                    " patch-uri din " + report.gaps().size + " gap-uri."
            )
        }
        return PatchPlannerResult(report, candidates, plans, warnings, emptyList())
    }

    private fun toCandidate(regionId: String, gap: VillageGap): PatchCandidate {
        val patchType = patchTypeFor(gap)
        val cost = costFor(patchType)
        val risk = riskFor(patchType)
        val priority = maxOf(1, gap.severity() * 10 - cost - risk)
        return PatchCandidate(
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
        )
    }

    private fun toPlan(candidate: PatchCandidate, options: PatchPlannerOptions): PatchPlan {
        val warnings = ArrayList<String>()
        val errors = ArrayList<String>()
        for (capability in candidate.requiredCapabilities()) {
            if (!options.hasCapability(capability)) {
                errors.add("Lipseste capabilitatea $capability.")
            }
        }

        val buildMode = buildModeFor(candidate.patchType())
        val plannedPlaces = plannedPlaces(candidate)
        val plannedNodes = plannedNodes(candidate)
        if (plannedPlaces.isEmpty() && plannedNodes.isEmpty()) {
            errors.add("Patch-ul nu produce mapping semantic.")
        }
        val status = if (errors.isNotEmpty()) {
            PatchValidationStatus.BLOCKED
        } else if (warnings.isEmpty()) {
            PatchValidationStatus.VALID
        } else {
            PatchValidationStatus.WARNING
        }

        return PatchPlan(
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
        )
    }

    private fun patchTypeFor(gap: VillageGap): PatchType {
        return when (gap.type()) {
            PatchGapType.MISSING_BEDS,
            PatchGapType.MISSING_HOUSE_CAPACITY,
            PatchGapType.HOUSE_TOO_SMALL_FOR_FAMILY -> PatchType.ADD_HOUSE

            PatchGapType.MISSING_WORKPLACE_FOR_PROFESSION -> PatchType.ADD_WORKPLACE
            PatchGapType.MISSING_SOCIAL_HUB -> PatchType.ADD_SOCIAL_PLACE
            PatchGapType.MISSING_QUEST_TRIGGER_NODE,
            PatchGapType.MISSING_ENTRANCE_NODE,
            PatchGapType.WORKPLACE_WITHOUT_WORK_NODE -> PatchType.ADD_NODE
        }
    }

    private fun buildModeFor(patchType: PatchType): PatchBuildMode {
        return when (patchType) {
            PatchType.ADD_NODE, PatchType.MARK_EXISTING_PLACE -> PatchBuildMode.SEMANTIC_ONLY
            PatchType.ADD_HOUSE, PatchType.ADD_WORKPLACE, PatchType.ADD_SOCIAL_PLACE,
            PatchType.EXPAND_HOUSE, PatchType.DECORATE_PLACE, PatchType.CONNECT_PATH -> PatchBuildMode.NATIVE_PATCH
        }
    }

    private fun requiredCapabilities(patchType: PatchType): List<String> {
        return when (patchType) {
            PatchType.ADD_NODE, PatchType.MARK_EXISTING_PLACE -> listOf("semantic-place-mapping")
            PatchType.ADD_HOUSE, PatchType.ADD_WORKPLACE, PatchType.ADD_SOCIAL_PLACE,
            PatchType.EXPAND_HOUSE, PatchType.DECORATE_PLACE, PatchType.CONNECT_PATH -> listOf(
                "native-block-build",
                "semantic-place-mapping"
            )
        }
    }

    private fun plannedPlaces(candidate: PatchCandidate): List<String> {
        return when (candidate.patchType()) {
            PatchType.ADD_HOUSE -> listOf(candidate.targetRegionId() + ":patch_house_01")
            PatchType.ADD_WORKPLACE -> listOf(candidate.targetRegionId() + ":patch_" + localReference(candidate))
            PatchType.ADD_SOCIAL_PLACE -> listOf(candidate.targetRegionId() + ":patch_social_hub")
            else -> emptyList()
        }
    }

    private fun plannedNodes(candidate: PatchCandidate): List<String> {
        return when (candidate.gapType()) {
            PatchGapType.MISSING_QUEST_TRIGGER_NODE -> listOf(nodeScope(candidate) + ":patch_quest_board")
            PatchGapType.MISSING_ENTRANCE_NODE -> listOf(nodeScope(candidate) + ":patch_entrance")
            PatchGapType.WORKPLACE_WITHOUT_WORK_NODE -> listOf(nodeScope(candidate) + ":patch_work_anchor")
            PatchGapType.MISSING_SOCIAL_HUB -> listOf(candidate.targetRegionId() + ":patch_social_hub:meeting_point")
            PatchGapType.MISSING_BEDS,
            PatchGapType.MISSING_HOUSE_CAPACITY,
            PatchGapType.HOUSE_TOO_SMALL_FOR_FAMILY -> listOf(
                candidate.targetRegionId() + ":patch_house_01:bed_1",
                candidate.targetRegionId() + ":patch_house_01:npc_spawn_1"
            )

            PatchGapType.MISSING_WORKPLACE_FOR_PROFESSION ->
                listOf(candidate.targetRegionId() + ":patch_" + localReference(candidate) + ":work_1")
        }
    }

    private fun templateFor(candidate: PatchCandidate): String {
        return when (candidate.patchType()) {
            PatchType.ADD_HOUSE -> "small_house"
            PatchType.ADD_WORKPLACE -> "workplace_" + localReference(candidate)
            PatchType.ADD_SOCIAL_PLACE -> "social_hub"
            PatchType.ADD_NODE -> ""
            else -> candidate.patchType().id()
        }
    }

    private fun costFor(patchType: PatchType): Int {
        return when (patchType) {
            PatchType.ADD_NODE, PatchType.MARK_EXISTING_PLACE -> 1
            PatchType.CONNECT_PATH, PatchType.DECORATE_PLACE -> 2
            PatchType.EXPAND_HOUSE -> 3
            PatchType.ADD_HOUSE, PatchType.ADD_SOCIAL_PLACE -> 4
            PatchType.ADD_WORKPLACE -> 5
        }
    }

    private fun riskFor(patchType: PatchType): Int {
        return when (patchType) {
            PatchType.ADD_NODE, PatchType.MARK_EXISTING_PLACE -> 1
            PatchType.DECORATE_PLACE -> 2
            PatchType.CONNECT_PATH, PatchType.EXPAND_HOUSE -> 3
            PatchType.ADD_HOUSE, PatchType.ADD_SOCIAL_PLACE -> 4
            PatchType.ADD_WORKPLACE -> 5
        }
    }

    private fun candidateId(regionId: String, gap: VillageGap): String {
        return valueOrFallback(gap.targetRegionId(), regionId) +
            ":candidate:" +
            normalize(gap.type().name) +
            if (gap.reference().isBlank()) "" else ":" + normalize(gap.reference())
    }

    private fun nodeScope(candidate: PatchCandidate): String {
        return if (candidate.targetPlaceId().isBlank()) candidate.targetRegionId() else candidate.targetPlaceId()
    }

    private fun localReference(candidate: PatchCandidate): String {
        val raw = candidate.candidateId()
        val separator = raw.lastIndexOf(':')
        if (separator >= 0 && separator < raw.length - 1) {
            return normalize(raw.substring(separator + 1))
        }
        return normalize(candidate.gapType().name)
    }

    private fun valueOrFallback(value: String?, fallback: String): String {
        val safeValue = value?.trim().orEmpty()
        return if (safeValue.isBlank()) fallback else safeValue
    }

    private fun normalize(value: String?): String {
        var normalized = value?.trim()?.lowercase(Locale.ROOT).orEmpty()
        normalized = normalized.replace(Regex("[^a-z0-9]+"), "_").replace(Regex("^_+|_+$"), "")
        return if (normalized.isBlank()) "patch" else normalized
    }
}
