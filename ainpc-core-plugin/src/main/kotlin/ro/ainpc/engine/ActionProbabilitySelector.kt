package ro.ainpc.engine

import org.bukkit.configuration.ConfigurationSection
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NPCState
import java.util.Locale
import kotlin.math.exp

internal data class ActionProbabilitySettings(
    val maxChoices: Int = 3,
    val scoreWindow: Int = 15,
    val temperature: Double = 8.0,
    val repeatWeight: Double = 0.5,
    val repeatWindowSeconds: Int = 90,
    val idleWeight: Double = 0.0,
    val statisticsWindow: Int = 32,
    val actionWeights: Map<NPCAction, Double> = emptyMap(),
    val repeatHistorySize: Int = 1,
    val randomSeed: Long? = null,
    val actionCooldowns: Map<NPCAction, Int> = emptyMap(),
) {
    init {
        require(maxChoices in 1..8)
        require(scoreWindow in 0..100)
        require(temperature.isFinite() && temperature in 0.5..50.0)
        require(repeatWeight.isFinite() && repeatWeight in 0.1..1.0)
        require(repeatWindowSeconds in 0..3600)
        require(idleWeight.isFinite() && idleWeight in 0.0..1.0)
        require(statisticsWindow in 1..256)
        require(actionWeights.size <= NPCAction.values().size)
        require(actionWeights.values.all { it.isFinite() && it in MIN_ACTION_WEIGHT..MAX_ACTION_WEIGHT })
        require(repeatHistorySize in 1..8)
        require(actionCooldowns.size <= NPCAction.values().size)
        require(actionCooldowns.values.all { it in 0..MAX_ACTION_COOLDOWN_SECONDS })
    }

    fun includesPreviousAction(ageMillis: Long): Boolean =
        repeatWindowSeconds > 0 && ageMillis in 0 until repeatWindowSeconds * 1000L

    fun repeatMultiplier(ageMillis: Long): Double =
        if (includesPreviousAction(ageMillis)) {
            repeatWeight + (1.0 - repeatWeight) * ageMillis / (repeatWindowSeconds * 1000.0)
        } else {
            1.0
        }

    companion object {
        fun fromConfig(
            config: ConfigurationSection, occupation: String? = null,
            timeOfDay: String? = null, weather: String? = null,
        ): ActionProbabilitySettings {
            val maxChoices = config.getInt("simulation.decision.max_choices", 3)
                .takeIf { it >= 1 }?.coerceAtMost(8) ?: 3
            val scoreWindow = config.getInt("simulation.decision.score_window", 15)
                .takeIf { it >= 0 }?.coerceAtMost(100) ?: 15
            val temperature = config.getDouble("simulation.decision.temperature", 8.0)
                .takeIf { it.isFinite() && it > 0.0 }?.coerceIn(0.5, 50.0) ?: 8.0
            val repeatWeight = config.getDouble("simulation.decision.repeat_weight", 0.5)
                .takeIf { it.isFinite() && it >= 0.1 }?.coerceAtMost(1.0) ?: 0.5
            val repeatWindowSeconds = config.getInt("simulation.decision.repeat_window_seconds", 90)
                .takeIf { it >= 0 }?.coerceAtMost(3600) ?: 90
            val idleWeight = config.getDouble("simulation.decision.idle_weight", 0.0)
                .takeIf { it.isFinite() && it >= 0.0 }?.coerceAtMost(1.0) ?: 0.0
            val statisticsWindow = config.getInt("simulation.decision.statistics_window", 32)
                .takeIf { it >= 1 }?.coerceAtMost(256) ?: 32
            val actionWeights = readActionWeights(config.getConfigurationSection("simulation.decision.action_weights"))
                .toMutableMap()
            occupationWeightSection(config, occupation)?.let { section ->
                actionWeights.putAll(readActionWeights(section))
            }
            timeWeightSection(config, timeOfDay)?.let { section ->
                actionWeights.putAll(readActionWeights(section))
            }
            weatherWeightSection(config, weather)?.let { section ->
                actionWeights.putAll(readActionWeights(section))
            }
            val repeatHistorySize = config.getInt("simulation.decision.repeat_history_size", 1)
                .takeIf { it >= 1 }?.coerceAtMost(8) ?: 1
            val randomSeed = when (val configuredSeed = config.get("simulation.decision.random_seed")) {
                is Number -> configuredSeed.toLong()
                is String -> configuredSeed.toLongOrNull()
                else -> null
            }
            val actionCooldowns = config.getConfigurationSection("simulation.decision.action_cooldowns")
                ?.getKeys(false)
                ?.mapNotNull { key ->
                    val action = runCatching { NPCAction.valueOf(key.uppercase(Locale.ROOT)) }.getOrNull()
                    val seconds = action?.let {
                        config.getInt("simulation.decision.action_cooldowns.$key", 0)
                    }
                    if (action == null || seconds == null || seconds <= 0) {
                        null
                    } else {
                        action to seconds.coerceAtMost(MAX_ACTION_COOLDOWN_SECONDS)
                    }
                }
                ?.toMap()
                ?: emptyMap()
            return ActionProbabilitySettings(
                maxChoices, scoreWindow, temperature, repeatWeight, repeatWindowSeconds, idleWeight,
                statisticsWindow, actionWeights, repeatHistorySize, randomSeed, actionCooldowns,
            )
        }

        const val MIN_ACTION_WEIGHT: Double = 0.1
        const val MAX_ACTION_WEIGHT: Double = 10.0
        const val MAX_ACTION_COOLDOWN_SECONDS: Int = 3600

        private fun readActionWeights(section: ConfigurationSection?): Map<NPCAction, Double> =
            section?.getKeys(false)
                ?.mapNotNull { key ->
                    val action = runCatching { NPCAction.valueOf(key.uppercase(Locale.ROOT)) }.getOrNull()
                    val weight = action?.let { section.getDouble(key, Double.NaN) }
                    if (action == null || weight == null || !weight.isFinite() || weight <= 0.0) {
                        null
                    } else {
                        action to weight.coerceIn(MIN_ACTION_WEIGHT, MAX_ACTION_WEIGHT)
                    }
                }
                ?.toMap()
                ?: emptyMap()

        private fun occupationWeightSection(
            config: ConfigurationSection, occupation: String?,
        ): ConfigurationSection? = profileWeightSection(
            config, "simulation.decision.action_weights_by_occupation", occupation,
        )

        private fun timeWeightSection(
            config: ConfigurationSection, timeOfDay: String?,
        ): ConfigurationSection? = profileWeightSection(
            config, "simulation.decision.action_weights_by_time", timeOfDay,
        )

        private fun weatherWeightSection(
            config: ConfigurationSection, weather: String?,
        ): ConfigurationSection? = profileWeightSection(
            config, "simulation.decision.action_weights_by_weather", weather,
        )

        private fun profileWeightSection(
            config: ConfigurationSection, path: String, profile: String?,
        ): ConfigurationSection? {
            val normalized = profile?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val profiles = config.getConfigurationSection(path) ?: return null
            val key = profiles.getKeys(false).firstOrNull { it.equals(normalized, ignoreCase = true) }
                ?: return null
            return profiles.getConfigurationSection(key)
        }
    }

    fun actionWeight(action: NPCAction): Double = actionWeights[action] ?: 1.0

    fun actionCooldownMillis(action: NPCAction): Long =
        actionCooldowns[action]?.toLong()?.times(1000L) ?: 0L
}

