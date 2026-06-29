package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ro.ainpc.engine.FeaturePackLoader.QuestEntryDefinition

class ResolveRewardAmountTest {

    @Test
    fun `default reward is positive`() {
        val reward = QuestEntryDefinition("reputation:region", "demo_sat", 10, "Region rep")
        assertEquals(10, resolveRewardAmount(reward))
    }

    @Test
    fun `penalty true flips sign`() {
        val reward = QuestEntryDefinition(
            "reputation:faction", "bandits", 25, "Bandit penalty",
            mapOf("penalty" to "true"), emptyMap(), emptyMap()
        )
        assertEquals(-25, resolveRewardAmount(reward))
    }

    @Test
    fun `penalty 1 flips sign`() {
        val reward = QuestEntryDefinition(
            "reputation:region", "demo_sat", 5, "Tax",
            mapOf("penalty" to "1"), emptyMap(), emptyMap()
        )
        assertEquals(-5, resolveRewardAmount(reward))
    }

    @Test
    fun `sign negative flips sign`() {
        val reward = QuestEntryDefinition(
            "reputation:faction", "knights", 10, "Penalty",
            mapOf("sign" to "-"), emptyMap(), emptyMap()
        )
        assertEquals(-10, resolveRewardAmount(reward))
    }

    @Test
    fun `sign positive is explicit positive`() {
        val reward = QuestEntryDefinition(
            "reputation:region", "demo_sat", 10, "Region rep",
            mapOf("sign" to "+"), emptyMap(), emptyMap()
        )
        assertEquals(10, resolveRewardAmount(reward))
    }

    @Test
    fun `zero amount with penalty becomes -1`() {
        val reward = QuestEntryDefinition(
            "reputation:faction", "knights", 0, "Zero with penalty",
            mapOf("penalty" to "true"), emptyMap(), emptyMap()
        )
        assertEquals(-1, resolveRewardAmount(reward))
    }

    @Test
    fun `case insensitive penalty true`() {
        val reward = QuestEntryDefinition(
            "reputation:region", "demo_sat", 10, "Tax",
            mapOf("penalty" to "TRUE"), emptyMap(), emptyMap()
        )
        assertEquals(-10, resolveRewardAmount(reward))
    }

    @Test
    fun `unknown metadata values keep positive sign`() {
        val reward = QuestEntryDefinition(
            "reputation:region", "demo_sat", 10, "Reward",
            mapOf("penalty" to "maybe"), emptyMap(), emptyMap()
        )
        assertEquals(10, resolveRewardAmount(reward))
    }
}
