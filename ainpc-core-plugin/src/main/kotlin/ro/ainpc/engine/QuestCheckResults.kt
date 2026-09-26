package ro.ainpc.engine

/**
 * Questinventorycheck.
 */
class QuestInventoryCheck(
    private val complete: Boolean,
    private val missingItems: List<String>,
) {
    fun complete(): Boolean = complete

    fun missingItems(): List<String> = missingItems
}

/**
 * Questobjectivecheck.
 */
class QuestObjectiveCheck(
    private val complete: Boolean,
    private val missingObjectives: List<String>,
) {
    fun complete(): Boolean = complete

    fun missingObjectives(): List<String> = missingObjectives
}
