package ro.ainpc.engine.runtime.actions

import ro.ainpc.engine.runtime.ScenarioActionHandler
import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition
import java.util.logging.Logger

class SetStoryStateAction : ScenarioActionHandler {
    override fun type(): String = "set_story_state"

    companion object {
        private val logger = Logger.getLogger(SetStoryStateAction::class.java.name)
    }

    override fun execute(context: ScenarioExecutionContext, action: ScenarioRuntimeDefinition) {
        val stateKey = action.parameter("state").ifBlank { action.parameter("state_key").ifBlank { "default" } }
        logger.fine("[RuntimeAction] set_story_state: $stateKey")
        logger.fine("[RuntimeAction]   scope: ${context.regionId()} / ${context.placeId()}")
        logger.fine("[RuntimeAction]   params: ${action.parameters()}")
    }
}
