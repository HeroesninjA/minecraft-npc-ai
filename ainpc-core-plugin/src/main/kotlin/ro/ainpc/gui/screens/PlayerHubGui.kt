package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen

class PlayerHubGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.PLAYER_HUB
    override fun title(player: Player): String = "&0AINPC"
    override fun size(player: Player): Int = 36

    override fun render(context: GuiRenderContext) {
        context.item(4, GuiItemFactory.item(Material.NETHER_STAR, "&6AINPC", listOf(
            "&7NPC-uri in raza 32: &f${context.plugin().npcManager.getNPCsNear(context.player().location, 32.0).size}"
        )))

        openBtn(context, 10, GuiKey.QUEST, Material.WRITABLE_BOOK, "&eProgresii", "&7Questuri, contracte si tracking.")
        openBtn(context, 11, GuiKey.INTERACT, Material.VILLAGER_SPAWN_EGG, "&aNPC", "&7NPC-uri apropiate si actiuni.")
        openBtn(context, 12, GuiKey.STATS, Material.CLOCK, "&dStatistici", "&7Snapshot personal.")
        openBtn(context, 13, GuiKey.ROUTINE, Material.COMPASS, "&eRutine", "&7Programul NPC-urilor.")
        openBtn(context, 14, GuiKey.SHOP, Material.EMERALD, "&2Shop", "&7Tranzactii cu NPC-uri.")

        context.button(49, GuiButton.enabled(GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", ""),
            action = { click -> click.service().open(click.player(), GuiKey.MAIN) }))
        context.button(53, GuiButton.enabled(GuiItemFactory.item(Material.BARRIER, "&cInchide", ""),
            action = { click -> click.player().closeInventory() }))
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun openBtn(ctx: GuiRenderContext, slot: Int, key: GuiKey, mat: Material, title: String, lore: String) {
        if (ctx.service().canOpen(ctx.player(), key)) {
            ctx.button(slot, GuiButton.enabled(GuiItemFactory.item(mat, title, listOf(lore)),
                action = { click -> click.service().open(click.player(), key) }))
        } else {
            ctx.button(slot, GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, title, listOf("&8Necesita permisiune."))))
        }
    }
}
