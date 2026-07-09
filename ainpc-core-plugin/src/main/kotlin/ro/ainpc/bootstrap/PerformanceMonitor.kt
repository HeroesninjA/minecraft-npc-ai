package ro.ainpc.bootstrap

import ro.ainpc.AINPCPlugin
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong

class PerformanceMonitor(private val plugin: AINPCPlugin) {
    private val tickTimings = ConcurrentLinkedDeque<TickTiming>()
    private val maxSamples = 100

    data class TickTiming(
        val timestamp: Long,
        val durationMs: Long,
        val npcCount: Int,
        val label: String
    )

    data class TickProfile(
        val avgDurationMs: Double,
        val maxDurationMs: Long,
        val minDurationMs: Long,
        val sampleCount: Int,
        val lastDurationMs: Long,
        val npcCount: Int,
        val label: String
    )

    private val timers = mutableMapOf<String, TickTimer>()

    inner class TickTimer(private val name: String) {
        private val start = AtomicLong(0)

        fun begin() {
            start.set(System.currentTimeMillis())
        }

        fun end(npcCount: Int = 0) {
            val duration = System.currentTimeMillis() - start.get()
            record(name, duration, npcCount)
        }
    }

    fun timer(name: String): TickTimer = timers.getOrPut(name) { TickTimer(name) }

    fun record(label: String, durationMs: Long, npcCount: Int = 0) {
        tickTimings.addFirst(TickTiming(System.currentTimeMillis(), durationMs, npcCount, label))
        while (tickTimings.size > maxSamples) {
            tickTimings.pollLast()
        }
    }

    fun getProfile(label: String): TickProfile? {
        val samples = tickTimings.filter { it.label == label }
        if (samples.isEmpty()) return null
        return TickProfile(
            avgDurationMs = samples.map { it.durationMs }.average(),
            maxDurationMs = samples.maxOf { it.durationMs },
            minDurationMs = samples.minOf { it.durationMs },
            sampleCount = samples.size,
            lastDurationMs = samples.first().durationMs,
            npcCount = samples.firstOrNull()?.npcCount ?: 0,
            label = label
        )
    }

    fun getAllProfiles(): List<TickProfile> {
        return tickTimings.map { it.label }.distinct().mapNotNull { getProfile(it) }
    }

    fun getWorstPerformer(): TickProfile? {
        return getAllProfiles().maxByOrNull { it.avgDurationMs }
    }

    fun formatSummary(): String {
        val profiles = getAllProfiles()
        if (profiles.isEmpty()) return "Nicio metrica disponibila."
        val sb = StringBuilder()
        sb.append("&6=== Performanta Tick-uri ===\n")
        for (p in profiles.sortedByDescending { it.avgDurationMs }) {
            val color = when {
                p.avgDurationMs > 50 -> "&c"
                p.avgDurationMs > 20 -> "&e"
                else -> "&a"
            }
            sb.append("$color${p.label}&7: ${"%.1f".format(p.avgDurationMs)}ms avg &8(${p.minDurationMs}-${p.maxDurationMs}ms, ${p.sampleCount} samples, ${p.npcCount} NPC)\n")
        }
        return sb.toString()
    }
}
