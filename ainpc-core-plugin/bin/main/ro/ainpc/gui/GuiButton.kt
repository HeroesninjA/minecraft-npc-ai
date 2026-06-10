package ro.ainpc.gui

import org.bukkit.inventory.ItemStack

class GuiButton(
    icon: ItemStack?,
    action: GuiAction?,
    enabled: Boolean
) {
    private val iconValue = icon
    private val actionValue = action
    private val enabledValue = enabled

    fun icon(): ItemStack? = iconValue

    fun action(): GuiAction? = actionValue

    fun enabled(): Boolean = enabledValue

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is GuiButton) {
            return false
        }

        return iconValue == other.iconValue &&
            actionValue == other.actionValue &&
            enabledValue == other.enabledValue
    }

    override fun hashCode(): Int {
        var result = iconValue?.hashCode() ?: 0
        result = 31 * result + (actionValue?.hashCode() ?: 0)
        result = 31 * result + enabledValue.hashCode()
        return result
    }

    override fun toString(): String =
        "GuiButton[icon=$iconValue, action=$actionValue, enabled=$enabledValue]"

    companion object {
        @JvmStatic
        fun enabled(icon: ItemStack?, action: GuiAction?): GuiButton =
            GuiButton(icon, action, true)

        @JvmStatic
        fun disabled(icon: ItemStack?): GuiButton =
            GuiButton(icon, null, false)
    }
}
