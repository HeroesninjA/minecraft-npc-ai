package ro.ainpc.engine.runtime.triggers

import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition
import ro.ainpc.engine.runtime.ScenarioTriggerHandler

class PlayerEntersRegionTrigger : ScenarioTriggerHandler {
    override fun type(): String = "player_enters_region"

    companion object {
        private val logger = java.util.logging.Logger.getLogger(PlayerEntersRegionTrigger::class.java.name)
    }

    override fun bind(context: ScenarioExecutionContext, trigger: ScenarioRuntimeDefinition) {
        logger.fine("[Trigger] player_enters_region: regiona curenta=${context.regionId()}, trigger params=${trigger.parameters()}")
    }
}
