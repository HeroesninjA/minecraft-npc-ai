package ro.ainpc.economy

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ro.ainpc.database.DatabaseManager
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

class ShopTransactionTest {
    private lateinit var connection: Connection
    private lateinit var databaseManager: DatabaseManager
    private lateinit var economy: EconomyService
    private lateinit var service: ShopService
    private lateinit var player: Player
    private var storage = arrayOfNulls<ItemStack>(2)
    private var equipment = arrayOfNulls<ItemStack>(5)
    private var inventoryWrites = 0
    private var failInventoryWrite = false
    private var allowed = true
    private var primaryThread = true
    private var stackLimit = 64
    private val logger = Logger.getAnonymousLogger().apply { level = Level.OFF }
    private val offer = ShopOffer(
        "logs", Material.OAK_LOG, 16, mapOf(Material.EMERALD to 5), mapOf(Material.OAK_LOG to 16), -1, emptyList(),
    )

    @BeforeEach
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        connection.createStatement().use { it.execute("PRAGMA foreign_keys = ON") }
        databaseManager = DatabaseManager(null)
        DatabaseManager::class.java.getDeclaredField("connection").apply {
            isAccessible = true
            set(databaseManager, connection)
        }
        DatabaseManager::class.java.getDeclaredMethod("createTables").apply { isAccessible = true }.invoke(databaseManager)
        val inventory = Proxy.newProxyInstance(
            PlayerInventory::class.java.classLoader, arrayOf(PlayerInventory::class.java),
        ) { _, method, arguments ->
            when (method.name) {
                "getStorageContents" -> storage.copyOf()
                "getContents" -> storage + equipment
                "getMaxStackSize" -> stackLimit
                "setStorageContents" -> {
                    inventoryWrites++
                    @Suppress("UNCHECKED_CAST")
                    val updated = arguments!![0] as Array<ItemStack?>
                    storage[0] = updated[0]
                    if (failInventoryWrite) {
                        failInventoryWrite = false
                        throw IllegalStateException("simulated partial inventory write")
                    }
                    storage = updated.copyOf()
                    null
                }
                else -> throw UnsupportedOperationException(method.name)
            }
        } as PlayerInventory
        val uuid = UUID.randomUUID()
        player = Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, _ ->
            when (method.name) {
                "getUniqueId" -> uuid
                "getInventory" -> inventory
                "hasPermission" -> allowed
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Player
        economy = EconomyService(databaseManager, logger)
        assertTrue(economy.setBalance(player, 100))
        service = ShopService(economy, null, ::createStack, { primaryThread })
    }

    @AfterEach
    fun tearDown() {
        databaseManager.close()
    }

    @Test
    fun coinPurchaseDoesNotRequireOrConsumeItemCurrency() {
        stock(TestStack(Material.EMERALD, 2), null)
        val shop = shop(ShopCurrency.VAULT_OPTIONAL)

        assertEquals(5, service.getCoinPrice(shop, offer))
        assertTrue(service.canAfford(player, shop, offer))
        assertTrue(service.executePurchase(player, shop, offer).success)
        assertEquals(2, amount(Material.EMERALD))
        assertEquals(16, amount(Material.OAK_LOG))
        assertBalance(95)
    }

    @Test
    fun coinSalePaysOnlyCoinsAndDoesNotRequireSpaceForEmeralds() {
        stock(TestStack(Material.OAK_LOG, 16))

        assertTrue(service.executeSell(player, shop(ShopCurrency.VAULT_OPTIONAL), offer).success)
        assertTrue(storage.all { it == null })
        assertBalance(105)
    }

    @Test
    fun optionalCurrencyFallsBackToItemsWhenEconomyIsAbsent() {
        service = ShopService(null, null, ::createStack, { true })
        stock(TestStack(Material.EMERALD, 5))

        assertTrue(service.executePurchase(player, shop(ShopCurrency.VAULT_OPTIONAL), offer).success)
        assertEquals(0, amount(Material.EMERALD))
        assertEquals(16, amount(Material.OAK_LOG))
        assertBalance(100)
    }

    @Test
    fun itemPurchaseReusesTheSlotFreedByPayment() {
        stock(TestStack(Material.EMERALD, 5))

        assertTrue(service.executePurchase(player, shop(), offer).success)
        assertEquals(16, amount(Material.OAK_LOG))
        assertEquals(0, amount(Material.EMERALD))
        assertBalance(100)
    }

    @Test
    fun itemSaleReusesTheSlotFreedBySoldItems() {
        stock(TestStack(Material.OAK_LOG, 16))

        assertTrue(service.executeSell(player, shop(), offer).success)
        assertEquals(5, amount(Material.EMERALD))
        assertEquals(0, amount(Material.OAK_LOG))
        assertBalance(100)
    }

