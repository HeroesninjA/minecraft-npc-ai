package ro.ainpc.gui

import org.bukkit.entity.Player
import java.util.UUID

/**
 * Guiscreen.
 */
interface GuiScreen {
    fun key(): GuiKey

    fun title(player: Player): String

    fun size(player: Player): Int

    fun render(context: GuiRenderContext)

    fun afterOpen(context: GuiRenderContext, sessionId: UUID) = Unit
}
