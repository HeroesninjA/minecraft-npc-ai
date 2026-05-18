package ro.ainpc.spawn

class HouseholdSpawnResult(
    success: Boolean,
    dryRun: Boolean,
    rolledBack: Boolean,
    spawnPlans: List<NpcSpawnPlan>?,
    spawnResults: List<NpcSpawnResult>?,
    familyBindingResult: FamilyBindingResult?,
    errors: List<String>?,
    warnings: List<String>?
) {
    private val successValue = success
    private val dryRunValue = dryRun
    private val rolledBackValue = rolledBack
    private val spawnPlansValue = java.util.List.copyOf(spawnPlans ?: emptyList())
    private val spawnResultsValue = java.util.List.copyOf(spawnResults ?: emptyList())
    private val familyBindingResultValue = familyBindingResult
    private val errorsValue = java.util.List.copyOf(errors ?: emptyList())
    private val warningsValue = java.util.List.copyOf(warnings ?: emptyList())

    fun success(): Boolean = successValue

    fun dryRun(): Boolean = dryRunValue

    fun rolledBack(): Boolean = rolledBackValue

    fun spawnPlans(): List<NpcSpawnPlan> = spawnPlansValue

    fun spawnResults(): List<NpcSpawnResult> = spawnResultsValue

    fun familyBindingResult(): FamilyBindingResult? = familyBindingResultValue

    fun errors(): List<String> = errorsValue

    fun warnings(): List<String> = warningsValue

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is HouseholdSpawnResult) {
            return false
        }

        return successValue == other.successValue &&
            dryRunValue == other.dryRunValue &&
            rolledBackValue == other.rolledBackValue &&
            spawnPlansValue == other.spawnPlansValue &&
            spawnResultsValue == other.spawnResultsValue &&
            familyBindingResultValue == other.familyBindingResultValue &&
            errorsValue == other.errorsValue &&
            warningsValue == other.warningsValue
    }

    override fun hashCode(): Int {
        var result = successValue.hashCode()
        result = 31 * result + dryRunValue.hashCode()
        result = 31 * result + rolledBackValue.hashCode()
        result = 31 * result + spawnPlansValue.hashCode()
        result = 31 * result + spawnResultsValue.hashCode()
        result = 31 * result + (familyBindingResultValue?.hashCode() ?: 0)
        result = 31 * result + errorsValue.hashCode()
        result = 31 * result + warningsValue.hashCode()
        return result
    }

    override fun toString(): String =
        "HouseholdSpawnResult[success=$successValue, dryRun=$dryRunValue, rolledBack=$rolledBackValue, " +
            "spawnPlans=$spawnPlansValue, spawnResults=$spawnResultsValue, " +
            "familyBindingResult=$familyBindingResultValue, errors=$errorsValue, warnings=$warningsValue]"

    companion object {
        @JvmStatic
        fun dryRunSuccess(spawnPlans: List<NpcSpawnPlan>?, warnings: List<String>?): HouseholdSpawnResult =
            HouseholdSpawnResult(true, true, false, spawnPlans, emptyList(), null, emptyList(), warnings)

        @JvmStatic
        fun failed(
            dryRun: Boolean,
            rolledBack: Boolean,
            spawnPlans: List<NpcSpawnPlan>?,
            spawnResults: List<NpcSpawnResult>?,
            familyBindingResult: FamilyBindingResult?,
            errors: List<String>?,
            warnings: List<String>?
        ): HouseholdSpawnResult =
            HouseholdSpawnResult(false, dryRun, rolledBack, spawnPlans, spawnResults, familyBindingResult, errors, warnings)

        @JvmStatic
        fun success(
            spawnPlans: List<NpcSpawnPlan>?,
            spawnResults: List<NpcSpawnResult>?,
            familyBindingResult: FamilyBindingResult?,
            warnings: List<String>?
        ): HouseholdSpawnResult =
            HouseholdSpawnResult(true, false, false, spawnPlans, spawnResults, familyBindingResult, emptyList(), warnings)
    }
}
