package ro.ainpc.managers

class ManagedVillagerAuditIssue(
    private val error: Boolean,
    message: String?,
) {
    private val message: String = message ?: ""

    fun error(): Boolean = error

    fun message(): String = message

    companion object {
        @JvmStatic
        fun error(message: String?): ManagedVillagerAuditIssue = ManagedVillagerAuditIssue(true, message)

        @JvmStatic
        fun warning(message: String?): ManagedVillagerAuditIssue = ManagedVillagerAuditIssue(false, message)
    }
}
