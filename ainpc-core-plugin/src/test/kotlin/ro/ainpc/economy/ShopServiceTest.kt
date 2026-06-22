package ro.ainpc.economy

import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShopServiceTest {

    private val testOffer = ShopOffer(
        offerId = "test_buy_wood",
        displayItem = Material.OAK_LOG,
        displayAmount = 1,
        costItems = mapOf(Material.EMERALD to 1),
        resultItems = mapOf(Material.OAK_LOG to 16),
        maxUses = -1,
        requirements = emptyList()
    )

    private val testShop = NpcShopDefinition(
        shopId = "test_shop",
        npcRole = "negustor",
        currency = ShopCurrency.ITEM,
        offers = listOf(testOffer),
        restockPolicy = RestockPolicy.NEVER,
        permission = null
    )

    @Test
    fun registerAndLookupShop() {
        val svc = ShopService(null)
        assertEquals(0, svc.shopCount())
        svc.registerShop(testShop)
        assertEquals(1, svc.shopCount())
        val retrieved = svc.getShop("test_shop")
        assertNotNull(retrieved)
        assertEquals("negustor", retrieved!!.npcRole)
    }

    @Test
    fun unregisterShop() {
        val svc = ShopService(null)
        svc.registerShop(testShop)
        assertEquals(1, svc.shopCount())
        svc.unregisterShop("test_shop")
        assertEquals(0, svc.shopCount())
        assertNull(svc.getShop("test_shop"))
    }

    @Test
    fun findShopsForRole() {
        val svc = ShopService(null)
        svc.registerShop(testShop)
        svc.registerShop(testShop.copy(shopId = "test_shop_2", npcRole = "fermier"))

        val negustorShops = svc.findShopsForRole("negustor")
        assertEquals(1, negustorShops.size)
        assertEquals("test_shop", negustorShops[0].shopId)

        val fermierShops = svc.findShopsForRole("fermier")
        assertEquals(1, fermierShops.size)
        assertEquals("test_shop_2", fermierShops[0].shopId)
    }

    @Test
    fun getAllShops() {
        val svc = ShopService(null)
        svc.registerShop(testShop)
        svc.registerShop(testShop.copy(shopId = "test_shop_2"))
        assertEquals(2, svc.getAllShops().size)
    }

    @Test
    fun clearShops() {
        val svc = ShopService(null)
        svc.registerShop(testShop)
        svc.clear()
        assertEquals(0, svc.shopCount())
        assertTrue(svc.getAllShops().isEmpty())
    }

    @Test
    fun offersForRole() {
        val result = testShop.offersForRole("negustor")
        assertEquals(1, result.size)
        assertEquals("test_buy_wood", result[0].offerId)

        val emptyResult = testShop.offersForRole("fermier")
        assertTrue(emptyResult.isEmpty())
    }

    @Test
    fun getOfferById() {
        val offer = testShop.getOffer("test_buy_wood")
        assertNotNull(offer)
        assertEquals(Material.OAK_LOG, offer!!.displayItem)

        assertNull(testShop.getOffer("nonexistent"))
    }

    @Test
    fun unlimitedOffer() {
        assertTrue(testOffer.isUnlimited())
    }

    @Test
    fun limitedOffer() {
        val limited = testOffer.copy(maxUses = 5)
        assertTrue(!limited.isUnlimited())
    }

    @Test
    fun multipleShopsDifferentRoles() {
        val svc = ShopService(null)
        val negustorShop = NpcShopDefinition(
            shopId = "negustor_shop",
            npcRole = "negustor",
            currency = ShopCurrency.ITEM,
            offers = listOf(
                ShopOffer("sell_sword", Material.IRON_SWORD, 1,
                    mapOf(Material.EMERALD to 5), mapOf(Material.IRON_SWORD to 1), -1, emptyList())
            ),
            restockPolicy = RestockPolicy.NEVER,
            permission = null
        )
        val fermierShop = NpcShopDefinition(
            shopId = "fermier_shop",
            npcRole = "fermier",
            currency = ShopCurrency.ITEM,
            offers = listOf(
                ShopOffer("sell_wheat", Material.WHEAT, 1,
                    mapOf(Material.EMERALD to 1), mapOf(Material.WHEAT to 3), 10, emptyList())
            ),
            restockPolicy = RestockPolicy.DAILY,
            permission = null
        )
        svc.registerShop(negustorShop)
        svc.registerShop(fermierShop)

        assertEquals(2, svc.shopCount())
        assertEquals(1, svc.findShopsForRole("negustor").size)
        assertEquals(1, svc.findShopsForRole("fermier").size)
        assertEquals(0, svc.findShopsForRole("nonexistent").size)
    }

    @Test
    fun economyIntegrationDetection() {
        val svc = ShopService(null)
        val vaultShop = NpcShopDefinition(
            shopId = "vault_shop",
            npcRole = "negustor",
            currency = ShopCurrency.VAULT_OPTIONAL,
            offers = listOf(testOffer),
            restockPolicy = RestockPolicy.NEVER,
            permission = null
        )
        svc.registerShop(vaultShop)
        assertEquals(ShopCurrency.VAULT_OPTIONAL, svc.getShop("vault_shop")!!.currency)
    }
}
