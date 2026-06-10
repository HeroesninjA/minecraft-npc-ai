package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QuestLogFilterTest {
    @Test
    fun parseReturnsAllForAllAliases() {
        assertEquals(QuestLogFilter.ALL, parseQuestLogFilter("all"))
        assertEquals(QuestLogFilter.ALL, parseQuestLogFilter("toate"))
    }

    @Test
    fun parseReturnsSummaryForNullFilter() {
        assertEquals(QuestLogFilter.SUMMARY, parseQuestLogFilter(null))
    }

    @Test
    fun parseReturnsSummaryForEmptyFilter() {
        assertEquals(QuestLogFilter.SUMMARY, parseQuestLogFilter(""))
    }

    @Test
    fun parseReturnsSummaryForUnknownFilter() {
        assertEquals(QuestLogFilter.SUMMARY, parseQuestLogFilter("unknown_filter_value"))
    }

    @Test
    fun parseIsCaseInsensitive() {
        assertEquals(QuestLogFilter.ACTIVE, parseQuestLogFilter("ACTIVE"))
        assertEquals(QuestLogFilter.ACTIVE, parseQuestLogFilter("Active"))
        assertEquals(QuestLogFilter.CURRENT, parseQuestLogFilter("CURRENT"))
        assertEquals(QuestLogFilter.COMPLETED, parseQuestLogFilter("COMPLETED"))
        assertEquals(QuestLogFilter.FAILED, parseQuestLogFilter("FAILED"))
    }

    @Test
    fun parseReturnsCurrentForCurrentAliases() {
        assertEquals(QuestLogFilter.CURRENT, parseQuestLogFilter("current"))
        assertEquals(QuestLogFilter.CURRENT, parseQuestLogFilter("curent"))
        assertEquals(QuestLogFilter.CURRENT, parseQuestLogFilter("curente"))
    }

    @Test
    fun parseReturnsActiveForActiveAliases() {
        assertEquals(QuestLogFilter.ACTIVE, parseQuestLogFilter("active"))
        assertEquals(QuestLogFilter.ACTIVE, parseQuestLogFilter("activ"))
    }

    @Test
    fun parseReturnsOfferedForOfferedAliases() {
        assertEquals(QuestLogFilter.OFFERED, parseQuestLogFilter("offered"))
        assertEquals(QuestLogFilter.OFFERED, parseQuestLogFilter("oferit"))
        assertEquals(QuestLogFilter.OFFERED, parseQuestLogFilter("oferite"))
    }

    @Test
    fun parseReturnsTrackedForTrackedAliases() {
        assertEquals(QuestLogFilter.TRACKED, parseQuestLogFilter("tracked"))
        assertEquals(QuestLogFilter.TRACKED, parseQuestLogFilter("urmarit"))
    }

    @Test
    fun parseReturnsQuestKindForQuestAliases() {
        assertEquals(QuestLogFilter.QUEST_KIND, parseQuestLogFilter("quest"))
        assertEquals(QuestLogFilter.QUEST_KIND, parseQuestLogFilter("questuri"))
    }

    @Test
    fun parseReturnsMainForMainAliases() {
        assertEquals(QuestLogFilter.MAIN, parseQuestLogFilter("main"))
        assertEquals(QuestLogFilter.MAIN, parseQuestLogFilter("principal"))
    }

    @Test
    fun parseReturnsSideForSideAliases() {
        assertEquals(QuestLogFilter.SIDE, parseQuestLogFilter("side"))
        assertEquals(QuestLogFilter.SIDE, parseQuestLogFilter("secundar"))
        assertEquals(QuestLogFilter.SIDE, parseQuestLogFilter("secundare"))
    }

    @Test
    fun parseReturnsRepeatableForRepeatableAliases() {
        assertEquals(QuestLogFilter.REPEATABLE, parseQuestLogFilter("repeatable"))
        assertEquals(QuestLogFilter.REPEATABLE, parseQuestLogFilter("repetabil"))
        assertEquals(QuestLogFilter.REPEATABLE, parseQuestLogFilter("repetabile"))
    }

    @Test
    fun parseReturnsCompletedForCompletedAliases() {
        assertEquals(QuestLogFilter.COMPLETED, parseQuestLogFilter("completed"))
        assertEquals(QuestLogFilter.COMPLETED, parseQuestLogFilter("complete"))
        assertEquals(QuestLogFilter.COMPLETED, parseQuestLogFilter("completat"))
        assertEquals(QuestLogFilter.COMPLETED, parseQuestLogFilter("finalizat"))
        assertEquals(QuestLogFilter.COMPLETED, parseQuestLogFilter("finalizate"))
    }

    @Test
    fun parseReturnsFailedForFailedAliases() {
        assertEquals(QuestLogFilter.FAILED, parseQuestLogFilter("failed"))
        assertEquals(QuestLogFilter.FAILED, parseQuestLogFilter("esuat"))
        assertEquals(QuestLogFilter.FAILED, parseQuestLogFilter("abandonat"))
        assertEquals(QuestLogFilter.FAILED, parseQuestLogFilter("abandonate"))
    }

    @Test
    fun parseReturnsArchivedForArchivedAliases() {
        assertEquals(QuestLogFilter.ARCHIVED, parseQuestLogFilter("archived"))
        assertEquals(QuestLogFilter.ARCHIVED, parseQuestLogFilter("archive"))
        assertEquals(QuestLogFilter.ARCHIVED, parseQuestLogFilter("arhivat"))
        assertEquals(QuestLogFilter.ARCHIVED, parseQuestLogFilter("arhivate"))
    }

    @Test
    fun parseReturnsContractKindForContractAliases() {
        assertEquals(QuestLogFilter.CONTRACT_KIND, parseQuestLogFilter("contract"))
        assertEquals(QuestLogFilter.CONTRACT_KIND, parseQuestLogFilter("contracts"))
        assertEquals(QuestLogFilter.CONTRACT_KIND, parseQuestLogFilter("contracte"))
    }

    @Test
    fun parseReturnsContractCurrentForContractCurrentAliases() {
        assertEquals(QuestLogFilter.CONTRACT_CURRENT, parseQuestLogFilter("contract_current"))
        assertEquals(QuestLogFilter.CONTRACT_CURRENT, parseQuestLogFilter("contract_curent"))
        assertEquals(QuestLogFilter.CONTRACT_CURRENT, parseQuestLogFilter("contracte_curente"))
    }

    @Test
    fun parseReturnsContractActiveForContractActiveAliases() {
        assertEquals(QuestLogFilter.CONTRACT_ACTIVE, parseQuestLogFilter("contract_active"))
        assertEquals(QuestLogFilter.CONTRACT_ACTIVE, parseQuestLogFilter("contract_activ"))
        assertEquals(QuestLogFilter.CONTRACT_ACTIVE, parseQuestLogFilter("contracte_active"))
    }

    @Test
    fun parseReturnsContractOfferedForContractOfferedAliases() {
        assertEquals(QuestLogFilter.CONTRACT_OFFERED, parseQuestLogFilter("contract_offered"))
        assertEquals(QuestLogFilter.CONTRACT_OFFERED, parseQuestLogFilter("contract_oferit"))
        assertEquals(QuestLogFilter.CONTRACT_OFFERED, parseQuestLogFilter("contracte_oferite"))
    }

    @Test
    fun parseReturnsContractTrackedForContractTrackedAliases() {
        assertEquals(QuestLogFilter.CONTRACT_TRACKED, parseQuestLogFilter("contract_tracked"))
        assertEquals(QuestLogFilter.CONTRACT_TRACKED, parseQuestLogFilter("contract_urmarit"))
        assertEquals(QuestLogFilter.CONTRACT_TRACKED, parseQuestLogFilter("contracte_urmarite"))
    }

    @Test
    fun parseReturnsContractCompletedForCompleted() {
        assertEquals(QuestLogFilter.CONTRACT_COMPLETED, parseQuestLogFilter("contract_completed"))
        assertEquals(QuestLogFilter.CONTRACT_COMPLETED, parseQuestLogFilter("contract_completat"))
    }

    @Test
    fun parseReturnsContractFailedForFailed() {
        assertEquals(QuestLogFilter.CONTRACT_FAILED, parseQuestLogFilter("contract_failed"))
        assertEquals(QuestLogFilter.CONTRACT_FAILED, parseQuestLogFilter("contract_esuat"))
    }

    @Test
    fun parseReturnsContractArchivedForArchived() {
        assertEquals(QuestLogFilter.CONTRACT_ARCHIVED, parseQuestLogFilter("contract_archived"))
        assertEquals(QuestLogFilter.CONTRACT_ARCHIVED, parseQuestLogFilter("contract_arhivat"))
    }

    @Test
    fun parseReturnsDutyKindForDutyAliases() {
        assertEquals(QuestLogFilter.DUTY_KIND, parseQuestLogFilter("duty"))
        assertEquals(QuestLogFilter.DUTY_KIND, parseQuestLogFilter("duties"))
        assertEquals(QuestLogFilter.DUTY_KIND, parseQuestLogFilter("sarcina"))
        assertEquals(QuestLogFilter.DUTY_KIND, parseQuestLogFilter("sarcini"))
    }

    @Test
    fun parseReturnsBountyKindForBountyAliases() {
        assertEquals(QuestLogFilter.BOUNTY_KIND, parseQuestLogFilter("bounty"))
        assertEquals(QuestLogFilter.BOUNTY_KIND, parseQuestLogFilter("bounties"))
        assertEquals(QuestLogFilter.BOUNTY_KIND, parseQuestLogFilter("recompensa"))
        assertEquals(QuestLogFilter.BOUNTY_KIND, parseQuestLogFilter("recompense"))
    }

    @Test
    fun parseReturnsEventKindForEventAliases() {
        assertEquals(QuestLogFilter.EVENT_KIND, parseQuestLogFilter("event"))
        assertEquals(QuestLogFilter.EVENT_KIND, parseQuestLogFilter("events"))
        assertEquals(QuestLogFilter.EVENT_KIND, parseQuestLogFilter("eveniment"))
        assertEquals(QuestLogFilter.EVENT_KIND, parseQuestLogFilter("evenimente"))
    }

    @Test
    fun parseReturnsTutorialKindForTutorialAliases() {
        assertEquals(QuestLogFilter.TUTORIAL_KIND, parseQuestLogFilter("tutorial"))
        assertEquals(QuestLogFilter.TUTORIAL_KIND, parseQuestLogFilter("tutorials"))
        assertEquals(QuestLogFilter.TUTORIAL_KIND, parseQuestLogFilter("onboarding"))
        assertEquals(QuestLogFilter.TUTORIAL_KIND, parseQuestLogFilter("indrumare"))
    }

    @Test
    fun parseReturnsRitualKindForRitualAliases() {
        assertEquals(QuestLogFilter.RITUAL_KIND, parseQuestLogFilter("ritual"))
        assertEquals(QuestLogFilter.RITUAL_KIND, parseQuestLogFilter("rituals"))
        assertEquals(QuestLogFilter.RITUAL_KIND, parseQuestLogFilter("ceremony"))
        assertEquals(QuestLogFilter.RITUAL_KIND, parseQuestLogFilter("ceremonies"))
        assertEquals(QuestLogFilter.RITUAL_KIND, parseQuestLogFilter("ceremonie"))
        assertEquals(QuestLogFilter.RITUAL_KIND, parseQuestLogFilter("ceremonii"))
    }

    @Test
    fun parseHandlesRomanianDiacriticsViaNormalization() {
        assertEquals(QuestLogFilter.ALL, parseQuestLogFilter("toate"))
        assertEquals(QuestLogFilter.CURRENT, parseQuestLogFilter("curente"))
        assertEquals(QuestLogFilter.OFFERED, parseQuestLogFilter("oferite"))
        assertEquals(QuestLogFilter.FAILED, parseQuestLogFilter("esuat"))
    }

    @Test
    fun parseReturnsSummaryForWhitespaceFilter() {
        assertEquals(QuestLogFilter.SUMMARY, parseQuestLogFilter("   "))
        assertEquals(QuestLogFilter.SUMMARY, parseQuestLogFilter("\t\n"))
    }

    @Test
    fun questLogStatusPriorityReturns3ForNull() {
        assertEquals(3, questLogStatusPriority(null))
    }

    @Test
    fun questLogStatusPriorityReturns0ForActive() {
        val p = PlayerQuestProgress("t", "q", QuestStatus.ACTIVE, 0L, 0L, 0L, "", emptyMap(), emptyMap())
        assertTrue(p.isActive())
        assertEquals(0, questLogStatusPriority(p))
    }

    @Test
    fun questLogStatusPriorityReturns1ForOffered() {
        val p = PlayerQuestProgress("t", "q", QuestStatus.OFFERED, 0L, 0L, 0L, "", emptyMap(), emptyMap())
        assertTrue(p.isOffered())
        assertEquals(1, questLogStatusPriority(p))
    }

    @Test
    fun questLogStatusPriorityReturns2ForFailed() {
        val p = PlayerQuestProgress("t", "q", QuestStatus.FAILED, 0L, 0L, 0L, "", emptyMap(), emptyMap())
        assertEquals(2, questLogStatusPriority(p))
    }

    @Test
    fun questLogStatusPriorityReturns2ForCompleted() {
        val p = PlayerQuestProgress("t", "q", QuestStatus.COMPLETED, 0L, 0L, 0L, "", emptyMap(), emptyMap())
        assertFalse(p.isActive())
        assertFalse(p.isOffered())
        assertEquals(2, questLogStatusPriority(p))
    }
}
