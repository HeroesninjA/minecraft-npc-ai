package ro.ainpc.managers

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.ai.DialogueAddressCandidate
import ro.ainpc.ai.DialogueAddressClarifications
import ro.ainpc.ai.DialogueAddressReply
import ro.ainpc.ai.PendingDialogueAddress
import ro.ainpc.npc.AINPC
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Pastreaza doar starea de sesiune a conversatiilor active.
 * Listener-ele nu ar trebui sa detina aceasta stare direct.
 */
/**
 * Conversationsessionmanager.
 */
class ConversationSessionManager @JvmOverloads constructor(
    private val plugin: AINPCPlugin,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val sessions = ConcurrentHashMap<UUID, ConversationSession>()
    private val addressClarifications = DialogueAddressClarifications(nowMillis = nowMillis)

    fun startConversation(player: Player, npc: AINPC) {
        addressClarifications.clear(player.uniqueId)
        sessions[player.uniqueId] = ConversationSession(npc.uuid, nowMillis())
    }

    fun touchConversation(player: Player) {
        sessions.computeIfPresent(player.uniqueId) { _, session ->
            val now = nowMillis()
            if (session.isExpired(now, IDLE_TIMEOUT_MILLIS)) session else session.touch(now)
        }
    }

    fun isInConversation(player: Player): Boolean =
        sessions[player.uniqueId]?.isExpired(nowMillis(), IDLE_TIMEOUT_MILLIS) == false

    @JvmOverloads
    fun isExpired(player: Player, timeoutMillis: Long = IDLE_TIMEOUT_MILLIS): Boolean {
        val session = sessions[player.uniqueId]
        return session != null && session.isExpired(nowMillis(), timeoutMillis)
    }

    fun getConversationNpcId(player: Player): UUID? = sessions[player.uniqueId]?.npcUuid()

    fun getConversationSessionId(player: Player): UUID? = sessions[player.uniqueId]?.sessionId

    fun isCurrentSession(player: Player, sessionId: UUID?): Boolean {
        val session = sessions[player.uniqueId] ?: return false
        return session.sessionId == sessionId && !session.isExpired(nowMillis(), IDLE_TIMEOUT_MILLIS)
    }

    internal fun beginAddressClarification(
        player: Player,
        message: String,
        candidates: List<DialogueAddressCandidate>,
    ): PendingDialogueAddress? = addressClarifications.begin(
        player.uniqueId, message, candidates, getConversationSessionId(player),
    )

    internal fun replyToAddressClarification(
        player: Player,
        input: String,
        sessionTimeoutMillis: Long = IDLE_TIMEOUT_MILLIS,
    ): DialogueAddressReply = addressClarifications.reply(
        player.uniqueId, input, getConversationSessionId(player), isExpired(player, sessionTimeoutMillis),
    )

    internal fun clearAddressClarification(player: Player) {
        addressClarifications.clear(player.uniqueId)
    }

    fun getConversationPartner(player: Player): AINPC? {
        val npcUuid = getConversationNpcId(player)
        return if (npcUuid != null) plugin.npcManager.getNPCByUuid(npcUuid) else null
    }

    fun clearConversation(player: Player) {
        clearConversation(player.uniqueId)
    }

    fun clearConversation(playerUuid: UUID) {
        addressClarifications.clear(playerUuid)
        sessions.remove(playerUuid)
    }

    private data class ConversationSession(
        val npcUuid: UUID,
        val lastInteractionAt: Long,
        val sessionId: UUID = UUID.randomUUID(),
    ) {
        fun npcUuid(): UUID = npcUuid
        fun isExpired(now: Long, timeoutMillis: Long): Boolean = now - lastInteractionAt >= timeoutMillis

        fun touch(now: Long): ConversationSession = copy(lastInteractionAt = now)
    }

    companion object {
        const val IDLE_TIMEOUT_MILLIS = 300_000L
    }
}
