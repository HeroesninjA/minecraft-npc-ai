package ro.ainpc.engine

class QuestInventoryCheck(
    private val complete: Boolean,
    private val missingItems: List<String>,
) {
    fun complete(): Boolean = complete

    fun missingItems(): List<String> = missingItems
}

class QuestObjectiveCheck(
    private val complete: Boolean,
    private val missingObjectives: List<String>,
) {
    fun complete(): Boolean = complete

    fun missingObjectives(): List<String> = missingObjectives
}
