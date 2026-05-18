package ro.ainpc.gui

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import java.util.UUID

class AINPCGuiHolder(
    private val sessionIdValue: UUID,
    private val keyValue: GuiKey
) : InventoryHolder {
    private lateinit var inventoryValue: Inventory

    fun sessionId(): UUID = sessionIdValue

    fun key(): GuiKey = keyValue

    fun attach(inventory: Inventory) {
        inventoryValue = inventory
    }

    override fun getInventory(): Inventory = inventoryValue
}
