package ro.ainpc.engine.runtime.actions

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import ro.ainpc.engine.runtime.ScenarioActionHandler
import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition

class GiveItemAction : ScenarioActionHandler {
    override fun type(): String = "give_item"

    override fun execute(context: ScenarioExecutionContext, action: ScenarioRuntimeDefinition) {
        val itemType = action.parameter("item").ifBlank { action.parameter("material").ifBlank { "STONE" } }
        val amount = (action.parameter("amount").toIntOrNull() ?: 1).coerceAtLeast(1)
        val material = runCatching { Material.valueOf(itemType.uppercase()) }.getOrNull() ?: Material.STONE
        val player = Bukkit.getPlayer(context.playerUuid()) ?: return
        val stack = ItemStack(material, amount)
        val leftovers = player.inventory.addItem(stack)
        for (overflow in leftovers.values) {
            player.world.dropItemNaturally(player.location, overflow)
        }
    }
}
