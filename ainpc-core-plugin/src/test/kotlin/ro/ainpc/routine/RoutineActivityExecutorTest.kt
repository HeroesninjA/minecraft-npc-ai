package ro.ainpc.routine

import java.lang.reflect.Proxy
import org.bukkit.Location
import org.bukkit.World
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState

class RoutineActivityExecutorTest {
    @Test
    fun workHomeAndSocialStartAtInclusiveThreeDimensionalArrivalBoundary() {
        for (slot in listOf(RoutineSlot.WORK, RoutineSlot.HOME, RoutineSlot.SOCIAL)) {
            val fixture = Fixture()
            fixture.location = Location(fixture.world, 13.0, 69.0, 20.0)

            assertEquals(RoutineActivityExecutor.Outcome.ARRIVED, fixture.execute(fixture.assignment(slot), 5.0))
            assertEquals(fixture.stateFor(slot), fixture.npc.currentState)
            fixture.assertNoMovement()
        }
    }

    @Test
    fun verticalSeparationOutsideArrivalRadiusDoesNotStartActivity() {
        for (slot in listOf(RoutineSlot.WORK, RoutineSlot.HOME, RoutineSlot.SOCIAL)) {
            val fixture = Fixture()
            fixture.location = Location(fixture.world, 10.0, 67.01, 20.0)
            fixture.acceptMovement = true

            assertEquals(RoutineActivityExecutor.Outcome.MOVING, fixture.execute(fixture.assignment(slot)))
            assertEquals(NPCState.WALKING, fixture.npc.currentState)
            assertEquals(0, fixture.teleportCalls)
        }
    }

    @Test
    fun acceptedPathRemainsWalkingUntilCurrentLocationActuallyArrives() {
        for (slot in listOf(RoutineSlot.WORK, RoutineSlot.HOME, RoutineSlot.SOCIAL)) {
            val fixture = Fixture()
            fixture.acceptMovement = true
            val assignment = fixture.assignment(slot)

            assertEquals(RoutineActivityExecutor.Outcome.MOVING, fixture.execute(assignment))
            assertEquals(NPCState.WALKING, fixture.npc.currentState)
            assertEquals(NPCState.IDLE, fixture.stateAtMove)
            assertEquals(RoutineActivityExecutor.Outcome.MOVING, fixture.execute(assignment))
            assertEquals(NPCState.WALKING, fixture.npc.currentState)

            fixture.location = fixture.target!!.clone()

            assertEquals(RoutineActivityExecutor.Outcome.ARRIVED, fixture.execute(assignment))
            assertEquals(fixture.stateFor(slot), fixture.npc.currentState)
            assertEquals(2, fixture.moveCalls)
            assertEquals(0, fixture.teleportCalls)
        }
    }

    @Test
    fun failedPathAndRefusedTeleportLeaveNpcIdleInsteadOfWorking() {
        val fixture = Fixture()
        assertTrue(fixture.npc.changeState(NPCState.WORKING))

        assertEquals(RoutineActivityExecutor.Outcome.WAITING, fixture.execute())
        assertEquals(NPCState.IDLE, fixture.npc.currentState)
        assertEquals(NPCState.WORKING, fixture.stateAtMove)
        assertEquals(1, fixture.moveCalls)
        assertEquals(1, fixture.teleportCalls)
    }

    @Test
    fun acceptedTeleportWithoutVerifiedArrivalDoesNotStartActivity() {
        val fixture = Fixture()
        fixture.acceptTeleport = true

        assertEquals(RoutineActivityExecutor.Outcome.WAITING, fixture.execute())
        assertEquals(NPCState.IDLE, fixture.npc.currentState)
        assertEquals(1, fixture.teleportCalls)
    }

    @Test
    fun verifiedTeleportArrivalStartsAssignedActivity() {
        for (slot in listOf(RoutineSlot.WORK, RoutineSlot.HOME, RoutineSlot.SOCIAL)) {
            val fixture = Fixture()
            fixture.acceptTeleport = true
            fixture.teleportChangesLocation = true

            assertEquals(RoutineActivityExecutor.Outcome.TELEPORTED, fixture.execute(fixture.assignment(slot)))
            assertEquals(fixture.stateFor(slot), fixture.npc.currentState)
            assertEquals(fixture.target, fixture.location)
            assertEquals(1, fixture.moveCalls)
            assertEquals(1, fixture.teleportCalls)
        }
    }

