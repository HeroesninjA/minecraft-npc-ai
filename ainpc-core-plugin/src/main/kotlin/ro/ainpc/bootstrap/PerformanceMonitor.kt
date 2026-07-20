package ro.ainpc.bootstrap

import ro.ainpc.AINPCPlugin
import java.time.Instant
import java.util.ArrayDeque
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

object RuntimeMetricNames {
    const val ROUTINE_TICK = "tick.routine"
    const val DATABASE_OPERATION = "database.operation"
    const val COMMAND_DISPATCH = "command.dispatch"
    const val AI_ORCHESTRATION = "ai.orchestration"
    const val DEBUG_DUMP_EXPORT = "export.debug_dump"
    const val RUNTIME_SNAPSHOT_EXPORT = "export.runtime_snapshot"

    fun scheduler(taskName: String): String = "scheduler.$taskName"
}

enum class MetricDomain(val wireName: String, val configKey: String) {
    TICK("tick", "tick"),
    DATABASE("database", "database"),
    COMMAND("command", "command"),
    SCHEDULER("scheduler", "scheduler"),
    AI("ai", "ai"),
    EXPORT("export", "export"),
}

data class MetricBudgetSnapshot(
    val warnMillis: Double,
    val failMillis: Double,
)

data class RuntimeMetricSnapshot(
    val name: String,
    val domain: String,
    val status: String,
    val totalCount: Long,
    val windowSamples: Int,
    val windowFailures: Int,
    val lastDurationMillis: Double,
    val avgDurationMillis: Double,
    val maxDurationMillis: Double,
    val lastObservedAt: String,
    val itemCount: Int,
    val warnBudgetMillis: Double,
    val failBudgetMillis: Double,
)

data class RuntimeHealthSnapshot(
    val schemaVersion: Int = 1,
    val status: String,
    val generatedAt: String,
    val activeSeries: Int,
    val droppedMeasurements: Long,
    val maxSeries: Int,
    val samplesPerSeries: Int,
    val budgets: Map<String, MetricBudgetSnapshot>,
    val metrics: List<RuntimeMetricSnapshot>,
)

data class RuntimeTraceSpanSnapshot(
    val sequence: Long,
    val completedAt: String,
    val metric: String,
    val domain: String,
    val trigger: String,
    val durationMillis: Double,
    val itemCount: Int,
    val success: Boolean,
    val warnBudgetMillis: Double,
    val failBudgetMillis: Double,
)

data class RuntimeTraceSnapshot(
    val schemaVersion: Int = 1,
    val generatedAt: String,
    val enabled: Boolean,
    val policy: String,
    val maxSpans: Int,
    val retainedSpans: Int,
    val droppedSpans: Long,
    val spans: List<RuntimeTraceSpanSnapshot>,
)

class PerformanceMonitor(private val plugin: AINPCPlugin? = null) {
    data class TickTiming(
        val timestamp: Long,
        val durationMs: Double,
        val npcCount: Int,
        val label: String,
        val success: Boolean = true,
    )

    data class TickProfile(
        val avgDurationMs: Double,
        val maxDurationMs: Double,
        val minDurationMs: Double,
        val sampleCount: Int,
        val lastDurationMs: Double,
        val npcCount: Int,
        val label: String,
        val failureCount: Int = 0,
        val lastSuccess: Boolean = true,
    )

    private data class TraceSpan(
        val sequence: Long,
        val completedAtMillis: Long,
        val metric: String,
        val domain: MetricDomain,
        val trigger: String,
        val durationMillis: Double,
        val itemCount: Int,
        val success: Boolean,
        val warnBudgetMillis: Double,
        val failBudgetMillis: Double,
    )

    private data class MetricBudget(
        val warnMillis: Double,
        val failMillis: Double,
    )

    private data class Settings(
        val maxSeries: Int,
        val samplesPerSeries: Int,
        val tracingEnabled: Boolean,
        val maxTraceSpans: Int,
        val budgets: Map<MetricDomain, MetricBudget>,
    )

