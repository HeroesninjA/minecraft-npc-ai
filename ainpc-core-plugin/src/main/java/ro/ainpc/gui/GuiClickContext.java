package ro.ainpc.gui;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.entity.Player;
import ro.ainpc.AINPCPlugin;

public record GuiClickContext(
    AINPCPlugin plugin,
    GuiService service,
    Player player,
    GuiSession session,
    int rawSlot,
    ClickType clickType,
    InventoryAction inventoryAction
) {
}
