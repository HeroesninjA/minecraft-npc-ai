package ro.ainpc.npc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AINPCProfileDefaultsTest {
    @Test
    fun newNpcHasStableInspectableIdentityDefaults() {
        val npc = AINPC(null)

        assertEquals(0, npc.databaseId)
        assertNotNull(npc.uuid)
        assertFalse(npc.spawned)
        assertFalse(npc.isSpawned())
        assertEquals("", npc.name)
        assertEquals("manual", npc.profileSource)
        assertEquals("{}", npc.profileDataJson)
        assertEquals(1, npc.profileVersion)
        assertEquals("", npc.sourceKey)
    }

    @Test
    fun newNpcHasReasonableDemoNeedAndEmotionDefaults() {
        val npc = AINPC(null)

        assertEquals(82, npc.hungerLevel)
        assertEquals(78, npc.energyLevel)
        assertEquals(72, npc.socialNeedLevel)
        assertEquals(70, npc.comfortLevel)
        assertEquals(84, npc.safetyLevel)
        assertEquals("neutral", npc.emotions.dominantEmotion)
        assertEquals(0.5, npc.emotions.happiness)
        assertEquals(0.5, npc.emotions.trust)
        assertEquals(0.3, npc.emotions.anticipation)
    }

    @Test
    fun relationshipLevelMapsToClearStatusBands() {
        val npc = AINPC(null)

        npc.context.relationshipLevel = -75
        assertEquals("ENEMY", npc.context.relationshipStatus)

        npc.context.relationshipLevel = -1
        assertEquals("STRANGER", npc.context.relationshipStatus)

        npc.context.relationshipLevel = 24
        assertEquals("ACQUAINTANCE", npc.context.relationshipStatus)

        npc.context.relationshipLevel = 74
        assertEquals("FRIEND", npc.context.relationshipStatus)

        npc.context.relationshipLevel = 75
        assertEquals("CLOSE_FRIEND", npc.context.relationshipStatus)
    }

    @Test
    fun contextDescriptionIncludesReadableDemoProfileAndAnchors() {
        val npc = AINPC(null)
        npc.name = "Ion Fierarul"
        npc.age = 42
        npc.gender = "male"
        npc.occupation = "fierar"
        npc.backstory = "Repara unelte pentru sat."
        npc.currentGoal = "verifice nicovala"
        npc.plannedRoutineActivity = "munca la fierarie"
        npc.homeAnchor = AINPC.OwnedLocation("home", "Casa lui Ion", "world", 1.0, 64.0, 2.0)
        npc.workAnchor = AINPC.OwnedLocation("work", "Fierarie", "world", 8.0, 64.0, 4.0)
        npc.socialAnchor = AINPC.OwnedLocation("social", "Piata", "world", 0.0, 64.0, 0.0)

        val description = npc.generateContextDescription()

        assertTrue(description.contains("Nume: Ion Fierarul"))
        assertTrue(description.contains("Ocupatie: fierar"))
        assertTrue(description.contains("Rutina actuala: munca la fierarie"))
        assertTrue(description.contains("Obiectiv imediat: verifice nicovala"))
        assertTrue(description.contains("Casa: Casa lui Ion"))
        assertTrue(description.contains("Loc de munca: Fierarie"))
        assertTrue(description.contains("Loc social: Piata"))
    }
}
