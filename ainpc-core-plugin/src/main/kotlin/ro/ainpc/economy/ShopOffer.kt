package ro.ainpc.economy

import org.bukkit.Material

data class ShopOffer(
    val offerId: String,
    val displayItem: Material,
    val displayAmount: Int,
    val costItems: Map<Material, Int>,
    val resultItems: Map<Material, Int>,
    val maxUses: Int,
    val requirements: List<String>
) {
    fun isUnlimited(): Boolean = maxUses <= 0
}
