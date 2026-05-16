package ro.ainpc.spawn

class SettlementSpawnResult(
    success: Boolean,
    dryRun: Boolean,
    rolledBack: Boolean,
    allocations: List<HouseAllocation>?,
    householdResults: List<HouseholdSpawnResult>?,
    errors: List<String>?,
    warnings: List<String>?
) {
    private val successValue = success
    private val dryRunValue = dryRun
    private val rolledBackValue = rolledBack
    private val allocationsValue = java.util.List.copyOf(allocations ?: emptyList())
    private val householdResultsValue = java.util.List.copyOf(householdResults ?: emptyList())
    private val errorsValue = java.util.List.copyOf(errors ?: emptyList())
    private val warningsValue = java.util.List.copyOf(warnings ?: emptyList())

    fun success(): Boolean = successValue

    fun dryRun(): Boolean = dryRunValue

    fun rolledBack(): Boolean = rolledBackValue

    fun allocations(): List<HouseAllocation> = allocationsValue

    fun householdResults(): List<HouseholdSpawnResult> = householdResultsValue

    fun errors(): List<String> = errorsValue

    fun warnings(): List<String> = warningsValue

    fun successfulHouseholds(): Int = householdResultsValue.count { result -> result.success() }

    fun totalSpawnPlans(): Int = householdResultsValue.sumOf { result -> result.spawnPlans().size }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is SettlementSpawnResult) {
            return false
        }

        return successValue == other.successValue &&
            dryRunValue == other.dryRunValue &&
            rolledBackValue == other.rolledBackValue &&
            allocationsValue == other.allocationsValue &&
            householdResultsValue == other.householdResultsValue &&
            errorsValue == other.errorsValue &&
            warningsValue == other.warningsValue
    }

    override fun hashCode(): Int {
        var result = successValue.hashCode()
        result = 31 * result + dryRunValue.hashCode()
        result = 31 * result + rolledBackValue.hashCode()
        result = 31 * result + allocationsValue.hashCode()
        result = 31 * result + householdResultsValue.hashCode()
        result = 31 * result + errorsValue.hashCode()
        result = 31 * result + warningsValue.hashCode()
        return result
    }

    override fun toString(): String =
        "SettlementSpawnResult[success=$successValue, dryRun=$dryRunValue, rolledBack=$rolledBackValue, " +
            "allocations=$allocationsValue, householdResults=$householdResultsValue, errors=$errorsValue, warnings=$warningsValue]"

    companion object {
        @JvmStatic
        fun success(
            dryRun: Boolean,
            allocations: List<HouseAllocation>?,
            householdResults: List<HouseholdSpawnResult>?,
            warnings: List<String>?
        ): SettlementSpawnResult =
            SettlementSpawnResult(true, dryRun, false, allocations, householdResults, emptyList(), warnings)

        @JvmStatic
        fun failed(
            dryRun: Boolean,
            rolledBack: Boolean,
            allocations: List<HouseAllocation>?,
            householdResults: List<HouseholdSpawnResult>?,
            errors: List<String>?,
            warnings: List<String>?
        ): SettlementSpawnResult =
            SettlementSpawnResult(false, dryRun, rolledBack, allocations, householdResults, errors, warnings)
    }
}
