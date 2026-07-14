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
        val player = context.player()
        val wa = context.plugin().platform.worldAdmin
        val loc = player.location
        val region = wa.findRegion(loc.world.name, loc.blockX, loc.blockY, loc.blockZ)
        val place = wa.findPlace(loc.world.name, loc.blockX, loc.blockY, loc.blockZ)

        context.item(4, GuiItemFactory.item(Material.NETHER_STAR, "&6AINPC", listOf(
            "&7NPC-uri in raza 32: &f${context.plugin().npcManager.getNPCsNear(player.location, 32.0).size}",
            if (region != null) "&7Regiune: &f${region.name()}" else "&7Regiune: &f<nemapata>",
            if (place != null) "&7Place: &f${place.displayName()}" else "&7Place: &f<nemapat>"
        )))

        openBtn(context, 10, GuiKey.QUEST, Material.WRITABLE_BOOK, "&eProgresii", "&7Questuri, contracte si tracking.")
        openBtn(context, 11, GuiKey.INTERACT, Material.VILLAGER_SPAWN_EGG, "&aNPC", "&7NPC-uri apropiate si actiuni.")
        openBtn(context, 12, GuiKey.STATS, Material.CLOCK, "&dStatistici", "&7Snapshot personal.")
        openBtn(context, 13, GuiKey.ROUTINE, Material.COMPASS, "&eRutine", "&7Programul NPC-urilor.")
        openBtn(context, 14, GuiKey.SHOP, Material.EMERALD, "&2Shop", "&7Tranzactii cu NPC-uri.")
        openBtn(context, 15, GuiKey.QUEST_MAP, Material.FILLED_MAP, "&bQuest Map", "&7Vezi locatiile questurilor.")

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
