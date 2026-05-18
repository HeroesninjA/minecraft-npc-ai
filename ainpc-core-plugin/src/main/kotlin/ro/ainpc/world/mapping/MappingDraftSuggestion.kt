package ro.ainpc.world.mapping

import java.util.Collections

class MappingDraftSuggestion(
    localId: String?,
    displayName: String?,
    typeId: String?,
    tags: List<String>?,
    metadata: Map<String, String>?,
    radius: Double,
    warnings: List<String>?
) {
    private val localIdValue = if (localId.isNullOrBlank()) "mapping_draft" else localId.trim()
    private val displayNameValue = if (displayName.isNullOrBlank()) localIdValue else displayName.trim()
    private val typeIdValue = if (typeId.isNullOrBlank()) "custom" else typeId.trim()
    private val tagsValue = Collections.unmodifiableList(ArrayList(tags ?: emptyList()))
    private val metadataValue = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
    private val radiusValue = if (radius <= 0.0) 2.5 else radius
    private val warningsValue = Collections.unmodifiableList(ArrayList(warnings ?: emptyList()))

    fun localId(): String = localIdValue

    fun displayName(): String = displayNameValue

    fun typeId(): String = typeIdValue

    fun tags(): List<String> = tagsValue

    fun metadata(): Map<String, String> = metadataValue

    fun radius(): Double = radiusValue

    fun warnings(): List<String> = warningsValue

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is MappingDraftSuggestion) {
            return false
        }

        return localIdValue == other.localIdValue &&
            displayNameValue == other.displayNameValue &&
            typeIdValue == other.typeIdValue &&
            tagsValue == other.tagsValue &&
            metadataValue == other.metadataValue &&
            radiusValue == other.radiusValue &&
            warningsValue == other.warningsValue
    }

    override fun hashCode(): Int {
        var result = localIdValue.hashCode()
        result = 31 * result + displayNameValue.hashCode()
        result = 31 * result + typeIdValue.hashCode()
        result = 31 * result + tagsValue.hashCode()
        result = 31 * result + metadataValue.hashCode()
        result = 31 * result + radiusValue.hashCode()
        result = 31 * result + warningsValue.hashCode()
        return result
    }

    override fun toString(): String =
        "MappingDraftSuggestion[localId=$localIdValue, displayName=$displayNameValue, " +
            "typeId=$typeIdValue, tags=$tagsValue, metadata=$metadataValue, radius=$radiusValue, warnings=$warningsValue]"
}
