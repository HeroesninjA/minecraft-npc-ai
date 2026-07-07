package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class RelationshipServiceTest {

    private val npcA = UUID.randomUUID()
    private val npcB = UUID.randomUUID()
    private val npcC = UUID.randomUUID()

    @Test
    fun relationshipKeyIsConsistent() {
        val rel = NPCRelationship()
        assertEquals(0.0, rel.affection)
        assertEquals(0.0, rel.trust)
        assertEquals(0, rel.interactionCount)
    }

    @Test
    fun affectionIsClampedToMax() {
        val rel = NPCRelationship()
        rel.affection = 150.0
        assertEquals(150.0, rel.affection)
    }

    @Test
    fun interactionCountIncrements() {
        val rel = NPCRelationship()
        rel.interactionCount = 5
        assertEquals(5, rel.interactionCount)
    }

    @Test
    fun relationshipTypeDefaultsToNull() {
        val rel = NPCRelationship()
        assertEquals(null, rel.relationshipType)
    }

    @Test
    fun familiarityStartsAtZero() {
        val rel = NPCRelationship()
        assertEquals(0.0, rel.familiarity)
    }

    @Test
    fun respectIsMutable() {
        val rel = NPCRelationship()
        rel.respect = 75.0
        assertEquals(75.0, rel.respect)
    }
}
