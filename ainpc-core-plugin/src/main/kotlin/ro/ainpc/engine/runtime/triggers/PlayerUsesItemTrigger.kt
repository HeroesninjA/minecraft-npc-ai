package ro.ainpc.engine.runtime.triggers

import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition
import ro.ainpc.engine.runtime.ScenarioTriggerHandler

class PlayerUsesItemTrigger : ScenarioTriggerHandler {
    override fun type(): String = "player_uses_item"

    override fun bind(context: ScenarioExecutionContext, trigger: ScenarioRuntimeDefinition) {
        val itemType = trigger.parameter("item").ifBlank { trigger.parameter("material").ifBlank { return } }
        val triggerKey = "trigger_fired_player_uses_item_${itemType.uppercase()}"
        val count = (context.variable(triggerKey).toIntOrNull() ?: 0) + 1
    }
}
