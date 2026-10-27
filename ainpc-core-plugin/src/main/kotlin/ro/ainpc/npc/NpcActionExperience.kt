package ro.ainpc.npc

import kotlin.math.min
import kotlin.math.roundToInt

class NpcActionExperience {
    data class Observation(
        val action: NPCAction,
        val contextKey: String,
        val needDelta: Int,
        val effect: Double,
        val observedAt: Long,
    )

    private data class Pending(
        val action: NPCAction,
        val state: NPCState,
        val contextKey: String,
        val needLevel: Int,
        val stateRevision: Long,
        val simulationTickAt: Long,
    )

    private var pending: Pending? = null
    private var observations: List<Observation> = emptyList()

    @Synchronized
    fun begin(
        action: NPCAction,
        state: NpcCognitiveState,
        contextKey: String,
        isAtHome: Boolean,
        stateRevision: Long,
        simulationTickAt: Long,
    ) {
        begin(action, state, contextKey, isAtHome, stateRevision, simulationTickAt, allowRest = false)
    }

    @Synchronized
    internal fun begin(
        action: NPCAction,
        state: NpcCognitiveState,
        contextKey: String,
        isAtHome: Boolean,
        stateRevision: Long,
        simulationTickAt: Long,
        allowRest: Boolean,
    ) {
        pending = null
        if (action == NPCAction.REST && !allowRest) return
        val expectedState = expectedState(action) ?: return
        val needLevel = needLevel(action, state) ?: return
        if (!isAtHome || !safeForCare(state) || !validContextKey(contextKey) ||
            state.currentState != expectedState || needLevel !in 0 until NpcSelfCareThresholds.RECOVERED ||
            stateRevision < 0L || simulationTickAt <= 0L
        ) return

        pending = Pending(action, expectedState, contextKey, needLevel, stateRevision, simulationTickAt)
    }

    @Synchronized
    fun observe(
        before: NpcCognitiveState,
        after: NpcCognitiveState,
        contextKey: String,
        isAtHome: Boolean,
        stateRevision: Long,
        previousTickAt: Long,
        observedAt: Long,
    ): Observation? {
        val applied = pending ?: return null
        pending = null
        if (!isAtHome || !safeForCare(before) || !safeForCare(after) ||
            applied.contextKey != contextKey || applied.stateRevision != stateRevision ||
            before.currentState != applied.state || after.currentState != applied.state ||
            previousTickAt != applied.simulationTickAt || observedAt <= previousTickAt
        ) return null

        val elapsed = observedAt - previousTickAt
        if (elapsed > MAX_OBSERVATION_INTERVAL) return null
        val beforeNeed = needLevel(applied.action, before) ?: return null
        val afterNeed = needLevel(applied.action, after) ?: return null
        if (beforeNeed != applied.needLevel || beforeNeed !in 0..100 || afterNeed !in 0..100) return null

        val tickFactor = (elapsed / 30_000.0).coerceIn(0.5, 4.0)
        val needDelta = afterNeed - beforeNeed
        val ambientRecovery = if (applied.action == NPCAction.SLEEP || applied.action == NPCAction.REST) {
            min(100 - beforeNeed, (1.6 * tickFactor).roundToInt())
        } else {
            0
        }
        val observation = Observation(
            applied.action, contextKey, needDelta, (needDelta - ambientRecovery) / tickFactor, observedAt,
        )
        if (!validObservation(observation)) return null
        observations = (observations + observation).sortedBy { it.observedAt }.takeLast(MAX_OBSERVATIONS)
        return observation
    }

