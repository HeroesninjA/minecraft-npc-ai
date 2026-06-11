package ro.ainpc.engine

enum class QuestDialogueContext(
    vararg dialogueKeys: String,
) {
    OFFER("offer", "available", "not_started"),
    OFFERED("offered", "pending", "acceptance"),
    ACCEPTED("accepted", "acceptance"),
    ACTIVE("active", "in_progress", "work"),
    READY("ready", "return", "turn_in"),
    COMPLETED("completed", "completion"),
    FAILED("failed", "abandoned"),
    UNAVAILABLE("unavailable", "locked");

    private val dialogueKeys: List<String> = listOf(*dialogueKeys)

    fun dialogueKeys(): List<String> = dialogueKeys
}

enum class QuestObjectiveState(
    private val id: String,
    private val displayName: String,
) {
    PENDING("pending", "In asteptare"),
    STARTED("started", "Inceput"),
    IN_PROGRESS("in_progress", "In progres"),
    COMPLETED("completed", "Completat"),
    FAILED("failed", "Esuat");

    fun id(): String = id

    fun displayName(): String = displayName
}
