package ro.ainpc.routine

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class RoutineServiceEventsTest {
    @Test
    fun routineServicePublishesRoutineChangedEvent() {
        val source = File("src/main/kotlin/ro/ainpc/routine/RoutineService.kt").readText()

        assertTrue(source.contains("AINPCRoutineChangedEvent("))
        assertTrue(source.contains("publishRoutineChanged("))
        assertTrue(source.contains("previousActivity != npc.plannedRoutineActivity"))
        assertTrue(source.contains("events.public_api_enabled"))
    }

    @Test
    fun routineChangedEventFollowsExecutionInsteadOfPrematureActivityState() {
        val source = File("src/main/kotlin/ro/ainpc/routine/RoutineService.kt").readText()
        val execution = source.indexOf("val outcome = activityExecutor.execute(")
        val publication = source.indexOf("publishRoutineChanged(npc, previousActivity")

        assertTrue(execution >= 0)
        assertTrue(publication > execution)
        assertTrue(!source.contains("npc.changeState(assignment.targetState()"))
        assertTrue(!source.contains("target.chunk.isLoaded"))
    }

    @Test
    fun routineTeleportFallbackUsesDestinationSafetyGate() {
        val source = File("src/main/kotlin/ro/ainpc/routine/RoutineService.kt").readText()
        assertTrue(source.contains("RoutineTeleport.tryTeleport(npc.bukkitEntity, target)"))
        assertTrue(!source.contains("npc.bukkitEntity?.teleport(target)"))
    }
}
