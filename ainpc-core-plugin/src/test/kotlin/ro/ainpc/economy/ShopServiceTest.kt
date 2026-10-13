package ro.ainpc.economy

import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldPlaceInfo

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
    fun offerConfigurationSeparatesSupportedAndUnsupportedTrades() {
        assertNull(testOffer.configurationIssue())
        assertEquals(
            ShopOffer.ConfigurationIssue.UNSUPPORTED_RESTRICTIONS,
            testOffer.copy(maxUses = 3).configurationIssue(),
        )
        assertEquals(
            ShopOffer.ConfigurationIssue.UNSUPPORTED_RESTRICTIONS,
            testOffer.copy(requirements = listOf("reputation:10")).configurationIssue(),
        )
        assertEquals(
            ShopOffer.ConfigurationIssue.INCOMPLETE_EXCHANGE,
            testOffer.copy(costItems = emptyMap()).configurationIssue(),
        )
        assertEquals(
            ShopOffer.ConfigurationIssue.INVALID_AMOUNT,
            testOffer.copy(resultItems = mapOf(Material.OAK_LOG to 0)).configurationIssue(),
        )
        assertEquals(
            ShopOffer.ConfigurationIssue.INVALID_AMOUNT,
            testOffer.copy(displayAmount = 0).configurationIssue(),
        )
    }

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
    fun shopPermissionIsCheckedConsistently() {
        val service = ShopService(null)
        val restricted = testShop.copy(permission = "ainpc.shop.guild")

        assertTrue(service.canAccess(testShop) { false })
        assertTrue(!service.canAccess(restricted) { false })
        assertTrue(service.canAccess(restricted) { it == "ainpc.shop.guild" })
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
    fun replacingShopUpdatesRoleIndex() {
        val svc = ShopService(null)
        svc.registerShop(testShop)
        svc.registerShop(testShop.copy(npcRole = "fermier"))

        assertTrue(svc.findShopsForRole("negustor").isEmpty())
        assertEquals(1, svc.findShopsForRole("fermier").size)
        assertEquals(1, svc.shopCount())
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
    fun placeRestrictedShopsRequireMatchingMappedWorkplace() {
        val service = ShopService(null)
        val unrestricted = testShop.copy(shopId = "unrestricted")
        val market = testShop.copy(shopId = "market", placeTypes = setOf(PlaceType.MARKET))
        val specificMarket = testShop.copy(
            shopId = "specific_market", placeTypes = setOf(PlaceType.MARKET), placeIds = setOf("REGION:MARKET"),
        )
        val otherMarket = testShop.copy(shopId = "other_market", placeIds = setOf("region:other_market"))
        val forge = testShop.copy(shopId = "forge", placeTypes = setOf(PlaceType.FORGE))
        service.replaceShops(listOf(unrestricted, market, specificMarket, otherMarket, forge))
        val marketPlace = WorldPlaceInfo(
            "region:market", "region", "Piata", "world", PlaceType.MARKET,
            0, 0, 0, 10, 10, 10, emptyList(), null, true, emptyMap(),
        )
        val neighboringMarket = WorldPlaceInfo(
            "region:other_market", "region", "Alta piata", "world", PlaceType.MARKET,
            20, 0, 0, 30, 10, 10, emptyList(), null, true, emptyMap(),
        )

        assertEquals(listOf(unrestricted), service.findShopsForRoleAtPlace("negustor", null))
        assertEquals(listOf(unrestricted, market, specificMarket), service.findShopsForRoleAtPlace("NEGUSTOR", marketPlace))
        assertEquals(listOf(unrestricted, market, otherMarket), service.findShopsForRoleAtPlace("negustor", neighboringMarket))
        assertTrue(service.findShopsForRoleAtPlace("fermier", marketPlace).isEmpty())
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
        assertTrue(svc.findShopsForRole("negustor").isEmpty())
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
    fun shopListingsAreSnapshots() {
        val svc = ShopService(null)
        svc.registerShop(testShop)
        val roleShops = svc.findShopsForRole("negustor")
        val allShops = svc.getAllShops()
        svc.registerShop(testShop.copy(shopId = "second"))

        assertEquals(1, roleShops.size)
        assertEquals(1, allShops.size)
        assertEquals(2, svc.shopCount())
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
