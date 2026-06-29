package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegionIdentityProviderTest {

    @Test
    fun everyRegionTypeHasIdentity() {
        for (type in RegionType.values()) {
            val identity = RegionIdentityProvider.identity(type)
            assertNotNull(identity, "Identity must exist for $type")
            assertTrue(identity.displayName.isNotBlank(), "displayName must not be blank for $type")
            assertTrue(identity.description.isNotBlank(), "description must not be blank for $type")
            assertTrue(identity.defaultStoryKey.isNotBlank(), "defaultStoryKey must not be blank for $type")
            assertTrue(identity.suggestedNpcRoles.isNotEmpty(), "suggestedNpcRoles must not be empty for $type")
            assertTrue(identity.suggestedPlaceTypes.isNotEmpty(), "suggestedPlaceTypes must not be empty for $type")
        }
    }

    @Test
    fun settlementHasExpectedRoles() {
        val identity = RegionIdentityProvider.identity(RegionType.SETTLEMENT)
        assertTrue(identity.suggestedNpcRoles.contains("villager"))
        assertTrue(identity.suggestedNpcRoles.contains("merchant"))
        assertTrue(identity.suggestedNpcRoles.contains("blacksmith"))
        assertTrue(identity.suggestedPlaceTypes.contains("house"))
        assertTrue(identity.suggestedPlaceTypes.contains("market"))
    }

    @Test
    fun castleHasExpectedRoles() {
        val identity = RegionIdentityProvider.identity(RegionType.CASTLE)
        assertTrue(identity.suggestedNpcRoles.contains("guard"))
        assertTrue(identity.suggestedNpcRoles.contains("knight"))
        assertTrue(identity.suggestedPlaceTypes.contains("barracks"))
        assertTrue(identity.suggestedPlaceTypes.contains("throne_room"))
    }

    @Test
    fun dungeonHasHighThreat() {
        val identity = RegionIdentityProvider.identity(RegionType.DUNGEON)
        assertEquals("ridicat", identity.threatLevel)
        assertTrue(identity.suggestedNpcRoles.contains("creature"))
        assertTrue(identity.suggestedPlaceTypes.contains("cell"))
    }

    @Test
    fun identityByStringType() {
        val identity = RegionIdentityProvider.identity("settlement")
        assertEquals("Asezare/Stat", identity.displayName)
        assertEquals("pacific", identity.mood)
    }

    @Test
    fun unknownTypeDefaultsToCustom() {
        val identity = RegionIdentityProvider.identity("nonexistent_type")
        assertEquals("Personalizat", identity.displayName)
        assertEquals("neutru", identity.mood)
    }

    @Test
    fun summaryLoreReturnsNonEmptyForAllTypes() {
        for (type in RegionType.values()) {
            val lore = RegionIdentityProvider.summaryLore(type)
            assertTrue(lore.isNotEmpty(), "summaryLore must not be empty for $type")
            assertTrue(lore.any { it.contains("Tip:") }, "Lore must contain type info for $type")
            assertTrue(lore.any { it.contains("Poveste:") }, "Lore must contain story key for $type")
        }
    }
}
