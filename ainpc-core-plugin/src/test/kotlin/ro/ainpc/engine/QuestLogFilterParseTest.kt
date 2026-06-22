package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class QuestLogFilterParseTest {

    @Test
    fun parseAll() {
        assertEquals(QuestLogFilter.ALL, parseQuestLogFilter("all"))
        assertEquals(QuestLogFilter.ALL, parseQuestLogFilter("toate"))
    }

    @Test
    fun parseActive() {
        assertEquals(QuestLogFilter.ACTIVE, parseQuestLogFilter("active"))
        assertEquals(QuestLogFilter.ACTIVE, parseQuestLogFilter("activ"))
    }

    @Test
    fun parseCompleted() {
        assertEquals(QuestLogFilter.COMPLETED, parseQuestLogFilter("completed"))
        assertEquals(QuestLogFilter.COMPLETED, parseQuestLogFilter("completat"))
        assertEquals(QuestLogFilter.COMPLETED, parseQuestLogFilter("finalizat"))
    }

    @Test
    fun parseFailed() {
        assertEquals(QuestLogFilter.FAILED, parseQuestLogFilter("failed"))
        assertEquals(QuestLogFilter.FAILED, parseQuestLogFilter("esuat"))
    }

    @Test
    fun parseTracked() {
        assertEquals(QuestLogFilter.TRACKED, parseQuestLogFilter("tracked"))
        assertEquals(QuestLogFilter.TRACKED, parseQuestLogFilter("urmarit"))
    }

    @Test
    fun parseArchived() {
        assertEquals(QuestLogFilter.ARCHIVED, parseQuestLogFilter("archived"))
        assertEquals(QuestLogFilter.ARCHIVED, parseQuestLogFilter("arhivate"))
    }

    @Test
    fun parseQuestKind() {
        assertEquals(QuestLogFilter.QUEST_KIND, parseQuestLogFilter("quest"))
        assertEquals(QuestLogFilter.QUEST_KIND, parseQuestLogFilter("questuri"))
    }

    @Test
    fun parseContractKind() {
        assertEquals(QuestLogFilter.CONTRACT_KIND, parseQuestLogFilter("contract"))
        assertEquals(QuestLogFilter.CONTRACT_KIND, parseQuestLogFilter("contracte"))
    }

    @Test
    fun parseMainQuest() {
        assertEquals(QuestLogFilter.MAIN, parseQuestLogFilter("main"))
        assertEquals(QuestLogFilter.MAIN, parseQuestLogFilter("principal"))
    }

    @Test
    fun parseRitual() {
        assertEquals(QuestLogFilter.RITUAL_KIND, parseQuestLogFilter("ritual"))
        assertEquals(QuestLogFilter.RITUAL_KIND, parseQuestLogFilter("rituals"))
        assertEquals(QuestLogFilter.RITUAL_KIND, parseQuestLogFilter("ceremony"))
    }

    @Test
    fun parseUnknownReturnsSummary() {
        assertEquals(QuestLogFilter.SUMMARY, parseQuestLogFilter("unknown_filter_xyz"))
    }

    @Test
    fun parseBlankReturnsSummary() {
        assertEquals(QuestLogFilter.SUMMARY, parseQuestLogFilter(""))
        assertEquals(QuestLogFilter.SUMMARY, parseQuestLogFilter("  "))
    }

    @Test
    fun showsCurrentReturnsCorrectly() {
        assert(QuestLogFilter.ACTIVE.showsCurrent())
        assert(!QuestLogFilter.ACTIVE.showsArchived())
        assert(QuestLogFilter.ALL.showsCurrent())
        assert(QuestLogFilter.ALL.showsArchived())
        assert(!QuestLogFilter.COMPLETED.showsCurrent())
        assert(QuestLogFilter.COMPLETED.showsArchived())
    }
}
