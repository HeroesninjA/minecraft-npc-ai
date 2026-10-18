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
    fun `meaning questions mentioning catalog phrases do not receive catalog answers`() {
        for (message in listOf(
            "Ce înseamnă «ce vinzi»?",
            "Bună, ce înseamnă ce ai de vânzare?",
            "Salut! Ce înseamnă ce vinzi?",
            "Bună ziua, ce înseamnă ce vinzi?",
            "Ce vrea sa spuna unde este magazinul?",
        )) {
            assertFalse(NpcShopFactResolver.isLookupQuestion(message), message)
            assertFalse(NpcShopFactResolver.requiresWorkPlace(message), message)
            assertNull(NpcShopFactResolver.resolve(message, listOf(shop), "Brutaria din sat"), message)
        }
        assertTrue(NpcShopFactResolver.isLookupQuestion("Ce vinzi?"))
        assertTrue(NpcShopFactResolver.isLookupQuestion("Unde este magazinul?"))
    }

    @Test
    fun `direct shop question after meaning question still receives factual answer`() {
        val message = "Ce inseamna ce vinzi? Dar ce vinzi tu?"

        assertTrue(NpcShopFactResolver.isLookupQuestion(message))
        assertEquals(NpcShopFactResolver.tradeSummary(listOf(shop)), NpcShopFactResolver.resolve(message, listOf(shop), null))
        val workplaceMessage = "Buna, ce inseamna magazin? Unde este magazinul tau?"
        assertTrue(NpcShopFactResolver.requiresWorkPlace(workplaceMessage))
        assertEquals("Lucrez la Brutaria din sat.", NpcShopFactResolver.resolve(workplaceMessage, listOf(shop), "Brutaria din sat"))
    }

    @Test
    fun `quoted catalog phrases are not direct shop requests`() {
        for (message in listOf(
            "Maria a zis \"ce vinzi?\"",
            "Maria a zis «ce ai de vânzare?»",
            "Mi-a repetat „unde este magazinul?”",
        )) {
            assertFalse(NpcShopFactResolver.isLookupQuestion(message), message)
            assertFalse(NpcShopFactResolver.requiresWorkPlace(message), message)
            assertNull(NpcShopFactResolver.resolve(message, listOf(shop), "Brutaria din sat"), message)
        }
        val directQuestion = "Maria a zis \"ce vinzi?\", dar ce vinzi tu?"
        assertTrue(NpcShopFactResolver.isLookupQuestion(directQuestion))
        assertEquals(NpcShopFactResolver.tradeSummary(listOf(shop)), NpcShopFactResolver.resolve(directQuestion, listOf(shop), null))
    }

    @Test
    fun `reported catalog questions do not receive catalog answers`() {
        for (message in listOf(
            "Maria a intrebat ce vinzi?",
            "A zis ce vinzi?",
            "Am auzit ca ce ai de vanzare e secret.",
            "Mi-a spus ca unde este magazinul ramane neclar.",
        )) {
            assertFalse(NpcShopFactResolver.isLookupQuestion(message), message)
            assertNull(NpcShopFactResolver.resolve(message, listOf(shop), "Brutaria din sat"), message)
        }
        val directQuestion = "Maria a intrebat ce vinzi; dar ce vinzi tu?"
        assertTrue(NpcShopFactResolver.isLookupQuestion(directQuestion))
        assertEquals(NpcShopFactResolver.tradeSummary(listOf(shop)), NpcShopFactResolver.resolve(directQuestion, listOf(shop), null))
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