    private class MetricSeries(
        val name: String,
        val domain: MetricDomain,
    ) {
        val samples = ConcurrentLinkedDeque<TickTiming>()
        val totalCount = AtomicLong(0)

        fun lastTimestamp(): Long = samples.peekFirst()?.timestamp ?: Long.MIN_VALUE

        fun trimTo(limit: Int) {
            while (samples.size > limit) {
                samples.pollLast()
            }
        }
    }

    private val seriesByName = ConcurrentHashMap<String, MetricSeries>()
    private val seriesLock = Any()
    private val droppedMeasurements = AtomicLong(0)
    private val traceSpans = ArrayDeque<TraceSpan>()
    private val traceLock = Any()
    private val traceSequence = AtomicLong(0)
    private val droppedTraceSpans = AtomicLong(0)

    @Volatile
    private var settings: Settings = loadSettings()

    inner class TickTimer internal constructor(private val name: String) {
        private val startNanos = AtomicLong(NOT_STARTED)
        private val completed = AtomicBoolean(false)

        fun begin() {
            completed.set(false)
            startNanos.set(System.nanoTime())
        }

        fun end(npcCount: Int = 0) {
            finish(success = true, itemCount = npcCount)
        }

        fun end(success: Boolean, itemCount: Int = 0) {
            finish(success, itemCount)
        }

        fun fail(itemCount: Int = 0) {
            finish(success = false, itemCount = itemCount)
        }

        private fun finish(success: Boolean, itemCount: Int) {
            val started = startNanos.get()
            if (started == NOT_STARTED || !completed.compareAndSet(false, true)) return
            recordNanos(name, System.nanoTime() - started, itemCount, success)
        }
    }

    fun reloadFromConfig() {
        settings = loadSettings()
        val current = settings
        for (series in seriesByName.values) {
            series.trimTo(current.samplesPerSeries)
        }
        if (current.tracingEnabled) {
            trimTraceSpans(current.maxTraceSpans)
        } else {
            synchronized(traceLock) {
                traceSpans.clear()
            }
        }
        synchronized(seriesLock) {
            if (seriesByName.size <= current.maxSeries) return@synchronized
            val retained = seriesByName.values
                .sortedByDescending { it.lastTimestamp() }
                .take(current.maxSeries)
                .mapTo(HashSet()) { it.name }
            val removed = seriesByName.keys.removeIf { it !in retained }
            if (removed) {
                droppedMeasurements.incrementAndGet()
            }
        }
    }

    fun timer(name: String): TickTimer = TickTimer(normalizeMetricName(name))

    @JvmOverloads
    fun record(label: String, durationMs: Long, npcCount: Int = 0, success: Boolean = true) {
        val boundedMillis = durationMs.coerceAtLeast(0).coerceAtMost(Long.MAX_VALUE / NANOS_PER_MILLI)
        recordNanos(label, boundedMillis * NANOS_PER_MILLI, npcCount, success)
    }

    fun recordNanos(label: String, durationNanos: Long, itemCount: Int = 0, success: Boolean = true) {
        val normalizedName = normalizeMetricName(label)
        val domain = domainFor(normalizedName)
        val series = getOrCreateSeries(normalizedName, domain) ?: return
        val current = settings
        val durationMillis = durationNanos.coerceAtLeast(0).toDouble() / NANOS_PER_MILLI.toDouble()
        val completedAtMillis = System.currentTimeMillis()
        series.samples.addFirst(
            TickTiming(
                timestamp = completedAtMillis,
                durationMs = durationMillis,
                npcCount = itemCount,
                label = normalizedName,
                success = success,
            )
        )
        series.totalCount.incrementAndGet()
        series.trimTo(current.samplesPerSeries)
        recordTraceIfNeeded(
            normalizedName,
            domain,
            durationMillis,
            itemCount,
            success,
            completedAtMillis,
            current,
        )
    }

    fun getProfile(label: String): TickProfile? {
        val normalizedName = normalizeMetricName(label)
        val series = seriesByName[normalizedName] ?: return null
        return profile(series)
    }

    fun getAllProfiles(): List<TickProfile> = seriesByName.values.mapNotNull(::profile)