internal enum class ActionRankingExclusion(val label: String) {
    NON_POSITIVE_SCORE("scor nepozitiv"),
    OUTSIDE_SCORE_WINDOW("sub pragul de scor"),
    CHOICE_LIMIT("limita de optiuni"),
    ACTION_COOLDOWN("cooldown activ"),
    DETERMINISTIC_SELECTION("selectie determinista"),
}

internal data class RecentExploration(val action: NPCAction, val timestamp: Long)

internal data class RepeatAdjustment(val action: NPCAction, val multiplier: Double, val remainingMillis: Long)

internal data class ActionCooldown(val action: NPCAction, val remainingMillis: Long)

internal object ActionProbabilitySelector {
    fun isEnabledFor(state: NPCState, inDanger: Boolean, enabled: Boolean): Boolean =
        enabled && !inDanger && state !in setOf(NPCState.COMBAT, NPCState.FLEEING, NPCState.TALKING)

    fun probabilities(
        scores: Map<NPCAction, Int>,
        previousAction: NPCAction? = null,
        settings: ActionProbabilitySettings = ActionProbabilitySettings(),
        previousActionAgeMillis: Long = 0L,
        recentExplorationAges: Map<NPCAction, Long> = emptyMap(),
        actionCooldownAges: Map<NPCAction, Long> = emptyMap(),
    ): Map<NPCAction, Double> {
        val ranked = scores.entries
            .filter { it.key != NPCAction.DO_NOTHING && it.value > 0 }
            .sortedWith(compareByDescending<Map.Entry<NPCAction, Int>> { it.value }.thenBy { it.key.ordinal })
        if (ranked.isEmpty()) return mapOf(NPCAction.DO_NOTHING to 1.0)
        val bestScore = ranked.first().value
        val withinScoreWindow = ranked.takeWhile { it.value >= bestScore - settings.scoreWindow }
        val available = withinScoreWindow.filterNot { candidate ->
            isOnCooldown(candidate.key, actionCooldownAges[candidate.key], settings)
        }
        val candidatesSource = available.ifEmpty { ranked.take(1) }
        val candidateBestScore = candidatesSource.first().value
        val idleWeight = settings.idleWeight * settings.actionWeight(NPCAction.DO_NOTHING)
        val includeIdle = idleWeight > 0.0 && settings.maxChoices > 1 &&
            !isOnCooldown(NPCAction.DO_NOTHING, actionCooldownAges[NPCAction.DO_NOTHING], settings)
        val usefulLimit = if (includeIdle) settings.maxChoices - 1 else settings.maxChoices
        val candidates = candidatesSource.take(usefulLimit)
        val weights = candidates.map { candidate ->
            val weight = exp((candidate.value.toDouble() - candidateBestScore) / settings.temperature) *
                settings.actionWeight(candidate.key)
            val repeatAgeMillis = recentExplorationAges[candidate.key]
                ?: candidate.key.takeIf { it == previousAction }?.let { previousActionAgeMillis }
            if (candidate.key in REPETITION_SENSITIVE_ACTIONS && repeatAgeMillis != null) {
                weight * settings.repeatMultiplier(repeatAgeMillis)
            } else {
                weight
            }
        }
        val totalWeight = weights.sum()
        val probabilities = linkedMapOf<NPCAction, Double>()
        candidates.forEachIndexed { index, candidate ->
            probabilities[candidate.key] = weights[index] / totalWeight
        }
        if (includeIdle) {
            val adjustedTotal = totalWeight + idleWeight
            probabilities.replaceAll { _, probability -> probability * totalWeight / adjustedTotal }
            probabilities[NPCAction.DO_NOTHING] = idleWeight / adjustedTotal
        }
        return probabilities
    }

