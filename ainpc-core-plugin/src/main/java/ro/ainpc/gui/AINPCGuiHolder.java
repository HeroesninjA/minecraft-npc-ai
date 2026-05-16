package ro.ainpc.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class AINPCGuiHolder implements InventoryHolder {

    private final UUID sessionId;
    private final GuiKey key;
    private Inventory inventory;

    public AINPCGuiHolder(UUID sessionId, GuiKey key) {
        this.sessionId = sessionId;
        this.key = key;
    }

    public UUID sessionId() {
        return sessionId;
    }

    public GuiKey key() {
        return key;
    }

    public void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
