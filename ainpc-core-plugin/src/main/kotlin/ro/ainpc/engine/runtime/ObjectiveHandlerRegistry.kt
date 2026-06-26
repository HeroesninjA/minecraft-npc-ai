package ro.ainpc.engine.runtime

import java.util.Collections
import java.util.LinkedHashMap

class ObjectiveHandlerRegistry {
    private val handlers = LinkedHashMap<String, ObjectiveHandler>()

    fun register(handler: ObjectiveHandler) {
        val type = normalize(handler.type())
        if (type.isNotBlank() && !handlers.containsKey(type)) {
            handlers[type] = handler
        }
    }

    fun find(type: String?): ObjectiveHandler? = handlers[normalize(type)]

    fun supports(type: String?): Boolean = find(type) != null

    fun handlers(): Map<String, ObjectiveHandler> =
        java.util.Collections.unmodifiableMap(LinkedHashMap(handlers))

    private fun normalize(value: String?): String = value?.trim()?.lowercase().orEmpty()
}
