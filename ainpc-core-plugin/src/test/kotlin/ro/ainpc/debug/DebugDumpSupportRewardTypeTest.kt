package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.debug.DebugDumpSupport.normalizeQuestRewardType
import ro.ainpc.debug.DebugDumpSupport.supportedQuestRewardTypes

class DebugDumpSupportRewardTypeTest {

    @Test
    fun normalizeQuestRewardTypeCollapsesBasicAliases() {
        assertEquals("item", normalizeQuestRewardType(""))
        assertEquals("item", normalizeQuestRewardType("item"))
        assertEquals("item", normalizeQuestRewardType("reward_item"))
        assertEquals("set_story_state", normalizeQuestRewardType("story_state"))
        assertEquals("set_story_state", normalizeQuestRewardType("set_story_flag"))
        assertEquals("record_story_event", normalizeQuestRewardType("story_event"))
        assertEquals("record_story_event", normalizeQuestRewardType("record_event"))
    }

    @Test
    fun normalizeQuestRewardTypeCollapsesProgressionAliases() {
        assertEquals("progression:xp", normalizeQuestRewardType("progression_xp"))
        assertEquals("progression:xp", normalizeQuestRewardType("progression_xp_award"))
        assertEquals("progression:level", normalizeQuestRewardType("progression_level"))
        assertEquals("progression:level", normalizeQuestRewardType("progression_set_level"))
        assertEquals("progression:skill", normalizeQuestRewardType("progression_skill"))
        assertEquals("progression:skill", normalizeQuestRewardType("progression_skill_xp"))
    }

    @Test
    fun normalizeQuestRewardTypePreservesReputationScope() {
        assertEquals("reputation:region", normalizeQuestRewardType("reputation_region"))
        assertEquals("reputation:faction", normalizeQuestRewardType("reputation_faction"))
        assertEquals("reputation:city", normalizeQuestRewardType("reputation_city"))
    }

    @Test
    fun normalizeQuestRewardTypePassesThroughUnknown() {
        assertEquals("experience", normalizeQuestRewardType("experience"))
        assertEquals("economy:money", normalizeQuestRewardType("economy:money"))
    }

    @Test
    fun supportedQuestRewardTypesContainsProgressionAndReputation() {
        val supported = supportedQuestRewardTypes()
        assertTrue(supported.contains("progression:xp"))
        assertTrue(supported.contains("progression:level"))
        assertTrue(supported.contains("progression:skill"))
        assertTrue(supported.contains("reputation:region"))
        assertTrue(supported.contains("reputation:faction"))
    }
}
