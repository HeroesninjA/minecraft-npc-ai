package ro.ainpc.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuiKeyTest {

    @Test
    void resolvesProgressionAliasesToQuestGui() {
        assertEquals(GuiKey.QUEST, GuiKey.fromId("progresii").orElseThrow());
        assertEquals(GuiKey.QUEST, GuiKey.fromId("progression").orElseThrow());
        assertEquals(GuiKey.QUEST_DETAIL, GuiKey.fromId("detalii_progresie").orElseThrow());
        assertEquals(GuiKey.QUEST_DETAIL, GuiKey.fromId("progression_details").orElseThrow());
    }
}