    @Test
    fun equipmentCannotBeSpentOrCountedAsStorageSpace() {
        stock(null)
        equipment[0] = TestStack(Material.EMERALD, 5)
        assertFalse(service.canAfford(player, shop(), offer))
        assertFalse(service.executePurchase(player, shop(), offer).success)
        assertEquals(5, equipment[0]!!.amount)

        stock(TestStack(Material.STONE, 64))
        assertFalse(service.executePurchase(player, shop(ShopCurrency.VAULT_OPTIONAL), offer).success)
        assertEquals(0, inventoryWrites)
        assertBalance(100)
    }

    @Test
    fun distinctMetadataDoesNotProvideFalseStackingSpace() {
        stock(TestStack(Material.OAK_LOG, 1, "named"), TestStack(Material.EMERALD, 10))

        assertFalse(service.executePurchase(player, shop(), offer).success)
        assertEquals(10, amount(Material.EMERALD))
        assertEquals(1, amount(Material.OAK_LOG))
        assertEquals("named", (storage[0] as TestStack).tag)
        assertEquals(0, inventoryWrites)

        stock(TestStack(Material.OAK_LOG, 1), TestStack(Material.EMERALD, 10))
        assertTrue(service.executePurchase(player, shop(), offer).success)
        assertEquals(17, amount(Material.OAK_LOG))
        assertEquals(5, amount(Material.EMERALD))
    }

    @Test
    fun partialStacksAndFreedSlotsArePlannedTogether() {
        stock(TestStack(Material.OAK_LOG, 60), TestStack(Material.EMERALD, 5))

        assertTrue(service.executePurchase(player, shop(), offer).success)
        assertEquals(listOf(64, 12), storage.map { it!!.amount })
        assertEquals(76, amount(Material.OAK_LOG))
    }

    @Test
    fun inventoryAndItemStackLimitsAreBothRespected() {
        stackLimit = 8
        stock(TestStack(Material.EMERALD, 5), null)
        assertTrue(service.executePurchase(player, shop(), offer).success)
        assertEquals(listOf(8, 8), storage.map { it!!.amount })

        val swords = offer.copy(resultItems = mapOf(Material.IRON_SWORD to 2))
        stock(TestStack(Material.EMERALD, 5), null)
        assertTrue(service.executePurchase(player, shop(offer = swords), swords).success)
        assertEquals(listOf(1, 1), storage.map { it!!.amount })
    }

    @Test
    fun hugeOutputIsRejectedWithoutAllocatingAllRequestedStacks() {
        val oversized = offer.copy(resultItems = mapOf(Material.OAK_LOG to Int.MAX_VALUE))
        assertFalse(service.executePurchase(player, shop(ShopCurrency.VAULT_OPTIONAL, oversized), oversized).success)
        assertEquals(0, inventoryWrites)
        assertBalance(100)
    }

    @Test
    fun invalidAmountsMaterialsAndOverflowingPricesDoNotMoveMoneyOrItems() {
        val invalidOffers = listOf(
            offer.copy(costItems = emptyMap()),
            offer.copy(resultItems = emptyMap()),
            offer.copy(costItems = mapOf(Material.EMERALD to -1)),
            offer.copy(resultItems = mapOf(Material.OAK_LOG to 0)),
            offer.copy(costItems = mapOf(Material.AIR to 1)),
            offer.copy(resultItems = mapOf(Material.WATER to 1)),
            offer.copy(costItems = mapOf(Material.EMERALD to Int.MAX_VALUE, Material.DIAMOND to Int.MAX_VALUE)),
        )
        for (invalid in invalidOffers) {
            val shop = shop(ShopCurrency.VAULT_OPTIONAL, invalid)
            assertFalse(service.canAfford(player, shop, invalid))
            assertFalse(service.executePurchase(player, shop, invalid).success)
            assertFalse(service.executeSell(player, shop, invalid).success)
        }
        assertEquals(0, inventoryWrites)
        assertBalance(100)
    }

    @Test
    fun unauthorizedForeignAndUnsupportedOffersAreRejected() {
        allowed = false
        val restrictedShop = shop(ShopCurrency.VAULT_OPTIONAL).copy(permission = "shop.allowed")
        assertFalse(service.canAfford(player, restrictedShop, offer))
        assertFalse(service.executePurchase(player, restrictedShop, offer).success)
        assertFalse(service.executeSell(player, restrictedShop, offer).success)
        allowed = true
        assertFalse(service.executePurchase(player, shop(), offer.copy(offerId = "foreign")).success)
        for (restricted in listOf(offer.copy(maxUses = 1), offer.copy(requirements = listOf("quest:done")))) {
            assertFalse(service.executePurchase(player, shop(offer = restricted), restricted).success)
        }
        assertEquals(0, inventoryWrites)
        assertBalance(100)
    }

