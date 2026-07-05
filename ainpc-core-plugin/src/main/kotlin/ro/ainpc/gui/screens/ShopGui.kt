package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.economy.NpcShopDefinition
import ro.ainpc.economy.ShopOffer
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.npc.AINPC
import java.util.Locale

class ShopGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.SHOP

    override fun title(player: Player): String = "&0AINPC Shop"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        val selectedNpcName = context.service().getShopSelectedNpcId(player)
        if (selectedNpcName != null) {
            renderOffers(context, player, selectedNpcName)
        } else {
            renderNpcList(context, player)
        }
    }

    private fun renderNpcList(context: GuiRenderContext, player: Player) {
        val location = player.location
        val adminView = player.hasPermission("ainpc.admin")
        val shopService = context.plugin().shopService
        val nearbyNpcs = context.plugin().npcManager.getNPCsNear(location, 16.0).stream()
            .sorted(Comparator.comparing { npc: AINPC -> npc.name.lowercase(Locale.ROOT) })
            .toList()
        val totalNpcs = context.plugin().npcManager.getNPCCount()
        val shopCount = shopService.shopCount()

        val headerLore = mutableListOf(
            "&7Total NPC-uri: &f$totalNpcs",
            "&7In raza 16m: &f${nearbyNpcs.size}"
        )
        if (shopCount > 0) headerLore.add("&7Magazine disponibile: &f$shopCount")
        context.item(4, GuiItemFactory.item(Material.EMERALD, "&2Shop & NPCs", headerLore))

        val balance = context.plugin().economyService.getBalance(player)
        context.button(5, GuiButton.enabled(
            GuiItemFactory.item(Material.GOLD_INGOT, "&6Balanța: &e$balance &7monede", "&7Click: /ainpc economy balance"),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc economy balance") }
        ))

        var slot = 19
        for (npc in nearbyNpcs.take(21)) {
            val distance = if (npc.location != null && npc.location!!.world == location.world) {
                String.format(Locale.ROOT, "%.1f", npc.location!!.distance(location))
            } else {
                "?"
            }
            val occupation = npc.occupation
            val npcShops = if (occupation != null) shopService.findShopsForRole(occupation) else emptyList()
            val hasShop = npcShops.isNotEmpty()
            val lore = mutableListOf(
                "&7Ocupatie: &f${occupation ?: "necunoscuta"}",
                "&7Stare: &f${npc.currentState.displayName}",
                "&7Distanta: &f$distance m"
            )
            val icon: Material
            if (hasShop) {
                lore.add("&a◆ Click pentru oferte (${npcShops.size} magazin)")
                icon = Material.EMERALD
            } else {
                lore.add("&8Click: info | Right: vorbeste")
                icon = Material.PLAYER_HEAD
            }
            val npcName = npc.name
            context.button(slot++, GuiButton.enabled(
                GuiItemFactory.item(icon, "&f$npcName", lore),
                GuiAction { click ->
                    if (hasShop) {
                        click.service().setShopSelectedNpcId(click.player(), npcName)
                        click.service().open(click.player(), GuiKey.SHOP)
                    } else {
                        when {
                            click.clickType().isRightClick ->
                                click.service().runCommand(click.player(), "ainpc quest status $npcName")
                            else ->
                                click.service().runCommand(click.player(), "ainpc info $npcName")
                        }
                    }
                }
            ))
        }

        if (nearbyNpcs.isEmpty()) {
            context.item(22, GuiItemFactory.item(
                Material.BARRIER, "&cNiciun NPC In Apropiere",
                listOf("&7Nu exista NPC-uri in raza de 16m.", "&7Muta-te mai aproape de sat.")
            ))
        }

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun renderOffers(context: GuiRenderContext, player: Player, selectedNpcName: String) {
        val shopService = context.plugin().shopService
        val npc = context.plugin().npcManager.getAllNPCs().find { it.name == selectedNpcName }

        if (npc == null) {
            context.service().setShopSelectedNpcId(player, null)
            renderNpcList(context, player)
            return
        }

        val occupation = npc.occupation
        val npcShops = if (occupation != null) shopService.findShopsForRole(occupation) else emptyList()
        if (npcShops.isEmpty()) {
            context.service().setShopSelectedNpcId(player, null)
            renderNpcList(context, player)
            return
        }

        context.item(4, GuiItemFactory.item(
            Material.EMERALD,
            "&2Oferte: &f${npc.name}",
            listOf(
                "&7Rol: &f${occupation ?: "necunoscut"}",
                "&7Magazine: &f${npcShops.size}"
            )
        ))

        val balance = context.plugin().economyService.getBalance(player)
        context.item(5, GuiItemFactory.item(
            Material.GOLD_INGOT, "&6Balanța: &e$balance &7monede"
        ))

        var slot = 19
        var offerIndex = 0
        for (shop in npcShops) {
            for (offer in shop.offers) {
                if (slot >= 44) break
                val costDesc = offer.costItems.entries.joinToString(", ") { "${it.value} x ${it.key.name.lowercase()}" }
                val resultDesc = offer.resultItems.entries.joinToString(", ") { "${it.value} x ${it.key.name.lowercase()}" }
                val usesDesc = if (offer.isUnlimited()) "&7Nelimitat" else "&7Max ${offer.maxUses} folosiri"
                context.button(slot++, GuiButton.enabled(
                    GuiItemFactory.item(
                        offer.displayItem,
                        "&f${offer.offerId}",
                        listOf(
                            "&7Platesti: &e$costDesc",
                            "&7Primesti: &a$resultDesc",
                            usesDesc,
                            "&8Click pentru a cumpara"
                        )
                    ),
                    GuiAction { click ->
                        click.service().runCommand(click.player(), "ainpc economy balance")
                    }
                ))
                offerIndex++
            }
        }

        if (offerIndex == 0) {
            context.item(22, GuiItemFactory.item(
                Material.BARRIER, "&cNicio Ofertă Disponibilă",
                listOf("&7Acest NPC Nu Are Oferte In Acest Moment.")
            ))
        }

        context.button(45, GuiButton.enabled(
            GuiItemFactory.item(Material.ARROW, "&7Inapoi la Lista NPC"),
            GuiAction { click ->
                click.service().setShopSelectedNpcId(click.player(), null)
                click.service().open(click.player(), GuiKey.SHOP)
            }
        ))

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }
}
