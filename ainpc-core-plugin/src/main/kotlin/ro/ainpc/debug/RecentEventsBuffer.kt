package ro.ainpc.debug

import org.bukkit.event.Cancellable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedDeque

class RecentEventsBuffer {
    private val buffer: ConcurrentLinkedDeque<RecentEvent> = ConcurrentLinkedDeque()
    @Volatile
    private var capacity: Int = DEFAULT_CAPACITY

    fun configure(maxCapacity: Int) {
        capacity = maxCapacity.coerceIn(MIN_CAPACITY, MAX_CAPACITY)
        trimToCapacity()
    }

    fun addEvent(event: org.bukkit.event.Event) {
        buffer.addLast(
            RecentEvent(
                System.currentTimeMillis(),
                event.eventName,
                event.isAsynchronous,
                (event as? Cancellable)?.isCancelled,
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
        val createdAtMillis: Long,
        val eventName: String,
        val isAsync: Boolean,
        val isCancelled: Boolean?,
    )

    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun buildRecentApiEventsText(): String {
        val sb = StringBuilder()
        val events = snapshot()
        sb.append("=== Recent AINPC API Events (in-memory buffer) ===\n\n")
        if (events.isEmpty()) {
            sb.append("Niciun eveniment API AINPC inregistrat in buffer.\n")
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
            if (entry.isCancelled == true) sb.append(" [CANCELLED]")
            sb.append("\n")
        }
        sb.append("\n--- Sfarsit buffer evenimente API ---\n")
        return sb.toString()
    }

    companion object {
        const val DEFAULT_CAPACITY = 100
        const val MIN_CAPACITY = 10
        const val MAX_CAPACITY = 1_000
    }
}
