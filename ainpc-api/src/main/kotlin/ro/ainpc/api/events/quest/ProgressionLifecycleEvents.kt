package ro.ainpc.api.events.quest

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

abstract class ProgressionLifecycleEvent(
    val payload: ProgressionEventPayload
) : Event()

class ProgressionAcceptedEvent(
    payload: ProgressionEventPayload
) : ProgressionLifecycleEvent(payload) {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}

class ProgressionDeclinedEvent(
    payload: ProgressionEventPayload
) : ProgressionLifecycleEvent(payload) {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}

class ProgressionAbandonedEvent(
    payload: ProgressionEventPayload
) : ProgressionLifecycleEvent(payload) {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}

class ProgressionCompletedEvent(
    payload: ProgressionEventPayload
) : ProgressionLifecycleEvent(payload) {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
