package ro.ainpc.gui.screens

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import java.util.ArrayList

class MainHubGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.MAIN

    override fun title(player: Player): String = "&0AINPC Hub"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        val worldAdmin: WorldAdminApi = context.plugin().platform.worldAdmin
        val location: Location = player.location

        context.item(
            4,
            GuiItemFactory.item(
                Material.NETHER_STAR,
                "&6AINPC Hub",
                listOf(
                    "&7Jucator: &f${player.name}",
                    "&7NPC-uri incarcate: &f${context.plugin().npcManager.npcCount}",
                    "&7World mapping: &f${worldAdmin.regionCount} regiuni, " +
                        "${worldAdmin.placeCount} places, ${worldAdmin.nodeCount} noduri",
                    "&7Locatie: &f${location.world.name} ${location.blockX}, ${location.blockY}, ${location.blockZ}"
                )
            )
        )

        openButton(
            context,
            10,
            GuiKey.QUEST,
            Material.WRITABLE_BOOK,
            "&eProgresii",
            listOf("&7Questuri, contracte, duty-uri si tracking.")
        )
        openButton(
            context,
            11,
            GuiKey.INTERACT,
            Material.VILLAGER_SPAWN_EGG,
            "&aInteractiune NPC",
            listOf("&7NPC-uri apropiate si actiuni rapide.")
        )
        openButton(
            context,
            12,
            GuiKey.WORLD,
            Material.COMPASS,
            "&bWorld",
            listOf("&7Regiune, place, noduri si context local.")
        )
        openButton(
            context,
            13,
            GuiKey.STATS,
            Material.CLOCK,
            "&dStatistici",
            listOf("&7Snapshot personal si NPC-uri din apropiere.")
        )
        openButton(
            context,
            14,
            GuiKey.SHOP,
            Material.EMERALD,
            "&2Shop NPC",
            listOf("&7Intrare pregatita pentru economie/shop.")
        )
        openButton(
            context,
            15,
            GuiKey.ROUTINE,
            Material.CLOCK,
            "&eRutine NPC",
            listOf("&7Preview program zilnic si status rutina.")
        )
        openButton(
            context,
            16,
            GuiKey.STORY,
            Material.AMETHYST_SHARD,
            "&dStory",
            listOf("&7State narativ local si evenimente recente.")
        )

        openButton(
            context,
            28,
            GuiKey.MANAGER,
            Material.NAME_TAG,
            "&6Manager NPC",
            listOf("&7Lista NPC admin, info si teleport.")
        )
        openButton(
            context,
            29,
            GuiKey.AUDIT,
            Material.REDSTONE_TORCH,
            "&cAudit",
            listOf("&7Ruleaza audituri operationale.")
        )
        openButton(
            context,
            30,
            GuiKey.DEBUG,
            Material.SPYGLASS,
            "&9Debug",
            listOf("&7Debugdump si test OpenAI.")
        )

        if (context.service().canOpen(player, GuiKey.DEBUG)) {
            context.button(
                32,
                GuiButton.enabled(
                    GuiItemFactory.item(Material.ENDER_EYE, "&bTest OpenAI", "&7Ruleaza /ainpc test."),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc test") }
                )
            )
        }

        context.button(
            49,
            GuiButton.enabled(
                GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", "&7Reincarca hub-ul."),
                GuiAction { click -> click.service().open(click.player(), GuiKey.MAIN) }
            )
        )
        context.button(
            53,
            GuiButton.enabled(
                GuiItemFactory.item(Material.BARRIER, "&cInchide", "&7Inchide interfata."),
                GuiAction { click -> click.player().closeInventory() }
            )
        )
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun openButton(
        context: GuiRenderContext,
        slot: Int,
        target: GuiKey,
        material: Material,
        title: String,
        lore: List<String>
    ) {
        if (context.service().canOpen(context.player(), target)) {
            context.button(
                slot,
                GuiButton.enabled(
                    GuiItemFactory.item(material, title, lore),
                    GuiAction { click -> click.service().open(click.player(), target) }
                )
            )
            return
        }

        val lockedLore = ArrayList(lore)
        lockedLore.add("&8Necesita permisiune pentru ${target.displayName()}.")
        context.button(slot, GuiButton.disabled(GuiItemFactory.disabled(Material.BARRIER, title, lockedLore)))
    }
}
