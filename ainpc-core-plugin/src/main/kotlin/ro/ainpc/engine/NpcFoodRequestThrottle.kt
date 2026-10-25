package ro.ainpc.engine

import java.util.UUID

internal class NpcFoodRequestThrottle(private val maxEntries: Int = 4096) {
    private val npcRequests = mutableMapOf<UUID, Long>()
    private val playerRequests = mutableMapOf<UUID, Long>()
    private var delivering = false

    init {
        require(maxEntries in 1..4096)
    }

    @Synchronized
    fun deliver(npcUuid: UUID, playerUuid: UUID, now: Long, send: () -> Unit): Boolean {
        if (now <= 0L || delivering) return false
        npcRequests.entries.removeIf { expired(it.value, now, NPC_COOLDOWN) }
        playerRequests.entries.removeIf { expired(it.value, now, PLAYER_COOLDOWN) }
        if (npcUuid in npcRequests || playerUuid in playerRequests ||
            npcRequests.size >= maxEntries || playerRequests.size >= maxEntries
        ) return false

        delivering = true
        try {
            send()
        } finally {
            delivering = false
        }
        npcRequests[npcUuid] = now
        playerRequests[playerUuid] = now
        return true
    }

    private fun expired(sentAt: Long, now: Long, cooldown: Long): Boolean =
        now >= sentAt && now - sentAt >= cooldown

    companion object {
        private const val NPC_COOLDOWN = 300_000L
        private const val PLAYER_COOLDOWN = 60_000L
    }
}
