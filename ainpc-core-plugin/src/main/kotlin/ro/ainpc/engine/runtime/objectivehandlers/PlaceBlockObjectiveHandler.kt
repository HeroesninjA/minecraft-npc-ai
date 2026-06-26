package ro.ainpc.engine.runtime.objectivehandlers

import ro.ainpc.engine.runtime.ObjectiveContext
import ro.ainpc.engine.runtime.ObjectiveHandler
import ro.ainpc.engine.runtime.ObjectiveResult

class PlaceBlockObjectiveHandler : ObjectiveHandler {
    override fun type(): String = "place_block"
    override fun handleProgress(context: ObjectiveContext): ObjectiveResult {
        val current = context.currentProgress
        val required = context.requiredAmount
        if (current >= required) return ObjectiveResult(progressed = 0, completed = true)
        return ObjectiveResult(progressed = 1, completed = (current + 1 >= required))
    }
}
