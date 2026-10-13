package ro.ainpc.economy

import org.bukkit.Material

/**
 * Shopoffer.
 */
data class ShopOffer(
    val offerId: String,
    val displayItem: Material,
    val displayAmount: Int,
    val costItems: Map<Material, Int>,
    val resultItems: Map<Material, Int>,
    val maxUses: Int,
    val requirements: List<String>
) {
    enum class ConfigurationIssue {
        UNSUPPORTED_RESTRICTIONS,
        INCOMPLETE_EXCHANGE,
        INVALID_AMOUNT,
    }

    fun isUnlimited(): Boolean = maxUses <= 0

    fun configurationIssue(): ConfigurationIssue? = when {
        !isUnlimited() || requirements.isNotEmpty() -> ConfigurationIssue.UNSUPPORTED_RESTRICTIONS
        costItems.isEmpty() || resultItems.isEmpty() -> ConfigurationIssue.INCOMPLETE_EXCHANGE
        displayAmount <= 0 || costItems.values.any { it <= 0 } || resultItems.values.any { it <= 0 } ->
            ConfigurationIssue.INVALID_AMOUNT
        else -> null
    }
}
