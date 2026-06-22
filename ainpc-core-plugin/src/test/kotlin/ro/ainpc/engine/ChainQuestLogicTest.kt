package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChainQuestLogicTest {

    @Test
    fun questAvailabilityAllowsWhenNoPrerequisites() {
        val template = ScenarioTemplate(ScenarioType.QUEST)
        template.questPrerequisites = mutableListOf()
        val availability = QuestAvailability.allowed()
        assertTrue(availability.available())
        assertTrue(availability.issues().isEmpty())
    }

    @Test
    fun questAvailabilityUnavailableWithIssues() {
        val availability = QuestAvailability.unavailable(listOf("Q01 not completed"))
        assertFalse(availability.available())
        assertEquals(1, availability.issues().size)
        assertEquals("Q01 not completed", availability.issues()[0])
    }

    @Test
    fun playerQuestProgressStoresStatus() {
        val now = System.currentTimeMillis()
        val progress = PlayerQuestProgress(
            "template:Q01", "Q01", QuestStatus.OFFERED,
            now, 0L, now, "INTRODUCTION",
            mapOf("collect_iron" to 0), mapOf("quest_giver" to "blacksmith")
        )
        assertEquals("template:Q01", progress.templateId())
        assertEquals("Q01", progress.questCode())
        assertEquals(QuestStatus.OFFERED, progress.status())
        assertTrue(progress.isOffered())
        assertFalse(progress.isActive())
        assertFalse(progress.isCompleted())
        assertEquals("INTRODUCTION", progress.currentPhase())
    }

    @Test
    fun playerQuestProgressActiveAfterAccept() {
        val now = System.currentTimeMillis()
        val progress = PlayerQuestProgress(
            "template:Q01", "Q01", QuestStatus.ACTIVE,
            now, 0L, now, "GATHERING",
            mapOf("collect_iron" to 0), emptyMap()
        )
        assertTrue(progress.isActive())
        assertFalse(progress.isOffered())
        assertFalse(progress.isCompleted())
    }

    @Test
    fun playerQuestProgressCompleted() {
        val now = System.currentTimeMillis()
        val progress = PlayerQuestProgress(
            "template:Q01", "Q01", QuestStatus.COMPLETED,
            now, now, now, "COMPLETION",
            mapOf("collect_iron" to 3), emptyMap()
        )
        assertTrue(progress.isCompleted())
        assertFalse(progress.isActive())
        assertFalse(progress.isOffered())
    }

    @Test
    fun playerQuestProgressFailed() {
        val now = System.currentTimeMillis()
        val progress = PlayerQuestProgress(
            "template:Q01", "Q01", QuestStatus.FAILED,
            now, 0L, now, "FAILED",
            mapOf("collect_iron" to 1), emptyMap()
        )
        assertEquals(QuestStatus.FAILED, progress.status())
    }
}
