package ro.ainpc.engine.runtime.actions

import ro.ainpc.engine.runtime.ScenarioActionHandler
import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition

class SetStoryStateAction : ScenarioActionHandler {
    override fun type(): String = "set_story_state"

    override fun execute(context: ScenarioExecutionContext, action: ScenarioRuntimeDefinition) {
        val stateKey = action.parameter("state").ifBlank { action.parameter("state_key").ifBlank { "default" } }
        println("[RuntimeAction] set_story_state: $stateKey")
        println("[RuntimeAction]   scope: ${context.regionId()} / ${context.placeId()}")
        println("[RuntimeAction]   params: ${action.parameters()}")
    }
}
