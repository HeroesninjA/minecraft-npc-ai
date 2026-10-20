package ro.ainpc.engine

import ro.ainpc.npc.NPCAction
import java.util.ArrayDeque
import java.util.EnumMap

internal class DecisionStatistics(maxSamples: Int = DEFAULT_MAX_SAMPLES) {
    init {
        require(maxSamples in 1..MAX_ALLOWED_SAMPLES)
    }

    @Volatile
    private var sampleLimit = maxSamples

    private data class Sample(
        val action: NPCAction,
        val timestamp: Long,
        val probabilities: Map<NPCAction, Double>,
    )

    private val samples = ArrayDeque<Sample>()

    @Synchronized
    fun resize(maxSamples: Int) {
        require(maxSamples in 1..MAX_ALLOWED_SAMPLES)
        this.sampleLimit = maxSamples
        while (samples.size > sampleLimit) samples.removeFirst()
    }

    @Synchronized
    fun record(
        action: NPCAction,
        timestamp: Long,
        probabilities: Map<NPCAction, Double> = emptyMap(),
    ) {
        samples.addLast(Sample(action, timestamp, probabilities.toMap()))
        while (samples.size > sampleLimit) samples.removeFirst()
    }

    @Synchronized
    fun snapshot(): Snapshot {
        val counts = EnumMap<NPCAction, Int>(NPCAction::class.java)
        val expectedCounts = EnumMap<NPCAction, Double>(NPCAction::class.java)
        var samplesWithProbabilities = 0
        samples.forEach { sample ->
            counts[sample.action] = (counts[sample.action] ?: 0) + 1
            val total = sample.probabilities.values
                .filter { it.isFinite() && it >= 0.0 }
                .sum()
            if (total > 0.0) {
                samplesWithProbabilities++
                sample.probabilities.forEach { (action, probability) ->
                    if (probability.isFinite() && probability >= 0.0) {
                        expectedCounts[action] = (expectedCounts[action] ?: 0.0) + probability / total
                    }
                }
            }
        }
        val drift = if (samples.isNotEmpty() && samplesWithProbabilities == samples.size) {
            val actions = counts.keys + expectedCounts.keys
            actions.sumOf { action ->
                val observedRate = (counts[action] ?: 0).toDouble() / samples.size
                val expectedRate = (expectedCounts[action] ?: 0.0) / samples.size
                kotlin.math.abs(observedRate - expectedRate)
            } / 2.0
        } else {
            null
        }
        return Snapshot(
            total = samples.size,
            counts = counts.toMap(),
            firstTimestamp = samples.firstOrNull()?.timestamp,
            expectedCounts = expectedCounts.toMap(),
            distributionDrift = drift,
        )
    }

    internal data class Snapshot(
        val total: Int,
        val counts: Map<NPCAction, Int>,
        val firstTimestamp: Long?,
        val expectedCounts: Map<NPCAction, Double> = emptyMap(),
        val distributionDrift: Double? = null,
    )

    companion object {
        const val DEFAULT_MAX_SAMPLES: Int = 32
        const val MAX_ALLOWED_SAMPLES: Int = 256
    }
}
