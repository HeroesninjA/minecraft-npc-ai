@file:Suppress("SENSELESS_COMPARISON")
package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.npc.AINPC
import java.util.Locale

class QuestOfferNpcGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.QUEST_OFFER_NPC

    override fun title(player: Player): String = "&0Quest NPC"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        val plugin = context.plugin()
        val wa = plugin.platform.worldAdmin
        val loc = player.location
        val region = wa.findRegion(loc.world.name, loc.blockX, loc.blockY, loc.blockZ)
        val place = wa.findPlace(loc.world.name, loc.blockX, loc.blockY, loc.blockZ)

        context.item(4, GuiItemFactory.item(
            Material.VILLAGER_SPAWN_EGG,
            "&eNPC in apropiere",
            listOf(
                "&7Click pe un NPC pentru a interactiona.",
                "&7Regiune: &f${region?.name() ?: "<nemapata>"}",
                "&7Place: &f${place?.displayName() ?: "<nemapat>"}"
            )
        ))

        if (region != null || place != null) {
            context.button(8, GuiButton.enabled(
                GuiItemFactory.item(Material.FILLED_MAP, "&6Quest Map",
                    "&7Leaga obiectivele de locatii.",
                    "&7Click: deschide Quest Map."),
                GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_MAP) }
            ))
        }

        val nearbyNpcs = plugin.npcManager.getNPCsNear(player.location, 16.0)
            .sortedBy { it.name.lowercase(Locale.ROOT) }
            .take(36)

        val slots = intArrayOf(
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
        )

        for (i in nearbyNpcs.indices) {
            if (i >= slots.size) break
            val npc = nearbyNpcs[i]
            context.button(slots[i], GuiButton.enabled(
                GuiItemFactory.item(
                    Material.PLAYER_HEAD,
                    "&f${npc.name}",
                    listOf(
                        "&7Ocupatie: &f${npc.occupation ?: "N/A"}",
                        "&7Stare: &f${npc.currentState.displayName}",
                        "&7Click: info NPC",
                        "&8Right click: vorbeste cu NPC-ul"
                    )
                ),
                GuiAction { click ->
                    if (click.clickType().isRightClick) {
                        click.service().runCommand(click.player(), "ainpc talk ${npc.name}")
                    } else {
                        click.service().runCommand(click.player(), "ainpc info ${npc.name}")
                    }
                }
            ))
        }

        if (nearbyNpcs.isEmpty()) {
            context.item(22, GuiItemFactory.item(
                Material.BARRIER, "&7Nu exista NPC-uri in apropiere",
                listOf("&8Apropie-te de un NPC.")
            ))
        }
    }
}
