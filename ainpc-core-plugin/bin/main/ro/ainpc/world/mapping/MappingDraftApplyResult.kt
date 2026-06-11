package ro.ainpc.world.mapping

class MappingDraftApplyResult(
    kind: MappingDraftKind?,
    createdId: String?,
    message: String?
) {
    private val kindValue = kind
    private val createdIdValue = createdId
    private val messageValue = message

    fun kind(): MappingDraftKind? = kindValue

    fun createdId(): String? = createdIdValue

    fun message(): String? = messageValue

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is MappingDraftApplyResult) {
            return false
        }

        return kindValue == other.kindValue &&
            createdIdValue == other.createdIdValue &&
            messageValue == other.messageValue
    }

    override fun hashCode(): Int {
        var result = kindValue?.hashCode() ?: 0
        result = 31 * result + (createdIdValue?.hashCode() ?: 0)
        result = 31 * result + (messageValue?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "MappingDraftApplyResult[kind=$kindValue, createdId=$createdIdValue, message=$messageValue]"
}
