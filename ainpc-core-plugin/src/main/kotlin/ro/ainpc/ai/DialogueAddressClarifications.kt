package ro.ainpc.ai

import java.util.Collections
import java.util.UUID

internal data class PendingDialogueAddress(
    val message: String,
    val candidates: List<DialogueAddressCandidate>,
    val sessionId: UUID?,
    val expiresAtMillis: Long,
)

internal sealed interface DialogueAddressReply {
    data object None : DialogueAddressReply
    data class Selected(val npcId: UUID, val message: String) : DialogueAddressReply
    data class Retry(val pending: PendingDialogueAddress) : DialogueAddressReply
    data object Expired : DialogueAddressReply
    data object Cancelled : DialogueAddressReply
}

internal class DialogueAddressClarifications(
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val lifetimeMillis: Long = 60_000L,
    private val maxPending: Int = 512,
) {
    private val pendingByPlayer = LinkedHashMap<UUID, PendingDialogueAddress>()
    private val integerChoice = Regex("[+-]?\\d+")
    private val cancellationWords = setOf("anuleaza", "cancel", "renunta", "niciunul", "niciuna")

    init {
        require(lifetimeMillis > 0)
        require(maxPending > 0)
    }

    @Synchronized
    fun begin(
        playerId: UUID,
        message: String,
        candidates: List<DialogueAddressCandidate>,
        sessionId: UUID?,
    ): PendingDialogueAddress? {
        val now = nowMillis()
        pendingByPlayer.entries.removeIf { it.value.expiresAtMillis <= now }
        pendingByPlayer.remove(playerId)
        val distinctCandidates = candidates.distinctBy { it.npcId }
        if (distinctCandidates.size !in 2..8) return null

        while (pendingByPlayer.size >= maxPending) {
            val entries = pendingByPlayer.entries.iterator()
            entries.next()
            entries.remove()
        }
        val pending = PendingDialogueAddress(
            message,
            Collections.unmodifiableList(distinctCandidates),
            sessionId,
            now + lifetimeMillis,
        )
        pendingByPlayer[playerId] = pending
        return pending
    }

    @Synchronized
    fun reply(
        playerId: UUID,
        input: String,
        currentSessionId: UUID?,
        sessionExpired: Boolean = false,
    ): DialogueAddressReply {
        val pending = pendingByPlayer[playerId] ?: return DialogueAddressReply.None
        val normalized = DialogueMessageText.normalize(input)
        val cancellation = normalized in cancellationWords
        val numberedChoice = integerChoice.matches(normalized)
        val matchingCandidates = if (normalized.isBlank()) emptyList() else pending.candidates.filter { candidate ->
            listOfNotNull(candidate.name, candidate.displayName)
                .any { DialogueMessageText.normalize(it) == normalized }
        }
        val recognized = cancellation || numberedChoice || matchingCandidates.isNotEmpty()
        if (sessionExpired || pending.sessionId != currentSessionId || nowMillis() >= pending.expiresAtMillis) {
            pendingByPlayer.remove(playerId)
            return if (recognized) DialogueAddressReply.Expired else DialogueAddressReply.None
        }
        if (cancellation) {
            pendingByPlayer.remove(playerId)
            return DialogueAddressReply.Cancelled
        }
        val selected = if (numberedChoice) {
            normalized.toIntOrNull()?.takeIf { it in 1..pending.candidates.size }
                ?.let { pending.candidates[it - 1] }
        } else {
            matchingCandidates.singleOrNull()
        }
        if (selected != null) {
            pendingByPlayer.remove(playerId)
            return DialogueAddressReply.Selected(selected.npcId, pending.message)
        }
        if (recognized) return DialogueAddressReply.Retry(pending)

        pendingByPlayer.remove(playerId)
        return DialogueAddressReply.None
    }

    @Synchronized
    fun clear(playerId: UUID) {
        pendingByPlayer.remove(playerId)
    }
}
