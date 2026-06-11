package ro.ainpc.progression

import ro.ainpc.engine.ScenarioEngine
import java.util.function.Function

class ProgressionGuiSnapshot(
    private val handledValue: Boolean,
    playerName: String?,
    filterLabel: String?,
    summaryLines: List<String>?,
    currentEntries: List<ProgressionGuiEntry>?,
    archivedEntries: List<ProgressionGuiEntry>?,
    totalMatchingArchived: Long
) {
    private val playerNameValue: String = valueOrEmpty(playerName)
    private val filterLabelValue: String = valueOrEmpty(filterLabel)
    private val summaryLinesValue: List<String> = (summaryLines ?: emptyList()).toList()
    private val currentEntriesValue: List<ProgressionGuiEntry> = (currentEntries ?: emptyList()).toList()
    private val archivedEntriesValue: List<ProgressionGuiEntry> = (archivedEntries ?: emptyList()).toList()
    private val totalMatchingArchivedValue: Long = totalMatchingArchived.coerceAtLeast(0L)

    fun handled(): Boolean = handledValue
    fun playerName(): String = playerNameValue
    fun filterLabel(): String = filterLabelValue
    fun summaryLines(): List<String> = summaryLinesValue
    fun currentEntries(): List<ProgressionGuiEntry> = currentEntriesValue
    fun archivedEntries(): List<ProgressionGuiEntry> = archivedEntriesValue
    fun totalMatchingArchived(): Long = totalMatchingArchivedValue

    fun allEntries(): List<ProgressionGuiEntry> {
        val entries = ArrayList(currentEntriesValue)
        entries.addAll(archivedEntriesValue)
        return entries.toList()
    }

    companion object {
        @JvmStatic
        fun empty(): ProgressionGuiSnapshot =
            ProgressionGuiSnapshot(false, "", "", emptyList(), emptyList(), emptyList(), 0L)

        @JvmStatic
        fun fromQuestGuiSnapshot(
            snapshot: ScenarioEngine.QuestGuiSnapshot?,
            definitionResolver: Function<ScenarioEngine.QuestGuiEntry, ProgressionDefinition?>?
        ): ProgressionGuiSnapshot {
            if (snapshot == null || !snapshot.handled()) {
                return empty()
            }

            val safeResolver: Function<ScenarioEngine.QuestGuiEntry, ProgressionDefinition?> =
                definitionResolver ?: Function { _: ScenarioEngine.QuestGuiEntry -> null }

            return ProgressionGuiSnapshot(
                true,
                snapshot.playerName(),
                snapshot.filterLabel(),
                snapshot.summaryLines(),
                snapshot.currentEntries().map { entry ->
                    ProgressionGuiEntry.fromQuestGuiEntry(entry, safeResolver.apply(entry))
                },
                snapshot.archivedEntries().map { entry ->
                    ProgressionGuiEntry.fromQuestGuiEntry(entry, safeResolver.apply(entry))
                },
                snapshot.totalMatchingArchived()
            )
        }

        private fun valueOrEmpty(value: String?): String = value?.trim().orEmpty()
    }
}
