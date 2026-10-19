package ro.ainpc.engine

import org.bukkit.Location
import org.bukkit.World
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

class SocialTargetProximityTest {
    @Test
    fun `only nearby targets in the same world remain eligible`() {
        val world = world("village")
        val otherWorld = world("other")
        val origin = Location(world, 0.0, 64.0, 0.0)

        assertTrue(SocialTargetProximity.hasNearbyTarget(origin, listOf(Location(world, 8.0, 64.0, 0.0)), 8.0))
        assertFalse(SocialTargetProximity.hasNearbyTarget(origin, listOf(Location(world, 8.1, 64.0, 0.0)), 8.0))
        assertFalse(SocialTargetProximity.hasNearbyTarget(origin, listOf(Location(otherWorld, 0.0, 64.0, 0.0)), 8.0))
        assertFalse(SocialTargetProximity.hasNearbyTarget(origin, listOf(Location(null, 0.0, 64.0, 0.0)), 8.0))
    }

    @Test
    fun `stale distant target does not mask a nearby one`() {
        val world = world("village")
        val origin = Location(world, 0.0, 64.0, 0.0)
        val distant = Location(world, 20.0, 64.0, 0.0)
        val nearby = Location(world, 2.0, 64.0, 0.0)

        assertFalse(SocialTargetProximity.hasNearbyTarget(origin, listOf(distant), 8.0))
        assertTrue(SocialTargetProximity.hasNearbyTarget(origin, listOf(distant, nearby), 8.0))
        assertEquals(
            SocialTargetStatus(hasSocialTarget = true, hasInteractingPlayer = false),
            SocialTargetProximity.evaluate(origin, distant, listOf(nearby), 8.0),
        )
        assertEquals(
            SocialTargetStatus(hasSocialTarget = true, hasInteractingPlayer = true),
            SocialTargetProximity.evaluate(origin, nearby, listOf(distant), 8.0),
        )
    }

    @Test
    fun `recent greeting uses elapsed time rather than absolute timestamp`() {
        val now = 1_700_000_000_000L

        assertTrue(SocialTargetProximity.isRecentInteraction(now - 4_999L, now))
        assertFalse(SocialTargetProximity.isRecentInteraction(now - 5_000L, now))
        assertFalse(SocialTargetProximity.isRecentInteraction(0L, now))
        assertFalse(SocialTargetProximity.isRecentInteraction(now + 1L, now))
    }

    @Test
    fun `invalid origin or radius never enables speech`() {
        val world = world("village")
        val origin = Location(world, 0.0, 64.0, 0.0)
        val target = Location(world, 1.0, 64.0, 0.0)

        assertFalse(SocialTargetProximity.hasNearbyTarget(null, listOf(target), 8.0))
        assertFalse(SocialTargetProximity.hasNearbyTarget(Location(null, 0.0, 64.0, 0.0), listOf(target), 8.0))
        assertFalse(SocialTargetProximity.hasNearbyTarget(origin, listOf(target), 0.0))
        assertFalse(SocialTargetProximity.hasNearbyTarget(origin, listOf(target), Double.NaN))
        assertFalse(SocialTargetProximity.hasNearbyTarget(origin, listOf(target), Double.POSITIVE_INFINITY))
    }

    private fun world(name: String): World = Proxy.newProxyInstance(
        World::class.java.classLoader,
        arrayOf(World::class.java),
    ) { proxy, method, args ->
        when (method.name) {
            "getName" -> name
            "equals" -> proxy === args?.firstOrNull()
            "hashCode" -> System.identityHashCode(proxy)
            "toString" -> "World($name)"
            else -> throw UnsupportedOperationException("World.${method.name}")
        }
    } as World
}
