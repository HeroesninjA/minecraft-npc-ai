package ro.ainpc.engine.runtime.triggers

import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition
import ro.ainpc.engine.runtime.ScenarioTriggerHandler
import java.util.logging.Logger

class PlayerEntersRegionTrigger : ScenarioTriggerHandler {
    override fun type(): String = "player_enters_region"

    companion object {
        private val logger = Logger.getLogger(PlayerEntersRegionTrigger::class.java.name)
    }

    override fun bind(context: ScenarioExecutionContext, trigger: ScenarioRuntimeDefinition) {
        val regionId = trigger.parameter("region_id").ifBlank { context.regionId() }
        if (regionId.isBlank()) return
        val triggerKey = "trigger_fired_player_enters_region_$regionId"
        val existingTriggers = context.variable(triggerKey)
        val count = (existingTriggers.toIntOrNull() ?: 0) + 1
        logger.fine("[Trigger] player_enters_region: $regionId (fired #$count)")
    }
}
