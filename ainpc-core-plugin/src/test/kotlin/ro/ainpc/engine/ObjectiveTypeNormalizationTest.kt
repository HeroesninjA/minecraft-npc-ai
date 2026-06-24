package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ObjectiveTypeNormalizationTest {

    @Test
    fun talkToNpcVariants() {
        assertEquals("talk_to_npc", ObjectiveTypeAliasRegistry.normalize("talk_to_npc"))
        assertEquals("talk_to_npc", ObjectiveTypeAliasRegistry.normalize("Talk_To_Npc"))
        assertEquals("talk_to_npc", ObjectiveTypeAliasRegistry.normalize("TALK_TO_NPC"))
        assertEquals("talk_to_npc", ObjectiveTypeAliasRegistry.normalize("talk npc"))
        assertEquals("talk_to_npc", ObjectiveTypeAliasRegistry.normalize("Talk Npc"))
    }

    @Test
    fun visitPlaceVariants() {
        assertEquals("visit_place", ObjectiveTypeAliasRegistry.normalize("visit_place"))
        assertEquals("visit_place", ObjectiveTypeAliasRegistry.normalize("Visit_Place"))
        assertEquals("visit_place", ObjectiveTypeAliasRegistry.normalize("visit place"))
        assertEquals("visit_place", ObjectiveTypeAliasRegistry.normalize("go-to-place"))
        assertEquals("visit_place", ObjectiveTypeAliasRegistry.normalize("go to place"))
    }

    @Test
    fun inspectNodeVariants() {
        assertEquals("inspect_node", ObjectiveTypeAliasRegistry.normalize("inspect_node"))
        assertEquals("inspect_node", ObjectiveTypeAliasRegistry.normalize("Inspect_Node"))
        assertEquals("inspect_node", ObjectiveTypeAliasRegistry.normalize("inspect node"))
        assertEquals("inspect_node", ObjectiveTypeAliasRegistry.normalize("interact node"))
    }

    @Test
    fun killMobVariants() {
        assertEquals("kill_mob", ObjectiveTypeAliasRegistry.normalize("kill_mob"))
        assertEquals("kill_mob", ObjectiveTypeAliasRegistry.normalize("Kill_Mob"))
        assertEquals("kill_mob", ObjectiveTypeAliasRegistry.normalize("kill mob"))
        assertEquals("kill_mob", ObjectiveTypeAliasRegistry.normalize("kill"))
    }

    @Test
    fun collectItemVariants() {
        assertEquals("collect_item", ObjectiveTypeAliasRegistry.normalize("collect_item"))
        assertEquals("collect_item", ObjectiveTypeAliasRegistry.normalize("Collect_Item"))
        assertEquals("collect_item", ObjectiveTypeAliasRegistry.normalize("collect item"))
        assertEquals("collect_item", ObjectiveTypeAliasRegistry.normalize("fetch"))
    }

    @Test
    fun placeBlockVariants() {
        assertEquals("place_block", ObjectiveTypeAliasRegistry.normalize("place_block"))
        assertEquals("place_block", ObjectiveTypeAliasRegistry.normalize("place block"))
        assertEquals("place_block", ObjectiveTypeAliasRegistry.normalize("Place-Block"))
        assertEquals("place_block", ObjectiveTypeAliasRegistry.normalize("build"))
        assertEquals("place_block", ObjectiveTypeAliasRegistry.normalize("construct"))
    }

    @Test
    fun breakBlockVariants() {
        assertEquals("break_block", ObjectiveTypeAliasRegistry.normalize("break_block"))
        assertEquals("break_block", ObjectiveTypeAliasRegistry.normalize("break block"))
        assertEquals("break_block", ObjectiveTypeAliasRegistry.normalize("Break-Block"))
        assertEquals("break_block", ObjectiveTypeAliasRegistry.normalize("mine"))
        assertEquals("break_block", ObjectiveTypeAliasRegistry.normalize("dig"))
    }

    @Test
    fun craftItemVariants() {
        assertEquals("craft_item", ObjectiveTypeAliasRegistry.normalize("craft_item"))
        assertEquals("craft_item", ObjectiveTypeAliasRegistry.normalize("craft item"))
        assertEquals("craft_item", ObjectiveTypeAliasRegistry.normalize("Craft-Item"))
        assertEquals("craft_item", ObjectiveTypeAliasRegistry.normalize("make"))
    }

    @Test
    fun useItemVariants() {
        assertEquals("use_item", ObjectiveTypeAliasRegistry.normalize("use_item"))
        assertEquals("use_item", ObjectiveTypeAliasRegistry.normalize("use item"))
        assertEquals("use_item", ObjectiveTypeAliasRegistry.normalize("Use-Item"))
        assertEquals("use_item", ObjectiveTypeAliasRegistry.normalize("consume"))
        assertEquals("use_item", ObjectiveTypeAliasRegistry.normalize("drink"))
    }

    @Test
    fun equipItemVariants() {
        assertEquals("equip_item", ObjectiveTypeAliasRegistry.normalize("equip_item"))
        assertEquals("equip_item", ObjectiveTypeAliasRegistry.normalize("equip item"))
        assertEquals("equip_item", ObjectiveTypeAliasRegistry.normalize("Equip-Item"))
        assertEquals("equip_item", ObjectiveTypeAliasRegistry.normalize("wear"))
        assertEquals("equip_item", ObjectiveTypeAliasRegistry.normalize("don"))
    }

    @Test
    fun deliverToNpcVariants() {
        assertEquals("deliver_to_npc", ObjectiveTypeAliasRegistry.normalize("deliver_to_npc"))
        assertEquals("deliver_to_npc", ObjectiveTypeAliasRegistry.normalize("deliver to npc"))
        assertEquals("deliver_to_npc", ObjectiveTypeAliasRegistry.normalize("turn_in"))
        assertEquals("deliver_to_npc", ObjectiveTypeAliasRegistry.normalize("turn in"))
    }

    @Test
    fun unknownTypePassesThrough() {
        assertEquals("custom_type", ObjectiveTypeAliasRegistry.normalize("custom_type"))
        assertEquals("my_special_objective", ObjectiveTypeAliasRegistry.normalize("My Special Objective"))
        assertEquals("collect_item", ObjectiveTypeAliasRegistry.normalize(""))
        assertEquals("collect_item", ObjectiveTypeAliasRegistry.normalize(null))
    }
}