    fun select(probabilities: Map<NPCAction, Double>, roll: Double): NPCAction {
        require(roll >= 0.0 && roll < 1.0)
        var cumulative = 0.0
        for ((action, probability) in probabilities) {
            cumulative += probability
            if (roll < cumulative) return action
        }
        return probabilities.keys.lastOrNull() ?: NPCAction.DO_NOTHING
    }

    fun recentExplorationAfter(
        previous: RecentExploration?, action: NPCAction, now: Long, settings: ActionProbabilitySettings,
    ): RecentExploration? {
        return recentExplorationsAfter(previous?.let(::listOf) ?: emptyList(), action, now, settings)
            .firstOrNull()
    }

    fun recentExplorationsAfter(
        previous: List<RecentExploration>, action: NPCAction, now: Long, settings: ActionProbabilitySettings,
    ): List<RecentExploration> {
        if (settings.repeatWindowSeconds == 0) return emptyList()
        val retained = previous
            .filter { settings.includesPreviousAction(now - it.timestamp) }
            .distinctBy { it.action }
        val updated = if (action in REPETITION_SENSITIVE_ACTIONS) {
            listOf(RecentExploration(action, now)) + retained.filterNot { it.action == action }
        } else {
            retained
        }
        return updated.take(settings.repeatHistorySize)
    }

