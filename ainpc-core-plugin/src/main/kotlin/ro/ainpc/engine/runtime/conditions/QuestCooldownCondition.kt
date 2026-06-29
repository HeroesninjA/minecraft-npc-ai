package ro.ainpc.engine.runtime.conditions

import ro.ainpc.engine.runtime.ScenarioConditionHandler
import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition

class QuestCooldownCondition : ScenarioConditionHandler {
    override fun type(): String = "quest_cooldown"

    override fun evaluate(context: ScenarioExecutionContext, condition: ScenarioRuntimeDefinition): Boolean {
        val cooldownParam = condition.parameter("seconds").toLongOrNull() ?: return true
        val templateId = condition.parameter("template_id").ifBlank {
            condition.parameter("quest_code").ifBlank { return true }
        }
        val completedKey = "quest_completed_$templateId"
        val completedAt = context.variable(completedKey + "_at").toLongOrNull() ?: return true
        val elapsed = (System.currentTimeMillis() - completedAt) / 1000L
        return elapsed >= cooldownParam
    }
}
