package ro.ainpc.engine.runtime.actions

import org.bukkit.Bukkit
import org.bukkit.Location
import ro.ainpc.engine.runtime.ScenarioActionHandler
import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition

class TeleportPlayerAction : ScenarioActionHandler {
    override fun type(): String = "teleport_player"

    override fun execute(context: ScenarioExecutionContext, action: ScenarioRuntimeDefinition) {
        val player = Bukkit.getPlayer(context.playerUuid()) ?: return
        val world = action.parameter("world").ifBlank { player.world.name }
        val x = action.parameter("x").toDoubleOrNull()
        val y = action.parameter("y").toDoubleOrNull()
        val z = action.parameter("z").toDoubleOrNull()
        if (x == null || y == null || z == null) return
        val bukkitWorld = Bukkit.getWorld(world) ?: player.world
        player.teleport(Location(bukkitWorld, x, y, z))
    }
}
