package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class QuestOfferNpcMatcherTest {
    private val npcUuid = UUID.randomUUID()

    @Test
    fun `uuid binding identifies only the npc that offered the quest`() {
        val progress = progress(mapOf(
            "quest_giver_uuid" to npcUuid.toString(),
            "quest_giver_name" to "Nume vechi",
        ))

        assertTrue(QuestOfferNpcMatcher.matches(progress, npcUuid, 7, "Mara"))
        assertFalse(QuestOfferNpcMatcher.matches(progress, UUID.randomUUID(), 7, "Nume vechi"))
    }

    @Test
    fun `database id supports current and legacy quest giver keys`() {
        assertTrue(QuestOfferNpcMatcher.matches(
            progress(mapOf("quest_giver_db_id" to "7")), npcUuid, 7, "Mara",
        ))
        assertTrue(QuestOfferNpcMatcher.matches(
            progress(mapOf("quest_giver_npc_id" to "7")), npcUuid, 7, "Mara",
        ))
        assertFalse(QuestOfferNpcMatcher.matches(
            progress(mapOf("quest_giver_npc_id" to "8")), npcUuid, 7, "Mara",
        ))
    }

    @Test
    fun `name is used only when stronger identifiers are absent`() {
        assertTrue(QuestOfferNpcMatcher.matches(
            progress(mapOf("quest_giver_npc_name" to "mara")), npcUuid, 7, "Mara",
        ))
        assertFalse(QuestOfferNpcMatcher.matches(
            progress(mapOf("quest_giver_db_id" to "8", "quest_giver_name" to "Mara")), npcUuid, 7, "Mara",
        ))
    }

    @Test
    fun `active completed and unbound quests do not open quest only dialogue`() {
        assertFalse(QuestOfferNpcMatcher.matches(
            progress(emptyMap(), QuestStatus.OFFERED), npcUuid, 7, "Mara",
        ))
        for (status in listOf(QuestStatus.ACTIVE, QuestStatus.COMPLETED, QuestStatus.FAILED)) {
            assertFalse(QuestOfferNpcMatcher.matches(
                progress(mapOf("quest_giver_uuid" to npcUuid.toString()), status), npcUuid, 7, "Mara",
            ))
        }
    }

    private fun progress(
        variables: Map<String, String>,
        status: QuestStatus = QuestStatus.OFFERED,
    ): PlayerQuestProgress = PlayerQuestProgress(
        "quest:test", "test", status, 1L, 0L, 1L, "offer", emptyMap(), variables,
    )
}
