package ro.ainpc.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.progression.ProgressionGuiEntry

class QuestLogGuiPageTest {

    @Test
    fun groupsEntriesByMechanicPreservingFirstSeenGroupOrder() {
        val page = QuestLogGuiPage.fromEntries(
            listOf(
                entry("side_quests", "quest", "Questuri secundare", "Q06"),
                entry("local_bounties", "bounty", "Bounty locale", "B01"),
                entry("side_quests", "quest", "Questuri secundare", "Q07")
            ),
            0,
            10
        )

        assertEquals(5, page.totalRows())
        assertEquals(3, page.totalEntries())
        assertEquals(1, page.pageCount())
        assertFalse(page.hasPrevious())
        assertFalse(page.hasNext())

        assertTrue(page.rows()[0].header())
        assertEquals("side_quests", page.rows()[0].groupId())
        assertEquals("Questuri secundare", page.rows()[0].groupLabel())
        assertEquals(2, page.rows()[0].groupSize())
        assertFalse(page.rows()[1].header())
        assertEquals("Q06", page.rows()[1].entry()!!.code())
        assertFalse(page.rows()[2].header())
        assertEquals("Q07", page.rows()[2].entry()!!.code())

        assertTrue(page.rows()[3].header())
        assertEquals("local_bounties", page.rows()[3].groupId())
        assertEquals("B01", page.rows()[4].entry()!!.code())
    }

    @Test
    fun paginatesGroupedRowsAndClampsRequestedPage() {
        val page = QuestLogGuiPage.fromEntries(
            listOf(
                entry("side_quests", "quest", "Questuri secundare", "Q06"),
                entry("local_bounties", "bounty", "Bounty locale", "B01"),
                entry("village_rituals", "ritual", "Ritualuri de sat", "R01")
            ),
            99,
            2
        )

        assertEquals(6, page.totalRows())
        assertEquals(3, page.totalEntries())
        assertEquals(3, page.pageCount())
        assertEquals(2, page.pageIndex())
        assertEquals(3, page.displayPage())
        assertTrue(page.hasPrevious())
        assertFalse(page.hasNext())
        assertEquals(2, page.rows().size)
        assertEquals("village_rituals", page.rows()[0].groupId())
        assertEquals("R01", page.rows()[1].entry()!!.code())
    }

    @Test
    fun emptyEntriesProduceSingleEmptyPage() {
        val page = QuestLogGuiPage.fromEntries(emptyList(), -4, 0)

        assertEquals(0, page.totalRows())
        assertEquals(0, page.totalEntries())
        assertEquals(1, page.pageCount())
        assertEquals(0, page.pageIndex())
        assertEquals(1, page.displayPage())
        assertFalse(page.hasPrevious())
        assertFalse(page.hasNext())
        assertTrue(page.rows().isEmpty())
    }

    @Test
    fun avoidsOrphanGroupHeaderAtEndOfPage() {
        val page = QuestLogGuiPage.fromEntries(
            listOf(
                entry("side_quests", "quest", "Questuri secundare", "Q06"),
                entry("side_quests", "quest", "Questuri secundare", "Q07"),
                entry("local_bounties", "bounty", "Bounty locale", "B01")
            ),
            0,
            4
        )

        assertEquals(5, page.totalRows())
        assertEquals(2, page.pageCount())
        assertEquals(3, page.rows().size)
        assertEquals("side_quests", page.rows()[0].groupId())
        assertEquals("Q07", page.rows()[2].entry()!!.code())

        val secondPage = QuestLogGuiPage.fromEntries(
            listOf(
                entry("side_quests", "quest", "Questuri secundare", "Q06"),
                entry("side_quests", "quest", "Questuri secundare", "Q07"),
                entry("local_bounties", "bounty", "Bounty locale", "B01")
            ),
            1,
            4
        )
        assertTrue(secondPage.rows()[0].header())
        assertEquals("local_bounties", secondPage.rows()[0].groupId())
        assertEquals("B01", secondPage.rows()[1].entry()!!.code())
    }

    @Test
    fun repeatsGroupHeaderWhenLargeGroupSpansPages() {
        val page = QuestLogGuiPage.fromEntries(
            listOf(
                entry("side_quests", "quest", "Questuri secundare", "Q06"),
                entry("side_quests", "quest", "Questuri secundare", "Q07"),
                entry("side_quests", "quest", "Questuri secundare", "Q08"),
                entry("side_quests", "quest", "Questuri secundare", "Q09")
            ),
            1,
            3
        )

        assertEquals(5, page.totalRows())
        assertEquals(2, page.pageCount())
        assertTrue(page.rows()[0].header())
        assertEquals("side_quests", page.rows()[0].groupId())
        assertEquals("Q08", page.rows()[1].entry()!!.code())
        assertEquals("Q09", page.rows()[2].entry()!!.code())
    }

    private fun entry(mechanicId: String, kind: String, mechanicDisplay: String, code: String): ProgressionGuiEntry {
        return ProgressionGuiEntry(
            "$mechanicId:$code",
            "medieval:$mechanicId:$code",
            "medieval",
            mechanicId,
            kind,
            code,
            "medieval:$code",
            code,
            "$code title",
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
            listOf(),
            listOf(),
            listOf(),
            listOf(),
            listOf()
        )
    }
}
