package ro.ainpc.gui

import org.bukkit.entity.Player
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class GuiSessionManager {
    private val sessionsById: ConcurrentMap<UUID, GuiSession> = ConcurrentHashMap()
    private val sessionsByPlayer: ConcurrentMap<UUID, UUID> = ConcurrentHashMap()

    fun create(player: Player, key: GuiKey): GuiSession {
        closePlayer(player.uniqueId)

        val session = GuiSession(
            UUID.randomUUID(),
            player.uniqueId,
            key,
            System.currentTimeMillis()
        )
        sessionsById[session.getSessionId()] = session
        sessionsByPlayer[player.uniqueId] = session.getSessionId()
        return session
    }

    fun find(sessionId: UUID): Optional<GuiSession> = Optional.ofNullable(sessionsById[sessionId])

    fun close(sessionId: UUID) {
        val removed = sessionsById.remove(sessionId)
        if (removed != null) {
            sessionsByPlayer.remove(removed.getPlayerId(), sessionId)
        }
    }

    fun closePlayer(playerId: UUID) {
        val sessionId = sessionsByPlayer.remove(playerId)
        if (sessionId != null) {
            sessionsById.remove(sessionId)
        }
    }

    fun closeAll() {
        sessionsById.clear()
        sessionsByPlayer.clear()
    }
}