    @Test
    fun failedBalanceWriteLeavesInventoryUntouchedForBuyAndSell() {
        connection.createStatement().use { statement ->
            statement.execute("""
                CREATE TRIGGER reject_balance BEFORE INSERT ON economy_balances
                BEGIN SELECT RAISE(ABORT, 'simulated payment failure'); END
            """.trimIndent())
        }
        assertFalse(service.executePurchase(player, shop(ShopCurrency.VAULT_OPTIONAL), offer).success)
        stock(TestStack(Material.OAK_LOG, 16))
        assertFalse(service.executeSell(player, shop(ShopCurrency.VAULT_OPTIONAL), offer).success)
        assertEquals(16, amount(Material.OAK_LOG))
        assertEquals(0, inventoryWrites)
        assertBalance(100)
    }

    @Test
    fun failedCommitRestoresInventoryAndBalance() {
        connection.createStatement().use { statement ->
            statement.execute("CREATE TABLE commit_parent (id INTEGER PRIMARY KEY)")
            statement.execute("CREATE TABLE commit_child (parent_id INTEGER REFERENCES commit_parent(id) DEFERRABLE INITIALLY DEFERRED)")
            statement.execute("""
                CREATE TRIGGER reject_commit AFTER INSERT ON economy_balances
                BEGIN INSERT INTO commit_child VALUES (1); END
            """.trimIndent())
        }

        assertFalse(service.executePurchase(player, shop(ShopCurrency.VAULT_OPTIONAL), offer).success)
        assertTrue(storage.all { it == null })
        assertEquals(2, inventoryWrites)
        assertBalance(100)
    }

    @Test
    fun partiallyFailedInventoryWriteRollsBackMoneyAndRestoresItems() {
        stock(TestStack(Material.EMERALD, 3), null)
        failInventoryWrite = true

        assertFalse(service.executePurchase(player, shop(ShopCurrency.VAULT_OPTIONAL), offer).success)
        assertEquals(3, amount(Material.EMERALD))
        assertEquals(0, amount(Material.OAK_LOG))
        assertEquals(2, inventoryWrites)
        assertBalance(100)
    }

    @Test
    fun itemTradeAlsoRestoresAPartiallyFailedWrite() {
        stock(TestStack(Material.EMERALD, 5))
        failInventoryWrite = true

        assertFalse(service.executePurchase(player, shop(), offer).success)
        assertEquals(5, amount(Material.EMERALD))
        assertEquals(0, amount(Material.OAK_LOG))
        assertBalance(100)
    }

    @Test
    fun fullWalletDoesNotConsumeItemsOfferedForSale() {
        assertTrue(economy.setBalance(player, Int.MAX_VALUE))
        stock(TestStack(Material.OAK_LOG, 16))

        assertFalse(service.executeSell(player, shop(ShopCurrency.VAULT_OPTIONAL), offer).success)
        assertEquals(16, amount(Material.OAK_LOG))
        assertEquals(0, inventoryWrites)
        assertBalance(Int.MAX_VALUE)
    }

    @Test
    fun tradesAreRejectedOffThePrimaryThread() {
        primaryThread = false

        assertFalse(service.canAfford(player, shop(), offer))
        assertFalse(service.executePurchase(player, shop(), offer).success)
        assertFalse(service.executeSell(player, shop(), offer).success)
        assertEquals(0, inventoryWrites)
        assertBalance(100)
    }

    private fun shop(currency: ShopCurrency = ShopCurrency.ITEM, offer: ShopOffer = this.offer) =
        NpcShopDefinition("shop", "merchant", currency, listOf(offer), RestockPolicy.NEVER, null)

    private fun stock(vararg stacks: ItemStack?) { storage = arrayOf(*stacks) }

    private fun amount(material: Material): Int = storage.filterNotNull().filter { it.type == material }.sumOf { it.amount }

    private fun assertBalance(expected: Int) {
        assertEquals(expected, economy.getBalance(player))
        assertEquals(expected, EconomyService(databaseManager, logger).getBalance(player))
    }

    private fun createStack(material: Material, amount: Int): ItemStack {
        require(material != Material.WATER)
        return TestStack(material, amount)
    }

    private class TestStack(private val material: Material, private var count: Int, val tag: String? = null) : ItemStack() {
        override fun getType(): Material = material
        override fun getAmount(): Int = count
        override fun setAmount(amount: Int) { count = amount }
        override fun getMaxStackSize(): Int = if (material == Material.IRON_SWORD) 1 else 64
        override fun isSimilar(stack: ItemStack?): Boolean = stack is TestStack && material == stack.material && tag == stack.tag
        public override fun clone(): ItemStack = TestStack(material, count, tag)
    }
}