    @Test
    fun unloadedDestinationChunkIsRejectedWithoutLoadingOrMovement() {
        val fixture = Fixture()
        fixture.chunkLoaded = false
        fixture.target = Location(fixture.world, 32.0, 65.0, -17.0)

        assertEquals(RoutineActivityExecutor.Outcome.INVALID_TARGET, fixture.execute())
        assertEquals(listOf(2 to -2), fixture.checkedChunks)
        assertEquals(NPCState.IDLE, fixture.npc.currentState)
        fixture.assertNoMovement()
    }

    @Test
    fun unresolvedWorldRejectsDestinationWithoutMovement() {
        val fixture = Fixture()
        fixture.target = null

        assertEquals(RoutineActivityExecutor.Outcome.INVALID_TARGET, fixture.execute())
        assertEquals(NPCState.IDLE, fixture.npc.currentState)
        assertTrue(fixture.checkedChunks.isEmpty())
        fixture.assertNoMovement()
    }

    @Test
    fun invalidAnchorWorldOrCoordinatesAreRejectedBeforeResolvingLocation() {
        val invalidAnchors = listOf(
            AINPC.OwnedLocation("work", "shop", " ", 10.0, 65.0, 20.0),
            AINPC.OwnedLocation("work", "shop", "world", Double.NaN, 65.0, 20.0),
            AINPC.OwnedLocation("work", "shop", "world", 10.0, Double.POSITIVE_INFINITY, 20.0),
            AINPC.OwnedLocation("work", "shop", "world", 10.0, 65.0, Double.NEGATIVE_INFINITY),
        )
        for (anchor in invalidAnchors) {
            val fixture = Fixture()
            assertTrue(fixture.npc.changeState(NPCState.WORKING))

            assertEquals(RoutineActivityExecutor.Outcome.INVALID_TARGET, fixture.execute(fixture.assignment(anchor = anchor)))
            assertEquals(0, fixture.resolveCalls)
            assertEquals(NPCState.IDLE, fixture.npc.currentState)
            fixture.assertNoMovement()
        }
    }

    @Test
    fun invalidResolvedDestinationDoesNotMoveNpc() {
        val fixture = Fixture()
        for (target in listOf(
            Location(null, 10.0, 65.0, 20.0),
            Location(fixture.world, Double.NaN, 65.0, 20.0),
            Location(fixture.world, 10.0, Double.POSITIVE_INFINITY, 20.0),
            Location(fixture.world, 10.0, 65.0, Double.NEGATIVE_INFINITY),
        )) {
            fixture.target = target
            assertEquals(RoutineActivityExecutor.Outcome.INVALID_TARGET, fixture.execute())
            assertEquals(NPCState.IDLE, fixture.npc.currentState)
            fixture.assertNoMovement()
        }
        assertTrue(fixture.checkedChunks.isEmpty())
    }

    @Test
    fun missingOrInvalidCurrentLocationDoesNotMoveNpc() {
        val fixture = Fixture()
        for (location in listOf(
            null,
            Location(null, 10.0, 65.0, 20.0),
            Location(fixture.world, Double.NaN, 65.0, 20.0),
            Location(fixture.world, 10.0, Double.POSITIVE_INFINITY, 20.0),
            Location(fixture.world, 10.0, 65.0, Double.NEGATIVE_INFINITY),
        )) {
            fixture.location = location
            assertEquals(RoutineActivityExecutor.Outcome.INVALID_TARGET, fixture.execute())
            assertEquals(NPCState.IDLE, fixture.npc.currentState)
            fixture.assertNoMovement()
        }
    }

    @Test
    fun nonPositiveOrNonFiniteArrivalRadiusIsRejectedWithoutMovement() {
        val fixture = Fixture()
        fixture.location = fixture.target!!.clone()
        for (radius in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertEquals(RoutineActivityExecutor.Outcome.INVALID_TARGET, fixture.execute(radius = radius))
            assertEquals(NPCState.IDLE, fixture.npc.currentState)
            fixture.assertNoMovement()
        }
    }

