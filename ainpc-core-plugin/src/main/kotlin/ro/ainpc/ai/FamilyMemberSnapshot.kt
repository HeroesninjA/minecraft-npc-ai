package ro.ainpc.ai

data class FamilyMemberSnapshot(
    val name: String,
    val relationType: String,
    val alive: Boolean
) {
    fun name(): String = name
    fun relationType(): String = relationType
    fun alive(): Boolean = alive
}
