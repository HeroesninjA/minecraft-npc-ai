package ro.ainpc.npc

class NpcSelfCareGoal {
    enum class Stage {
        RETURNING_HOME, RECOVERING, BLOCKED, SATISFIED, INTERRUPTED;

        fun isActive(): Boolean = this in setOf(RETURNING_HOME, RECOVERING, BLOCKED)
    }

    data class Snapshot(
        val action: NPCAction,
        val stage: Stage,
        val initialNeed: Int,
        val observedNeed: Int,
        val homeKey: String,
        val observedRecovery: Boolean,
        val updatedAt: Long,
        val withoutProgressMillis: Long = 0L,
    ) {
        fun lacksProgress(): Boolean = stage.isActive() && withoutProgressMillis >= PROGRESS_LIMIT

        fun description(): String {
            val objective = when (action) {
                NPCAction.EAT -> "hrana"
                NPCAction.REST -> "odihna fara somn"
                else -> "odihna"
            }
            val progress = when (stage) {
                Stage.RETURNING_HOME ->
                    "La ultima observatie, pentru obiectivul de $objective, alesesem sa revin acasa; sosirea nu era inca confirmata."
                Stage.RECOVERING -> "La ultima observatie, eram acasa pentru obiectivul de $objective. " +
                    if (observedRecovery) {
                        "Am observat o imbunatatire in timpul ingrijirii; atunci, obiectivul nu era inca incheiat."
                    } else {
                        "La acel moment, inca nu confirmasem refacerea nevoii."
                    }
                Stage.BLOCKED -> "La ultima observatie, imi lipsea o casa confirmata pentru obiectivul de $objective."
                Stage.SATISFIED -> "Ultimul obiectiv de $objective este incheiat: am observat refacerea nevoii. " +
                    if (observedRecovery) {
                        "Ultima imbunatatire a fost observata in timpul ingrijirii de acasa."
                    } else {
                        "Planul nu confirma cauza acestei schimbari."
                    }
                Stage.INTERRUPTED ->
                    "Ultimul obiectiv de $objective a fost intrerupt; nu am confirmat incheierea lui."
            }
            return if (lacksProgress()) {
                "$progress Pana la acea observatie, nu confirmasem sosirea sau refacerea in ultimele minute; cauza nu este stabilita."
            } else progress
        }
    }

    private var current: Snapshot? = null
    private var progressObservedAt: Long = 0L

    @Synchronized
    fun applied(action: NPCAction, state: NpcCognitiveState, homeKey: String, isAtHome: Boolean, now: Long) {
        applied(action, state, homeKey, isAtHome, now, allowRest = false)
    }

    @Synchronized
    internal fun applied(
        action: NPCAction, state: NpcCognitiveState, homeKey: String, isAtHome: Boolean, now: Long, allowRest: Boolean,
    ) {
        if (now <= 0L) return
        val careState = careState(action)
        val level = level(action, state)
        if ((action == NPCAction.REST && (!allowRest || !isAtHome || homeKey.isBlank() || state.hasInteractingPlayer)) ||
            careState == null || level !in 0 until NpcSelfCareThresholds.RECOVERED || !isSafe(state) ||
            state.currentState != if (isAtHome) careState else NPCState.WALKING
        ) {
            interrupt(now)
            return
        }
        val previous = snapshot(now)?.takeIf {
            it.stage.isActive() && it.action == action && it.homeKey == homeKey
        }
        val arrived = isAtHome && previous?.stage == Stage.RETURNING_HOME
        if (previous == null || arrived) progressObservedAt = now
        current = Snapshot(
            action, phase(homeKey, isAtHome), previous?.initialNeed ?: level, level, homeKey,
            previous?.observedRecovery ?: false, now,
            if (arrived) 0L else previous?.withoutProgressMillis ?: 0L,
        )
    }

    @Synchronized
    fun observe(
        before: NpcCognitiveState, after: NpcCognitiveState, homeKey: String, isAtHome: Boolean,
        previousTickAt: Long, observedAt: Long,
    ) {
        val goal = current?.takeIf { it.stage.isActive() } ?: return
        if (snapshot(observedAt) == null || previousTickAt <= 0L ||
            observedAt - previousTickAt !in 1..120_000L
        ) {
            clear()
            return
        }
        val beforeLevel = level(goal.action, before)
        val afterLevel = level(goal.action, after)
        if ((goal.action == NPCAction.REST && (before.hasInteractingPlayer || after.hasInteractingPlayer)) ||
            !isSafe(before) || !isSafe(after) || homeKey != goal.homeKey ||
            beforeLevel !in 0..100 || afterLevel !in 0..100 ||
            before.currentState !in setOf(NPCState.WALKING, careState(goal.action)) ||
            after.currentState != before.currentState
        ) {
            interrupt(observedAt)
            return
        }
        val recoveredDuringCare = homeKey.isNotBlank() && isAtHome &&
            before.currentState == careState(goal.action) && afterLevel > beforeLevel
        val satisfied = afterLevel >= NpcSelfCareThresholds.RECOVERED
        val arrived = isAtHome && goal.stage == Stage.RETURNING_HOME
        val observedMillis = (observedAt - maxOf(previousTickAt, progressObservedAt)).coerceAtLeast(0L)
        current = goal.copy(
            stage = if (satisfied) Stage.SATISFIED else phase(homeKey, isAtHome),
            observedNeed = afterLevel,
            observedRecovery = if (satisfied) recoveredDuringCare else goal.observedRecovery || recoveredDuringCare,
            updatedAt = observedAt,
            withoutProgressMillis = if (recoveredDuringCare || arrived) 0L
                else minOf(PROGRESS_LIMIT, goal.withoutProgressMillis + observedMillis),
        )
        progressObservedAt = observedAt
    }

