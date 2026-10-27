package ro.ainpc.managers

import java.util.Collections
import java.util.UUID

internal data class NpcMemoryQuery(
    val npcUuid: UUID,
    val runtimeIdentity: UUID,
    val npcDatabaseId: Int,
    val playerUuid: UUID,
    val context: String,
    val limit: Int,
    val candidateLimit: Int,
    val maxPromptChars: Int,
)

class NpcMemorySnapshot internal constructor(
    request: NpcMemoryQuery,
    val status: Status,
    entries: List<MemoryRecallCandidate>,
    val recalledAtMillis: Long = System.currentTimeMillis(),
) {
    val npcUuid: UUID = request.npcUuid
    val runtimeIdentity: UUID = request.runtimeIdentity
    val playerUuid: UUID = request.playerUuid
    val query: String = request.context
    val entries: List<MemoryRecallCandidate> = Collections.unmodifiableList(ArrayList(entries))
    private val promptEntries: List<String> = Collections.unmodifiableList(this.entries.map(MemoryRecallCandidate::promptText))

    fun promptEntries(): List<String> = promptEntries

    enum class Status {
        READY, DISABLED, UNAVAILABLE,
    }
}
