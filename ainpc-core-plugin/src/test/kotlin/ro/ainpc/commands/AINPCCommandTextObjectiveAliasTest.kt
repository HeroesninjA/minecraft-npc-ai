package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AINPCCommandTextObjectiveAliasTest {
    @Test
    fun normalizesCommonObjectiveTyposToCanonicalTypes() {
        assertEquals("talk_to_npc", normalizeQuestObjectiveType("talk_nlc"))
        assertEquals("inspect_node", normalizeQuestObjectiveType("interact_nkde"))
        assertTrue(isSupportedQuestObjectiveType("talk_nlc"))
        assertTrue(isSupportedQuestObjectiveType("interact_nkde"))
    }
}
