package ro.ainpc.progression

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ProgressionFormatUtil {
    private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun formatOptional(value: String?): String =
        if (value.isNullOrBlank()) "<nesetat>" else value

    fun compactUuid(uuid: String?): String {
        if (uuid.isNullOrBlank()) return "<nesetat>"
        return if (uuid.length > 8) uuid.substring(0, 8) else uuid
    }

    fun formatCountMap(values: Map<String, Int>?): String {
        if (values.isNullOrEmpty()) return "<gol>"
        return values.entries
            .sortedBy { it.key }
            .joinToString(", ") { "${it.key}=${it.value}" }
            .ifBlank { "<gol>" }
    }

    fun formatStoryTime(epochMillis: Long): String {
        if (epochMillis <= 0L) return "<necunoscut>"
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
            .format(TIME_FORMAT)
    }
}
