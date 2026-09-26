package ro.ainpc.api

/**
 * Reputationapi.
 */
interface ReputationApi {
    fun getReputation(playerUuid: String, scopeType: String, scopeId: String): Int
    fun addReputation(playerUuid: String, scopeType: String, scopeId: String, amount: Int)
    fun setReputation(playerUuid: String, scopeType: String, scopeId: String, value: Int)
    fun getTopReputations(scopeType: String, scopeId: String, limit: Int): List<ReputationEntry>
}

/**
 * Reputationentry.
 */
data class ReputationEntry(
    val playerUuid: String,
    val playerName: String,
    val reputation: Int,
)
