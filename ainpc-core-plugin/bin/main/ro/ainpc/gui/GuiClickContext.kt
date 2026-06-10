package ro.ainpc.gui

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import ro.ainpc.AINPCPlugin

class GuiClickContext(
    plugin: AINPCPlugin,
    service: GuiService,
    player: Player,
    session: GuiSession,
    rawSlot: Int,
    clickType: ClickType,
    inventoryAction: InventoryAction
) {
    private val pluginValue = plugin
    private val serviceValue = service
    private val playerValue = player
    private val sessionValue = session
    private val rawSlotValue = rawSlot
    private val clickTypeValue = clickType
    private val inventoryActionValue = inventoryAction

    fun plugin(): AINPCPlugin = pluginValue

    fun service(): GuiService = serviceValue

    fun player(): Player = playerValue

    fun session(): GuiSession = sessionValue

    fun rawSlot(): Int = rawSlotValue

    fun clickType(): ClickType = clickTypeValue

    fun inventoryAction(): InventoryAction = inventoryActionValue

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is GuiClickContext) {
            return false
        }

        return pluginValue == other.pluginValue &&
            serviceValue == other.serviceValue &&
            playerValue == other.playerValue &&
            sessionValue == other.sessionValue &&
            rawSlotValue == other.rawSlotValue &&
            clickTypeValue == other.clickTypeValue &&
            inventoryActionValue == other.inventoryActionValue
    }

    override fun hashCode(): Int {
        var result = pluginValue.hashCode()
        result = 31 * result + serviceValue.hashCode()
        result = 31 * result + playerValue.hashCode()
        result = 31 * result + sessionValue.hashCode()
        result = 31 * result + rawSlotValue
        result = 31 * result + clickTypeValue.hashCode()
        result = 31 * result + inventoryActionValue.hashCode()
        return result
    }

    override fun toString(): String =
        "GuiClickContext[plugin=$pluginValue, service=$serviceValue, player=$playerValue, " +
            "session=$sessionValue, rawSlot=$rawSlotValue, clickType=$clickTypeValue, " +
            "inventoryAction=$inventoryActionValue]"
}
