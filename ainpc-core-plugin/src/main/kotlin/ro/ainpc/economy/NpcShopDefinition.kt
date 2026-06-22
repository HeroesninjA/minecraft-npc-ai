package ro.ainpc.economy

enum class ShopCurrency {
    ITEM,
    VAULT_OPTIONAL
}

enum class RestockPolicy {
    NEVER,
    DAILY,
    WEEKLY,
    ON_EMPTY
}

data class NpcShopDefinition(
    val shopId: String,
    val npcRole: String,
    val currency: ShopCurrency,
    val offers: List<ShopOffer>,
    val restockPolicy: RestockPolicy,
    val permission: String?
) {
    fun offersForRole(role: String): List<ShopOffer> {
        if (npcRole.equals(role, ignoreCase = true)) return offers
        return emptyList()
    }

    fun getOffer(offerId: String): ShopOffer? {
        return offers.find { it.offerId.equals(offerId, ignoreCase = true) }
    }
}
