package ro.ainpc.engine

data class QuestGuiSnapshot @JvmOverloads constructor(
    val handled: Boolean = false,
    val playerName: String = "",
    val filterLabel: String = "",
    val summaryLines: List<String> = emptyList(),
    val currentEntries: List<QuestGuiEntry> = emptyList(),
    val archivedEntries: List<QuestGuiEntry> = emptyList(),
    val totalMatchingArchived: Long = 0L
) {
    fun allEntries(): List<QuestGuiEntry> = (currentEntries + archivedEntries).toList()

    companion object {
        @JvmStatic
        fun empty() = QuestGuiSnapshot()
    }
}
