package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ObjectiveTypeAliasRegressionTest {

    @Test
    fun talkNlcNormalizesToTalkToNpc() {
        assertEquals("talk_to_npc", ObjectiveTypeAliasRegistry.normalize("talk_nlc"))
    }

    @Test
    fun interactNkdeNormalizesToInspectNode() {
        assertEquals("inspect_node", ObjectiveTypeAliasRegistry.normalize("interact_nkde"))
    }

    @Test
    fun fetchNormalizesToCollectItem() {
        assertEquals("collect_item", ObjectiveTypeAliasRegistry.normalize("fetch"))
    }

    @Test
    fun turnInNormalizesToDeliverToNpc() {
        assertEquals("deliver_to_npc", ObjectiveTypeAliasRegistry.normalize("turn_in"))
    }

    @Test
    fun slayNormalizesToKillMob() {
        assertEquals("kill_mob", ObjectiveTypeAliasRegistry.normalize("slay"))
    }

    @Test
    fun buildNormalizesToPlaceBlock() {
        assertEquals("place_block", ObjectiveTypeAliasRegistry.normalize("build"))
    }

    @Test
    fun mineNormalizesToBreakBlock() {
        assertEquals("break_block", ObjectiveTypeAliasRegistry.normalize("mine"))
    }

    @Test
    fun craftNormalizesToCraftItem() {
        assertEquals("craft_item", ObjectiveTypeAliasRegistry.normalize("craft"))
    }

    @Test
    fun supportedTypesContainsAllCanonicalTypes() {
        val types = ObjectiveTypeAliasRegistry.supportedTypes()
        assertEquals(12, types.size)
        assertEquals(true, types.contains("collect_item"))
        assertEquals(true, types.contains("deliver_to_npc"))
        assertEquals(true, types.contains("talk_to_npc"))
        assertEquals(true, types.contains("visit_region"))
        assertEquals(true, types.contains("visit_place"))
        assertEquals(true, types.contains("inspect_node"))
        assertEquals(true, types.contains("kill_mob"))
        assertEquals(true, types.contains("place_block"))
        assertEquals(true, types.contains("break_block"))
        assertEquals(true, types.contains("craft_item"))
    }

    @Test
    fun unknownTypePassesThrough() {
        assertEquals("custom_type", ObjectiveTypeAliasRegistry.normalize("custom_type"))
    }
}
