package ro.ainpc.managers

data class FamilyMemberRecord(
    private val nameValue: String?,
    private val relationTypeValue: String?,
    private val aliveValue: Boolean,
    private val relatedNpcIdValue: Int?
) {
    fun name(): String? = nameValue
    fun relationType(): String? = relationTypeValue
    fun alive(): Boolean = aliveValue
    fun relatedNpcId(): Int? = relatedNpcIdValue
}
