package ro.ainpc.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestLogGuiFilterTest {

    @Test
    void normalizesSupportedGuiQuestFilters() {
        assertEquals("all", QuestLogGuiFilter.normalizeFilter(""));
        assertEquals("contract", QuestLogGuiFilter.normalizeFilter("contracte"));
        assertEquals("duty", QuestLogGuiFilter.normalizeFilter("sarcini"));
        assertEquals("bounty", QuestLogGuiFilter.normalizeFilter("recompense"));
        assertEquals("event", QuestLogGuiFilter.normalizeFilter("evenimente"));
        assertEquals("tutorial", QuestLogGuiFilter.normalizeFilter("onboarding"));
        assertEquals("ritual", QuestLogGuiFilter.normalizeFilter("ceremonii"));
    }

    @Test
    void preservesUnknownFilterForScenarioEngineFallback() {
        assertEquals("tracked", QuestLogGuiFilter.normalizeFilter("tracked"));
        assertEquals("main", QuestLogGuiFilter.normalizeFilter("main"));
    }

    @Test
    void exposesStablePrimaryFilterOrderForQuestLogRow() {
        assertEquals(
            List.of(
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
        );
        assertTrue(QuestLogGuiFilter.RITUAL.matches("ritual"));
    }
}
