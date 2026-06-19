package ro.ainpc.debug

import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedDeque

class RecentEventsBuffer(private val plugin: Plugin) : Listener {
    private val buffer: ConcurrentLinkedDeque<RecentEvent> = ConcurrentLinkedDeque()
    private var capacity: Int = 100

    private val AINPC_EVENT_PREFIXES = setOf(
        "AINPC", "Progression", "Dialog", "Story",
        "WorldContext", "NPCContext", "ContextSignal", "PlayerContext"
    )

    fun configure(maxCapacity: Int) {
        capacity = maxOf(10, maxCapacity)
        trimToCapacity()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPublicEvent(event: Event) {
        if (capacity <= 0) return
        val name = event.eventName
        if (!AINPC_EVENT_PREFIXES.any { name.startsWith(it) }) return
        buffer.addLast(
            RecentEvent(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                name,
                event.isAsynchronous
            )
        )
        trimToCapacity()
    }

    fun snapshot(): List<RecentEvent> {
        return buffer.toList().reversed()
    }

    fun clear() {
        buffer.clear()
    }

    private fun trimToCapacity() {
        while (buffer.size > capacity) {
            buffer.pollFirst()
        }
    }

    data class RecentEvent(
        val eventId: UUID,
        val createdAtMillis: Long,
        val eventName: String,
        val isAsync: Boolean
    )

    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun buildRecentEventsText(): String {
        val sb = StringBuilder()
        val events = snapshot()
        sb.append("=== Recent Public Events (in-memory buffer) ===\n\n")
        if (events.isEmpty()) {
            sb.append("Niciun eveniment public inregistrat in buffer.\n")
            sb.append("Asigura-te ca events.public_api_enabled = true si ca exista activitate.\n")
            return sb.toString()
        }
        sb.append("Capacitate maxima: $capacity\n")
        sb.append("Evenimente in buffer: ${events.size}\n\n")
        for ((index, entry) in events.withIndex()) {
            val time = Instant.ofEpochMilli(entry.createdAtMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DATE_FORMAT)
            sb.append("#${index + 1} [$time] ${entry.eventName}")
            if (entry.isAsync) sb.append(" [ASYNC]")
            sb.append("\n")
        }
        sb.append("\n--- Sfarsit buffer evenimente ---\n")
        return sb.toString()
    }
}
