package ro.ainpc.worldgen

import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.world.WorldRegionInfo

class ActivityBuildRegionPolicyTest {
    private fun region(id: String = "test", world: String = "world", minX: Int = 0, maxX: Int = 10) =
        WorldRegionInfo(id, id, world, "settlement", minX, 60, 0, maxX, 80, 10, emptyList(), null, null, emptyList())

    private fun operation(blockX: Int = 1, world: String = "world") = BlockOperation(world, blockX, 65, 1, Material.STONE)

    @Test
    fun allowsOnlyExplicitlyListedRegionContainingEveryOperation() {
        assertTrue(ActivityBuildRegionPolicy.allows(listOf(operation(0), operation(10)), setOf("test"), listOf(region())))
        assertFalse(ActivityBuildRegionPolicy.allows(listOf(operation()), emptySet(), listOf(region())))
        assertFalse(ActivityBuildRegionPolicy.allows(listOf(operation()), setOf("missing"), listOf(region())))
        assertFalse(ActivityBuildRegionPolicy.allows(emptyList(), setOf("test"), listOf(region())))
        assertFalse(ActivityBuildRegionPolicy.allows(listOf(operation(11)), setOf("test"), listOf(region())))
    }

    @Test
    fun refusesSpanningMultipleRegionsOrWorlds() {
        val regions = listOf(region("left", maxX = 5), region("right", minX = 6))
        assertFalse(ActivityBuildRegionPolicy.allows(listOf(operation(1), operation(9)), setOf("left", "right"), regions))
        assertFalse(ActivityBuildRegionPolicy.allows(listOf(operation(), operation(world = "other")), setOf("test"), listOf(region())))
        assertFalse(ActivityBuildRegionPolicy.allows(listOf(operation(world = "other")), setOf("test"), listOf(region())))
    }

    @Test
    fun currentRegionRemovalAndResizeRevokeAuthorization() {
        val operations = listOf(operation(9))
        assertTrue(ActivityBuildRegionPolicy.allows(operations, setOf("test"), listOf(region())))
        assertFalse(ActivityBuildRegionPolicy.allows(operations, setOf("test"), emptyList()))
        assertFalse(ActivityBuildRegionPolicy.allows(operations, setOf("test"), listOf(region(maxX = 5))))
        assertFalse(ActivityBuildRegionPolicy.allows(listOf(operation().copy(y = 81)), setOf("test"), listOf(region())))
    }
}
