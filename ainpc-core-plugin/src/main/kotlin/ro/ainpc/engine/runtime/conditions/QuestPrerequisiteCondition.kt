package ro.ainpc.engine.runtime.conditions

import ro.ainpc.engine.runtime.ScenarioConditionHandler
import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition

class QuestPrerequisiteCondition : ScenarioConditionHandler {
    override fun type(): String = "quest_prerequisite"

    override fun evaluate(context: ScenarioExecutionContext, condition: ScenarioRuntimeDefinition): Boolean {
        val prereq = condition.parameter("template_id").ifBlank {
            condition.parameter("quest_code").ifBlank { return true }
        }
        val statusKey = "quest_completed_$prereq"
        return context.variable(statusKey).equals("true", ignoreCase = true)
    }
}
