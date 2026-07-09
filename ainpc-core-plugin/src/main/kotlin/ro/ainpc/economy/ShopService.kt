package ro.ainpc.economy

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ShopService(
    private val economyService: EconomyService?,
    private val npcEconomyService: NpcEconomyService? = null
) {
    private val shops: MutableMap<String, NpcShopDefinition> = LinkedHashMap()
    private val roleIndex: MutableMap<String, MutableList<NpcShopDefinition>> = LinkedHashMap()

    companion object {
        private const val BASE_PRICE_THRESHOLD = 100
        private const val MIN_MULTIPLIER = 0.6
        private const val MAX_MULTIPLIER = 1.8
        private const val WEALTH_FACTOR = 0.003
    }

    fun registerShop(shop: NpcShopDefinition) {
        shops[shop.shopId] = shop
        val role = shop.npcRole.lowercase()
        roleIndex.getOrPut(role) { mutableListOf() }.add(shop)
    }

    fun unregisterShop(shopId: String) {
        val shop = shops.remove(shopId) ?: return
        val role = shop.npcRole.lowercase()
        roleIndex[role]?.remove(shop)
        if (roleIndex[role]?.isEmpty() == true) {
            roleIndex.remove(role)
        }
    }

    fun getShop(shopId: String): NpcShopDefinition? = shops[shopId]

    fun findShopsForRole(role: String): List<NpcShopDefinition> {
        return roleIndex[role.lowercase()].orEmpty()
    }

    fun getAllShops(): Collection<NpcShopDefinition> = shops.values

    fun clear() {
        shops.clear()
    }

    fun shopCount(): Int = shops.size

    fun getDynamicPriceMultiplier(shop: NpcShopDefinition): Double {
        if (!npcEconomyService) return 1.0
        val npcKey = "role_${shop.npcRole.lowercase()}"
        val balance = npcEconomyService?.getBalance(npcKey) ?: return 1.0
        if (balance <= BASE_PRICE_THRESHOLD) return MIN_MULTIPLIER
        val multiplier = 1.0 + (balance - BASE_PRICE_THRESHOLD) * WEALTH_FACTOR
        return multiplier.coerceIn(MIN_MULTIPLIER, MAX_MULTIPLIER)
    }

    fun getAdjustedCost(shop: NpcShopDefinition, originalCost: Int): Int {
        val multiplier = getDynamicPriceMultiplier(shop)
        return (originalCost * multiplier).toInt().coerceAtLeast(1)
    }

    fun canAfford(player: Player, shop: NpcShopDefinition, offer: ShopOffer): Boolean {
        for ((material, amount) in offer.costItems) {
            if (!hasEnough(player, material, amount)) return false
        }
        if (shop.currency == ShopCurrency.VAULT_OPTIONAL && economyService != null) {
            val baseCost = offer.costItems.entries.sumOf { it.value }
            val adjustedCost = getAdjustedCost(shop, baseCost)
            if (adjustedCost > 0) {
                return economyService.getBalance(player) >= adjustedCost
            }
        }
        return true
    }

    fun executePurchase(player: Player, shop: NpcShopDefinition, offer: ShopOffer): ShopTransactionResult {
        if (!canAfford(player, shop, offer)) {
            return ShopTransactionResult(false, "Fonduri insuficiente")
        }
        val hasSpace = hasInventorySpace(player, offer.resultItems)
        if (!hasSpace) {
            return ShopTransactionResult(false, "Inventarul este plin")
        }
        removeItems(player, offer.costItems)
        giveItems(player, offer.resultItems)
        if (shop.currency == ShopCurrency.VAULT_OPTIONAL) {
            val baseCost = offer.costItems.entries.sumOf { it.value }
            val adjustedCost = getAdjustedCost(shop, baseCost)
            if (adjustedCost > 0 && economyService != null) {
                economyService.withdraw(player, adjustedCost)
            }
        }
        return ShopTransactionResult(true, "Tranzactie reusita")
    }

    fun executeSell(player: Player, shop: NpcShopDefinition, offer: ShopOffer): ShopTransactionResult {
        val multiplier = getDynamicPriceMultiplier(shop)
        if (!hasEnoughItems(player, offer.resultItems)) {
            return ShopTransactionResult(false, "Nu ai itemele necesare pentru vanzare")
        }
        val hasSpace = hasInventorySpace(player, offer.costItems)
        if (!hasSpace) {
            return ShopTransactionResult(false, "Inventarul este plin pentru plata")
        }
        removeItems(player, offer.resultItems)
        giveItems(player, offer.costItems)
        if (shop.currency == ShopCurrency.VAULT_OPTIONAL) {
            val baseReward = offer.costItems.entries.sumOf { it.value }
            val adjustedReward = (baseReward * multiplier).toInt().coerceAtLeast(1)
            if (adjustedReward > 0 && economyService != null) {
                economyService.deposit(player, adjustedReward)
            }
        }
        return ShopTransactionResult(true, "Vanzare reusita")
    }

    private fun hasEnough(player: Player, material: Material, amount: Int): Boolean {
        var total = 0
        for (item in player.inventory.contents) {
            if (item != null && item.type == material) {
                total += item.amount
            }
        }
        return total >= amount
    }

    private fun hasEnoughItems(player: Player, items: Map<Material, Int>): Boolean {
        for ((material, amount) in items) {
            if (!hasEnough(player, material, amount)) return false
        }
        return true
    }

    private fun hasInventorySpace(player: Player, items: Map<Material, Int>): Boolean {
        var neededNewSlots = 0
        for ((material, amount) in items) {
            val maxStack = material.maxStackSize
            var remaining = amount
            for (item in player.inventory.contents) {
                if (item == null || item.type == Material.AIR) continue
                if (item.type == material && item.amount < maxStack) {
                    remaining -= (maxStack - item.amount)
                    if (remaining <= 0) break
                }
            }
            if (remaining > 0) {
                neededNewSlots += (remaining + maxStack - 1) / maxStack
            }
        }
        val emptySlots = player.inventory.contents.count { it == null || it.type == Material.AIR }
        return emptySlots >= neededNewSlots
    }

    private fun removeItems(player: Player, items: Map<Material, Int>) {
        for ((material, amount) in items) {
            var remaining = amount
            for (item in player.inventory.contents) {
                if (item == null || item.type != material) continue
                val toRemove = minOf(remaining, item.amount)
                item.amount -= toRemove
                remaining -= toRemove
                if (remaining <= 0) break
            }
        }
    }

    private fun giveItems(player: Player, items: Map<Material, Int>) {
        for ((material, amount) in items) {
            val maxStack = material.maxStackSize
            var remaining = amount
            while (remaining > 0) {
                val stackSize = minOf(remaining, maxStack)
                val leftover = player.inventory.addItem(ItemStack(material, stackSize))
                if (leftover.isNotEmpty()) {
                    val dropped = leftover.values.first().amount
                    player.world.dropItem(player.location, ItemStack(material, dropped))
                    remaining -= (stackSize - dropped)
                } else {
                    remaining -= stackSize
                }
            }
        }
    }

    private operator fun NpcEconomyService?.not(): Boolean = this == null
}

data class ShopTransactionResult(
    val success: Boolean,
    val message: String
)
