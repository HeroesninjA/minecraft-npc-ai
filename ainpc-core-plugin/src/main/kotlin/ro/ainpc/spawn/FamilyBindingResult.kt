package ro.ainpc.spawn

class FamilyBindingResult(
    success: Boolean,
    familyId: String?,
    relationsCreated: Int,
    errors: List<String>?,
    warnings: List<String>?
) {
    private val successValue = success
    private val familyIdValue = familyId?.trim().orEmpty()
    private val relationsCreatedValue = relationsCreated
    private val errorsValue = java.util.List.copyOf(errors ?: emptyList())
    private val warningsValue = java.util.List.copyOf(warnings ?: emptyList())

    fun success(): Boolean = successValue

    fun familyId(): String = familyIdValue

    fun relationsCreated(): Int = relationsCreatedValue

    fun errors(): List<String> = errorsValue

    fun warnings(): List<String> = warningsValue

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is FamilyBindingResult) {
            return false
        }

        return successValue == other.successValue &&
            familyIdValue == other.familyIdValue &&
            relationsCreatedValue == other.relationsCreatedValue &&
            errorsValue == other.errorsValue &&
            warningsValue == other.warningsValue
    }

    override fun hashCode(): Int {
        var result = successValue.hashCode()
        result = 31 * result + familyIdValue.hashCode()
        result = 31 * result + relationsCreatedValue
        result = 31 * result + errorsValue.hashCode()
        result = 31 * result + warningsValue.hashCode()
        return result
    }

    override fun toString(): String =
        "FamilyBindingResult[success=$successValue, familyId=$familyIdValue, " +
            "relationsCreated=$relationsCreatedValue, errors=$errorsValue, warnings=$warningsValue]"
}
