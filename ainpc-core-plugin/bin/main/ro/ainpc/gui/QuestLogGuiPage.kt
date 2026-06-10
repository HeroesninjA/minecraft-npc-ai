package ro.ainpc.gui

import ro.ainpc.progression.ProgressionGuiEntry
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.Locale

class QuestLogGuiPage(
    rows: List<Row>?,
    pageIndex: Int,
    pageCount: Int,
    totalRows: Int,
    totalEntries: Int
) {
    private val rowsValue = java.util.List.copyOf(rows ?: emptyList())
    private val pageIndexValue = maxOf(0, pageIndex)
    private val pageCountValue = maxOf(1, pageCount)
    private val totalRowsValue = maxOf(0, totalRows)
    private val totalEntriesValue = maxOf(0, totalEntries)

    fun rows(): List<Row> = rowsValue

    fun pageIndex(): Int = pageIndexValue

    fun pageCount(): Int = pageCountValue

    fun totalRows(): Int = totalRowsValue

    fun totalEntries(): Int = totalEntriesValue

    fun hasPrevious(): Boolean = pageIndexValue > 0

    fun hasNext(): Boolean = pageIndexValue + 1 < pageCountValue

    fun displayPage(): Int = pageIndexValue + 1

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is QuestLogGuiPage) {
            return false
        }

        return rowsValue == other.rowsValue &&
            pageIndexValue == other.pageIndexValue &&
            pageCountValue == other.pageCountValue &&
            totalRowsValue == other.totalRowsValue &&
            totalEntriesValue == other.totalEntriesValue
    }

    override fun hashCode(): Int {
        var result = rowsValue.hashCode()
        result = 31 * result + pageIndexValue
        result = 31 * result + pageCountValue
        result = 31 * result + totalRowsValue
        result = 31 * result + totalEntriesValue
        return result
    }

    override fun toString(): String =
        "QuestLogGuiPage[rows=$rowsValue, pageIndex=$pageIndexValue, pageCount=$pageCountValue, " +
            "totalRows=$totalRowsValue, totalEntries=$totalEntriesValue]"

    private class Group(
        val id: String,
        val label: String
    ) {
        val entries: MutableList<ProgressionGuiEntry> = ArrayList()
    }

    class Row(
        header: Boolean,
        groupId: String?,
        groupLabel: String?,
        groupSize: Int,
        entry: ProgressionGuiEntry?
    ) {
        private val headerValue = header
        private val groupIdValue = groupId?.trim().orEmpty()
        private val groupLabelValue = groupLabel?.trim().orEmpty()
        private val groupSizeValue = maxOf(0, groupSize)
        private val entryValue = entry

        fun header(): Boolean = headerValue

        fun groupId(): String = groupIdValue

        fun groupLabel(): String = groupLabelValue

        fun groupSize(): Int = groupSizeValue

        fun entry(): ProgressionGuiEntry? = entryValue

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other !is Row) {
                return false
            }

            return headerValue == other.headerValue &&
                groupIdValue == other.groupIdValue &&
                groupLabelValue == other.groupLabelValue &&
                groupSizeValue == other.groupSizeValue &&
                entryValue == other.entryValue
        }

        override fun hashCode(): Int {
            var result = headerValue.hashCode()
            result = 31 * result + groupIdValue.hashCode()
            result = 31 * result + groupLabelValue.hashCode()
            result = 31 * result + groupSizeValue
            result = 31 * result + (entryValue?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String =
            "Row[header=$headerValue, groupId=$groupIdValue, groupLabel=$groupLabelValue, " +
                "groupSize=$groupSizeValue, entry=$entryValue]"

        companion object {
            @JvmStatic
            fun header(groupId: String?, groupLabel: String?, groupSize: Int): Row =
                Row(true, groupId, groupLabel, groupSize, null)

            @JvmStatic
            fun entry(groupId: String?, groupLabel: String?, entry: ProgressionGuiEntry?): Row =
                Row(false, groupId, groupLabel, 0, entry)
        }
    }

    companion object {
        @JvmStatic
        fun fromEntries(entries: List<ProgressionGuiEntry>?, requestedPage: Int, pageSize: Int): QuestLogGuiPage {
            val safeEntries = java.util.List.copyOf(entries ?: emptyList())
            val groups = groupEntries(safeEntries)
            val groupedRows = rowsFromGroups(groups)
            val safePageSize = maxOf(1, pageSize)
            val pages = paginate(groups, groupedRows, safePageSize)
            val pageCount = pages.size
            val pageIndex = minOf(maxOf(0, requestedPage), pageCount - 1)

            return QuestLogGuiPage(
                pages[pageIndex],
                pageIndex,
                pageCount,
                groupedRows.size,
                safeEntries.size
            )
        }

        private fun groupEntries(entries: List<ProgressionGuiEntry>): List<Group> {
            val groups = LinkedHashMap<String, Group>()
            for (entry in entries) {
                val group = groupFor(entry)
                groups.computeIfAbsent(group.id) { group }.entries.add(entry)
            }
            return java.util.List.copyOf(groups.values)
        }

        private fun rowsFromGroups(groups: List<Group>): List<Row> {
            val rows = ArrayList<Row>()
            for (group in groups) {
                rows.add(Row.header(group.id, group.label, group.entries.size))
                for (entry in group.entries) {
                    rows.add(Row.entry(group.id, group.label, entry))
                }
            }
            return rows
        }

        private fun paginate(groups: List<Group>, groupedRows: List<Row>, pageSize: Int): List<List<Row>> {
            if (groupedRows.isEmpty()) {
                return listOf(emptyList())
            }
            if (pageSize < 2) {
                return paginateFlat(groupedRows, pageSize)
            }

            val pages = ArrayList<List<Row>>()
            val currentPage = ArrayList<Row>()
            for (group in groups) {
                if (group.entries.isEmpty()) {
                    continue
                }
                val header = Row.header(group.id, group.label, group.entries.size)
                if (currentPage.isNotEmpty() && currentPage.size > pageSize - 2) {
                    pages.add(java.util.List.copyOf(currentPage))
                    currentPage.clear()
                }
                currentPage.add(header)

                for (entry in group.entries) {
                    if (currentPage.size >= pageSize) {
                        pages.add(java.util.List.copyOf(currentPage))
                        currentPage.clear()
                        currentPage.add(header)
                    }
                    currentPage.add(Row.entry(group.id, group.label, entry))
                }
            }

            if (currentPage.isNotEmpty()) {
                pages.add(java.util.List.copyOf(currentPage))
            }
            return if (pages.isEmpty()) listOf(emptyList()) else java.util.List.copyOf(pages)
        }

        private fun paginateFlat(groupedRows: List<Row>, pageSize: Int): List<List<Row>> {
            val safePageSize = maxOf(1, pageSize)
            val pages = ArrayList<List<Row>>()
            var fromIndex = 0
            while (fromIndex < groupedRows.size) {
                val toIndex = minOf(groupedRows.size, fromIndex + safePageSize)
                pages.add(java.util.List.copyOf(groupedRows.subList(fromIndex, toIndex)))
                fromIndex += safePageSize
            }
            return if (pages.isEmpty()) listOf(emptyList()) else java.util.List.copyOf(pages)
        }

        private fun groupFor(entry: ProgressionGuiEntry?): Group {
            val id = firstNonBlank(
                entry?.mechanicId(),
                entry?.kind(),
                "unknown"
            )
            val label = firstNonBlank(
                entry?.mechanicDisplay(),
                entry?.label(),
                entry?.pluralLabel(),
                entry?.kind(),
                "Progresii"
            )
            return Group(normalize(id), label)
        }

        private fun firstNonBlank(vararg values: String?): String {
            for (value in values) {
                if (!value.isNullOrBlank()) {
                    return value.trim()
                }
            }
            return ""
        }

        private fun normalize(value: String?): String {
            val normalized = value?.trim()?.lowercase(Locale.ROOT).orEmpty()
            if (normalized.isBlank()) {
                return "unknown"
            }
            return normalized
                .replace('-', '_')
                .replace(Regex("[^\\p{L}\\p{Nd}]+"), "_")
                .replace(Regex("^_+|_+$"), "")
        }
    }
}
