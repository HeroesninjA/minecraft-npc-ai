package ro.ainpc.gui

import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import ro.ainpc.AINPCPlugin
import java.util.HashMap

/**
 * Guirendercontext.
 */
class GuiRenderContext(
    plugin: AINPCPlugin,
    service: GuiService,
    player: Player,
    inventory: Inventory
) {
    private val pluginValue = plugin
    private val serviceValue = service
    private val playerValue = player
    private val inventoryValue = inventory
    private val buttonsValue: MutableMap<Int, GuiButton> = HashMap()
    private val changedSlotsValue: MutableSet<Int> = HashSet()

    fun plugin(): AINPCPlugin = pluginValue

    fun service(): GuiService = serviceValue

    fun player(): Player = playerValue

    fun inventory(): Inventory = inventoryValue

    fun item(slot: Int, item: ItemStack?) {
        if (!isValidSlot(slot)) {
            return
        }
        inventoryValue.setItem(slot, item)
        buttonsValue.remove(slot)
        changedSlotsValue.add(slot)
    }

    fun button(slot: Int, button: GuiButton?) {
        if (!isValidSlot(slot) || button == null) {
            return
        }
        inventoryValue.setItem(slot, button.icon())
        buttonsValue[slot] = button
        changedSlotsValue.add(slot)
    }

    fun fillEmpty(item: ItemStack?) {
        for (slot in 0 until inventoryValue.size) {
            if (inventoryValue.getItem(slot) == null) {
                inventoryValue.setItem(slot, item)
            }
        }
    }

    fun buttons(): Map<Int, GuiButton> = java.util.Map.copyOf(buttonsValue)

    fun changedSlots(): Set<Int> = java.util.Set.copyOf(changedSlotsValue)

    private fun isValidSlot(slot: Int): Boolean = slot >= 0 && slot < inventoryValue.size
}
