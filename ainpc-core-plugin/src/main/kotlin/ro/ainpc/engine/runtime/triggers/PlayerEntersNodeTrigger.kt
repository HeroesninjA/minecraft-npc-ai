package ro.ainpc.engine.runtime.triggers

import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition
import ro.ainpc.engine.runtime.ScenarioTriggerHandler

class PlayerEntersNodeTrigger : ScenarioTriggerHandler {
    override fun type(): String = "player_enters_node"

    override fun bind(context: ScenarioExecutionContext, trigger: ScenarioRuntimeDefinition) {
        val nodeId = trigger.parameter("node_id").ifBlank { context.nodeId() }
        if (nodeId.isBlank()) return
        val triggerKey = "trigger_fired_player_enters_node_$nodeId"
        val count = (context.variable(triggerKey).toIntOrNull() ?: 0) + 1
    }
}
