package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen

class CreatorHubGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.CREATOR_HUB
    override fun title(player: Player): String = "&0Creator"
    override fun size(player: Player): Int = 36

    override fun render(context: GuiRenderContext) {
        context.item(4, GuiItemFactory.item(Material.CRAFTING_TABLE, "&6Creator Tools", listOf(
            "&7Creeaza si editezi mapping si questuri."
        )))

        context.button(10, GuiButton.enabled(GuiItemFactory.item(Material.GRASS_BLOCK, "&6Creator Mapping", listOf("&7Creaza regiuni, places si noduri.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.MAPPING_CREATOR) }))
        context.button(11, GuiButton.enabled(GuiItemFactory.item(Material.WRITABLE_BOOK, "&bQuest Creator", listOf("&7Submeniu quest: definitii, test, ancore, editor.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.CREATOR_QUEST) }))
        context.button(12, GuiButton.enabled(GuiItemFactory.item(Material.GRASS_BLOCK, "&6Demo mapping", listOf("&7Creeaza mapping demo la pozitia ta.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc world demo create") }))
        context.button(13, GuiButton.enabled(GuiItemFactory.item(Material.WRITABLE_BOOK, "&aSalveaza mapping", listOf("&7Persista modificarile.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc world save") }))
        context.button(14, GuiButton.enabled(GuiItemFactory.item(Material.WRITABLE_BOOK, "&bQuest Creator", listOf("&7Submeniu dedicat: definitii, test, ancore, authoring, quest map.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.CREATOR_QUEST) }))

        context.button(49, GuiButton.enabled(GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", ""),
            GuiAction { click -> click.service().open(click.player(), GuiKey.CREATOR_HUB) }))
        context.button(53, GuiButton.enabled(GuiItemFactory.item(Material.BARRIER, "&cInchide", ""),
            GuiAction { click -> click.player().closeInventory() }))
        context.fillEmpty(GuiItemFactory.filler())
    }
}
