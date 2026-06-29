package ro.ainpc.engine.runtime.conditions

import ro.ainpc.engine.runtime.ScenarioConditionHandler
import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition

class MechanicLimitCondition : ScenarioConditionHandler {
    override fun type(): String = "mechanic_limit"

    override fun evaluate(context: ScenarioExecutionContext, condition: ScenarioRuntimeDefinition): Boolean {
        val mechanicId = condition.parameter("mechanic_id").ifBlank { return true }
        val maxActive = condition.parameter("max_active").toIntOrNull() ?: return true
        val countKey = "mechanic_active_count_$mechanicId"
        val currentCount = context.variable(countKey).toIntOrNull() ?: 0
        return currentCount < maxActive
    }
}
