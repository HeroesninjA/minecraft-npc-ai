package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SampleQuestSmokeTest {

    @Test
    fun objectiveTypesAreSupportedByRegistry() {
        val supported = ObjectiveTypeAliasRegistry.supportedTypes()

        assertTrue(supported.contains("talk_to_npc"))
        assertTrue(supported.contains("collect_item"))
        assertTrue(supported.contains("visit_region"))
        assertTrue(supported.contains("visit_place"))
        assertTrue(supported.contains("inspect_node"))
        assertTrue(supported.contains("craft_item"))
        assertTrue(supported.contains("place_block"))
        assertTrue(supported.contains("break_block"))
        assertTrue(supported.contains("kill_mob"))
        assertTrue(supported.contains("deliver_to_npc"))
        assertTrue(supported.contains("use_item"))
        assertTrue(supported.contains("equip_item"))
        assertEquals(12, supported.size)
    }

    @Test
    fun objectiveTypesNormalizeCorrectly() {
        assertEquals("talk_to_npc", ObjectiveTypeAliasRegistry.normalize("talk_to_npc"))
        assertEquals("collect_item", ObjectiveTypeAliasRegistry.normalize("collect_item"))
        assertEquals("visit_region", ObjectiveTypeAliasRegistry.normalize("visit_region"))
        assertEquals("visit_place", ObjectiveTypeAliasRegistry.normalize("visit_place"))
        assertEquals("inspect_node", ObjectiveTypeAliasRegistry.normalize("inspect_node"))
        assertEquals("craft_item", ObjectiveTypeAliasRegistry.normalize("craft_item"))
        assertEquals("place_block", ObjectiveTypeAliasRegistry.normalize("place_block"))
        assertEquals("break_block", ObjectiveTypeAliasRegistry.normalize("break_block"))
        assertEquals("kill_mob", ObjectiveTypeAliasRegistry.normalize("kill_mob"))
        assertEquals("deliver_to_npc", ObjectiveTypeAliasRegistry.normalize("deliver_to_npc"))
    }

    @Test
    fun allObjectiveTypesPassValidation() {
        val supported = ObjectiveTypeAliasRegistry.supportedTypes()
        for (type in supported) {
            val normalized = ObjectiveTypeAliasRegistry.normalize(type)
            assertTrue(normalized in supported, "Type '$type' normalized to '$normalized' should be supported")
        }
    }
}
