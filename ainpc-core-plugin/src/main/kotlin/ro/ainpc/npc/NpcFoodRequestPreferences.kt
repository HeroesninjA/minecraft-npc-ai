package ro.ainpc.npc

import java.util.UUID

class NpcFoodRequestPreferences(private val maxEntries: Int = 32) {
    enum class Reply(val delayMillis: Long, val acknowledgement: String) {
        DECLINE(15 * 60 * 1_000L, "Inteleg. Nu iti voi mai cere mancare o vreme."),
        PROMISE(10 * 60 * 1_000L, "Bine, iti las timp sa aduci mancarea."),
    }

    private data class Pause(val recordedAt: Long, val reply: Reply) {
        fun isActive(now: Long): Boolean = now < recordedAt || now - recordedAt < reply.delayMillis
    }

    private val pauses = mutableMapOf<UUID, Pause>()

    init {
        require(maxEntries in 1..32)
    }

    @Synchronized
    fun isDeferred(playerUuid: UUID, now: Long): Boolean = pauses[playerUuid]?.isActive(now) == true

    @Synchronized
    fun canAsk(playerUuid: UUID, now: Long): Boolean {
        if (now <= 0L) return false
        prune(now)
        return playerUuid !in pauses && pauses.size < maxEntries
    }

    @Synchronized
    internal fun defer(playerUuid: UUID, reply: Reply, now: Long): Boolean {
        if (now <= 0L) return false
        prune(now)
        if (playerUuid !in pauses && pauses.size >= maxEntries) return false
        pauses[playerUuid] = Pause(now, reply)
        return true
    }

    @Synchronized
    internal fun confirmDelivery(playerUuid: UUID, receivedAt: Long): Boolean {
        val pause = pauses[playerUuid] ?: return false
        if (pause.reply != Reply.PROMISE || receivedAt < pause.recordedAt || !pause.isActive(receivedAt)) return false
        pauses.remove(playerUuid)
        return true
    }

    private fun prune(now: Long) {
        pauses.entries.removeIf { !it.value.isActive(now) }
    }
}
