package ro.ainpc.bootstrap

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PerformanceMonitorTest {
    @Test
    fun classifiesMetricsAgainstDeclaredDomainBudgets() {
        val monitor = PerformanceMonitor()
        monitor.record(RuntimeMetricNames.ROUTINE_TICK, 10)
        monitor.record(RuntimeMetricNames.DATABASE_OPERATION, 70)
        monitor.record(RuntimeMetricNames.COMMAND_DISPATCH, 1_500)

        val snapshot = monitor.snapshot()

        assertEquals("FAIL", snapshot.status)
        assertEquals("PASS", snapshot.metrics.single { it.name == RuntimeMetricNames.ROUTINE_TICK }.status)
        assertEquals("WARN", snapshot.metrics.single { it.name == RuntimeMetricNames.DATABASE_OPERATION }.status)
        assertEquals("FAIL", snapshot.metrics.single { it.name == RuntimeMetricNames.COMMAND_DISPATCH }.status)
    }

    @Test
    fun boundsSeriesAndSamplesWithoutUsingCallerCardinality() {
        val monitor = PerformanceMonitor()
        repeat(120) { monitor.record(RuntimeMetricNames.ROUTINE_TICK, 5) }
        repeat(40) { index -> monitor.record("scheduler.task_$index", 1) }

        val snapshot = monitor.snapshot()
        val routine = snapshot.metrics.single { it.name == RuntimeMetricNames.ROUTINE_TICK }

        assertEquals(32, snapshot.activeSeries)
        assertEquals(9, snapshot.droppedMeasurements)
        assertEquals(120, routine.totalCount)
        assertEquals(100, routine.windowSamples)
    }

    @Test
    fun keepsRecentFailuresVisibleInsideTheBoundedWindow() {
        val monitor = PerformanceMonitor()
        monitor.record(RuntimeMetricNames.AI_ORCHESTRATION, 25, success = false)
        monitor.record(RuntimeMetricNames.AI_ORCHESTRATION, 20, success = true)

        val metric = monitor.snapshot().metrics.single()

        assertEquals("WARN", metric.status)
        assertEquals(1, metric.windowFailures)
        assertTrue(metric.lastDurationMillis >= 0.0)
    }

    @Test
    fun tracesOnlyFailuresAndBudgetBreaches() {
        val monitor = PerformanceMonitor()
        monitor.record(RuntimeMetricNames.ROUTINE_TICK, 19)
        monitor.record(RuntimeMetricNames.DATABASE_OPERATION, 50, npcCount = 4)
        monitor.record(RuntimeMetricNames.COMMAND_DISPATCH, 1_000)
        monitor.record(RuntimeMetricNames.AI_ORCHESTRATION, 25, success = false)

        val trace = monitor.traceSnapshot()

        assertEquals(1, trace.schemaVersion)
        assertTrue(trace.enabled)
        assertEquals("failures_or_budget_exceeded", trace.policy)
        assertEquals(64, trace.maxSpans)
        assertEquals(3, trace.retainedSpans)
        assertEquals(0, trace.droppedSpans)
        assertEquals(
            listOf("failure", "fail_budget", "warn_budget"),
            trace.spans.map { it.trigger },
        )
        assertEquals(
            listOf(
                RuntimeMetricNames.AI_ORCHESTRATION,
                RuntimeMetricNames.COMMAND_DISPATCH,
                RuntimeMetricNames.DATABASE_OPERATION,
            ),
            trace.spans.map { it.metric },
        )
        assertEquals(4, trace.spans.last().itemCount)
    }

    @Test
    fun boundsTraceRetentionAndKeepsNewestSpansFirst() {
        val monitor = PerformanceMonitor()
        repeat(80) { monitor.record(RuntimeMetricNames.DATABASE_OPERATION, 50) }

        val trace = monitor.traceSnapshot()

        assertEquals(64, trace.retainedSpans)
        assertEquals(16, trace.droppedSpans)
        assertEquals(80, trace.spans.first().sequence)
        assertEquals(17, trace.spans.last().sequence)
    }

    @Test
    fun traceCardinalityCannotExceedAcceptedMetricSeries() {
        val monitor = PerformanceMonitor()
        repeat(40) { index -> monitor.record("scheduler.task_$index", 250) }

        val health = monitor.snapshot()
        val trace = monitor.traceSnapshot()
        val acceptedNames = health.metrics.mapTo(HashSet()) { it.name }

        assertEquals(32, health.activeSeries)
        assertEquals(8, health.droppedMeasurements)
        assertEquals(32, trace.retainedSpans)
        assertEquals(32, trace.spans.map { it.metric }.distinct().size)
        assertTrue(trace.spans.all { it.metric in acceptedNames })
    }

    @Test
    fun keepsTraceRetentionStrictUnderConcurrentAnomalies() {
        val monitor = PerformanceMonitor()
        val executor = Executors.newFixedThreadPool(8)
        try {
            repeat(8) {
                executor.submit {
                    repeat(100) { monitor.record(RuntimeMetricNames.DATABASE_OPERATION, 50) }
                }
            }
        } finally {
            executor.shutdown()
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
        }

        val trace = monitor.traceSnapshot()

        assertEquals(64, trace.retainedSpans)
        assertEquals(736, trace.droppedSpans)
        assertEquals(800, trace.retainedSpans + trace.droppedSpans)
        assertEquals(800, trace.spans.first().sequence)
        assertEquals(737, trace.spans.last().sequence)
    }
}
