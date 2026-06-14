package ro.ainpc.engine

class ScenarioRoleRule(
    val id: String,
    val description: String,
    val playerRole: Boolean,
    val optional: Boolean
) {
    var requiredProfessions: List<String> = emptyList()
    var preferredProfessions: List<String> = emptyList()
    var requiredTraits: List<String> = emptyList()
    var preferredTraits: List<String> = emptyList()

    fun hasHardRequirements(): Boolean = requiredProfessions.isNotEmpty() || requiredTraits.isNotEmpty()
}
