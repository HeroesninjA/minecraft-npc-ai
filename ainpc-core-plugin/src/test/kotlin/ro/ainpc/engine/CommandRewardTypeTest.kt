package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.debug.DebugDumpSupport

class CommandRewardTypeTest {

    @Test
    fun commandIsSupportedRewardType() {
        val supported = DebugDumpSupport.supportedQuestRewardTypes()
        assertTrue(supported.contains("command"), "command must be a supported reward type")
    }

    @Test
    fun commandRewardLabel() {
        val reward = FeaturePackLoader.QuestEntryDefinition(
            type = "command",
            itemId = "give \${player} diamond 5",
            amount = 1,
            description = ""
        )
        val label = formatRewardLabel(reward)
        assertTrue(label.contains("comanda:"), "Label must indicate command type")
        assertTrue(label.contains("diamond"), "Label must contain command snippet")
    }

    @Test
    fun commandRewardUsesItemIdAsCommand() {
        val reward = FeaturePackLoader.QuestEntryDefinition(
            type = "command",
            itemId = "say Hello \${player}",
            amount = 1,
            description = ""
        )
        val label = formatRewardLabel(reward)
        assertTrue(label.contains("Hello"), "Label must include command text")
    }
}
