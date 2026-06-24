package ro.ainpc.engine.runtime.actions

import ro.ainpc.engine.runtime.ScenarioActionHandler
import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition

class RecordStoryEventAction : ScenarioActionHandler {
    override fun type(): String = "record_story_event"

    companion object {
        private val logger = java.util.logging.Logger.getLogger(RecordStoryEventAction::class.java.name)
    }

    override fun execute(context: ScenarioExecutionContext, action: ScenarioRuntimeDefinition) {
        val eventType = action.parameter("event_type").ifBlank { "quest_action" }
        val eventKey = action.parameter("event_key").ifBlank { action.id() }
        logger.fine("[RuntimeAction] record_story_event: $eventType / $eventKey")
        logger.fine("[RuntimeAction]   scope: ${context.regionId()} / ${context.placeId()}")
        logger.fine("[RuntimeAction]   params: ${action.parameters()}")
    }
}
