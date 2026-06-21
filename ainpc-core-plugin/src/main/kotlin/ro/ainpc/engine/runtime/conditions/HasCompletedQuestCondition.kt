package ro.ainpc.engine.runtime.conditions

import ro.ainpc.engine.runtime.ScenarioConditionHandler
import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition

class HasCompletedQuestCondition : ScenarioConditionHandler {
    override fun type(): String = "has_completed_quest"

    override fun evaluate(context: ScenarioExecutionContext, condition: ScenarioRuntimeDefinition): Boolean {
        val templateId = condition.parameter("template_id").ifBlank {
            condition.parameter("quest_code").ifBlank { condition.parameter("id").ifBlank { return false } }
        }
        val statusKey = "quest_completed_$templateId"
        return context.variable(statusKey).equals("true", ignoreCase = true)
    }
}
