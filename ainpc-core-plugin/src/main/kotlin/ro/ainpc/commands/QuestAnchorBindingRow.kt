package ro.ainpc.commands

class QuestAnchorBindingRow(
    private val playerUuid: String?,
    private val templateId: String?,
    private val objectiveKey: String?,
    private val questCode: String?,
    private val objectiveType: String?,
    private val reference: String?,
    private val anchorType: String?,
    private val anchorId: String?,
    private val anchorLabel: String?,
    private val createdAt: Long,
    private val updatedAt: Long,
    private val status: String?,
) {
    fun playerUuid(): String? = playerUuid

    fun templateId(): String? = templateId

    fun objectiveKey(): String? = objectiveKey

    fun questCode(): String? = questCode

    fun objectiveType(): String? = objectiveType

    fun reference(): String? = reference

    fun anchorType(): String? = anchorType

    fun anchorId(): String? = anchorId

    fun anchorLabel(): String? = anchorLabel

    fun createdAt(): Long = createdAt

    fun updatedAt(): Long = updatedAt

    fun status(): String? = status
}
