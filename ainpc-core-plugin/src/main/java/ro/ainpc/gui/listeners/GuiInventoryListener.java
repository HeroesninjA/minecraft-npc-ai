package ro.ainpc.gui.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.gui.AINPCGuiHolder;

public class GuiInventoryListener implements Listener {

    private final AINPCPlugin plugin;

    public GuiInventoryListener(AINPCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof AINPCGuiHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() < 0 || event.getRawSlot() >= topInventory.getSize()) {
            return;
        }

        plugin.getGuiService().handleClick(
            player,
            holder.sessionId(),
            event.getRawSlot(),
            event.getClick(),
            event.getAction()
        );
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory.getHolder() instanceof AINPCGuiHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof AINPCGuiHolder holder) {
            plugin.getGuiService().sessions().close(holder.sessionId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getGuiService().sessions().closePlayer(event.getPlayer().getUniqueId());
    }
}