    fun repeatAdjustment(
        probabilities: Map<NPCAction, Double>, recentExploration: RecentExploration?,
        now: Long, settings: ActionProbabilitySettings,
    ): RepeatAdjustment? = repeatAdjustments(
        probabilities, recentExploration?.let(::listOf) ?: emptyList(), now, settings,
    ).firstOrNull()

    fun repeatAdjustments(
        probabilities: Map<NPCAction, Double>, recentExplorations: List<RecentExploration>,
        now: Long, settings: ActionProbabilitySettings,
    ): List<RepeatAdjustment> {
        if (probabilities.size < 2 || settings.repeatWeight == 1.0) return emptyList()
        return recentExplorations.mapNotNull { recent ->
            if (recent.action !in REPETITION_SENSITIVE_ACTIONS || recent.action !in probabilities) return@mapNotNull null
            val ageMillis = now - recent.timestamp
            if (!settings.includesPreviousAction(ageMillis)) return@mapNotNull null
            RepeatAdjustment(
                recent.action, settings.repeatMultiplier(ageMillis),
                settings.repeatWindowSeconds * 1000L - ageMillis,
            )
        }
    }

    fun activeCooldowns(
        actions: Set<NPCAction>, actionCooldownAges: Map<NPCAction, Long>,
        settings: ActionProbabilitySettings,
    ): List<ActionCooldown> = actions.mapNotNull { action ->
        val ageMillis = actionCooldownAges[action] ?: return@mapNotNull null
        val cooldownMillis = settings.actionCooldownMillis(action)
        if (cooldownMillis <= 0L || ageMillis !in 0 until cooldownMillis) return@mapNotNull null
        ActionCooldown(action, cooldownMillis - ageMillis)
    }.sortedBy { it.action.ordinal }

    fun recentActionTimestampsAfter(
        previous: Map<NPCAction, Long>, action: NPCAction, now: Long,
        settings: ActionProbabilitySettings,
    ): Map<NPCAction, Long> {
        if (settings.actionCooldowns.isEmpty()) return emptyMap()
        val retained = previous.filter { (previousAction, timestamp) ->
            previousAction in settings.actionCooldowns &&
                now - timestamp in 0 until settings.actionCooldownMillis(previousAction)
        }
        return if (action in settings.actionCooldowns) retained + (action to now) else retained
    }

    fun excludedByRanking(
        scores: Map<NPCAction, Int>,
        selected: Set<NPCAction>,
        probabilistic: Boolean,
        settings: ActionProbabilitySettings = ActionProbabilitySettings(),
        cooldownActions: Set<NPCAction> = emptySet(),
    ): Map<NPCAction, ActionRankingExclusion> {
        val ranked = scores.entries
            .filter { it.key != NPCAction.DO_NOTHING && it.key !in selected }
            .sortedWith(compareByDescending<Map.Entry<NPCAction, Int>> { it.value }.thenBy { it.key.ordinal })
        val bestPositiveScore = scores.entries
            .filter { it.key != NPCAction.DO_NOTHING && it.value > 0 }
            .maxOfOrNull { it.value }
        return ranked.associateTo(linkedMapOf()) { entry ->
            val reason = when {
                !probabilistic -> ActionRankingExclusion.DETERMINISTIC_SELECTION
                entry.value <= 0 -> ActionRankingExclusion.NON_POSITIVE_SCORE
                bestPositiveScore != null && entry.value < bestPositiveScore - settings.scoreWindow ->
                    ActionRankingExclusion.OUTSIDE_SCORE_WINDOW
                entry.key in cooldownActions -> ActionRankingExclusion.ACTION_COOLDOWN
                else -> ActionRankingExclusion.CHOICE_LIMIT
            }
            entry.key to reason
        }
    }

    private fun isOnCooldown(
        action: NPCAction, ageMillis: Long?, settings: ActionProbabilitySettings,
    ): Boolean {
        val age = ageMillis ?: return false
        val cooldownMillis = settings.actionCooldownMillis(action)
        return cooldownMillis > 0L && age in 0 until cooldownMillis
    }

    private val REPETITION_SENSITIVE_ACTIONS = setOf(
        NPCAction.WALK_RANDOM,
        NPCAction.OBSERVE,
        NPCAction.INVESTIGATE,
    )
}
