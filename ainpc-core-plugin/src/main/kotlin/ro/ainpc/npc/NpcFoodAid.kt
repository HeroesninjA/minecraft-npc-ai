package ro.ainpc.npc

import java.nio.charset.StandardCharsets
import java.util.UUID

internal object NpcFoodAid {
    enum class Status {
        ACCEPTED, NOT_HUNGRY, UNAVAILABLE, UNSUPPORTED, TRANSFER_FAILED,
    }

    data class Result(val status: Status, val receipt: NpcSocialExperience.Receipt? = null)

    fun foodGain(itemKey: String): Int = when (itemKey) {
        "minecraft:bread", "minecraft:baked_potato" -> 20
        "minecraft:apple" -> 12
        "minecraft:cooked_beef" -> 30
        else -> 0
    }

    fun offer(
        npc: AINPC,
        helperUuid: UUID,
        helperName: String,
        itemKey: String,
        now: Long,
        consumeOne: () -> Boolean,
        rollback: () -> Unit,
    ): Result {
        val gain = foodGain(itemKey)
        if (gain == 0) return Result(Status.UNSUPPORTED)
        if (npc.simulationMode != NpcSimulationMode.FULL || npc.context.isInDanger ||
            npc.safetyLevel !in 35..100 || !npc.currentState.allowsPlayerInteraction() || npc.hungerLevel !in 0..100
        ) return Result(Status.UNAVAILABLE)
        val beforeHunger = npc.hungerLevel
        if (beforeHunger >= NpcSelfCareThresholds.START) return Result(Status.NOT_HUNGRY)
        val afterHunger = (beforeHunger + gain).coerceAtMost(100)
        val receiptId = UUID.nameUUIDFromBytes(
            "${npc.uuid}|$helperUuid|$itemKey|$beforeHunger|$afterHunger|$now".toByteArray(StandardCharsets.UTF_8),
        )
        val receipt = NpcSocialExperience.Receipt(
            receiptId, helperUuid, helperName, itemKey, 1, beforeHunger, afterHunger, now,
        )
        if (!NpcSocialExperience.isValidReceipt(receipt)) return Result(Status.UNAVAILABLE)
        if (npc.socialExperience.snapshot().any { it.id == receiptId }) return Result(Status.TRANSFER_FAILED)

        val beforeContextHunger = npc.context.hungerLevel
        return try {
            if (!consumeOne()) return Result(Status.TRANSFER_FAILED)
            npc.actionExperience.cancelPending()
            npc.hungerLevel = afterHunger
            npc.context.hungerLevel = afterHunger
            if (!npc.socialExperience.record(receipt)) throw IllegalStateException("Food aid receipt was not recorded")
            Result(Status.ACCEPTED, receipt)
        } catch (failure: Exception) {
            npc.hungerLevel = beforeHunger
            npc.context.hungerLevel = beforeContextHunger
            rollback()
            Result(Status.TRANSFER_FAILED)
        }
    }
}
