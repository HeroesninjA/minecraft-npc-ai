package ro.ainpc.ai

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.economy.ShopConfigLoader
import ro.ainpc.economy.ShopService
import ro.ainpc.npc.AINPC
import ro.ainpc.world.WorldAdminService
import java.io.InputStreamReader
import java.util.logging.Logger

class NpcShopAssociationTest {
    @Test
    fun generatedShopRequiresMatchingMappedWorkplace() {
        val resource = requireNotNull(javaClass.classLoader.getResourceAsStream("shops.yml"))
        val config = resource.use { YamlConfiguration.loadConfiguration(InputStreamReader(it, Charsets.UTF_8)) }
        val shopService = ShopService(null)
        shopService.replaceShops(ShopConfigLoader.parse(config))
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("NpcShopAssociationTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        val forge = requireNotNull(worldAdmin.getPlace("demo_sat:fierarie"))
        val forgeShop = requireNotNull(shopService.getShop("fierarie"))
        shopService.registerShop(forgeShop.copy(shopId = "specific_forge", placeTypes = emptySet(), placeIds = setOf(forge.id())))
        shopService.registerShop(forgeShop.copy(shopId = "other_forge", placeTypes = emptySet(), placeIds = setOf("other:forge")))
        val npc = AINPC(null).apply { occupation = "fierar" }

        assertTrue(NpcShopAssociation.findShops(shopService, npc, worldAdmin).isEmpty())
        npc.workAnchor = AINPC.OwnedLocation(
            "work", forge.displayName(), forge.worldName(),
            (forge.minX() + forge.maxX()) / 2.0,
            (forge.minY() + forge.maxY()) / 2.0,
            (forge.minZ() + forge.maxZ()) / 2.0,
        )
        assertEquals(
            listOf("fierarie", "specific_forge"),
            NpcShopAssociation.findShops(shopService, npc, worldAdmin).map { it.shopId },
        )

        val anchor = requireNotNull(npc.workAnchor)
        npc.workAnchor = AINPC.OwnedLocation("work", forge.displayName(), "other_world", anchor.x(), anchor.y(), anchor.z())
        assertTrue(NpcShopAssociation.findShops(shopService, npc, worldAdmin).isEmpty())
    }
}
