package ro.ainpc.economy

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
class ShopService(private val economyService: EconomyService?) {
    private val shops: MutableMap<String, NpcShopDefinition> = LinkedHashMap()

    fun registerShop(shop: NpcShopDefinition) {
        shops[shop.shopId] = shop
    }

    fun unregisterShop(shopId: String) {
        shops.remove(shopId)
    }

    fun getShop(shopId: String): NpcShopDefinition? = shops[shopId]

    fun findShopsForRole(role: String): List<NpcShopDefinition> {
        return shops.values.filter { it.npcRole.equals(role, ignoreCase = true) }
    }

    fun getAllShops(): Collection<NpcShopDefinition> = shops.values

    fun clear() {
        shops.clear()
    }

    fun shopCount(): Int = shops.size

    fun canAfford(player: Player, shop: NpcShopDefinition, offer: ShopOffer): Boolean {
        for ((material, amount) in offer.costItems) {
            if (!hasEnough(player, material, amount)) return false
        }
        if (shop.currency == ShopCurrency.VAULT_OPTIONAL && economyService != null) {
            val coinCost = offer.costItems.entries.sumOf { it.value }
            if (coinCost > 0) {
                return economyService.getBalance(player) >= coinCost
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
            val coinCost = offer.costItems.entries.sumOf { it.value }
            if (coinCost > 0 && economyService != null) {
                economyService.withdraw(player, coinCost)
            }
        }
        return ShopTransactionResult(true, "Tranzactie reusita")
    }

    fun executeSell(player: Player, shop: NpcShopDefinition, offer: ShopOffer): ShopTransactionResult {
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
            val coinReward = offer.costItems.entries.sumOf { it.value }
            if (coinReward > 0 && economyService != null) {
                economyService.deposit(player, coinReward)
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
        var neededSlots = 0
        for ((material, amount) in items) {
            val maxStack = material.maxStackSize
            var remaining = amount
            for (item in player.inventory.contents) {
                if (item == null || item.type == Material.AIR) {
                    neededSlots++
                    break
                }
                if (item.type == material && item.amount < maxStack) {
                    remaining -= (maxStack - item.amount)
                    if (remaining <= 0) break
                }
            }
            if (remaining > 0) neededSlots++
        }
        val emptySlots = player.inventory.contents.count { it == null || it.type == Material.AIR }
        return emptySlots >= neededSlots
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
                player.inventory.addItem(ItemStack(material, stackSize))
                remaining -= stackSize
            }
        }
    }
}

data class ShopTransactionResult(
    val success: Boolean,
    val message: String
)
