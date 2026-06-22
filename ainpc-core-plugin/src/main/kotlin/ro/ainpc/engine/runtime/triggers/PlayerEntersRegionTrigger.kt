package ro.ainpc.engine.runtime.triggers

import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition
import ro.ainpc.engine.runtime.ScenarioTriggerHandler

class PlayerEntersRegionTrigger : ScenarioTriggerHandler {
    override fun type(): String = "player_enters_region"

    override fun bind(context: ScenarioExecutionContext, trigger: ScenarioRuntimeDefinition) {
        println("[Trigger] player_enters_region: regiona curenta=${context.regionId()}, trigger params=${trigger.parameters()}")
    }
}
