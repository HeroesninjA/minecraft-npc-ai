package ro.ainpc.gui;

import org.bukkit.inventory.ItemStack;

public record GuiButton(
    ItemStack icon,
    GuiAction action,
    boolean enabled
) {
    public static GuiButton enabled(ItemStack icon, GuiAction action) {
        return new GuiButton(icon, action, true);
    }

    public static GuiButton disabled(ItemStack icon) {
        return new GuiButton(icon, null, false);
    }
}
