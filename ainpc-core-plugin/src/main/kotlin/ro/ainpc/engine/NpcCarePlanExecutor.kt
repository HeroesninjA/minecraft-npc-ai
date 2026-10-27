package ro.ainpc.engine

import ro.ainpc.npc.NPCAction

internal object NpcCarePlanExecutor {
    enum class Outcome(val endsTick: Boolean) {
        NOT_READY(false), POLICY_BLOCKED(false), TRAVEL_REQUIRED(false), UNSUPPORTED(false),
        HOME_UNAVAILABLE(true), INVALIDATED(true), APPLIED(true), INTERRUPTED(true),
    }

    fun executeAtHome(
        plan: NpcCarePlanPreview,
        currentInput: () -> NpcCarePlanningInput,
        policyAllows: () -> Boolean,
        homeAvailable: () -> Boolean,
        apply: (NPCAction) -> Boolean,
    ): Outcome {
        if (plan.status != NpcCarePlanPreview.Status.READY) return Outcome.NOT_READY
        val selected = plan.selected ?: return Outcome.NOT_READY
        if (!plan.input.agent.perception.isAtHome || NpcCareStep.RETURN_HOME in selected.steps) {
            return Outcome.TRAVEL_REQUIRED
        }
        val careStep = when {
            selected.action == NPCAction.EAT && plan.goal == NpcCareGoal.SATIETY -> NpcCareStep.EAT_AT_HOME
            selected.action == NPCAction.SLEEP && plan.goal == NpcCareGoal.ENERGY -> NpcCareStep.SLEEP_AT_HOME
            selected.action == NPCAction.REST && plan.goal == NpcCareGoal.ENERGY -> NpcCareStep.REST_AT_HOME
            else -> return Outcome.UNSUPPORTED
        }
        if (selected.steps != listOf(careStep, NpcCareStep.OBSERVE_RECOVERY)) return Outcome.UNSUPPORTED
        if (plan.invalidationReason(currentInput()) != null) return Outcome.INVALIDATED
        if (!policyAllows()) return Outcome.POLICY_BLOCKED
        val available = homeAvailable()
        if (plan.invalidationReason(currentInput()) != null) return Outcome.INVALIDATED
        if (!available) return Outcome.HOME_UNAVAILABLE
        if (!policyAllows()) return Outcome.POLICY_BLOCKED
        return if (apply(selected.action)) Outcome.APPLIED else Outcome.INTERRUPTED
    }
}
