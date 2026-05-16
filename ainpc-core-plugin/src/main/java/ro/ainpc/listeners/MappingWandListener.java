package ro.ainpc.listeners;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.world.mapping.MappingPoint;
import ro.ainpc.world.mapping.MappingWandMode;
import ro.ainpc.world.mapping.MappingWandService;

public class MappingWandListener extends AbstractPluginListener {

    public MappingWandListener(AINPCPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        MappingWandService service = plugin.getMappingWandService();
        if (service == null || !service.isWandItem(event.getItem())) {
            return;
        }
        if (!player.hasPermission("ainpc.admin")) {
            messages().sendMessage(player, "no_permission");
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null
            || (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        event.setCancelled(true);
        MappingPoint point = toPoint(clickedBlock.getLocation());
        MappingWandMode mode = service.mode(player.getUniqueId());
        if (mode == MappingWandMode.NODE
            || mode == MappingWandMode.NPC_BIND
            || mode == MappingWandMode.QUEST_ANCHOR
            || player.isSneaking()) {
            service.setPoint(player, point);
            messages().send(player, "&aWand point setat: &f" + point.format());
            messages().send(player, "&7Draft: &f/ainpc map " + mode.id() + " <descriere>");
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            service.setPos1(player, point);
            messages().send(player, "&aWand pos1 setat: &f" + point.format());
        } else {
            service.setPos2(player, point);
            messages().send(player, "&aWand pos2 setat: &f" + point.format());
        }
        messages().send(player, "&7Draft: &f/ainpc map " + mode.id() + " <descriere>");
    }

    private MappingPoint toPoint(Location location) {
        return new MappingPoint(
            location.getWorld() != null ? location.getWorld().getName() : "",
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }
}
