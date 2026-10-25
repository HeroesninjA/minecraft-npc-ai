package ro.ainpc.engine

import java.util.UUID

internal class NpcFoodRequestFollowUps(private val maxEntries: Int = 4096) {
    data class Request(
        val npcUuid: UUID,
        val npcRuntimeIdentity: UUID,
        val npcRevision: Long,
        val playerUuid: UUID,
        val receiptId: UUID,
        val sentAt: Long,
    )

    private val pending = mutableMapOf<UUID, Request>()

    init {
        require(maxEntries in 1..4096)
    }

    @Synchronized
    fun canRecord(playerUuid: UUID, now: Long): Boolean {
        if (now <= 0L) return false
        prune(now)
        return playerUuid !in pending && pending.size < maxEntries
    }

    @Synchronized
    fun record(request: Request): Boolean {
        if (!canRecord(request.playerUuid, request.sentAt)) return false
        pending[request.playerUuid] = request
        return true
    }

    @Synchronized
    fun find(playerUuid: UUID, now: Long): Request? {
        if (now <= 0L) return null
        prune(now)
        return pending[playerUuid]?.takeIf { now >= it.sentAt }
    }

    @Synchronized
    fun consume(request: Request, now: Long): Boolean {
        if (find(request.playerUuid, now) != request) return false
        pending.remove(request.playerUuid)
        return true
    }

    @Synchronized
    fun forgetPlayer(playerUuid: UUID) {
        pending.remove(playerUuid)
    }

    private fun prune(now: Long) {
        pending.entries.removeIf { now >= it.value.sentAt && now - it.value.sentAt >= REPLY_WINDOW }
    }

    companion object {
        private const val REPLY_WINDOW = 45_000L
    }
}
