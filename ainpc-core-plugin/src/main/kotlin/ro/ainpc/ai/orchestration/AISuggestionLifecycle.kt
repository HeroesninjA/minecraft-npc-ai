package ro.ainpc.ai.orchestration

enum class AISuggestionLifecycle {
    GENERATED,
    QUEUED,
    REVIEWING,
    APPROVED,
    PUBLISHED,
    QUARANTINED,
    REJECTED,
    ROLLED_BACK;

    fun allowedTransitions(): List<AISuggestionLifecycle> {
        return when (this) {
            GENERATED -> listOf(QUEUED, REJECTED, QUARANTINED)
            QUEUED -> listOf(REVIEWING, REJECTED, QUARANTINED)
            REVIEWING -> listOf(APPROVED, REJECTED, QUARANTINED)
            APPROVED -> listOf(PUBLISHED, REJECTED, QUARANTINED, ROLLED_BACK)
            PUBLISHED -> listOf(ROLLED_BACK, QUARANTINED)
            QUARANTINED -> listOf(REVIEWING, REJECTED, APPROVED)
            REJECTED -> emptyList()
            ROLLED_BACK -> emptyList()
        }
    }

    fun canTransitionTo(target: AISuggestionLifecycle): Boolean = target in allowedTransitions()

    fun transitionTo(target: AISuggestionLifecycle): AISuggestionLifecycle {
        if (!canTransitionTo(target)) {
            throw IllegalStateException("Tranziție invalidă: $this -> $target")
        }
        return target
    }
}
