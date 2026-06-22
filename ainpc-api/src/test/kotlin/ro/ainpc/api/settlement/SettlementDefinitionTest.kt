package ro.ainpc.api.settlement

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettlementDefinitionTest {

    @Test
    fun settlementDefinitionDefaults() {
        val def = SettlementDefinition(
            id = "test_sat",
            worldName = "world",
            centerX = 0, centerY = 64, centerZ = 0,
            radius = 50
        )
        assertEquals("test_sat", def.id)
        assertEquals("compact", def.profileId)
        assertEquals("village", def.regionType)
        assertEquals("medieval", def.themeId)
    }

    @Test
    fun settlementDefinitionResolvesCompactProfile() {
        val def = SettlementDefinition(
            id = "test", worldName = "world",
            centerX = 0, centerY = 64, centerZ = 0, radius = 50
        )
        val profile = def.resolvedProfile()
        assertEquals("compact", profile.profileId)
    }

    @Test
    fun settlementDefinitionResolvesAllProfiles() {
        val profiles = listOf("spacious", "rural", "fortified")
        for (p in profiles) {
            val def = SettlementDefinition(
                id = "test_$p", worldName = "world",
                centerX = 0, centerY = 64, centerZ = 0, radius = 50,
                profileId = p
            )
            assertEquals(p, def.resolvedProfile().profileId)
        }
    }

    @Test
    fun compactProfileDefaults() {
        val p = SettlementLayoutProfile.compact()
        assertEquals("compact", p.profileId)
        assertTrue(p.houseDensity > 0)
        assertTrue(p.marketSize > 0)
        assertTrue(p.roadWidth > 0)
    }

    @Test
    fun spaciousProfile() {
        val p = SettlementLayoutProfile.spacious()
        assertEquals("spacious", p.profileId)
        assertTrue(p.houseDensity < SettlementLayoutProfile.compact().houseDensity)
    }

    @Test
    fun ruralProfile() {
        val p = SettlementLayoutProfile.rural()
        assertEquals("rural", p.profileId)
        assertTrue(p.minDistanceBetweenHouses > SettlementLayoutProfile.compact().minDistanceBetweenHouses)
    }

    @Test
    fun fortifiedProfile() {
        val p = SettlementLayoutProfile.fortified()
        assertEquals("fortified", p.profileId)
    }

    @Test
    fun settlementDefinitionWithAllFields() {
        val def = SettlementDefinition(
            id = "satul_mare",
            worldName = "lumea_mea",
            centerX = 100, centerY = 64, centerZ = 200,
            radius = 100,
            profileId = "spacious",
            regionType = "town",
            themeId = "fantasy",
            displayName = "Satul Mare",
            tags = setOf("capital", "trade"),
            metadata = mapOf("population" to "500")
        )
        assertEquals("Satul Mare", def.displayName)
        assertTrue(def.tags.contains("capital"))
        assertEquals("500", def.metadata["population"])
        val profile = def.resolvedProfile()
        assertEquals("spacious", profile.profileId)
    }
}
