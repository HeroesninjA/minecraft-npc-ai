package ro.ainpc.world.scan

import java.util.Collections

class SemanticVillageImportResult(
    private val regionIdValue: String,
    createdPlaceIds: List<String>,
    createdNodeIds: List<String>,
    warnings: List<String>,
    errors: List<String>
) {
    private val createdPlaceIdsValue = Collections.unmodifiableList(ArrayList(createdPlaceIds))
    private val createdNodeIdsValue = Collections.unmodifiableList(ArrayList(createdNodeIds))
    private val warningsValue = Collections.unmodifiableList(ArrayList(warnings))
    private val errorsValue = Collections.unmodifiableList(ArrayList(errors))

    fun regionId(): String = regionIdValue

    fun createdPlaceIds(): List<String> = createdPlaceIdsValue

    fun createdNodeIds(): List<String> = createdNodeIdsValue

    fun warnings(): List<String> = warningsValue

    fun errors(): List<String> = errorsValue

    fun success(): Boolean = errorsValue.isEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is SemanticVillageImportResult) {
            return false
        }

        return regionIdValue == other.regionIdValue &&
            createdPlaceIdsValue == other.createdPlaceIdsValue &&
            createdNodeIdsValue == other.createdNodeIdsValue &&
            warningsValue == other.warningsValue &&
            errorsValue == other.errorsValue
    }

    override fun hashCode(): Int {
        var result = regionIdValue.hashCode()
        result = 31 * result + createdPlaceIdsValue.hashCode()
        result = 31 * result + createdNodeIdsValue.hashCode()
        result = 31 * result + warningsValue.hashCode()
        result = 31 * result + errorsValue.hashCode()
        return result
    }

    override fun toString(): String =
        "SemanticVillageImportResult[regionId=$regionIdValue, createdPlaceIds=$createdPlaceIdsValue, " +
            "createdNodeIds=$createdNodeIdsValue, warnings=$warningsValue, errors=$errorsValue]"
}
