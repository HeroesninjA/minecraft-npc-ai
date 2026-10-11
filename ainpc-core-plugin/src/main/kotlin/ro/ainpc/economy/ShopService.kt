package ro.ainpc.economy

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Shopservice.
 */
class ShopService internal constructor(
    private val economyService: EconomyService?,
    private val npcEconomyService: NpcEconomyService?,
    private val createItem: (Material, Int) -> ItemStack,
    private val isPrimaryThread: () -> Boolean,
) {
    constructor(economyService: EconomyService?, npcEconomyService: NpcEconomyService? = null) : this(
        economyService, npcEconomyService, { material, amount -> ItemStack(material, amount) }, Bukkit::isPrimaryThread,
    )

    private val shops: MutableMap<String, NpcShopDefinition> = LinkedHashMap()
    private val roleIndex: MutableMap<String, MutableList<NpcShopDefinition>> = LinkedHashMap()

    companion object {
        private const val BASE_PRICE_THRESHOLD = 100
        private const val MIN_MULTIPLIER = 0.6
        private const val MAX_MULTIPLIER = 1.8
        private const val WEALTH_FACTOR = 0.003
    }

    fun registerShop(shop: NpcShopDefinition) {
        unregisterShop(shop.shopId)
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
        return roleIndex[role.lowercase()]?.toList() ?: emptyList()
    }

    fun getAllShops(): Collection<NpcShopDefinition> = shops.values.toList()

    fun clear() {
        shops.clear()
        roleIndex.clear()
    }

    fun shopCount(): Int = shops.size

    fun getDynamicPriceMultiplier(shop: NpcShopDefinition): Double {
        if (npcEconomyService == null) return 1.0
        val npcKey = "role_${shop.npcRole.lowercase()}"
        val balance = npcEconomyService.getBalance(npcKey)
        if (balance <= BASE_PRICE_THRESHOLD) return MIN_MULTIPLIER
        val multiplier = 1.0 + (balance - BASE_PRICE_THRESHOLD) * WEALTH_FACTOR
        return multiplier.coerceIn(MIN_MULTIPLIER, MAX_MULTIPLIER)
    }

    fun getAdjustedCost(shop: NpcShopDefinition, originalCost: Int): Int {
        return requireNotNull(adjustedAmount(shop, originalCost.toLong())) { "Cost invalid sau prea mare" }
    }

    fun canAfford(player: Player, shop: NpcShopDefinition, offer: ShopOffer): Boolean {
        if (!isPrimaryThread() || validateOffer(player, shop, offer) != null || itemTemplates(offer) == null) return false
        if (usesCoinCurrency(shop)) {
            val price = getCoinPrice(shop, offer) ?: return false
            return economyService!!.getBalance(player) >= price
        }
        return hasEnoughItems(player.inventory.storageContents, offer.costItems)
    }

    fun executePurchase(player: Player, shop: NpcShopDefinition, offer: ShopOffer): ShopTransactionResult {
        return executeTrade(player, shop, offer, purchase = true)
    }

    fun executeSell(player: Player, shop: NpcShopDefinition, offer: ShopOffer): ShopTransactionResult {
        return executeTrade(player, shop, offer, purchase = false)
    }

    private fun executeTrade(player: Player, shop: NpcShopDefinition, offer: ShopOffer, purchase: Boolean): ShopTransactionResult {
        if (!isPrimaryThread()) return ShopTransactionResult(false, "Tranzactia necesita threadul principal")
        val validationError = validateOffer(player, shop, offer)
        if (validationError != null) return ShopTransactionResult(false, validationError)
        val templates = itemTemplates(offer) ?: return ShopTransactionResult(false, "Oferta contine iteme invalide")
        val coins = usesCoinCurrency(shop)
        val price = if (coins) {
            getCoinPrice(shop, offer)
                ?: return ShopTransactionResult(false, "Pretul depaseste limita suportata")
        } else 0
        val removed = if (purchase) { if (coins) emptyMap() else offer.costItems } else offer.resultItems
        val added = if (purchase) offer.resultItems else { if (coins) emptyMap() else offer.costItems }
        val inventory = player.inventory
        val before = inventory.storageContents.map { it?.clone() }.toTypedArray()
        if (!hasEnoughItems(before, removed)) return ShopTransactionResult(false, "Nu ai itemele necesare")
        val after = planInventory(before, removed, added, templates, inventory.maxStackSize)
            ?: return ShopTransactionResult(false, "Inventarul este plin")
        var inventoryTouched = false
        var completed = false
        val applyInventory = {
            inventoryTouched = true
            inventory.storageContents = after
        }
        try {
            val paid = when {
                !coins -> { applyInventory(); true }
                purchase -> economyService!!.withdrawWithPersistence(player.uniqueId, price, applyInventory)
                else -> economyService!!.depositWithPersistence(player.uniqueId, price, applyInventory)
            }
            if (!paid) return ShopTransactionResult(false, "Plata nu a putut fi efectuata")
            completed = true
            return ShopTransactionResult(true, if (purchase) "Tranzactie reusita" else "Vanzare reusita")
        } catch (exception: Exception) {
            return ShopTransactionResult(false, "Tranzactia nu a putut fi aplicata")
        } finally {
            if (inventoryTouched && !completed) inventory.storageContents = before
        }
    }

    fun usesCoinCurrency(shop: NpcShopDefinition): Boolean =
        shop.currency == ShopCurrency.VAULT_OPTIONAL && economyService != null

    fun getCoinPrice(shop: NpcShopDefinition, offer: ShopOffer): Int? {
        if (!usesCoinCurrency(shop) || offer.costItems.isEmpty() || offer.costItems.values.any { it <= 0 }) return null
        return adjustedAmount(shop, offer.costItems.values.sumOf { it.toLong() })
    }

    private fun adjustedAmount(shop: NpcShopDefinition, base: Long): Int? {
        if (base <= 0) return null
        val adjusted = (base * getDynamicPriceMultiplier(shop)).toLong().coerceAtLeast(1L)
        return if (adjusted <= Int.MAX_VALUE) adjusted.toInt() else null
    }

    private fun validateOffer(player: Player, shop: NpcShopDefinition, offer: ShopOffer): String? {
        if (!shop.permission.isNullOrBlank() && !player.hasPermission(shop.permission)) return "Nu ai permisiunea necesara"
        if (offer !in shop.offers) return "Oferta nu apartine magazinului"
        if (!offer.isUnlimited() || offer.requirements.isNotEmpty()) return "Restrictiile acestei oferte nu sunt inca suportate"
        if (offer.costItems.isEmpty() || offer.resultItems.isEmpty()) return "Oferta este incompleta"
        if (offer.costItems.values.any { it <= 0 } || offer.resultItems.values.any { it <= 0 }) return "Cantitate invalida"
        return null
    }

    private fun itemTemplates(offer: ShopOffer): Map<Material, ItemStack>? {
        return try {
            (offer.costItems.keys + offer.resultItems.keys).associateWith { material ->
                if (material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR) return null
                createItem(material, 1).also { if (it.maxStackSize <= 0) return null }
            }
        } catch (exception: IllegalArgumentException) {
            null
        }
    }

    private fun hasEnoughItems(contents: Array<ItemStack?>, items: Map<Material, Int>): Boolean {
        val counts = mutableMapOf<Material, Long>()
        for (stack in contents) {
            if (stack != null && stack.amount > 0) {
                counts[stack.type] = counts.getOrDefault(stack.type, 0L) + stack.amount
            }
        }
        return items.all { (material, amount) -> counts.getOrDefault(material, 0L) >= amount }
    }

    private fun planInventory(
        before: Array<ItemStack?>,
        removed: Map<Material, Int>,
        added: Map<Material, Int>,
        templates: Map<Material, ItemStack>,
        inventoryStackLimit: Int,
    ): Array<ItemStack?>? {
        val contents = before.map { stack ->
            if (stack == null || stack.type == Material.AIR || stack.amount <= 0) null else stack.clone()
        }.toTypedArray()
        for ((material, amount) in removed) {
            var remaining = amount
            for (slot in contents.indices) {
                val stack = contents[slot] ?: continue
                if (stack.type != material) continue
                val toRemove = minOf(remaining, stack.amount)
                stack.amount -= toRemove
                if (stack.amount == 0) contents[slot] = null
                remaining -= toRemove
                if (remaining == 0) break
            }
            if (remaining > 0) return null
        }
        for ((material, amount) in added) {
            val template = templates.getValue(material)
            val limit = minOf(template.maxStackSize, inventoryStackLimit)
            if (limit <= 0) return null
            var remaining = amount
            for (stack in contents) {
                if (stack == null || !stack.isSimilar(template)) continue
                val moved = minOf(remaining, (minOf(limit, stack.maxStackSize) - stack.amount).coerceAtLeast(0))
                stack.amount += moved
                remaining -= moved
                if (remaining == 0) break
            }
            if (remaining == 0) continue
            for (slot in contents.indices) {
                if (contents[slot] != null) continue
                val moved = minOf(remaining, limit)
                contents[slot] = template.clone().also { it.amount = moved }
                remaining -= moved
                if (remaining == 0) break
            }
            if (remaining > 0) return null
        }
        return contents
    }
}

/**
 * Shoptransactionresult.
 */
data class ShopTransactionResult(
    val success: Boolean,
    val message: String
)
