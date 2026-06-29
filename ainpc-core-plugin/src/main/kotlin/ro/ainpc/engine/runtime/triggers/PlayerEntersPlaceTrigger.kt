package ro.ainpc.engine.runtime.triggers

import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition
import ro.ainpc.engine.runtime.ScenarioTriggerHandler

class PlayerEntersPlaceTrigger : ScenarioTriggerHandler {
    override fun type(): String = "player_enters_place"

    override fun bind(context: ScenarioExecutionContext, trigger: ScenarioRuntimeDefinition) {
        val placeId = trigger.parameter("place_id").ifBlank { context.placeId() }
        if (placeId.isBlank()) return
        val triggerKey = "trigger_fired_player_enters_place_$placeId"
        val count = (context.variable(triggerKey).toIntOrNull() ?: 0) + 1
    }
}
