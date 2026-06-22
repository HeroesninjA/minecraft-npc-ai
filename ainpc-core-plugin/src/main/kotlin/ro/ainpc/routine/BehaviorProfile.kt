package ro.ainpc.routine

data class BehaviorProfile(
    val profileId: String,
    val occupation: String = "",
    val displayName: String = "",
    val schedule: List<ScheduleEntry> = emptyList(),
    val movementSpeed: Double = 0.6,
    val wanderRadius: Double = 8.0,
    val homeReturn: Boolean = true,
    val socializeChance: Double = 0.3,
    val weatherReactions: Boolean = true,
    val nightReturn: Boolean = true,
    val dangerAvoidance: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
) {
    data class ScheduleEntry(
        val label: String,
        val startTick: Long,
        val endTick: Long,
        val slot: String,
        val activity: String = "",
        val target: String = ""
    )
}
