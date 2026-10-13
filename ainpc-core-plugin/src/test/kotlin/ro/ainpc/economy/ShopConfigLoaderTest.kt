package ro.ainpc.economy

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.world.PlaceType
import java.io.InputStreamReader

class ShopConfigLoaderTest {
    @Test
    fun bundledShopsMatchGeneratedWorkplaceOccupations() {
        val resource = requireNotNull(javaClass.classLoader.getResourceAsStream("shops.yml"))
        val config = resource.use { YamlConfiguration.loadConfiguration(InputStreamReader(it, Charsets.UTF_8)) }

        val shops = ShopConfigLoader.parse(config)

        assertEquals(setOf("negustor", "fierar", "fermier", "hangiu", "mestesugar"), shops.map { it.npcRole }.toSet())
        assertTrue(shops.all { shop -> shop.offers.all { it.configurationIssue() == null } })
        assertTrue(shops.all { it.placeTypes.isNotEmpty() })
        assertEquals(setOf(PlaceType.MARKET, PlaceType.SHOP), shops.first { it.npcRole == "negustor" }.placeTypes)
        assertEquals(Material.BREAD, shops.first { it.npcRole == "negustor" }.offers.single().displayItem)
    }

    @Test
    fun malformedOfferDoesNotReplaceExistingShops() {
        val service = ShopService(null)
        val existing = NpcShopDefinition("existing", "negustor", ShopCurrency.ITEM, emptyList(), RestockPolicy.NEVER, null)
        service.registerShop(existing)
        val config = YamlConfiguration.loadConfiguration(InputStreamReader("""
            shops:
              broken:
                npc_role: fierar
                offers:
                  invalid:
                    display_item: NOT_A_MATERIAL
                    cost_items:
                      EMERALD: 1
                    result_items:
                      BREAD: 1
        """.trimIndent().byteInputStream()))

        assertThrows(IllegalArgumentException::class.java) { service.replaceShops(ShopConfigLoader.parse(config)) }
        assertEquals(listOf(existing), service.findShopsForRole("negustor"))
    }

    @Test
    fun replacementRemovesOldRoleIndex() {
        val config = YamlConfiguration.loadConfiguration(InputStreamReader("""
            shops:
              bakery:
                npc_role: brutar
                offers:
                  bread:
                    display_item: BREAD
                    cost_items:
                      EMERALD: 1
                    result_items:
                      BREAD: 2
        """.trimIndent().byteInputStream()))
        val service = ShopService(null)
        service.replaceShops(ShopConfigLoader.parse(config))

        assertEquals(1, service.shopCount())
        assertEquals(Material.BREAD, service.findShopsForRole("brutar").single().offers.single().displayItem)
        service.replaceShops(emptyList())
        assertTrue(service.findShopsForRole("brutar").isEmpty())
    }

    @Test
    fun unknownPlaceTypeIsRejected() {
        val config = YamlConfiguration.loadConfiguration(InputStreamReader("""
            shops:
              bakery:
                npc_role: brutar
                place_types: [UNKNOWN_PLACE]
                offers:
                  bread:
                    display_item: BREAD
                    cost_items:
                      EMERALD: 1
                    result_items:
                      BREAD: 2
        """.trimIndent().byteInputStream()))

        assertThrows(IllegalArgumentException::class.java) { ShopConfigLoader.parse(config) }
    }

    @Test
    fun placeIdsAreTrimmedAndBlankIdsAreRejected() {
        val resource = requireNotNull(javaClass.classLoader.getResourceAsStream("shops.yml"))
        val config = resource.use { YamlConfiguration.loadConfiguration(InputStreamReader(it, Charsets.UTF_8)) }
        config.set("shops.piata_satului.place_ids", listOf(" demo_sat:piata "))

        val shop = ShopConfigLoader.parse(config).first { it.shopId == "piata_satului" }
        assertEquals(setOf("demo_sat:piata"), shop.placeIds)

        config.set("shops.piata_satului.place_ids", listOf(" "))
        assertThrows(IllegalArgumentException::class.java) { ShopConfigLoader.parse(config) }
    }

    @Test
    fun invalidPermissionDoesNotSilentlyOpenShop() {
        val resource = requireNotNull(javaClass.classLoader.getResourceAsStream("shops.yml"))
        val config = resource.use { YamlConfiguration.loadConfiguration(InputStreamReader(it, Charsets.UTF_8)) }
        config.set("shops.piata_satului.permission", true)

        assertThrows(IllegalArgumentException::class.java) { ShopConfigLoader.parse(config) }
    }
}
