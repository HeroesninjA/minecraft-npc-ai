package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.npc.AINPC
import java.util.Comparator
import java.util.Locale

class ShopGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.SHOP

    override fun title(player: Player): String = "&0AINPC Shop"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        val location = player.location
        val adminView = player.hasPermission("ainpc.admin")
        val nearbyNpcs = context.plugin().npcManager.getNPCsNear(location, 16.0).stream()
            .sorted(Comparator.comparing { npc: AINPC -> npc.name.lowercase(Locale.ROOT) })
            .toList()
        val totalNpcs = context.plugin().npcManager.getNPCCount()

        context.item(4, GuiItemFactory.item(
            Material.EMERALD,
            "&2Shop & NPCs",
            listOf(
                "&7Total NPC-uri: &f$totalNpcs",
                "&7In raza 16m: &f${nearbyNpcs.size}",
                "&7Click: &finfo NPC",
                "&7Right click: &fvorba rapida",
                if (adminView) "&8Shift click: /ainpc tp" else ""
            )
        ))

        context.button(5, GuiButton.enabled(
            GuiItemFactory.item(Material.VILLAGER_SPAWN_EGG, "&eNPC-uri in apropiere", "&7NPC-urile din raza ta de 16m."),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc list") }
        ))

        var slot = 19
        for (npc in nearbyNpcs.take(21)) {
            val distance = if (npc.location != null && npc.location!!.world == location.world) {
                String.format(Locale.ROOT, "%.1f", npc.location!!.distance(location))
            } else {
                "?"
            }
            context.button(slot++, GuiButton.enabled(
                GuiItemFactory.item(
                    Material.PLAYER_HEAD,
                    "&f${npc.name}",
                    listOf(
                        "&7Ocupatie: &f${npc.occupation ?: "necunoscuta"}",
                        "&7Stare: &f${npc.currentState.displayName}",
                        "&7Emotie: &f${npc.emotions.dominantEmotion}",
                        "&7Distanta: &f$distance m",
                        "&8Click: info | Right: vorbeste | Shift: tp"
                    )
                ),
                GuiAction { click ->
                    when {
                        click.clickType().isShiftClick && adminView ->
                            click.service().runCommand(click.player(), "ainpc tp ${npc.name}")
                        click.clickType().isRightClick ->
                            click.service().runCommand(click.player(), "ainpc quest status ${npc.name}")
                        else ->
                            click.service().runCommand(click.player(), "ainpc info ${npc.name}")
                    }
                }
            ))
        }

        if (nearbyNpcs.isEmpty()) {
            context.item(22, GuiItemFactory.item(
                Material.BARRIER, "&cNiciun NPC in apropiere",
                listOf("&7Nu exista NPC-uri in raza de 16m.", "&7Muta-te mai aproape de sat.")
            ))
        }

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }
}
