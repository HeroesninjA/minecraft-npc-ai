package ro.ainpc.api.settlement

data class SettlementPlan(
    val planId: String,
    val version: Int = 1,
    val source: SettlementPlanSource = SettlementPlanSource.MANUAL,
    val status: SettlementPlanStatus = SettlementPlanStatus.DRAFT,
    val seed: Long = 0L,
    val themeId: String = "",
    val regionPlan: RegionPlan? = null,
    val buildingPlans: List<BuildingPlan> = emptyList(),
    val nodePlans: List<NodePlan> = emptyList(),
    val householdPlans: List<HouseholdPlan> = emptyList(),
    val spawnPlanRefs: List<String> = emptyList(),
    val patchPlanRefs: List<String> = emptyList(),
    val validationReport: SettlementValidationReport = SettlementValidationReport(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun buildingCount(): Int = buildingPlans.size
    fun nodeCount(): Int = nodePlans.size
    fun householdCount(): Int = householdPlans.size

    fun isValid(): Boolean = status == SettlementPlanStatus.VALIDATED || status == SettlementPlanStatus.COMMITTED
    fun isCommittable(): Boolean = status == SettlementPlanStatus.VALIDATED
    fun isDraft(): Boolean = status == SettlementPlanStatus.DRAFT
}

data class SettlementValidationReport(
    val valid: Boolean = true,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val infos: List<String> = emptyList()
)
