package ro.ainpc.api.events.quest

import ro.ainpc.api.events.AINPCEventSource
import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID

class ProgressionFailedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val playerUuid: UUID,
    val playerName: String,
    val npcId: String?,
    val npcUuid: UUID?,
    val npcName: String?,
    val questId: String?,
    val questName: String?,
    val failReason: String,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class ProgressionFailedEvent(
    val payload: ProgressionFailedEventPayload
) : org.bukkit.event.Event() {
    override fun getHandlers(): org.bukkit.event.HandlerList = HANDLERS

    companion object {
        private val HANDLERS = org.bukkit.event.HandlerList()
        @JvmStatic fun getHandlerList(): org.bukkit.event.HandlerList = HANDLERS
    }
}
