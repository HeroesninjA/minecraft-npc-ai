package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.engine.FeaturePackLoader.ScenarioDefinition
import ro.ainpc.engine.ScenarioType

class DebugDumpQuestAuditSummaryTest {

    @Test
    fun `aggregateQuestRewardTypes counts normalized types from scenario list`() {
        val q1 = scenario("Q01", listOf(
            reward("item", "EMERALD", 5),
            reward("experience", null, 50)
        ))
        val q2 = scenario("Q02", listOf(
            reward("progression:xp", null, 100),
            reward("progression:skill", "combat", 25),
            reward("item", "GOLD_INGOT", 1)
        ))
        val q3 = scenario("Q03", listOf(
            reward("reputation:region", "demo_sat", 10)
        ))
        val result = DebugDumpQuestAudit.aggregateQuestRewardTypes(listOf(q1, q2, q3))
        assertEquals(2, result["item"])
        assertEquals(1, result["experience"])
        assertEquals(1, result["progression:xp"])
        assertEquals(1, result["progression:skill"])
        assertEquals(1, result["reputation:region"])
    }

    @Test
    fun `aggregateQuestRewardTypes normalizes alias reward types`() {
        val scenario = scenario("Q01", listOf(
            reward("economy:money", null, 50),
            reward("progression_xp", null, 100),
            reward("skill_level", "mining", 2)
        ))
        val result = DebugDumpQuestAudit.aggregateQuestRewardTypes(listOf(scenario))
        assertEquals(1, result["economy:money"])
        assertEquals(1, result["progression:xp"])
        assertEquals(1, result["progression:skill_level"])
    }

    @Test
    fun `aggregateQuestRewardTypes skips empty reward lists`() {
        val empty = scenario("Q01", emptyList())
        val withRewards = scenario("Q02", listOf(reward("item", "EMERALD", 1)))
        val result = DebugDumpQuestAudit.aggregateQuestRewardTypes(listOf(empty, withRewards))
        assertEquals(1, result["item"])
    }

    @Test
    fun `aggregateQuestRewardTypes returns empty for empty scenario list`() {
        val result = DebugDumpQuestAudit.aggregateQuestRewardTypes(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `aggregateQuestRewardTypes aggregates multiple rewards of same type`() {
        val scenario = scenario("Q01", listOf(
            reward("item", "EMERALD", 5),
            reward("item", "DIAMOND", 1),
            reward("item", "GOLD_INGOT", 10)
        ))
        val result = DebugDumpQuestAudit.aggregateQuestRewardTypes(listOf(scenario))
        assertEquals(3, result["item"])
    }

    private fun scenario(
        id: String,
        rewards: List<FeaturePackLoader.QuestEntryDefinition>,
    ): ScenarioDefinition {
        val s = ScenarioDefinition(
            packId = "test_pack",
            id = id,
            name = id,
            description = "",
            baseType = ScenarioType.QUEST,
        )
        s.rewards.addAll(rewards)
        return s
    }

    private fun reward(
        type: String,
        itemId: String?,
        amount: Int,
    ): FeaturePackLoader.QuestEntryDefinition =
        FeaturePackLoader.QuestEntryDefinition(type, itemId, amount, null)
}
