package ro.ainpc.gui;

import ro.ainpc.progression.ProgressionGuiEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record QuestLogGuiPage(
    List<Row> rows,
    int pageIndex,
    int pageCount,
    int totalRows,
    int totalEntries
) {
    public QuestLogGuiPage {
        rows = List.copyOf(rows != null ? rows : List.of());
        pageIndex = Math.max(0, pageIndex);
        pageCount = Math.max(1, pageCount);
        totalRows = Math.max(0, totalRows);
        totalEntries = Math.max(0, totalEntries);
    }

    public static QuestLogGuiPage fromEntries(List<ProgressionGuiEntry> entries, int requestedPage, int pageSize) {
        List<ProgressionGuiEntry> safeEntries = List.copyOf(entries != null ? entries : List.of());
        List<Group> groups = groupEntries(safeEntries);
        List<Row> groupedRows = rowsFromGroups(groups);
        int safePageSize = Math.max(1, pageSize);
        List<List<Row>> pages = paginate(groups, groupedRows, safePageSize);
        int pageCount = pages.size();
        int pageIndex = Math.min(Math.max(0, requestedPage), pageCount - 1);

        return new QuestLogGuiPage(
            pages.get(pageIndex),
            pageIndex,
            pageCount,
            groupedRows.size(),
            safeEntries.size()
        );
    }

    public boolean hasPrevious() {
        return pageIndex > 0;
    }

    public boolean hasNext() {
        return pageIndex + 1 < pageCount;
    }

    public int displayPage() {
        return pageIndex + 1;
    }

    private static List<Group> groupEntries(List<ProgressionGuiEntry> entries) {
        Map<String, Group> groups = new LinkedHashMap<>();
        for (ProgressionGuiEntry entry : entries) {
            Group group = groupFor(entry);
            groups.computeIfAbsent(group.id(), ignored -> group).entries().add(entry);
        }
        return List.copyOf(groups.values());
    }

    private static List<Row> rowsFromGroups(List<Group> groups) {
        List<Row> rows = new ArrayList<>();
        for (Group group : groups) {
            rows.add(Row.header(group.id(), group.label(), group.entries().size()));
            for (ProgressionGuiEntry entry : group.entries()) {
                rows.add(Row.entry(group.id(), group.label(), entry));
            }
        }
        return rows;
    }

    private static List<List<Row>> paginate(List<Group> groups, List<Row> groupedRows, int pageSize) {
        if (groupedRows.isEmpty()) {
            return List.of(List.of());
        }
        if (pageSize < 2) {
            return paginateFlat(groupedRows, pageSize);
        }

        List<List<Row>> pages = new ArrayList<>();
        List<Row> currentPage = new ArrayList<>();
        for (Group group : groups) {
            if (group.entries().isEmpty()) {
                continue;
            }
            Row header = Row.header(group.id(), group.label(), group.entries().size());
            if (!currentPage.isEmpty() && currentPage.size() > pageSize - 2) {
                pages.add(List.copyOf(currentPage));
                currentPage.clear();
            }
            currentPage.add(header);

            for (ProgressionGuiEntry entry : group.entries()) {
                if (currentPage.size() >= pageSize) {
                    pages.add(List.copyOf(currentPage));
                    currentPage.clear();
                    currentPage.add(header);
                }
                currentPage.add(Row.entry(group.id(), group.label(), entry));
            }
        }

        if (!currentPage.isEmpty()) {
            pages.add(List.copyOf(currentPage));
        }
        return pages.isEmpty() ? List.of(List.of()) : List.copyOf(pages);
    }

    private static List<List<Row>> paginateFlat(List<Row> groupedRows, int pageSize) {
        int safePageSize = Math.max(1, pageSize);
        List<List<Row>> pages = new ArrayList<>();
        for (int fromIndex = 0; fromIndex < groupedRows.size(); fromIndex += safePageSize) {
            int toIndex = Math.min(groupedRows.size(), fromIndex + safePageSize);
            pages.add(List.copyOf(groupedRows.subList(fromIndex, toIndex)));
        }
        return pages.isEmpty() ? List.of(List.of()) : List.copyOf(pages);
    }

    private static Group groupFor(ProgressionGuiEntry entry) {
        String id = firstNonBlank(
            entry != null ? entry.mechanicId() : "",
            entry != null ? entry.kind() : "",
            "unknown"
        );
        String label = firstNonBlank(
            entry != null ? entry.mechanicDisplay() : "",
            entry != null ? entry.label() : "",
            entry != null ? entry.pluralLabel() : "",
            entry != null ? entry.kind() : "",
            "Progresii"
        );
        return new Group(normalize(id), label);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "unknown";
        }
        return normalized
            .replace('-', '_')
            .replaceAll("[^\\p{L}\\p{Nd}]+", "_")
            .replaceAll("^_+|_+$", "");
    }

    private record Group(String id, String label, List<ProgressionGuiEntry> entries) {
        private Group(String id, String label) {
            this(id, label, new ArrayList<>());
        }
    }

    public record Row(
        boolean header,
        String groupId,
        String groupLabel,
        int groupSize,
        ProgressionGuiEntry entry
    ) {
        public Row {
            groupId = groupId == null ? "" : groupId.trim();
            groupLabel = groupLabel == null ? "" : groupLabel.trim();
            groupSize = Math.max(0, groupSize);
        }

        public static Row header(String groupId, String groupLabel, int groupSize) {
            return new Row(true, groupId, groupLabel, groupSize, null);
        }

        public static Row entry(String groupId, String groupLabel, ProgressionGuiEntry entry) {
            return new Row(false, groupId, groupLabel, 0, entry);
        }
    }
}
