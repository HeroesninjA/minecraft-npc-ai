package ro.ainpc.world

import java.util.Locale

internal object WorkplaceOccupancy {
    fun workerIds(metadata: Map<String, String>): Set<String> =
        mappedNpcIds(metadata, "worker_npc_ids")

    fun availableSlots(capacity: Int, metadata: Map<String, String>, reserved: Int = 0): Int {
        require(capacity >= 0 && reserved >= 0)
        return (capacity.toLong() - workerIds(metadata).size - reserved).coerceAtLeast(0L).toInt()
    }
}

internal fun mappedNpcIds(metadata: Map<String, String>, key: String): Set<String> =
    metadata[key].orEmpty()
        .split(Regex("[,;]"))
        .map { it.trim().lowercase(Locale.ROOT) }
        .filter { it.isNotBlank() }
        .toSet()
