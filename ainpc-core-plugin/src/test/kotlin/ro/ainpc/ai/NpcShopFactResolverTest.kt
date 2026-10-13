package ro.ainpc.ai

import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.economy.NpcShopDefinition
import ro.ainpc.economy.RestockPolicy
import ro.ainpc.economy.ShopCurrency
import ro.ainpc.economy.ShopOffer

class NpcShopFactResolverTest {
    private val shop = NpcShopDefinition(
        "brutarie", "brutar", ShopCurrency.ITEM,
        listOf(ShopOffer(
            "paine", Material.BREAD, 1,
            mapOf(Material.EMERALD to 2), mapOf(Material.BREAD to 3), 0, emptyList(),
        )),
        RestockPolicy.NEVER, null,
    )

    @Test
    fun `catalog question names only goods from configured offers`() {
        assertTrue(NpcShopFactResolver.hasListedGoods(listOf(shop)))
        assertEquals(
            "In catalog am 3 x bread. Preturile si conditiile le gasesti in magazin.",
            NpcShopFactResolver.catalogSummary(listOf(shop)),
        )
        assertEquals(
            "In catalog am 3 x bread. Preturile si conditiile le gasesti in magazin.",
            NpcShopFactResolver.resolve("Ce vinzi?", listOf(shop), null),
        )
        assertNull(NpcShopFactResolver.resolve("Ce mai faci?", listOf(shop), null))
    }

    @Test
    fun `workplace answer requires a mapped place`() {
        assertEquals(
            "Lucrez la Brutaria din sat.",
            NpcShopFactResolver.resolve("Unde este magazinul?", listOf(shop), "Brutaria din sat"),
        )
        assertNull(NpcShopFactResolver.resolve("Unde este magazinul?", listOf(shop), null))
    }

    @Test
    fun `restricted catalog gives an explicit access answer without listing goods`() {
        val reply = NpcShopFactResolver.resolve("Ce vinzi?", emptyList(), null, accessDenied = true)

        assertEquals("Magazinul acesta cere acces special; nu iti pot prezenta ofertele acum.", reply)
        assertNull(NpcShopFactResolver.resolve("Ce mai faci?", emptyList(), null, accessDenied = true))
        assertEquals(
            "In catalog am 3 x bread. Preturile si conditiile le gasesti in magazin.",
            NpcShopFactResolver.resolve("Ce vinzi?", listOf(shop), null, accessDenied = true),
        )
    }

    @Test
    fun `direct trade intent uses only accessible goods or a safe fallback`() {
        val catalog = "In catalog am 3 x bread. Preturile si conditiile le gasesti in magazin."

        assertEquals(catalog, NpcShopFactResolver.tradeSummary(listOf(shop)))
        assertEquals("Nu am oferte pregatite acum.", NpcShopFactResolver.tradeSummary(emptyList()))
        assertEquals(
            "Magazinul acesta cere acces special; nu iti pot prezenta ofertele acum.",
            NpcShopFactResolver.tradeSummary(emptyList(), accessDenied = true),
        )
        assertEquals("Nu am oferte pregatite acum.", NpcShopFactResolver.resolve("Ce vinzi?", emptyList(), null))
    }

    @Test
    fun `empty or incomplete catalogs do not count as listed goods`() {
        assertFalse(NpcShopFactResolver.hasListedGoods(emptyList()))
        assertNull(NpcShopFactResolver.catalogSummary(emptyList()))
        assertFalse(NpcShopFactResolver.hasListedGoods(listOf(shop.copy(offers = emptyList()))))
        assertNull(NpcShopFactResolver.catalogSummary(listOf(shop.copy(offers = emptyList()))))
        val incompleteOffer = shop.offers.single().copy(resultItems = emptyMap())
        assertFalse(NpcShopFactResolver.hasListedGoods(listOf(shop.copy(offers = listOf(incompleteOffer)))))
        val restrictedOffer = shop.offers.single().copy(maxUses = 2)
        assertFalse(NpcShopFactResolver.hasListedGoods(listOf(shop.copy(offers = listOf(restrictedOffer)))))
        assertNull(NpcShopFactResolver.catalogSummary(listOf(shop.copy(offers = listOf(restrictedOffer)))))
        val invalidOffer = shop.offers.single().copy(resultItems = mapOf(Material.DIAMOND to 0))
        assertEquals(
            "In catalog am 3 x bread. Preturile si conditiile le gasesti in magazin.",
            NpcShopFactResolver.resolve("Ce vinzi?", listOf(shop.copy(offers = listOf(invalidOffer) + shop.offers)), null),
        )
        assertEquals(
            "Nu am oferte pregatite acum.",
            NpcShopFactResolver.resolve("Ce ai de vanzare?", listOf(shop.copy(offers = emptyList())), null),
        )
    }
}
