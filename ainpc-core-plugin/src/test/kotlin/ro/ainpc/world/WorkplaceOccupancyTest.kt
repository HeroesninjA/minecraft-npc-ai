package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class WorkplaceOccupancyTest {
    @Test
    fun countsDistinctWorkerIdsWithBothSeparatorsAndIgnoresBlankEntries() {
        val metadata = mapOf("worker_npc_ids" to " NPC_1, npc_1 ; NPC_2; ;,npc_2 ")
        assertEquals(setOf("npc_1", "npc_2"), WorkplaceOccupancy.workerIds(metadata))
        assertEquals(1, WorkplaceOccupancy.availableSlots(4, metadata, 1))
    }

    @Test
    fun missingOccupancyLeavesOnlyInPlanReservations() {
        assertEquals(2, WorkplaceOccupancy.availableSlots(3, emptyMap(), 1))
        assertEquals(3, WorkplaceOccupancy.availableSlots(3, mapOf("worker_npc_ids" to " , ; ")))
    }

    @Test
    fun oversubscribedCapacityNeverCreatesNegativeOrOverflowedAvailability() {
        val metadata = mapOf("worker_npc_ids" to "npc_1,npc_2")
        assertEquals(0, WorkplaceOccupancy.availableSlots(1, metadata))
        assertEquals(0, WorkplaceOccupancy.availableSlots(0, metadata, Int.MAX_VALUE))
        assertEquals(Int.MAX_VALUE - 2, WorkplaceOccupancy.availableSlots(Int.MAX_VALUE, metadata))
    }

    @Test
    fun rejectsNegativeCapacityOrReservation() {
        assertThrows(IllegalArgumentException::class.java) { WorkplaceOccupancy.availableSlots(-1, emptyMap()) }
        assertThrows(IllegalArgumentException::class.java) { WorkplaceOccupancy.availableSlots(1, emptyMap(), -1) }
    }
}
