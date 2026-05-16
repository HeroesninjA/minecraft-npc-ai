package ro.ainpc.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ro.ainpc.AINPCPlugin;

import java.util.HashMap;
import java.util.Map;

public class GuiRenderContext {

    private final AINPCPlugin plugin;
    private final GuiService service;
    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, GuiButton> buttons = new HashMap<>();

    public GuiRenderContext(AINPCPlugin plugin, GuiService service, Player player, Inventory inventory) {
        this.plugin = plugin;
        this.service = service;
        this.player = player;
        this.inventory = inventory;
    }

    public AINPCPlugin plugin() {
        return plugin;
    }

    public GuiService service() {
        return service;
    }

    public Player player() {
        return player;
    }

    public Inventory inventory() {
        return inventory;
    }

    public void item(int slot, ItemStack item) {
        if (!isValidSlot(slot)) {
            return;
        }
        inventory.setItem(slot, item);
        buttons.remove(slot);
    }

    public void button(int slot, GuiButton button) {
        if (!isValidSlot(slot) || button == null) {
            return;
        }
        inventory.setItem(slot, button.icon());
        buttons.put(slot, button);
    }

    public void fillEmpty(ItemStack item) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, item);
            }
        }
    }

    public Map<Integer, GuiButton> buttons() {
        return Map.copyOf(buttons);
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < inventory.getSize();
    }
}
