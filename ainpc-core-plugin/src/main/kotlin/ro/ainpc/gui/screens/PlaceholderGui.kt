package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen

class PlaceholderGui(
    private val keyValue: GuiKey,
    private val titleValue: String,
    private val messageValue: String
) : GuiScreen {
    override fun key(): GuiKey = keyValue

    override fun title(player: Player): String = "&0AINPC $titleValue"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        context.item(
            22,
            GuiItemFactory.item(
                Material.CHEST,
                "&e$titleValue",
                listOf(
                    "&7$messageValue",
                    "&7Ecranul este conectat la hub si gata pentru provider dedicat."
                )
            )
        )
        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }
}
