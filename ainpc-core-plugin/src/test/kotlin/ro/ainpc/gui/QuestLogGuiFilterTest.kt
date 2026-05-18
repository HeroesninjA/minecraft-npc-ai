package ro.ainpc.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QuestLogGuiFilterTest {
    @Test
    fun normalizesSupportedGuiQuestFilters() {
        assertEquals("all", QuestLogGuiFilter.normalizeFilter(""))
        assertEquals("contract", QuestLogGuiFilter.normalizeFilter("contracte"))
        assertEquals("duty", QuestLogGuiFilter.normalizeFilter("sarcini"))
        assertEquals("bounty", QuestLogGuiFilter.normalizeFilter("recompense"))
        assertEquals("event", QuestLogGuiFilter.normalizeFilter("evenimente"))
        assertEquals("tutorial", QuestLogGuiFilter.normalizeFilter("onboarding"))
        assertEquals("ritual", QuestLogGuiFilter.normalizeFilter("ceremonii"))
    }

    @Test
    fun preservesUnknownFilterForScenarioEngineFallback() {
        assertEquals("tracked", QuestLogGuiFilter.normalizeFilter("tracked"))
        assertEquals("main", QuestLogGuiFilter.normalizeFilter("main"))
    }

    @Test
    fun exposesStablePrimaryFilterOrderForQuestLogRow() {
        assertEquals(
            listOf(
                QuestLogGuiFilter.ALL,
                QuestLogGuiFilter.ACTIVE,
                QuestLogGuiFilter.QUEST,
                QuestLogGuiFilter.CONTRACT,
                QuestLogGuiFilter.DUTY,
                QuestLogGuiFilter.BOUNTY,
                QuestLogGuiFilter.EVENT,
                QuestLogGuiFilter.TUTORIAL,
                QuestLogGuiFilter.RITUAL
            ),
            QuestLogGuiFilter.primaryFilters()
        )
        assertTrue(QuestLogGuiFilter.RITUAL.matches("ritual"))
    }
}
