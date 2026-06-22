package ro.ainpc.context

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContextSnapshotTest {

    @Test
    fun emptySnapshot() {
        val snap = ContextSnapshot(playerName = "", playerLevel = 0, playerHealth = 0.0, playerFood = 0)
        assertTrue(snap.isEmpty())
    }

    @Test
    fun nonEmptySnapshot() {
        val snap = ContextSnapshot(
            playerName = "TestPlayer",
            playerLevel = 10,
            playerHealth = 18.5,
            playerFood = 16
        )
        assertFalse(snap.isEmpty())
        assertEquals("TestPlayer", snap.playerName)
        assertEquals(10, snap.playerLevel)
    }

    @Test
    fun snapshotWithNpcData() {
        val snap = ContextSnapshot(
            playerName = "Player1",
            playerLevel = 5,
            playerHealth = 20.0,
            playerFood = 20,
            npcName = "Ion",
            npcOccupation = "fermier",
            npcState = "WORKING",
            npcEmotion = "happy",
            npcHomeAnchor = "house_01",
            npcWorkAnchor = "farm_01"
        )
        assertEquals("Ion", snap.npcName)
        assertEquals("fermier", snap.npcOccupation)
        assertEquals("WORKING", snap.npcState)
        assertEquals("house_01", snap.npcHomeAnchor)
    }

    @Test
    fun snapshotWithLocationData() {
        val snap = ContextSnapshot(
            playerName = "P",
            playerLevel = 1,
            playerHealth = 20.0,
            playerFood = 20,
            currentRegion = "satul_central",
            currentRegionType = "village",
            currentPlace = "Piata",
            currentPlaceType = "market",
            worldName = "world",
            worldTime = 6000L,
            economyBalance = 100,
            activeQuestCount = 2
        )
        assertEquals("satul_central", snap.currentRegion)
        assertEquals("village", snap.currentRegionType)
        assertEquals("Piata", snap.currentPlace)
        assertEquals(100, snap.economyBalance)
        assertEquals(2, snap.activeQuestCount)
    }

    @Test
    fun snapshotWithEnvironmentData() {
        val snap = ContextSnapshot(
            playerName = "P", playerLevel = 1, playerHealth = 20.0, playerFood = 20,
            worldWeather = "clear",
            worldBiome = "plains",
            worldTimePeriod = "day"
        )
        assertEquals("clear", snap.worldWeather)
        assertEquals("plains", snap.worldBiome)
        assertEquals("day", snap.worldTimePeriod)
    }

    @Test
    fun promptBlockIncludesEnvironment() {
        val snap = ContextSnapshot(
            playerName = "P", playerLevel = 1, playerHealth = 20.0, playerFood = 20,
            worldWeather = "rain",
            worldBiome = "forest",
            worldTimePeriod = "night"
        )
        val block = snap.toPromptBlock()
        assertTrue(block.contains("rain"))
        assertTrue(block.contains("forest"))
        assertTrue(block.contains("night"))
    }

    @Test
    fun timePeriodAtSunset() {
        val snap = ContextSnapshot(
            playerName = "P", playerLevel = 1, playerHealth = 20.0, playerFood = 20,
            worldTime = 8000L,
            worldTimePeriod = "sunset"
        )
        assertEquals("sunset", snap.worldTimePeriod)
    }

    @Test
    fun timePeriodAtNight() {
        val snap = ContextSnapshot(
            playerName = "P", playerLevel = 1, playerHealth = 20.0, playerFood = 20,
            worldTime = 14000L,
            worldTimePeriod = "night"
        )
        assertEquals("night", snap.worldTimePeriod)
    }

    @Test
    fun snapshotWithRecentEvents() {
        val snap = ContextSnapshot(
            playerName = "P", playerLevel = 1, playerHealth = 20.0, playerFood = 20,
            recentEvents = listOf("quest_completed: A ajutat la ferma", "story_event: A sosit un calator")
        )
        assertEquals(2, snap.recentEvents.size)
        assertTrue(snap.recentEvents[0].contains("quest_completed"))
    }

    @Test
    fun promptBlockIncludesRecentEvents() {
        val snap = ContextSnapshot(
            playerName = "P", playerLevel = 1, playerHealth = 20.0, playerFood = 20,
            recentEvents = listOf("quest_completed: Q01", "story_event: Evening bell")
        )
        val block = snap.toPromptBlock()
        assertTrue(block.contains("Recent events"))
        assertTrue(block.contains("Q01"))
    }

    @Test
    fun snapshotWithWarnings() {
        val snap = ContextSnapshot(
            playerName = "P",
            playerLevel = 1,
            playerHealth = 20.0,
            playerFood = 20,
            warnings = listOf("World admin dezactivat.")
        )
        assertEquals(1, snap.warnings.size)
    }

    @Test
    fun snapshotWithLocalQuestAnchors() {
        val snap = ContextSnapshot(
            playerName = "P", playerLevel = 1, playerHealth = 20.0, playerFood = 20,
            localQuestAnchors = listOf("place:market (q01_objective)", "region:spawn (q02_start)")
        )
        assertEquals(2, snap.localQuestAnchors.size)
        val block = snap.toPromptBlock()
        assertTrue(block.contains("Local quest anchors"))
        assertTrue(block.contains("q01_objective"))
    }

    @Test
    fun snapshotWithLocalEconomy() {
        val snap = ContextSnapshot(
            playerName = "P", playerLevel = 1, playerHealth = 20.0, playerFood = 20,
            localEconomySummary = "3 NPC shops"
        )
        assertEquals("3 NPC shops", snap.localEconomySummary)
        val block = snap.toPromptBlock()
        assertTrue(block.contains("Local economy"))
    }

    @Test
    fun snapshotWithSafetyAndComfort() {
        val snap = ContextSnapshot(
            playerName = "P", playerLevel = 1, playerHealth = 20.0, playerFood = 20,
            settlementSafety = "region_type=village",
            settlementComfort = "place_type=market"
        )
        assertEquals("region_type=village", snap.settlementSafety)
        assertEquals("place_type=market", snap.settlementComfort)
        val block = snap.toPromptBlock()
        assertTrue(block.contains("Settlement safety"))
        assertTrue(block.contains("Settlement comfort"))
    }

    @Test
    fun toPromptBlockWithAllData() {
        val snap = ContextSnapshot(
            playerName = "Player1",
            playerLevel = 10,
            playerHealth = 18.0,
            playerFood = 16,
            npcName = "Ion",
            npcOccupation = "fermier",
            currentRegion = "satul_central",
            currentRegionType = "village",
            economyBalance = 50,
            activeQuestCount = 1
        )
        val block = snap.toPromptBlock()
        assertTrue(block.contains("Player1"))
        assertTrue(block.contains("fermier"))
        assertTrue(block.contains("satul_central"))
        assertTrue(block.contains("50 coins"))
        assertTrue(block.contains("1"))
    }

    @Test
    fun toPromptBlockMinimal() {
        val snap = ContextSnapshot(
            playerName = "P",
            playerLevel = 1,
            playerHealth = 20.0,
            playerFood = 20
        )
        val block = snap.toPromptBlock()
        assertTrue(block.contains("Context Snapshot"))
        assertTrue(block.contains("P"))
    }
}
