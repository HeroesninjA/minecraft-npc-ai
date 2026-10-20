package ro.ainpc.engine

internal object ScenarioRecoveryActivation {
    fun activate(
        registerActors: () -> Boolean,
        releaseActors: () -> Unit,
        restoreSession: () -> ActiveScenario?,
        removeSession: (ActiveScenario) -> Unit,
        resumeTimers: (ActiveScenario) -> Boolean,
    ): ActiveScenario? {
        var restored: ActiveScenario? = null
        var committed = false
        try {
            if (!registerActors()) return null
            restored = restoreSession() ?: return null
            if (!resumeTimers(restored)) return null
            committed = true
            return restored
        } finally {
            if (!committed) {
                restored?.let(removeSession)
                releaseActors()
            }
        }
    }
}
