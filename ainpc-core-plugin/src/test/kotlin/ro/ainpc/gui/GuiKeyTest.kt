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

    @Test
    fun resolvesAdminMappingAliases() {
        assertEquals(GuiKey.ADMIN_MAPPING, GuiKey.fromId("admin_mapping").orElseThrow())
        assertEquals(GuiKey.ADMIN_MAPPING, GuiKey.fromId("adminmapping").orElseThrow())
        assertEquals(GuiKey.ADMIN_MAPPING, GuiKey.fromId("mapping_admin").orElseThrow())
        assertEquals(GuiKey.ADMIN_MAPPING, GuiKey.fromId("edit_mapping").orElseThrow())
    }

    @Test
    fun resolvesAdminQuestAliases() {
        assertEquals(GuiKey.ADMIN_QUEST, GuiKey.fromId("admin_quest").orElseThrow())
        assertEquals(GuiKey.ADMIN_QUEST, GuiKey.fromId("adminquest").orElseThrow())
        assertEquals(GuiKey.ADMIN_QUEST, GuiKey.fromId("quest_admin").orElseThrow())
        assertEquals(GuiKey.ADMIN_QUEST, GuiKey.fromId("edit_quest").orElseThrow())
    }
}
