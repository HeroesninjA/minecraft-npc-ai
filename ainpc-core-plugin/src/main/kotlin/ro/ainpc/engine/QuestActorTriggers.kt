package ro.ainpc.engine

object QuestActorTriggers {
    const val ON_ACCEPT = "on_accept"
    const val ON_OBJECTIVE_COMPLETE = "on_objective_complete"
    const val ON_STAGE_ENTER = "on_stage_enter"
    const val ON_STAGE_EXIT = "on_stage_exit"
    const val ON_STAGE_COMPLETE = "on_stage_complete"
    const val ON_COMPLETE = "on_complete"
    const val ON_RETURN_TO_GIVER = "on_return_to_giver"
    const val ON_FAIL = "on_fail"
    const val ON_RESET = "on_reset"

    private val triggerAliases = mapOf(
        "on_stage_start" to ON_STAGE_ENTER,
        "on_stage_begin" to ON_STAGE_ENTER,
        "on_stage_finish" to ON_STAGE_COMPLETE,
        "on_stage_end" to ON_STAGE_COMPLETE,
    )

    fun normalize(triggerId: String?): String? {
        val normalizedTriggerId = triggerId?.trim()?.lowercase()
        if (normalizedTriggerId.isNullOrBlank()) {
            return null
        }
        return triggerAliases[normalizedTriggerId] ?: normalizedTriggerId
    }

    fun isSupported(triggerId: String?): Boolean {
        val normalizedTriggerId = normalize(triggerId) ?: return false
        return normalizedTriggerId == ON_ACCEPT ||
            normalizedTriggerId == ON_OBJECTIVE_COMPLETE ||
            normalizedTriggerId == ON_STAGE_ENTER ||
            normalizedTriggerId == ON_STAGE_EXIT ||
            normalizedTriggerId == ON_STAGE_COMPLETE ||
            normalizedTriggerId == ON_COMPLETE ||
            normalizedTriggerId == ON_RETURN_TO_GIVER ||
            normalizedTriggerId == ON_FAIL ||
            normalizedTriggerId == ON_RESET
    }
}