    @Synchronized
    internal fun canKeepRecovery(state: NpcCognitiveState, homeKey: String, isAtHome: Boolean, now: Long): Boolean {
        val goal = snapshot(now) ?: return false
        return goal.stage == Stage.RECOVERING && !goal.lacksProgress() && isAtHome &&
            homeKey.isNotBlank() && homeKey == goal.homeKey && isSafe(state) && !state.hasInteractingPlayer &&
            state.currentState == careState(goal.action) && level(goal.action, state) in 0 until NpcSelfCareThresholds.RECOVERED
    }

    @Synchronized
    fun canContinueReturn(state: NpcCognitiveState, homeKey: String, isAtHome: Boolean, now: Long): Boolean =
        returnStatus(state, homeKey, isAtHome, now) == NpcSelfCareContinuity.READY

    @Synchronized
    internal fun returnStatus(
        state: NpcCognitiveState, homeKey: String, isAtHome: Boolean, now: Long,
    ): NpcSelfCareContinuity {
        val goal = snapshot(now) ?: return NpcSelfCareContinuity.NO_RECENT_GOAL
        return when {
            goal.stage != Stage.RETURNING_HOME -> NpcSelfCareContinuity.NOT_RETURNING
            goal.lacksProgress() -> NpcSelfCareContinuity.NO_PROGRESS
            homeKey.isBlank() -> NpcSelfCareContinuity.HOME_UNCONFIRMED
            homeKey != goal.homeKey -> NpcSelfCareContinuity.HOME_CHANGED
            isAtHome -> NpcSelfCareContinuity.AT_HOME
            !isSafe(state) -> NpcSelfCareContinuity.UNSAFE
            state.hasInteractingPlayer -> NpcSelfCareContinuity.INTERLOCUTOR
            state.currentState != NPCState.WALKING -> NpcSelfCareContinuity.STATE_MISMATCH
            level(goal.action, state) !in 0..100 -> NpcSelfCareContinuity.INVALID_NEED
            level(goal.action, state) >= NpcSelfCareThresholds.RECOVERED -> NpcSelfCareContinuity.NEED_SATISFIED
            else -> NpcSelfCareContinuity.READY
        }
    }

    @Synchronized
    fun continueReturn(state: NpcCognitiveState, homeKey: String, now: Long) {
        if (canContinueReturn(state, homeKey, false, now)) {
            applied(requireNotNull(current).action, state, homeKey, false, now)
        } else interrupt(now)
    }

    @Synchronized
    fun onStateChanged(state: NPCState, now: Long) {
        val goal = current?.takeIf { it.stage.isActive() } ?: return
        if (state != NPCState.WALKING && state != careState(goal.action)) interrupt(now)
    }

    @Synchronized
    fun interrupt(now: Long) {
        val goal = current?.takeIf { it.stage.isActive() } ?: return
        current = if (now > 0L && now >= goal.updatedAt) {
            goal.copy(stage = Stage.INTERRUPTED, updatedAt = now)
        } else null
    }

    @Synchronized
    fun clear() {
        current = null
        progressObservedAt = 0L
    }

    @Synchronized
    fun snapshot(now: Long): Snapshot? = current?.takeIf {
        now >= it.updatedAt && now - it.updatedAt < MAX_AGE
    }

    private fun phase(homeKey: String, isAtHome: Boolean): Stage = when {
        homeKey.isBlank() -> Stage.BLOCKED
        isAtHome -> Stage.RECOVERING
        else -> Stage.RETURNING_HOME
    }

    private fun isSafe(state: NpcCognitiveState): Boolean = !state.isInDanger && state.safetyLevel in 35..100

    private fun level(action: NPCAction, state: NpcCognitiveState): Int = when (action) {
        NPCAction.EAT -> state.hungerLevel
        NPCAction.SLEEP, NPCAction.REST -> state.energyLevel
        else -> -1
    }

    private fun careState(action: NPCAction): NPCState? = when (action) {
        NPCAction.EAT -> NPCState.EATING
        NPCAction.SLEEP -> NPCState.SLEEPING
        NPCAction.REST -> NPCState.RESTING
        else -> null
    }

    companion object {
        private const val MAX_AGE = 5 * 60 * 1_000L
        private const val PROGRESS_LIMIT = 3 * 60 * 1_000L

        internal fun homeKey(npc: AINPC): String {
            val home = npc.homeAnchor ?: return ""
            if (home.worldName().isBlank() || home.worldName() != npc.worldName ||
                !home.x().isFinite() || !home.y().isFinite() || !home.z().isFinite()
            ) return ""
            return "${home.worldName()}:${home.x()},${home.y()},${home.z()}"
        }
    }
}
