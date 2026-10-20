package ro.ainpc.engine

internal data class ScenarioManualStartReadiness(val minimumNpcCount: Int, val activeNpcCount: Int) {
    val meetsMinimum: Boolean get() = activeNpcCount >= minimumNpcCount
}

internal object ScenarioManualStartPolicy {
    const val RADIUS = 32.0

    fun inspect(template: ScenarioTemplate, activeNpcCount: Int): ScenarioManualStartReadiness =
        ScenarioManualStartReadiness(template.minimumNpcCount.coerceAtLeast(0), activeNpcCount.coerceAtLeast(0))
}
