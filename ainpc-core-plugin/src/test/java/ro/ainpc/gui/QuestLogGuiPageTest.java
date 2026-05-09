package ro.ainpc.gui;

import org.junit.jupiter.api.Test;
import ro.ainpc.progression.ProgressionGuiEntry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestLogGuiPageTest {

    @Test
    void groupsEntriesByMechanicPreservingFirstSeenGroupOrder() {
        QuestLogGuiPage page = QuestLogGuiPage.fromEntries(List.of(
            entry("side_quests", "quest", "Questuri secundare", "Q06"),
            entry("local_bounties", "bounty", "Bounty locale", "B01"),
            entry("side_quests", "quest", "Questuri secundare", "Q07")
        ), 0, 10);

        assertEquals(5, page.totalRows());
        assertEquals(3, page.totalEntries());
        assertEquals(1, page.pageCount());
        assertFalse(page.hasPrevious());
        assertFalse(page.hasNext());

        assertTrue(page.rows().get(0).header());
        assertEquals("side_quests", page.rows().get(0).groupId());
        assertEquals("Questuri secundare", page.rows().get(0).groupLabel());
        assertEquals(2, page.rows().get(0).groupSize());
        assertFalse(page.rows().get(1).header());
        assertEquals("Q06", page.rows().get(1).entry().code());
        assertFalse(page.rows().get(2).header());
        assertEquals("Q07", page.rows().get(2).entry().code());

        assertTrue(page.rows().get(3).header());
        assertEquals("local_bounties", page.rows().get(3).groupId());
        assertEquals("B01", page.rows().get(4).entry().code());
    }

    @Test
    void paginatesGroupedRowsAndClampsRequestedPage() {
        QuestLogGuiPage page = QuestLogGuiPage.fromEntries(List.of(
            entry("side_quests", "quest", "Questuri secundare", "Q06"),
            entry("local_bounties", "bounty", "Bounty locale", "B01"),
            entry("village_rituals", "ritual", "Ritualuri de sat", "R01")
        ), 99, 2);

        assertEquals(6, page.totalRows());
        assertEquals(3, page.totalEntries());
        assertEquals(3, page.pageCount());
        assertEquals(2, page.pageIndex());
        assertEquals(3, page.displayPage());
        assertTrue(page.hasPrevious());
        assertFalse(page.hasNext());
        assertEquals(2, page.rows().size());
        assertEquals("village_rituals", page.rows().get(0).groupId());
        assertEquals("R01", page.rows().get(1).entry().code());
    }

    @Test
    void emptyEntriesProduceSingleEmptyPage() {
        QuestLogGuiPage page = QuestLogGuiPage.fromEntries(List.of(), -4, 0);

        assertEquals(0, page.totalRows());
        assertEquals(0, page.totalEntries());
        assertEquals(1, page.pageCount());
        assertEquals(0, page.pageIndex());
        assertEquals(1, page.displayPage());
        assertFalse(page.hasPrevious());
        assertFalse(page.hasNext());
        assertTrue(page.rows().isEmpty());
    }

    @Test
    void avoidsOrphanGroupHeaderAtEndOfPage() {
        QuestLogGuiPage page = QuestLogGuiPage.fromEntries(List.of(
            entry("side_quests", "quest", "Questuri secundare", "Q06"),
            entry("side_quests", "quest", "Questuri secundare", "Q07"),
            entry("local_bounties", "bounty", "Bounty locale", "B01")
        ), 0, 4);

        assertEquals(5, page.totalRows());
        assertEquals(2, page.pageCount());
        assertEquals(3, page.rows().size());
        assertEquals("side_quests", page.rows().get(0).groupId());
        assertEquals("Q07", page.rows().get(2).entry().code());

        QuestLogGuiPage secondPage = QuestLogGuiPage.fromEntries(List.of(
            entry("side_quests", "quest", "Questuri secundare", "Q06"),
            entry("side_quests", "quest", "Questuri secundare", "Q07"),
            entry("local_bounties", "bounty", "Bounty locale", "B01")
        ), 1, 4);
        assertTrue(secondPage.rows().get(0).header());
        assertEquals("local_bounties", secondPage.rows().get(0).groupId());
        assertEquals("B01", secondPage.rows().get(1).entry().code());
    }

    @Test
    void repeatsGroupHeaderWhenLargeGroupSpansPages() {
        QuestLogGuiPage page = QuestLogGuiPage.fromEntries(List.of(
            entry("side_quests", "quest", "Questuri secundare", "Q06"),
            entry("side_quests", "quest", "Questuri secundare", "Q07"),
            entry("side_quests", "quest", "Questuri secundare", "Q08"),
            entry("side_quests", "quest", "Questuri secundare", "Q09")
        ), 1, 3);

        assertEquals(5, page.totalRows());
        assertEquals(2, page.pageCount());
        assertTrue(page.rows().get(0).header());
        assertEquals("side_quests", page.rows().get(0).groupId());
        assertEquals("Q08", page.rows().get(1).entry().code());
        assertEquals("Q09", page.rows().get(2).entry().code());
    }

    private static ProgressionGuiEntry entry(String mechanicId, String kind, String mechanicDisplay, String code) {
        return new ProgressionGuiEntry(
            mechanicId + ":" + code,
            "medieval:" + mechanicId + ":" + code,
            "medieval",
            mechanicId,
            kind,
            code,
            "medieval:" + code,
            code,
            code + " title",
            "activ",
            "side",
            mechanicDisplay,
            mechanicDisplay,
            kind,
            mechanicDisplay,
            false,
            true,
            true,
            false,
            false,
            false,
            "STAGE",
            "Stage",
            1L,
            "NPC",
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );
    }
}
