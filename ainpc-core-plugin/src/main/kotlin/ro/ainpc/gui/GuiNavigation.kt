package ro.ainpc.gui

import org.bukkit.Material

object GuiNavigation {
    @JvmStatic
    fun addStandardControls(context: GuiRenderContext, refreshKey: GuiKey) {
        context.button(
            45,
            GuiButton.enabled(
                GuiItemFactory.item(Material.ARROW, "&eInapoi", "&7Revine la hub-ul principal."),
                GuiAction { click -> click.service().open(click.player(), GuiKey.MAIN) }
            )
        )
        context.button(
            49,
            GuiButton.enabled(
                GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", "&7Reincarca ecranul curent."),
                GuiAction { click -> click.service().open(click.player(), refreshKey) }
            )
        )
        context.button(
            53,
            GuiButton.enabled(
                GuiItemFactory.item(Material.BARRIER, "&cInchide", "&7Inchide interfata."),
                GuiAction { click -> click.player().closeInventory() }
            )
        )
    }
}
