package ro.ainpc.ai

import ro.ainpc.api.WorldAdminApi
import ro.ainpc.economy.NpcShopDefinition
import ro.ainpc.economy.ShopService
import ro.ainpc.npc.AINPC

internal object NpcShopAssociation {
    fun findShops(shopService: ShopService, npc: AINPC, worldAdmin: WorldAdminApi): List<NpcShopDefinition> {
        val occupation = npc.occupation?.takeIf { it.isNotBlank() } ?: return emptyList()
        val roleShops = shopService.findShopsForRole(occupation)
        if (roleShops.isEmpty() || roleShops.all { it.placeTypes.isEmpty() && it.placeIds.isEmpty() }) return roleShops
        val place = OpenAISemanticWorldContextBuilder.resolveWorkPlace(worldAdmin, npc.workAnchor)
        return shopService.findShopsForRoleAtPlace(occupation, place)
    }
}
