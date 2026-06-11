package ro.ainpc.gui

import org.bukkit.entity.Player

interface GuiScreen {
    fun key(): GuiKey

    fun title(player: Player): String

    fun size(player: Player): Int

    fun render(context: GuiRenderContext)
}
