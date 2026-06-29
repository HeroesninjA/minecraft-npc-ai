package ro.ainpc.engine.runtime.actions

import org.bukkit.Bukkit
import net.md_5.bungee.api.ChatColor
import ro.ainpc.engine.runtime.ScenarioActionHandler
import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition

class SendMessageAction : ScenarioActionHandler {
    override fun type(): String = "send_message"

    override fun execute(context: ScenarioExecutionContext, action: ScenarioRuntimeDefinition) {
        val message = action.parameter("message").ifBlank { return }
        val player = Bukkit.getPlayer(context.playerUuid()) ?: return
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message))
    }
}
