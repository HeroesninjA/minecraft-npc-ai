package ro.ainpc.engine.runtime.triggers

import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition
import ro.ainpc.engine.runtime.ScenarioTriggerHandler

class PlayerTalksToNpcTrigger : ScenarioTriggerHandler {
    override fun type(): String = "player_talks_to_npc"

    override fun bind(context: ScenarioExecutionContext, trigger: ScenarioRuntimeDefinition) {
        val profession = trigger.parameter("profession").ifBlank { "" }
        val npcName = trigger.parameter("npc_name").ifBlank { "" }
        if (profession.isBlank() && npcName.isBlank()) return
        val triggerKey = "trigger_fired_player_talks_to_npc_${profession}_${npcName}"
        val count = (context.variable(triggerKey).toIntOrNull() ?: 0) + 1
    }
}
