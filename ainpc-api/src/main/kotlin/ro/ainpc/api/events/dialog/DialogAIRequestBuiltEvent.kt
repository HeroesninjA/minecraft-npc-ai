package ro.ainpc.api.events.dialog

import ro.ainpc.api.events.AINPCEventSource
import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID

class DialogAIRequestBuiltEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val conversationId: String,
    val playerUuid: UUID,
    val playerName: String,
    val npcId: String,
    val npcUuid: UUID?,
    val npcName: String,
    val message: String,
    val requestSummary: String,
    val directAddress: Boolean,
    val explicitConversation: Boolean,
    val triggerReason: String,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class DialogAIRequestBuiltEvent(
    val payload: DialogAIRequestBuiltEventPayload
) : org.bukkit.event.Event() {
    override fun getHandlers(): org.bukkit.event.HandlerList = HANDLERS

    companion object {
        private val HANDLERS = org.bukkit.event.HandlerList()
        @JvmStatic fun getHandlerList(): org.bukkit.event.HandlerList = HANDLERS
    }
}