    fun getWorstPerformer(): TickProfile? = getAllProfiles().maxByOrNull { it.avgDurationMs }

    fun snapshot(now: Instant = Instant.now()): RuntimeHealthSnapshot {
        val current = settings
        val metrics = seriesByName.values.mapNotNull { series ->
            val profile = profile(series) ?: return@mapNotNull null
            val budget = current.budgets.getValue(series.domain)
            RuntimeMetricSnapshot(
                name = series.name,
                domain = series.domain.wireName,
                status = statusFor(profile, budget),
                totalCount = series.totalCount.get(),
                windowSamples = profile.sampleCount,
                windowFailures = profile.failureCount,
                lastDurationMillis = profile.lastDurationMs,
                avgDurationMillis = profile.avgDurationMs,
                maxDurationMillis = profile.maxDurationMs,
                lastObservedAt = Instant.ofEpochMilli(series.lastTimestamp()).toString(),
                itemCount = profile.npcCount,
                warnBudgetMillis = budget.warnMillis,
                failBudgetMillis = budget.failMillis,
            )
        }.sortedWith(
            compareByDescending<RuntimeMetricSnapshot> { statusRank(it.status) }
                .thenByDescending { it.avgDurationMillis }
                .thenBy { it.name }
        )
        val overallStatus = metrics.maxByOrNull { statusRank(it.status) }?.status ?: STATUS_UNKNOWN
        val budgetSnapshots = MetricDomain.entries.associate { domain ->
            val budget = current.budgets.getValue(domain)
            domain.wireName to MetricBudgetSnapshot(budget.warnMillis, budget.failMillis)
        }
        return RuntimeHealthSnapshot(
            status = overallStatus,
            generatedAt = now.toString(),
            activeSeries = metrics.size,
            droppedMeasurements = droppedMeasurements.get(),
            maxSeries = current.maxSeries,
            samplesPerSeries = current.samplesPerSeries,
            budgets = budgetSnapshots,
            metrics = metrics,
        )
    }

    fun traceSnapshot(now: Instant = Instant.now()): RuntimeTraceSnapshot {
        val current = settings
        val spans = synchronized(traceLock) {
            if (current.tracingEnabled) {
                traceSpans.take(current.maxTraceSpans).map { span ->
                    RuntimeTraceSpanSnapshot(
                        sequence = span.sequence,
                        completedAt = Instant.ofEpochMilli(span.completedAtMillis).toString(),
                        metric = span.metric,
                        domain = span.domain.wireName,
                        trigger = span.trigger,
                        durationMillis = span.durationMillis,
                        itemCount = span.itemCount,
                        success = span.success,
                        warnBudgetMillis = span.warnBudgetMillis,
                        failBudgetMillis = span.failBudgetMillis,
                    )
                }
            } else {
                emptyList()
            }
        }
        return RuntimeTraceSnapshot(
            generatedAt = now.toString(),
            enabled = current.tracingEnabled,
            policy = TRACE_POLICY,
            maxSpans = current.maxTraceSpans,
            retainedSpans = spans.size,
            droppedSpans = droppedTraceSpans.get(),
            spans = spans,
        )
    }

    fun formatSummary(): String {
        val health = snapshot()
        if (health.metrics.isEmpty()) return "Nicio metrica disponibila."
        val summary = StringBuilder("&6=== Metrici runtime (${health.status}) ===\n")
        for (metric in health.metrics) {
            val color = when (metric.status) {
                STATUS_FAIL -> "&c"
                STATUS_WARN -> "&e"
                else -> "&a"
            }
            summary.append(
                "$color${metric.name}&7: ${"%.1f".format(metric.avgDurationMillis)}ms avg " +
                    "&8(last=${"%.1f".format(metric.lastDurationMillis)}ms, " +
                    "max=${"%.1f".format(metric.maxDurationMillis)}ms, ${metric.windowSamples} samples)\n"
            )
        }
        return summary.toString()
    }

