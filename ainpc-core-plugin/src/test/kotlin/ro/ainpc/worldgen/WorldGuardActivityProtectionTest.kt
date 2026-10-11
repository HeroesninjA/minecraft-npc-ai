package ro.ainpc.worldgen

import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldGuardActivityProtectionTest {
    private val operations = listOf(BlockOperation("world", 1, 65, 1, Material.STONE), BlockOperation("world", 2, 65, 1, Material.AIR))

    @Test
    fun checksEveryCellIncludingAirAndCreatesFreshQueryEachTime() {
        var queries = 0
        val checked = mutableListOf<BlockOperation>()
        val protection = WorldGuardActivityProtection { actor, world ->
            assertEquals("player:test", actor)
            assertEquals("world", world)
            queries++
            val check: (BlockOperation) -> Boolean = { checked.add(it); true }
            check
        }
        repeat(2) { assertTrue(protection.allows("player:test", operations)) }
        assertEquals(2, queries)
        assertEquals(operations + operations, checked)
    }

    @Test
    fun missingDisabledOrIncompatibleProtectionFailsClosed() {
        assertFalse(WorldGuardActivityProtection { _, _ -> null }.allows("player:test", operations))
        assertFalse(WorldGuardActivityProtection { _, _ -> throw IllegalStateException("Unavailable") }.allows("player:test", operations))
        assertFalse(WorldGuardActivityProtection { _, _ -> throw NoClassDefFoundError("Missing API") }.allows("player:test", operations))
        val protection = WorldGuardActivityProtection { _, _ -> { operation -> operation.x != 2 } }
        assertFalse(protection.allows("player:test", operations))
    }

    @Test
    fun changedPermissionIsNotCachedBetweenRequests() {
        var allowed = true
        val protection = WorldGuardActivityProtection { _, _ -> { _ -> allowed } }
        assertTrue(protection.allows("player:test", operations))
        allowed = false
        assertFalse(protection.allows("player:test", operations))
    }

    @Test
    fun malformedRequestsDoNotQueryProtection() {
        val protection = WorldGuardActivityProtection { _, _ -> error("Unexpected query") }
        assertFalse(protection.allows("console", operations))
        assertFalse(protection.allows("player:test", emptyList()))
        assertFalse(protection.allows("player:test", operations + operations.first().copy(worldName = "other")))
        assertFalse(protection.allows("player:test", List(10001) { operations.first() }))
    }
}
