package ro.ainpc.progression

import java.util.Locale

class ProgressionAnchorBinding(
    playerUuid: String?,
    templateId: String?,
    objectiveKey: String?,
    questCode: String?,
    objectiveType: String?,
    reference: String?,
    anchorType: String?,
    anchorId: String?,
    anchorLabel: String?,
    createdAt: Long,
    updatedAt: Long,
    status: String?
) {
    private val playerUuidValue: String = valueOrEmpty(playerUuid)
    private val templateIdValue: String = valueOrEmpty(templateId)
    private val objectiveKeyValue: String = valueOrEmpty(objectiveKey)
    private val questCodeValue: String = valueOrEmpty(questCode)
    private val objectiveTypeValue: String = valueOrEmpty(objectiveType)
    private val referenceValue: String = valueOrEmpty(reference)
    private val anchorTypeValue: String = valueOrEmpty(anchorType)
    private val anchorIdValue: String = valueOrEmpty(anchorId)
    private val anchorLabelValue: String = valueOrEmpty(anchorLabel)
    private val createdAtValue: Long = createdAt.coerceAtLeast(0L)
    private val updatedAtValue: Long = updatedAt.coerceAtLeast(0L)
    private val statusValue: String = valueOrEmpty(status)

    fun playerUuid(): String = playerUuidValue
    fun templateId(): String = templateIdValue
    fun objectiveKey(): String = objectiveKeyValue
    fun questCode(): String = questCodeValue
    fun objectiveType(): String = objectiveTypeValue
    fun reference(): String = referenceValue
    fun anchorType(): String = anchorTypeValue
    fun anchorId(): String = anchorIdValue
    fun anchorLabel(): String = anchorLabelValue
    fun createdAt(): Long = createdAtValue
    fun updatedAt(): Long = updatedAtValue
    fun status(): String = statusValue

    fun matchesAnchor(type: String?, id: String?): Boolean {
        val normalizedType = normalize(type)
        val normalizedId = normalize(id)
        return normalizedType.isNotBlank() &&
            normalizedId.isNotBlank() &&
            normalize(anchorTypeValue) == normalizedType &&
            normalize(anchorIdValue) == normalizedId
    }

    fun anchorSelector(): String {
        if (anchorTypeValue.isBlank() && anchorIdValue.isBlank()) {
            return ""
        }
        return "$anchorTypeValue:$anchorIdValue"
    }

    fun displayLabel(): String {
        if (anchorLabelValue.isNotBlank()) {
            return anchorLabelValue
        }
        if (referenceValue.isNotBlank()) {
            return referenceValue
        }
        return anchorIdValue
    }

    companion object {
        private fun normalize(value: String?): String = valueOrEmpty(value).lowercase(Locale.ROOT)

        private fun valueOrEmpty(value: String?): String = value?.trim().orEmpty()
    }
}
