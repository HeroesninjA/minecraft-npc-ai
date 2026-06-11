package ro.ainpc.managers

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Pastreaza doar starea de sesiune a conversatiilor active.
 * Listener-ele nu ar trebui sa detina aceasta stare direct.
 */
class ConversationSessionManager(private val plugin: AINPCPlugin) {
    private val sessions = ConcurrentHashMap<UUID, ConversationSession>()

    fun startConversation(player: Player, npc: AINPC) {
        sessions[player.uniqueId] = ConversationSession(npc.uuid, System.currentTimeMillis())
    }

    fun touchConversation(player: Player) {
        sessions.computeIfPresent(player.uniqueId) { _, session -> session.touch() }
    }

    fun isInConversation(player: Player): Boolean = sessions.containsKey(player.uniqueId)

    fun isExpired(player: Player, timeoutMillis: Long): Boolean {
        val session = sessions[player.uniqueId]
        return session != null && System.currentTimeMillis() - session.lastInteractionAt() > timeoutMillis
    }

    fun getConversationNpcId(player: Player): UUID? = sessions[player.uniqueId]?.npcUuid()

    fun getConversationPartner(player: Player): AINPC? {
        val npcUuid = getConversationNpcId(player)
        return if (npcUuid != null) plugin.npcManager.getNPCByUuid(npcUuid) else null
    }

    fun clearConversation(player: Player) {
        clearConversation(player.uniqueId)
    }

    fun clearConversation(playerUuid: UUID) {
        sessions.remove(playerUuid)
    }

    private data class ConversationSession(val npcUuid: UUID, val lastInteractionAt: Long) {
        fun npcUuid(): UUID = npcUuid
        fun lastInteractionAt(): Long = lastInteractionAt

        fun touch(): ConversationSession = ConversationSession(npcUuid, System.currentTimeMillis())
    }
}
