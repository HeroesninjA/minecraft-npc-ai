package ro.ainpc.engine.runtime.actions

import ro.ainpc.engine.runtime.ScenarioActionHandler
import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition

class RecordStoryEventAction : ScenarioActionHandler {
    override fun type(): String = "record_story_event"

    override fun execute(context: ScenarioExecutionContext, action: ScenarioRuntimeDefinition) {
        val eventType = action.parameter("event_type").ifBlank { "quest_action" }
        val eventKey = action.parameter("event_key").ifBlank { action.id() }
        println("[RuntimeAction] record_story_event: $eventType / $eventKey")
        println("[RuntimeAction]   scope: ${context.regionId()} / ${context.placeId()}")
        println("[RuntimeAction]   params: ${action.parameters()}")
    }
}
