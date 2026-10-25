package ro.ainpc.npc

import java.util.UUID

class NpcSocialExperience {
    data class Receipt(
        val id: UUID,
        val helperUuid: UUID,
        val helperName: String,
        val itemKey: String,
        val amount: Int,
        val beforeHunger: Int,
        val afterHunger: Int,
        val recordedAt: Long,
    )

    private var receipts: List<Receipt> = emptyList()

    @Synchronized
    fun record(receipt: Receipt): Boolean {
        if (!isValidReceipt(receipt) || receipts.any { it.id == receipt.id }) return false
        receipts = (receipts + receipt).sortedBy { it.recordedAt }.takeLast(MAX_RECEIPTS)
        return true
    }

    @Synchronized
    fun snapshot(): List<Receipt> = receipts.toList()

    @Synchronized
    fun restore(receipts: List<Receipt>) {
        this.receipts = receipts.filter(::isValidReceipt).sortedBy { it.recordedAt }.asReversed()
            .distinctBy { it.id }.take(MAX_RECEIPTS).reversed()
    }

    fun latestFor(uuid: UUID, now: Long): Receipt? = snapshot()
        .filter { it.helperUuid == uuid && isRecent(it, now) }.maxByOrNull { it.recordedAt }

    fun descriptionFor(uuid: UUID, now: Long): String {
        if (latestFor(uuid, now) == null) return ""
        return "Mi-ai oferit mancare cand imi era foame si m-ai ajutat sa ma simt mai satul."
    }

    fun scoreModifier(uuid: UUID, action: NPCAction, state: NpcCognitiveState, now: Long): Int {
        if (action !in setOf(NPCAction.GREET, NPCAction.TALK, NPCAction.THANK) ||
            state.hungerLevel !in 0 until NpcSelfCareThresholds.START ||
            state.isInDanger || state.safetyLevel !in 35..100 || !state.currentState.allowsPlayerInteraction()
        ) return 0

        val count = snapshot().count { it.helperUuid == uuid && isRecent(it, now) }
        return if (count == 0) 0 else (4 + count).coerceAtMost(8)
    }

    private fun isRecent(receipt: Receipt, now: Long): Boolean =
        now >= receipt.recordedAt && now - receipt.recordedAt <= MAX_AGE

    companion object {
        private const val MAX_RECEIPTS = 32
        private const val MAX_AGE = 7 * 24 * 60 * 60 * 1_000L

        internal fun isValidReceipt(receipt: Receipt): Boolean =
            receipt.helperName.isNotBlank() && receipt.helperName.length <= 64 &&
                receipt.helperName.none { it.isISOControl() } && NpcFoodAid.foodGain(receipt.itemKey) > 0 &&
                receipt.amount == 1 && receipt.beforeHunger in 0 until NpcSelfCareThresholds.START &&
                receipt.afterHunger > receipt.beforeHunger && receipt.afterHunger <= 100 && receipt.recordedAt > 0L
    }
}