    private fun getOrCreateSeries(name: String, domain: MetricDomain): MetricSeries? {
        seriesByName[name]?.let { return it }
        synchronized(seriesLock) {
            seriesByName[name]?.let { return it }
            if (seriesByName.size >= settings.maxSeries) {
                droppedMeasurements.incrementAndGet()
                return null
            }
            return MetricSeries(name, domain).also { seriesByName[name] = it }
        }
    }

    private fun profile(series: MetricSeries): TickProfile? {
        val samples = series.samples.toList()
        if (samples.isEmpty()) return null
        return TickProfile(
            avgDurationMs = samples.map { it.durationMs }.average(),
            maxDurationMs = samples.maxOf { it.durationMs },
            minDurationMs = samples.minOf { it.durationMs },
            sampleCount = samples.size,
            lastDurationMs = samples.first().durationMs,
            npcCount = samples.first().npcCount,
            label = series.name,
            failureCount = samples.count { !it.success },
            lastSuccess = samples.first().success,
        )
    }

    private fun statusFor(profile: TickProfile, budget: MetricBudget): String = when {
        !profile.lastSuccess -> STATUS_FAIL
        profile.avgDurationMs >= budget.failMillis || profile.lastDurationMs >= budget.failMillis -> STATUS_FAIL
        profile.failureCount > 0 -> STATUS_WARN
        profile.avgDurationMs >= budget.warnMillis || profile.lastDurationMs >= budget.warnMillis -> STATUS_WARN
        else -> STATUS_PASS
    }

    private fun recordTraceIfNeeded(
        metric: String,
        domain: MetricDomain,
        durationMillis: Double,
        itemCount: Int,
        success: Boolean,
        completedAtMillis: Long,
        current: Settings,
    ) {
        if (!current.tracingEnabled) return
        val budget = current.budgets.getValue(domain)
        val trigger = when {
            !success -> TRACE_TRIGGER_FAILURE
            durationMillis >= budget.failMillis -> TRACE_TRIGGER_FAIL_BUDGET
            durationMillis >= budget.warnMillis -> TRACE_TRIGGER_WARN_BUDGET
            else -> return
        }
        synchronized(traceLock) {
            val latest = settings
            if (!latest.tracingEnabled) return
            traceSpans.addFirst(
                TraceSpan(
                    sequence = traceSequence.incrementAndGet(),
                    completedAtMillis = completedAtMillis,
                    metric = metric,
                    domain = domain,
                    trigger = trigger,
                    durationMillis = durationMillis,
                    itemCount = itemCount,
                    success = success,
                    warnBudgetMillis = budget.warnMillis,
                    failBudgetMillis = budget.failMillis,
                )
            )
            trimTraceSpansLocked(latest.maxTraceSpans)
        }
    }

    private fun trimTraceSpans(limit: Int) {
        synchronized(traceLock) {
            trimTraceSpansLocked(limit)
        }
    }

    private fun trimTraceSpansLocked(limit: Int) {
        while (traceSpans.size > limit) {
            traceSpans.removeLast()
            droppedTraceSpans.incrementAndGet()
        }
    }

    private fun loadSettings(): Settings {
        val config = plugin?.config
        val maxSeries = config?.getInt("observability.metrics.max_series", DEFAULT_MAX_SERIES)
            ?.coerceIn(MIN_MAX_SERIES, MAX_MAX_SERIES) ?: DEFAULT_MAX_SERIES
        val samplesPerSeries = config?.getInt("observability.metrics.samples_per_series", DEFAULT_SAMPLES_PER_SERIES)
            ?.coerceIn(MIN_SAMPLES_PER_SERIES, MAX_SAMPLES_PER_SERIES) ?: DEFAULT_SAMPLES_PER_SERIES
        val tracingEnabled = config?.getBoolean("observability.tracing.enabled", DEFAULT_TRACING_ENABLED)
            ?: DEFAULT_TRACING_ENABLED
        val maxTraceSpans = config?.getInt("observability.tracing.max_spans", DEFAULT_MAX_TRACE_SPANS)
            ?.coerceIn(MIN_TRACE_SPANS, MAX_TRACE_SPANS) ?: DEFAULT_MAX_TRACE_SPANS
        val budgets = MetricDomain.entries.associateWith { domain ->
            val defaults = DEFAULT_BUDGETS.getValue(domain)
            val prefix = "observability.metrics.budgets.${domain.configKey}"
            val configuredWarn = if (config?.contains("$prefix.warn_ms") == true) {
                config.getDouble("$prefix.warn_ms")
            } else defaults.warnMillis
            val warn = configuredWarn.coerceIn(MIN_BUDGET_MILLIS, MAX_BUDGET_MILLIS)
            val configuredFail = if (config?.contains("$prefix.fail_ms") == true) {
                config.getDouble("$prefix.fail_ms")
            } else defaults.failMillis
            val fail = configuredFail.coerceIn(warn, MAX_BUDGET_MILLIS)
            MetricBudget(warn, fail)
        }
        return Settings(maxSeries, samplesPerSeries, tracingEnabled, maxTraceSpans, budgets)
    }

