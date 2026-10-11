package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlayerRecognitionTest {
    @Test
    fun `regional reputation alone does not imply a personal meeting`() {
        val recognition = PlayerRecognition.from(null, 0, 0, 25)

        assertEquals(PlayerRecognition.Familiarity.FIRST_MEETING, recognition.familiarity)
        assertEquals(25, recognition.regionalReputation)
    }

    @Test
    fun `past conversation recognizes player even without relationship row`() {
        val recognition = PlayerRecognition.from(null, 2, 0)

        assertEquals(PlayerRecognition.Familiarity.KNOWN, recognition.familiarity)
        assertEquals(2, recognition.interactionCount)
    }

    @Test
    fun `persisted friend and rival relationships select different attitudes`() {
        val friend = PlayerRecognition.from(
            NPCRelationship(interactionCount = 6, relationshipType = "close_friend"), 2, 1
        )
        val rival = PlayerRecognition.from(
            NPCRelationship(interactionCount = 4, relationshipType = "rival"), 2, 0
        )

        assertEquals(PlayerRecognition.Familiarity.FRIEND, friend.familiarity)
        assertEquals(PlayerRecognition.Familiarity.GUARDED, rival.familiarity)
    }

    @Test
    fun `negative trust makes known player guarded without inventing hostility`() {
        val recognition = PlayerRecognition.from(
            NPCRelationship(trust = -0.5, interactionCount = 3, relationshipType = "stranger"), 0, 0
        )

        assertEquals(PlayerRecognition.Familiarity.GUARDED, recognition.familiarity)
    }
}
