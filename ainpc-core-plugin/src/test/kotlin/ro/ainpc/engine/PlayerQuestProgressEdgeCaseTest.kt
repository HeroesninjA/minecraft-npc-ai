package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerQuestProgressEdgeCaseTest {

    @Test
    fun progressWithNullTemplateId() {
        val progress = PlayerQuestProgress(null, null, null, 0, 0, 0, null, null, null)
        assertEquals(null, progress.templateId())
        assertEquals(null, progress.questCode())
        assertEquals(null, progress.status())
        assertFalse(progress.isCurrent())
        assertFalse(progress.isOffered())
        assertFalse(progress.isActive())
        assertFalse(progress.isCompleted())
    }

    @Test
    fun objectiveProgressIsEmptyByDefault() {
        val progress = PlayerQuestProgress("t", "c", QuestStatus.ACTIVE, 0, 0, 0, "", null, null)
        assertTrue(progress.objectiveProgress().isEmpty())
    }

    @Test
    fun questVariablesAreEmptyByDefault() {
        val progress = PlayerQuestProgress("t", "c", QuestStatus.ACTIVE, 0, 0, 0, "", emptyMap(), null)
        assertTrue(progress.questVariables().isEmpty())
    }

    @Test
    fun progressCurrentPhaseDefaultsToEmpty() {
        val progress = PlayerQuestProgress("t", "c", QuestStatus.ACTIVE, 0, 0, 0, null, emptyMap(), emptyMap())
        assertEquals("", progress.currentPhase())
    }

    @Test
    fun progressTimestamps() {
        val now = System.currentTimeMillis()
        val progress = PlayerQuestProgress("t", "c", QuestStatus.OFFERED, now, 0, now, "P1", emptyMap(), emptyMap())
        assertEquals(now, progress.startedAt())
        assertEquals(0, progress.completedAt())
        assertEquals(now, progress.updatedAt())
    }

    @Test
    fun progressCopyWithNewStatus() {
        val now = System.currentTimeMillis()
        val offered = PlayerQuestProgress("t", "c", QuestStatus.OFFERED, now, 0, now, "P1", mapOf("k" to 1), mapOf("v" to "x"))
        val accepted = PlayerQuestProgress(
            offered.templateId(), offered.questCode(), QuestStatus.ACTIVE,
            offered.startedAt(), offered.completedAt(), System.currentTimeMillis(), "P2",
            offered.objectiveProgress(), offered.questVariables()
        )
        assertEquals(QuestStatus.ACTIVE, accepted.status())
        assertEquals("P2", accepted.currentPhase())
        assertEquals(mapOf("k" to 1), accepted.objectiveProgress())
        assertEquals(mapOf("v" to "x"), accepted.questVariables())
    }
}