    private fun normalizeMetricName(label: String): String {
        val normalized = label.trim()
            .lowercase(Locale.ROOT)
            .replace(INVALID_METRIC_CHARACTERS, "_")
            .trim('_', '.')
            .take(MAX_METRIC_NAME_LENGTH)
        return normalized.ifBlank { "unknown" }
    }

    private fun domainFor(name: String): MetricDomain = when {
        name.startsWith("tick.") || name == "routinetick" -> MetricDomain.TICK
        name.startsWith("database.") || name.startsWith("db.") -> MetricDomain.DATABASE
        name.startsWith("command.") -> MetricDomain.COMMAND
        name.startsWith("scheduler.") -> MetricDomain.SCHEDULER
        name.startsWith("ai.") -> MetricDomain.AI
        name.startsWith("export.") -> MetricDomain.EXPORT
        else -> MetricDomain.COMMAND
    }

    companion object {
        private const val NOT_STARTED = Long.MIN_VALUE
        private const val NANOS_PER_MILLI = 1_000_000L
        private const val DEFAULT_MAX_SERIES = 32
        private const val MIN_MAX_SERIES = 8
        private const val MAX_MAX_SERIES = 64
        private const val DEFAULT_SAMPLES_PER_SERIES = 100
        private const val MIN_SAMPLES_PER_SERIES = 10
        private const val MAX_SAMPLES_PER_SERIES = 1_000
        private const val DEFAULT_TRACING_ENABLED = true
        private const val DEFAULT_MAX_TRACE_SPANS = 64
        private const val MIN_TRACE_SPANS = 8
        private const val MAX_TRACE_SPANS = 256
        private const val MIN_BUDGET_MILLIS = 1.0
        private const val MAX_BUDGET_MILLIS = 600_000.0
        private const val MAX_METRIC_NAME_LENGTH = 64
        private const val STATUS_PASS = "PASS"
        private const val STATUS_WARN = "WARN"
        private const val STATUS_FAIL = "FAIL"
        private const val STATUS_UNKNOWN = "UNKNOWN"
        private const val TRACE_POLICY = "failures_or_budget_exceeded"
        private const val TRACE_TRIGGER_FAILURE = "failure"
        private const val TRACE_TRIGGER_WARN_BUDGET = "warn_budget"
        private const val TRACE_TRIGGER_FAIL_BUDGET = "fail_budget"
        private val INVALID_METRIC_CHARACTERS = Regex("[^a-z0-9._-]")
        private val DEFAULT_BUDGETS = mapOf(
            MetricDomain.TICK to MetricBudget(20.0, 50.0),
            MetricDomain.DATABASE to MetricBudget(50.0, 250.0),
            MetricDomain.COMMAND to MetricBudget(100.0, 1_000.0),
            MetricDomain.SCHEDULER to MetricBudget(50.0, 250.0),
            MetricDomain.AI to MetricBudget(3_000.0, 15_000.0),
            MetricDomain.EXPORT to MetricBudget(2_000.0, 10_000.0),
        )

        private fun statusRank(status: String): Int = when (status) {
            STATUS_FAIL -> 3
            STATUS_WARN -> 2
            STATUS_PASS -> 1
            else -> 0
        }
    }
}
