package ro.ainpc.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GuiKeyTest {
    @Test
    fun resolvesProgressionAliasesToQuestGui() {
        assertEquals(GuiKey.QUEST, GuiKey.fromId("progresii")!!)
        assertEquals(GuiKey.QUEST, GuiKey.fromId("progression")!!)
        assertEquals(GuiKey.QUEST_DETAIL, GuiKey.fromId("detalii_progresie")!!)
        assertEquals(GuiKey.QUEST_DETAIL, GuiKey.fromId("progression_details")!!)
        assertEquals(GuiKey.AUTHORING, GuiKey.fromId("authoring")!!)
        assertEquals(GuiKey.AUTHORING, GuiKey.fromId("quest_authoring")!!)
    }

    @Test
    fun resolvesAdminMappingAliases() {
        assertEquals(GuiKey.ADMIN_MAPPING, GuiKey.fromId("admin_mapping")!!)
        assertEquals(GuiKey.ADMIN_MAPPING, GuiKey.fromId("adminmapping")!!)
        assertEquals(GuiKey.ADMIN_MAPPING, GuiKey.fromId("mapping_admin")!!)
        assertEquals(GuiKey.ADMIN_MAPPING, GuiKey.fromId("edit_mapping")!!)
    }

    @Test
    fun resolvesShopAliases() {
        assertEquals(GuiKey.SHOP, GuiKey.fromId("shop")!!)
        assertEquals(GuiKey.SHOP, GuiKey.fromId("magazin")!!)
        assertEquals(GuiKey.SHOP, GuiKey.fromId("comert")!!)
        assertEquals(GuiKey.SHOP, GuiKey.fromId("comercial")!!)
        assertEquals(GuiKey.SHOP, GuiKey.fromId("cumparaturi")!!)
        assertEquals(GuiKey.SHOP, GuiKey.fromId("negustor")!!)
        assertEquals(GuiKey.SHOP, GuiKey.fromId("market")!!)
        assertEquals(GuiKey.SHOP, GuiKey.fromId("piata")!!)
    }

    @Test
    fun resolvesAdminQuestAliases() {
        assertEquals(GuiKey.ADMIN_QUEST, GuiKey.fromId("admin_quest")!!)
        assertEquals(GuiKey.ADMIN_QUEST, GuiKey.fromId("adminquest")!!)
        assertEquals(GuiKey.ADMIN_QUEST, GuiKey.fromId("quest_admin")!!)
        assertEquals(GuiKey.ADMIN_QUEST, GuiKey.fromId("edit_quest")!!)
    }
}
