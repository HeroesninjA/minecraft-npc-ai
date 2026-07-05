package ro.ainpc.progression

import ro.ainpc.engine.*
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
    private val allEntriesValue: List<ProgressionGuiEntry> = (currentEntries ?: emptyList()) + (archivedEntries ?: emptyList())

    fun handled(): Boolean = handledValue
    fun playerName(): String = playerNameValue
    fun filterLabel(): String = filterLabelValue
    fun summaryLines(): List<String> = summaryLinesValue
    fun currentEntries(): List<ProgressionGuiEntry> = currentEntriesValue
    fun archivedEntries(): List<ProgressionGuiEntry> = archivedEntriesValue
    fun totalMatchingArchived(): Long = totalMatchingArchivedValue

    fun findEntry(selector: String?): ProgressionGuiEntry? {
        if (selector.isNullOrBlank()) {
            return currentEntriesValue.firstOrNull()
        }
        val normalized = selector.trim()
        return allEntries().firstOrNull { entry ->
            entry.selector().equals(normalized, ignoreCase = true) ||
                entry.commandSelector().equals(normalized, ignoreCase = true) ||
                entry.guiDetailSelector().equals(normalized, ignoreCase = true) ||
                entry.progressionId().equals(normalized, ignoreCase = true) ||
                entry.code().equals(normalized, ignoreCase = true) ||
                entry.templateId().equals(normalized, ignoreCase = true) ||
                entry.definitionId().equals(normalized, ignoreCase = true) ||
                "${entry.mechanicId()}:${entry.code()}".equals(normalized, ignoreCase = true) ||
                "${entry.mechanicId()}:${entry.definitionId()}".equals(normalized, ignoreCase = true) ||
                "${entry.kind()}:${entry.code()}".equals(normalized, ignoreCase = true) ||
                "${entry.kind()}:${entry.definitionId()}".equals(normalized, ignoreCase = true)
        }
    }

    fun allEntries(): List<ProgressionGuiEntry> = allEntriesValue

    companion object {
        @JvmStatic
        fun empty(): ProgressionGuiSnapshot =
            ProgressionGuiSnapshot(false, "", "", emptyList(), emptyList(), emptyList(), 0L)

        @JvmStatic
        fun fromQuestGuiSnapshot(
            snapshot: QuestGuiSnapshot?,
            definitionResolver: Function<QuestGuiEntry, ProgressionDefinition?>?
        ): ProgressionGuiSnapshot {
            if (snapshot == null || !snapshot.handled) {
                return empty()
            }

            val safeResolver: Function<QuestGuiEntry, ProgressionDefinition?> =
                definitionResolver ?: Function { _: QuestGuiEntry -> null }

            return ProgressionGuiSnapshot(
                true,
                snapshot.playerName,
                snapshot.filterLabel,
                snapshot.summaryLines,
                snapshot.currentEntries.map { entry ->
                    ProgressionGuiEntry.fromQuestGuiEntry(entry, safeResolver.apply(entry))
                },
                snapshot.archivedEntries.map { entry ->
                    ProgressionGuiEntry.fromQuestGuiEntry(entry, safeResolver.apply(entry))
                },
                snapshot.totalMatchingArchived
            )
        }

        private fun valueOrEmpty(value: String?): String = value?.trim().orEmpty()
    }
}