    @Test
    fun identicalCoordinatesInAnotherWorldNeverCountAsArrival() {
        val fixture = Fixture()
        val otherWorld = world("other") { true }
        fixture.location = Location(otherWorld, 10.0, 65.0, 20.0)
        fixture.acceptTeleport = true

        assertEquals(RoutineActivityExecutor.Outcome.WAITING, fixture.execute())
        assertEquals(NPCState.IDLE, fixture.npc.currentState)
        assertEquals(1, fixture.moveCalls)
        assertEquals(1, fixture.teleportCalls)
    }

    @Test
    fun missingActivityAnchorClearsStaleWorkState() {
        for (slot in listOf(RoutineSlot.WORK, RoutineSlot.HOME, RoutineSlot.SOCIAL)) {
            val fixture = Fixture()
            assertTrue(fixture.npc.changeState(NPCState.WORKING))

            assertEquals(RoutineActivityExecutor.Outcome.MISSING_TARGET, fixture.execute(fixture.assignment(slot, anchor = null)))
            assertEquals(NPCState.IDLE, fixture.npc.currentState)
            assertEquals(0, fixture.resolveCalls)
            fixture.assertNoMovement()
        }
    }

    @Test
    fun sleepingNpcCanWakeAndWalkToNextScheduledActivity() {
        val fixture = Fixture()
        assertTrue(fixture.npc.changeState(NPCState.SLEEPING))
        fixture.acceptMovement = true

        assertEquals(RoutineActivityExecutor.Outcome.MOVING, fixture.execute())
        assertEquals(NPCState.WALKING, fixture.npc.currentState)
        assertEquals(NPCState.SLEEPING, fixture.stateAtMove)
    }

    @Test
    fun sleepingNpcCanStartActivityWhenAlreadyAtDestination() {
        val fixture = Fixture()
        assertTrue(fixture.npc.changeState(NPCState.SLEEPING))
        fixture.location = fixture.target!!.clone()

        assertEquals(RoutineActivityExecutor.Outcome.ARRIVED, fixture.execute())
        assertEquals(NPCState.WORKING, fixture.npc.currentState)
        fixture.assertNoMovement()
    }

    @Test
    fun idleAssignmentWithoutAnchorPreservesConfiguredCustomState() {
        val fixture = Fixture()
        val assignment = RoutineAssignment(RoutineSlot.IDLE, "rest", "recover", NPCState.RESTING, null)

        assertEquals(RoutineActivityExecutor.Outcome.MISSING_TARGET, fixture.execute(assignment))
        assertEquals(NPCState.RESTING, fixture.npc.currentState)
        fixture.assertNoMovement()
    }

    @Test
    fun protectedInteractionDangerAndStoryStatesAreNotInterrupted() {
        for (state in listOf(
            NPCState.TALKING,
            NPCState.LISTENING,
            NPCState.TRADING,
            NPCState.COMBAT,
            NPCState.FLEEING,
            NPCState.PANICKING,
            NPCState.HIDING,
            NPCState.QUEST_GIVING,
            NPCState.FOLLOWING,
        )) {
            val fixture = Fixture()
            assertTrue(fixture.npc.changeState(state))

            assertEquals(RoutineActivityExecutor.Outcome.BUSY, fixture.execute())
            assertEquals(state, fixture.npc.currentState)
            assertEquals(0, fixture.resolveCalls)
            fixture.assertNoMovement()
        }
    }

    @Test
    fun overrideWalksOnlyAfterAcceptedNavigationAndWaitsAtDestination() {
        val fixture = Fixture()
        fixture.acceptMovement = true
        assertEquals(RoutineActivityExecutor.Outcome.MOVING, fixture.executeOverride())
        assertEquals(NPCState.WALKING, fixture.npc.currentState)
        fixture.location = fixture.target!!.clone()
        assertEquals(RoutineActivityExecutor.Outcome.ARRIVED, fixture.executeOverride())
        assertEquals(NPCState.IDLE, fixture.npc.currentState)
        assertEquals(1, fixture.moveCalls)
        assertEquals(0, fixture.teleportCalls)
    }