    fun scoreModifier(
        action: NPCAction,
        state: NpcCognitiveState,
        contextKey: String,
        isAtHome: Boolean,
        now: Long,
    ): Int {
        val expectedState = expectedState(action) ?: return 0
        val needLevel = needLevel(action, state) ?: return 0
        if (!isAtHome || !safeForCare(state) || !validContextKey(contextKey) || needLevel !in 0..100 ||
            !NpcSelfCareThresholds.needsCare(needLevel, state.currentState == expectedState)
        ) return 0

        val matching = snapshot().filter {
            it.action == action && it.contextKey == contextKey && isRecent(it, now)
        }
        if (matching.isEmpty()) return 0
        return (matching.map { it.effect }.average() * matching.size / (matching.size + 2.0))
            .roundToInt().coerceIn(-MAX_SCORE_MODIFIER, MAX_SCORE_MODIFIER)
    }

    @Synchronized
    fun snapshot(): List<Observation> = observations.toList()

    @Synchronized
    fun restore(observations: List<Observation>) {
        pending = null
        this.observations = observations.filter(::validObservation)
            .sortedBy { it.observedAt }.takeLast(MAX_OBSERVATIONS)
    }

    @Synchronized
    fun cancelPending() {
        pending = null
    }

    fun latestDescription(now: Long): String {
        val latest = snapshot().filter { isRecent(it, now) }.maxByOrNull { it.observedAt } ?: return ""
        return if (latest.action == NPCAction.EAT) {
            when {
                latest.needDelta > 0 -> "In timpul mesei de acasa, m-am simtit mai satul."
                latest.needDelta < 0 -> "In timpul mesei de acasa, foamea mea a crescut."
                else -> "In timpul mesei de acasa, nu am simtit ca foamea s-a redus."
            }
        } else if (latest.action == NPCAction.REST) {
            when {
                latest.needDelta > 0 -> "In timpul odihnei de acasa, m-am simtit mai odihnit."
                latest.needDelta < 0 -> "In timpul odihnei de acasa, m-am simtit mai obosit."
                else -> "In timpul odihnei de acasa, nu am simtit ca mi-am refacut energia."
            }
        } else {
            when {
                latest.needDelta > 0 -> "In timpul somnului de acasa, m-am simtit mai odihnit."
                latest.needDelta < 0 -> "In timpul somnului de acasa, m-am simtit mai obosit."
                else -> "In timpul somnului de acasa, nu am simtit ca mi-am refacut energia."
            }
        }
    }

    private fun expectedState(action: NPCAction): NPCState? = when (action) {
        NPCAction.EAT -> NPCState.EATING
        NPCAction.SLEEP -> NPCState.SLEEPING
        NPCAction.REST -> NPCState.RESTING
        else -> null
    }

    private fun needLevel(action: NPCAction, state: NpcCognitiveState): Int? = when (action) {
        NPCAction.EAT -> state.hungerLevel
        NPCAction.SLEEP, NPCAction.REST -> state.energyLevel
        else -> null
    }

    private fun safeForCare(state: NpcCognitiveState): Boolean =
        !state.isInDanger && !state.hasInteractingPlayer && state.safetyLevel in 35..100 &&
            state.currentState !in setOf(
                NPCState.COMBAT, NPCState.FLEEING, NPCState.PANICKING,
                NPCState.TALKING, NPCState.LISTENING, NPCState.TRADING,
            )

    private fun validContextKey(contextKey: String): Boolean =
        contextKey.isNotBlank() && contextKey.length <= MAX_CONTEXT_KEY_LENGTH

    private fun validObservation(observation: Observation): Boolean =
        expectedState(observation.action) != null && validContextKey(observation.contextKey) &&
            observation.needDelta in -100..100 && observation.effect.isFinite() &&
            observation.effect in -200.0..200.0 && observation.observedAt > 0L

    private fun isRecent(observation: Observation, now: Long): Boolean =
        now >= observation.observedAt && now - observation.observedAt <= MAX_AGE

    companion object {
        private const val MAX_OBSERVATIONS = 32
        private const val MAX_CONTEXT_KEY_LENGTH = 256
        private const val MAX_SCORE_MODIFIER = 12
        private const val MAX_OBSERVATION_INTERVAL = 120_000L
        private const val MAX_AGE = 7 * 24 * 60 * 60 * 1_000L
    }
}
