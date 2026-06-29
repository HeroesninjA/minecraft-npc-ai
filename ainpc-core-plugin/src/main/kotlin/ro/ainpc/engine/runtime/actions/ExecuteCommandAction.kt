package ro.ainpc.engine.runtime.actions

import org.bukkit.Bukkit
import ro.ainpc.engine.runtime.ScenarioActionHandler
import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition

class ExecuteCommandAction : ScenarioActionHandler {
    override fun type(): String = "execute_command"

    override fun execute(context: ScenarioExecutionContext, action: ScenarioRuntimeDefinition) {
        val command = action.parameter("command").ifBlank { return }
        val asConsole = action.parameter("as_console").toBoolean()
        val resolved = command
            .replace("{player}", context.playerName())
            .replace("{uuid}", context.playerUuid())
        if (asConsole) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved)
        } else {
            val player = Bukkit.getPlayer(context.playerUuid()) ?: return
            Bukkit.dispatchCommand(player, resolved)
        }
    }
}
