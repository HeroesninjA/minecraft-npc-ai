package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.debug.DebugDumpQuestAudit.auditQuestRewardPayload
import ro.ainpc.engine.FeaturePackLoader

class DebugDumpQuestAuditRewardPayloadTest {

    @Test
    fun `progression skill reward without itemId is reported as error`() {
        val rewards = listOf(
            FeaturePackLoader.QuestEntryDefinition(
                "progression:skill", null, 25, "Skill XP award"
            )
        )
        val (errors, warnings) = auditQuestRewardPayload("Q01", rewards)
        assertTrue(errors.any { it.contains("progression:skill cere itemId") })
        assertEquals(0, warnings.size)
    }

    @Test
    fun `progression skill reward with valid itemId produces no issues`() {
        val rewards = listOf(
            FeaturePackLoader.QuestEntryDefinition(
                "progression:skill", "combat", 25, "Combat XP"
            )
        )
        val (errors, warnings) = auditQuestRewardPayload("Q02", rewards)
        assertEquals(emptyList<String>(), errors)
        assertEquals(emptyList<String>(), warnings)
    }

    @Test
    fun `progression skill reward with coerced amount still gets a warning marker`() {
        val rewards = listOf(
            FeaturePackLoader.QuestEntryDefinition(
                "progression:skill", "combat", -5, "Negative coerced to 1"
            )
        )
        val (errors, warnings) = auditQuestRewardPayload("Q03", rewards)
        assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun `reputation reward without itemId is reported as error`() {
        val rewards = listOf(
            FeaturePackLoader.QuestEntryDefinition(
                "reputation:region", null, 10, "Region rep"
            )
        )
        val (errors, warnings) = auditQuestRewardPayload("Q04", rewards)
        assertTrue(errors.any { it.contains("reputation:region cere itemId cu scope ID") })
        assertEquals(emptyList<String>(), warnings)
    }

    @Test
    fun `reputation reward with valid itemId produces no issues`() {
        val rewards = listOf(
            FeaturePackLoader.QuestEntryDefinition(
                "reputation:faction", "knights", 5, "Knights rep"
            )
        )
        val (errors, warnings) = auditQuestRewardPayload("Q05", rewards)
        assertEquals(emptyList<String>(), errors)
        assertEquals(emptyList<String>(), warnings)
    }

    @Test
    fun `reputation reward with negative amount is allowed`() {
        val rewards = listOf(
            FeaturePackLoader.QuestEntryDefinition(
                "reputation:faction", "bandits", -10, "Bandit penalty"
            )
        )
        val (errors, warnings) = auditQuestRewardPayload("Q06", rewards)
        assertEquals(emptyList<String>(), errors)
        assertEquals(emptyList<String>(), warnings)
    }

    @Test
    fun `progression level reward with valid amount passes`() {
        val rewards = listOf(
            FeaturePackLoader.QuestEntryDefinition(
                "progression:level", null, 5, "Set level 5"
            )
        )
        val (errors, warnings) = auditQuestRewardPayload("Q07", rewards)
        assertEquals(emptyList<String>(), errors)
        assertEquals(emptyList<String>(), warnings)
    }

    @Test
    fun `economy money reward with valid amount passes`() {
        val rewards = listOf(
            FeaturePackLoader.QuestEntryDefinition(
                "economy:money", null, 100, "Coin reward"
            )
        )
        val (errors, warnings) = auditQuestRewardPayload("Q08", rewards)
        assertEquals(emptyList<String>(), errors)
        assertEquals(emptyList<String>(), warnings)
    }

    @Test
    fun `item reward produces no issues`() {
        val rewards = listOf(
            FeaturePackLoader.QuestEntryDefinition(
                "item", "EMERALD", 5, "Reward emeralds"
            )
        )
        val (errors, warnings) = auditQuestRewardPayload("Q09", rewards)
        assertEquals(emptyList<String>(), errors)
        assertEquals(emptyList<String>(), warnings)
    }

    @Test
    fun `empty rewards list produces no issues`() {
        val (errors, warnings) = auditQuestRewardPayload("Q10", emptyList())
        assertEquals(emptyList<String>(), errors)
        assertEquals(emptyList<String>(), warnings)
    }

    @Test
    fun `null rewards list produces no issues`() {
        val (errors, warnings) = auditQuestRewardPayload("Q11", null)
        assertEquals(emptyList<String>(), errors)
        assertEquals(emptyList<String>(), warnings)
    }
}
