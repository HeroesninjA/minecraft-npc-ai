package ro.ainpc.world.patch

class PatchPlannerResult(
    private val gapReport: GapReport?,
    candidates: List<PatchCandidate>?,
    patchPlans: List<PatchPlan>?,
    warnings: List<String>?,
    errors: List<String>?
) {
    private val candidates: List<PatchCandidate> = (candidates ?: emptyList()).toList()
    private val patchPlans: List<PatchPlan> = (patchPlans ?: emptyList()).toList()
    private val warnings: List<String> = (warnings ?: emptyList()).toList()
    private val errors: List<String> = (errors ?: emptyList()).toList()

    fun gapReport(): GapReport? = gapReport
    fun candidates(): List<PatchCandidate> = candidates
    fun patchPlans(): List<PatchPlan> = patchPlans
    fun warnings(): List<String> = warnings
    fun errors(): List<String> = errors

    fun success(): Boolean = errors.isEmpty()
}
