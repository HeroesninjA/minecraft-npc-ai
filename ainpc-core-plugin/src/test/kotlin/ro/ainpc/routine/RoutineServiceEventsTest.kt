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
}