    @Test
    fun refusedOverrideNavigationNeverReportsMovementOrTeleports() {
        val fixture = Fixture()
        fixture.acceptTeleport = true
        fixture.npc.changeState(NPCState.WORKING)
        assertEquals(RoutineActivityExecutor.Outcome.WAITING, fixture.executeOverride())
        assertEquals(NPCState.IDLE, fixture.npc.currentState)
        assertEquals(1, fixture.moveCalls)
        assertEquals(0, fixture.teleportCalls)
    }

    @Test
    fun overrideRespectsDisabledMovementAndMaximumDistance() {
        val fixture = Fixture()
        fixture.acceptMovement = true
        assertEquals(RoutineActivityExecutor.Outcome.WAITING, fixture.executeOverride(enabled = false))
        assertEquals(RoutineActivityExecutor.Outcome.WAITING, fixture.executeOverride(maxDistance = 1.0))
        fixture.assertNoMovement()
    }

    @Test
    fun overrideCannotNavigateAcrossWorldsOrIntoUnloadedChunks() {
        val fixture = Fixture()
        fixture.acceptMovement = true
        fixture.location = Location(world("other") { true }, 0.0, 65.0, 0.0)
        assertEquals(RoutineActivityExecutor.Outcome.WAITING, fixture.executeOverride())
        fixture.chunkLoaded = false
        assertEquals(RoutineActivityExecutor.Outcome.INVALID_TARGET, fixture.executeOverride())
        fixture.assertNoMovement()
    }

    @Test
    fun overrideWithoutDestinationReleasesSleepWithoutInventingAnchor() {
        val fixture = Fixture()
        fixture.npc.changeState(NPCState.SLEEPING)
        assertEquals(RoutineActivityExecutor.Outcome.MISSING_TARGET, fixture.executeOverride(anchor = null))
        assertEquals(NPCState.IDLE, fixture.npc.currentState)
        fixture.assertNoMovement()
    }

    @Test
    fun overrideRejectsInvalidAnchorAndPreservesBusyStates() {
        val fixture = Fixture()
        val invalid = AINPC.OwnedLocation("social", "garden", "world", Double.NaN, 65.0, 20.0)
        assertEquals(RoutineActivityExecutor.Outcome.INVALID_TARGET, fixture.executeOverride(anchor = invalid))
        fixture.npc.changeState(NPCState.TALKING)
        assertEquals(RoutineActivityExecutor.Outcome.BUSY, fixture.executeOverride())
        assertEquals(NPCState.TALKING, fixture.npc.currentState)
        fixture.assertNoMovement()
    }

    @Test
    fun unavailableChunkExplainsWaitingAndArrivalRestoresPlannedGoal() {
        val fixture = Fixture()
        fixture.chunkLoaded = false
        fixture.execute()
        assertEquals("chunk-ul destinatiei nu este incarcat", fixture.npc.currentGoal)

        fixture.chunkLoaded = true
        fixture.acceptMovement = true
        fixture.execute()
        assertTrue(fixture.npc.currentGoal.startsWith("deplasare spre shop"))

        fixture.location = fixture.target!!.clone()
        fixture.execute()
        assertEquals("destination", fixture.npc.currentGoal)
    }

    @Test
    fun diagnosticsDistinguishMissingWorldPositionAndInvalidRadius() {
        val fixture = Fixture()
        val target = fixture.target
        fixture.target = null
        fixture.execute()
        assertTrue(fixture.npc.currentGoal.startsWith("destinatia sau lumea"))
        fixture.target = target
        fixture.location = null
        fixture.execute()
        assertTrue(fixture.npc.currentGoal.startsWith("pozitia NPC"))
        fixture.location = target!!.clone()
        fixture.execute(radius = Double.NaN)
        assertTrue(fixture.npc.currentGoal.startsWith("raza de sosire"))
    }

    @Test
    fun diagnosticDoesNotClaimTeleportRefusalWhenMovementWasOnlyUnavailable() {
        val fixture = Fixture()
        fixture.execute()
        assertEquals("deplasare indisponibila; sosire neconfirmata", fixture.npc.currentGoal)
        fixture.acceptTeleport = true
        fixture.execute()
        assertEquals("teleport acceptat; sosire neconfirmata", fixture.npc.currentGoal)
        fixture.teleportChangesLocation = true
        fixture.execute()
        assertEquals("destination", fixture.npc.currentGoal)
    }

