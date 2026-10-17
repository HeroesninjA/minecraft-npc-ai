package ro.ainpc.ai

import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

internal class DialogueRequestGate {
    private val active = ConcurrentHashMap<Participants, Any>()

    fun isActive(playerId: UUID, npcId: UUID): Boolean = active.containsKey(Participants(playerId, npcId))

    fun <Result> submit(
        playerId: UUID,
        npcId: UUID,
        busyResult: () -> Result,
        operation: () -> CompletableFuture<Result>,
    ): CompletableFuture<Result> {
        val participants = Participants(playerId, npcId)
        val token = Any()
        if (active.putIfAbsent(participants, token) != null) {
            return CompletableFuture.completedFuture(busyResult())
        }
        val work = try {
            operation()
        } catch (failure: Throwable) {
            active.remove(participants, token)
            throw failure
        }
        val result = CompletableFuture<Result>()
        work.whenComplete { response, failure ->
            active.remove(participants, token)
            if (failure == null) {
                result.complete(response)
            } else {
                result.completeExceptionally(failure)
            }
        }
        return result
    }

    private data class Participants(val playerId: UUID, val npcId: UUID)
}
