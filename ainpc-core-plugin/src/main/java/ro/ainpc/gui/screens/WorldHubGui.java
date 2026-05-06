package ro.ainpc.gui.screens;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ro.ainpc.api.WorldAdminApi;
import ro.ainpc.gui.GuiButton;
import ro.ainpc.gui.GuiItemFactory;
import ro.ainpc.gui.GuiKey;
import ro.ainpc.gui.GuiNavigation;
import ro.ainpc.gui.GuiRenderContext;
import ro.ainpc.gui.GuiScreen;
import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegionInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WorldHubGui implements GuiScreen {

    @Override
    public GuiKey key() {
        return GuiKey.WORLD;
    }

    @Override
    public String title(Player player) {
        return "&0AINPC World";
    }

    @Override
    public int size(Player player) {
        return 54;
    }

    @Override
    public void render(GuiRenderContext context) {
        Player player = context.player();
        WorldAdminApi worldAdmin = context.plugin().getPlatform().getWorldAdmin();
        Location location = player.getLocation();
        String worldName = location.getWorld().getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        WorldRegionInfo region = worldAdmin.findRegion(worldName, x, y, z);
        WorldPlaceInfo place = worldAdmin.findPlace(worldName, x, y, z);
        WorldNodeInfo node = worldAdmin.findNode(worldName, x, y, z);
        List<WorldNodeInfo> nearbyNodes = worldAdmin.findNodesNear(worldName, location.getX(), location.getY(), location.getZ(), 24.0, 7)
            .stream()
            .sorted(Comparator.comparing(WorldNodeInfo::id))
            .toList();

        context.item(4, GuiItemFactory.item(
            Material.COMPASS,
            "&bWorld context",
            List.of(
                "&7Coordonate: &f" + worldName + " " + x + ", " + y + ", " + z,
                "&7Mapping: &f" + worldAdmin.getRegionCount() + " regiuni / "
                    + worldAdmin.getPlaceCount() + " places / " + worldAdmin.getNodeCount() + " noduri"
            )
        ));

        context.item(10, GuiItemFactory.item(Material.FILLED_MAP, "&eRegiune curenta", regionLore(region)));
        context.item(11, GuiItemFactory.item(Material.OAK_DOOR, "&aPlace curent", placeLore(place)));
        context.item(12, GuiItemFactory.item(Material.TARGET, "&dNode curent", nodeLore(node)));

        int slot = 19;
        for (WorldNodeInfo nearbyNode : nearbyNodes) {
            context.item(slot++, GuiItemFactory.item(Material.LODESTONE, "&f" + nearbyNode.id(), nodeLore(nearbyNode)));
        }

        context.button(28, GuiButton.enabled(
            GuiItemFactory.item(Material.ENDER_EYE, "&bWhere am I", "&7Ruleaza /ainpc world whereami."),
            click -> click.service().runCommand(click.player(), "ainpc world whereami")
        ));
        context.button(29, GuiButton.enabled(
            GuiItemFactory.item(Material.NETHER_STAR, "&dStory context", "&7Ruleaza context story pentru cel mai apropiat NPC."),
            click -> click.service().runCommand(click.player(), "ainpc story context " + click.player().getName() + " nearest")
        ));

        if (context.player().hasPermission("ainpc.admin")) {
            context.button(30, GuiButton.enabled(
                GuiItemFactory.item(Material.SPYGLASS, "&6Scan sat", "&7Ruleaza scan vanilla pe raza 48."),
                click -> click.service().runCommand(click.player(), "ainpc world scan village 48")
            ));
            context.button(31, GuiButton.enabled(
                GuiItemFactory.item(Material.GRASS_BLOCK, "&6Demo mapping", "&7Creeaza mapping demo in jurul tau."),
                click -> click.service().runCommand(click.player(), "ainpc world demo create")
            ));
        }

        GuiNavigation.addStandardControls(context, key());
        context.fillEmpty(GuiItemFactory.filler());
    }

    private List<String> regionLore(WorldRegionInfo region) {
        if (region == null) {
            return List.of("&7Nicio regiune mapata aici.");
        }
        return List.of(
            "&7ID: &f" + region.id(),
            "&7Nume: &f" + region.name(),
            "&7Tip: &f" + region.typeId(),
            "&7Story state: &f" + region.storyStateKey(),
            "&7Tags: &f" + String.join(", ", region.tags())
        );
    }

    private List<String> placeLore(WorldPlaceInfo place) {
        if (place == null) {
            return List.of("&7Niciun place mapat aici.");
        }
        return List.of(
            "&7ID: &f" + place.id(),
            "&7Regiune: &f" + place.regionId(),
            "&7Nume: &f" + place.displayName(),
            "&7Tip: &f" + place.placeType().getId(),
            "&7Access: &f" + (place.publicAccess() ? "public" : "restrictionat")
        );
    }

    private List<String> nodeLore(WorldNodeInfo node) {
        if (node == null) {
            return List.of("&7Niciun node activ aici.");
        }
        List<String> lore = new ArrayList<>();
        lore.add("&7ID: &f" + node.id());
        lore.add("&7Regiune: &f" + node.regionId());
        if (!node.placeId().isBlank()) {
            lore.add("&7Place: &f" + node.placeId());
        }
        lore.add("&7Tip: &f" + node.typeId());
        lore.add("&7Raza: &f" + String.format("%.1f", node.radius()));
        return lore;
    }
}
