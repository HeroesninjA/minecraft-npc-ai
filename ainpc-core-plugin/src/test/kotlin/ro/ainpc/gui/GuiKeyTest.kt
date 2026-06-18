package ro.ainpc.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GuiKeyTest {
    @Test
    fun resolvesProgressionAliasesToQuestGui() {
        assertEquals(GuiKey.QUEST, GuiKey.fromId("progresii").orElseThrow())
        assertEquals(GuiKey.QUEST, GuiKey.fromId("progression").orElseThrow())
        assertEquals(GuiKey.QUEST_DETAIL, GuiKey.fromId("detalii_progresie").orElseThrow())
        assertEquals(GuiKey.QUEST_DETAIL, GuiKey.fromId("progression_details").orElseThrow())
        assertEquals(GuiKey.AUTHORING, GuiKey.fromId("authoring").orElseThrow())
        assertEquals(GuiKey.AUTHORING, GuiKey.fromId("quest_authoring").orElseThrow())
    }
}
