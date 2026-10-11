package ro.ainpc.routine

import com.destroystokyo.paper.entity.Pathfinder
import java.lang.reflect.Proxy
import org.bukkit.Location
import org.bukkit.World
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState

class RoutineNavigationTest {
    @Test
    fun missingPathDoesNotStartNavigation() {
        val fixture = Fixture()
        fixture.hasPath = false
        assertFalse(fixture.start())
        assertEquals(1, fixture.findCalls)
        assertEquals(0, fixture.moveCalls)
    }

    @Test
    fun unreachablePathIsRejectedEvenWhenEndpointIsNearby() {
        val fixture = Fixture()
        fixture.reachable = false
        assertFalse(fixture.start())
        assertEquals(0, fixture.moveCalls)
    }

    @Test
    fun missingOrInvalidEndpointIsRejected() {
        val fixture = Fixture()
        for (endpoint in listOf(
            null,
            Location(null, 10.0, 65.0, 20.0),
            Location(fixture.world, Double.NaN, 65.0, 20.0),
            Location(fixture.world, 10.0, Double.POSITIVE_INFINITY, 20.0),
            Location(fixture.world, 10.0, 65.0, Double.NEGATIVE_INFINITY),
            Location(world(), 10.0, 65.0, 20.0),
        )) {
            fixture.endpoint = endpoint
            assertFalse(fixture.start())
        }
        assertEquals(0, fixture.moveCalls)
    }

    @Test
    fun endpointMustBeInsideInclusiveThreeDimensionalArrivalRadius() {
        val fixture = Fixture()
        fixture.endpoint = Location(fixture.world, 13.0, 69.0, 20.0)
        assertFalse(fixture.start(radius = 4.99))
        assertTrue(fixture.start(radius = 5.0))
        assertEquals(1, fixture.moveCalls)
    }

    @Test
    fun acceptedPathUsesCalculatedResultWithoutRecomputingLocationRoute() {
        val fixture = Fixture()
        assertTrue(fixture.start(speed = 1.25))
        assertEquals(1, fixture.findCalls)
        assertEquals(1, fixture.moveCalls)
        assertEquals(1.25, fixture.requestedSpeed)
        assertEquals(listOf("setCanOpenDoors", "setCanPassDoors", "findPath", "moveTo"), fixture.calls)
    }

    @Test
    fun engineRefusalAfterValidPathIsPropagated() {
        val fixture = Fixture()
        fixture.acceptMovement = false
        assertFalse(fixture.start())
        assertEquals(1, fixture.moveCalls)
    }

    @Test
    fun invalidSpeedRadiusOrTargetNeverCallsPathfinder() {
        val fixture = Fixture()
        for (value in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertFalse(fixture.start(speed = value))
            assertFalse(fixture.start(radius = value))
        }
        for (target in listOf(
            Location(null, 10.0, 65.0, 20.0),
            Location(fixture.world, Double.NaN, 65.0, 20.0),
            Location(fixture.world, 10.0, Double.POSITIVE_INFINITY, 20.0),
        )) {
            assertFalse(RoutineNavigation.start(fixture.pathfinder, target, 1.0, 2.0))
        }
        assertTrue(fixture.calls.isEmpty())
    }

    @Test
    fun partialPathLeavesRoutineIdleRatherThanReportingWalking() {
        val fixture = Fixture()
        fixture.reachable = false
        val npc = AINPC(null)
        val executor = RoutineActivityExecutor(
            resolveLocation = { fixture.target },
            currentLocation = { Location(fixture.world, 0.0, 65.0, 0.0) },
        )
        val assignment = RoutineAssignment(
            RoutineSlot.WORK, "work", "shop", NPCState.WORKING,
            AINPC.OwnedLocation("work", "shop", "world", 10.0, 65.0, 20.0),
        )
        val outcome = executor.execute(npc, assignment, 2.0, { fixture.start() }, { false })
        assertEquals(RoutineActivityExecutor.Outcome.WAITING, outcome)
        assertEquals(NPCState.IDLE, npc.currentState)
        assertEquals(0, fixture.moveCalls)
    }

    private class Fixture {
        val world = world()
        val target = Location(world, 10.0, 65.0, 20.0)
        var endpoint: Location? = target.clone()
        var hasPath = true
        var reachable = true
        var acceptMovement = true
        var findCalls = 0
        var moveCalls = 0
        var requestedSpeed: Double? = null
        val calls = mutableListOf<String>()
        val path = proxy(Pathfinder.PathResult::class.java) { method, _ ->
            when (method) {
                "canReachFinalPoint" -> reachable
                "getFinalPoint" -> endpoint
                else -> error("Unexpected path access: $method")
            }
        }
        val pathfinder = proxy(Pathfinder::class.java) { method, arguments ->
            calls.add(method)
            when (method) {
                "setCanOpenDoors", "setCanPassDoors" -> {
                    assertEquals(true, arguments!![0])
                    null
                }
                "findPath" -> {
                    assertSame(target, arguments!![0])
                    findCalls++
                    if (hasPath) path else null
                }
                "moveTo" -> {
                    assertSame(path, arguments!![0])
                    requestedSpeed = arguments[1] as Double
                    moveCalls++
                    acceptMovement
                }
                else -> error("Unexpected pathfinder access: $method")
            }
        }

        fun start(speed: Double = 1.0, radius: Double = 2.0): Boolean =
            RoutineNavigation.start(pathfinder, target, speed, radius)
    }

    private companion object {
        fun world(): World = proxy(World::class.java) { method, _ ->
            when (method) {
                "isChunkLoaded" -> true
                else -> error("Unexpected world access: $method")
            }
        }

        fun <Type> proxy(type: Class<Type>, handler: (String, Array<out Any?>?) -> Any?): Type = type.cast(
            Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { instance, method, arguments ->
                when (method.name) {
                    "equals" -> instance === arguments?.get(0)
                    "hashCode" -> System.identityHashCode(instance)
                    "toString" -> type.simpleName
                    else -> handler(method.name, arguments)
                }
            },
        )
    }
}
