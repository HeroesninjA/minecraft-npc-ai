package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen

class AdminHubGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.ADMIN_HUB
    override fun title(player: Player): String = "&0Admin"
    override fun size(player: Player): Int = 45

    override fun render(context: GuiRenderContext) {
        val wa: WorldAdminApi = context.plugin().platform.worldAdmin

        context.item(4, GuiItemFactory.item(Material.COMMAND_BLOCK, "&6Admin Panel", listOf(
            "&7Mapping: &f${wa.regionCount}r / ${wa.placeCount}p / ${wa.nodeCount}n",
            "&7NPC: &f${context.plugin().npcManager.getNPCCount()}",
            if (wa.hasUnsavedChanges()) "&cModificari nesalvate!" else "&aSalvat"
        )))

        if (context.service().canOpen(context.player(), GuiKey.WORLD)) {
            context.button(10, GuiButton.enabled(GuiItemFactory.item(Material.COMPASS, "&bWorld", listOf("&7Context world, regiuni, places.")),
                GuiAction { click -> click.service().open(click.player(), GuiKey.WORLD) }))
        }
        if (context.service().canOpen(context.player(), GuiKey.ADMIN_MAPPING)) {
            context.button(11, GuiButton.enabled(GuiItemFactory.item(Material.FILLED_MAP, "&6Mapping admin", listOf("&7Lista regiuni, demo, save.")),
                GuiAction { click -> click.service().open(click.player(), GuiKey.ADMIN_MAPPING) }))
        }
        if (context.service().canOpen(context.player(), GuiKey.MANAGER)) {
            context.button(12, GuiButton.enabled(GuiItemFactory.item(Material.NAME_TAG, "&6Manager NPC", listOf("&7Lista, info, teleport.")),
                GuiAction { click -> click.service().open(click.player(), GuiKey.MANAGER) }))
        }
        if (context.service().canOpen(context.player(), GuiKey.AUDIT)) {
            context.button(13, GuiButton.enabled(GuiItemFactory.item(Material.REDSTONE_TORCH, "&cAudit", listOf("&7Ruleaza audit operational.")),
                GuiAction { click -> click.service().open(click.player(), GuiKey.AUDIT) }))
        }
        if (context.service().canOpen(context.player(), GuiKey.DEBUG)) {
            context.button(14, GuiButton.enabled(GuiItemFactory.item(Material.SPYGLASS, "&9Debug", listOf("&7Debugdump si teste.")),
                GuiAction { click -> click.service().open(click.player(), GuiKey.DEBUG) }))
        }
        if (context.service().canOpen(context.player(), GuiKey.AUTHORING)) {
            context.button(15, GuiButton.enabled(GuiItemFactory.item(Material.ENCHANTED_BOOK, "&bAuthoring", listOf("&7Snapshot quest design.")),
                GuiAction { click -> click.service().open(click.player(), GuiKey.AUTHORING) }))
        }
        if (context.service().canOpen(context.player(), GuiKey.STORY)) {
            context.button(16, GuiButton.enabled(GuiItemFactory.item(Material.AMETHYST_SHARD, "&dStory", listOf("&7Context narativ.")),
                GuiAction { click -> click.service().open(click.player(), GuiKey.STORY) }))
        }

        context.button(28, GuiButton.enabled(GuiItemFactory.item(Material.GRASS_BLOCK, "&6Demo mapping", listOf("&7Creeaza mapping demo.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc world demo create") }))
        context.button(29, GuiButton.enabled(GuiItemFactory.item(Material.WRITABLE_BOOK, "&aSalveaza mapping", listOf("&7Persista modificarile.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc world save") }))

        context.button(49, GuiButton.enabled(GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", ""),
            GuiAction { click -> click.service().open(click.player(), GuiKey.ADMIN_HUB) }))
        context.button(53, GuiButton.enabled(GuiItemFactory.item(Material.BARRIER, "&cInchide", ""),
            GuiAction { click -> click.player().closeInventory() }))
        context.fillEmpty(GuiItemFactory.filler())
    }
}
