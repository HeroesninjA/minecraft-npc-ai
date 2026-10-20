package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ro.ainpc.npc.NPCAction

class DecisionStatisticsTest {
    @Test
    fun `snapshot counts recent real decisions and keeps insertion order boundary`() {
        val statistics = DecisionStatistics(maxSamples = 3)

        statistics.record(NPCAction.OBSERVE, 1_000L)
        statistics.record(NPCAction.WALK_RANDOM, 2_000L)
        statistics.record(NPCAction.OBSERVE, 3_000L)
        statistics.record(NPCAction.REST, 4_000L)

        val snapshot = statistics.snapshot()
        assertEquals(3, snapshot.total)
        assertEquals(mapOf(NPCAction.WALK_RANDOM to 1, NPCAction.OBSERVE to 1, NPCAction.REST to 1), snapshot.counts)
        assertEquals(2_000L, snapshot.firstTimestamp)
    }

    @Test
    fun `empty snapshot is stable`() {
        val snapshot = DecisionStatistics().snapshot()

        assertEquals(0, snapshot.total)
        assertEquals(emptyMap<NPCAction, Int>(), snapshot.counts)
        assertEquals(null, snapshot.firstTimestamp)
        assertEquals(emptyMap<NPCAction, Double>(), snapshot.expectedCounts)
        assertEquals(null, snapshot.distributionDrift)
    }

    @Test
    fun `snapshot compares observed and expected distributions`() {
        val statistics = DecisionStatistics()
        val probabilities = mapOf(NPCAction.WALK_RANDOM to 0.9, NPCAction.OBSERVE to 0.1)

        statistics.record(NPCAction.WALK_RANDOM, 1_000L, probabilities)
        statistics.record(NPCAction.WALK_RANDOM, 2_000L, probabilities)

        val snapshot = statistics.snapshot()
        assertEquals(mapOf(NPCAction.WALK_RANDOM to 1.8, NPCAction.OBSERVE to 0.2), snapshot.expectedCounts)
        assertEquals(0.1, snapshot.distributionDrift!!, 1e-9)
    }

    @Test
    fun `distribution drift stays unavailable when a sample has no probabilities`() {
        val statistics = DecisionStatistics()
        statistics.record(NPCAction.WALK_RANDOM, 1_000L)
        statistics.record(NPCAction.OBSERVE, 2_000L, mapOf(NPCAction.OBSERVE to 1.0))

        assertEquals(null, statistics.snapshot().distributionDrift)
    }

    @Test
    fun `resizing keeps only the newest samples`() {
        val statistics = DecisionStatistics(maxSamples = 3)
        statistics.record(NPCAction.WALK_RANDOM, 1_000L)
        statistics.record(NPCAction.OBSERVE, 2_000L)
        statistics.record(NPCAction.REST, 3_000L)

        statistics.resize(2)

        val snapshot = statistics.snapshot()
        assertEquals(2, snapshot.total)
        assertEquals(mapOf(NPCAction.OBSERVE to 1, NPCAction.REST to 1), snapshot.counts)
        assertEquals(2_000L, snapshot.firstTimestamp)
    }
}
