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
    val routineBiasTicks: Long = 0L,
    val routineGoals: Map<String, String> = emptyMap(),
    val phaseTicks: Map<String, Long> = emptyMap(),
    val routineTexts: Map<String, String> = emptyMap(),
    val fallbackRules: List<FallbackRule> = emptyList(),
    val thresholds: Map<String, Int> = emptyMap(),
    val slotStates: Map<String, String> = emptyMap(),
    val zoneStates: Map<String, String> = emptyMap(),
    val zoneActivitySuffixes: Map<String, String> = emptyMap(),
    val previewPoints: List<PreviewPoint> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) {
    data class ScheduleEntry(
        val label: String,
        val startTick: Long,
        val endTick: Long,
        val slot: String,
        val activity: String = "",
        val target: String = "",
        val state: String = ""
    )

    data class FallbackRule(
        val condition: String,
        val slot: String,
        val activityKey: String,
        val state: String = ""
    )

    data class PreviewPoint(
        val label: String,
        val worldTime: Long
    )
}