    @Test
    fun busyNpcKeepsExistingGoalAndIdleProfileKeepsItsGoal() {
        val fixture = Fixture()
        fixture.npc.currentGoal = "conversatie activa"
        fixture.npc.changeState(NPCState.TALKING)
        fixture.execute()
        assertEquals("conversatie activa", fixture.npc.currentGoal)

        val idle = Fixture()
        idle.execute(RoutineAssignment(RoutineSlot.IDLE, "rest", "recover", NPCState.RESTING, null))
        assertEquals("recover", idle.npc.currentGoal)
    }

    @Test
    fun overrideArrivalClearsPreviousFailureWithoutInventingWorkGoal() {
        val fixture = Fixture()
        fixture.executeOverride()
        assertTrue(fixture.npc.currentGoal.startsWith("deplasare indisponibila"))
        fixture.location = fixture.target!!.clone()
        fixture.executeOverride()
        assertEquals("sa astepte la destinatia impusa", fixture.npc.currentGoal)
        assertEquals(NPCState.IDLE, fixture.npc.currentState)
    }

    private class Fixture {
        val npc = AINPC(null)
        var chunkLoaded = true
        val checkedChunks = mutableListOf<Pair<Int, Int>>()
        val world = world("world") { coordinates ->
            checkedChunks.add(coordinates)
            chunkLoaded
        }
        var target: Location? = Location(world, 10.0, 65.0, 20.0)
        var location: Location? = Location(world, 0.0, 65.0, 0.0)
        var resolveCalls = 0
        var moveCalls = 0
        var teleportCalls = 0
        var acceptMovement = false
        var acceptTeleport = false
        var teleportChangesLocation = false
        var stateAtMove: NPCState? = null
        private val executor = RoutineActivityExecutor(
            resolveLocation = { resolveCalls++; target },
            currentLocation = { location },
        )

        fun stateFor(slot: RoutineSlot): NPCState = when (slot) {
            RoutineSlot.WORK -> NPCState.WORKING
            RoutineSlot.HOME -> NPCState.RESTING
            RoutineSlot.SOCIAL -> NPCState.SOCIALIZING
            RoutineSlot.IDLE -> NPCState.IDLE
        }

        fun assignment(
            slot: RoutineSlot = RoutineSlot.WORK,
            anchor: AINPC.OwnedLocation? = AINPC.OwnedLocation("work", "shop", "world", 10.0, 65.0, 20.0),
        ) = RoutineAssignment(slot, "activity", "destination", stateFor(slot), anchor)

        fun execute(assignment: RoutineAssignment = assignment(), radius: Double = 2.0): RoutineActivityExecutor.Outcome =
            executor.execute(
                npc,
                assignment,
                radius,
                moveNaturally = {
                    moveCalls++
                    stateAtMove = npc.currentState
                    acceptMovement
                },
                teleport = { destination ->
                    teleportCalls++
                    if (acceptTeleport && teleportChangesLocation) location = destination.clone()
                    acceptTeleport
                },
            )

        fun executeOverride(
            enabled: Boolean = true,
            maxDistance: Double = 48.0,
            anchor: AINPC.OwnedLocation? = assignment().targetAnchor(),
        ): RoutineActivityExecutor.Outcome = executor.executeOverride(
            npc,
            RoutineCoordinator.RoutineOverride(forcedSlot = RoutineSlot.WORK, forcedAnchor = anchor),
            2.0,
            enabled,
            maxDistance,
        ) {
            moveCalls++
            acceptMovement
        }

        fun assertNoMovement() {
            assertEquals(0, moveCalls)
            assertEquals(0, teleportCalls)
        }
    }

    private companion object {
        fun world(name: String, chunkLoaded: (Pair<Int, Int>) -> Boolean): World = Proxy.newProxyInstance(
            World::class.java.classLoader,
            arrayOf(World::class.java),
        ) { proxy, method, arguments ->
            when (method.name) {
                "isChunkLoaded" -> chunkLoaded(arguments!![0] as Int to arguments[1] as Int)
                "getName", "toString" -> name
                "equals" -> proxy === arguments?.get(0)
                "hashCode" -> System.identityHashCode(proxy)
                else -> error("Unexpected world access: ${method.name}")
            }
        } as World
    }
}
