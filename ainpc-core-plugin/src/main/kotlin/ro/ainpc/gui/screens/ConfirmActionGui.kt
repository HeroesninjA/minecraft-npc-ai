package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.gui.GuiService

class ConfirmActionGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.CONFIRM

    override fun title(player: Player): String = "&0AINPC Confirmare"

    override fun size(player: Player): Int = 27

    override fun render(context: GuiRenderContext) {
        val optionalRequest = context.service().getConfirmRequest(context.player())
        if (optionalRequest.isEmpty) {
            context.item(
                13,
                GuiItemFactory.item(
                    Material.BARRIER,
                    "&cConfirmare expirata",
                    "&7Actiunea nu mai este disponibila."
                )
            )
            context.button(
                18,
                GuiButton.enabled(
                    GuiItemFactory.item(Material.ARROW, "&eInapoi", "&7Revine la hub."),
                    GuiAction { click -> click.service().open(click.player(), GuiKey.MAIN) }
                )
            )
            context.fillEmpty(GuiItemFactory.filler())
            return
        }

        val request: GuiService.ConfirmRequest = optionalRequest.get()
        val lore = ArrayList(request.warningLines())
        lore.add("&8Comanda: /${request.command()}")
        context.item(13, GuiItemFactory.item(Material.REDSTONE_BLOCK, "&c${request.title()}", lore))

        context.button(
            11,
            GuiButton.enabled(
                GuiItemFactory.item(Material.LIME_CONCRETE, "&aConfirma", "&7Executa actiunea."),
                GuiAction { click -> click.service().runConfirmedCommand(click.player(), request) }
            )
        )
        context.button(
            15,
            GuiButton.enabled(
                GuiItemFactory.item(Material.RED_CONCRETE, "&cAnuleaza", "&7Revine fara modificari."),
                GuiAction { click -> click.service().returnFromConfirm(click.player(), request) }
            )
        )
        context.fillEmpty(GuiItemFactory.filler())
    }
}
