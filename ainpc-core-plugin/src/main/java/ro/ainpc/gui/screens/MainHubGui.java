package ro.ainpc.gui.screens;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ro.ainpc.api.WorldAdminApi;
import ro.ainpc.gui.GuiButton;
import ro.ainpc.gui.GuiItemFactory;
import ro.ainpc.gui.GuiKey;
import ro.ainpc.gui.GuiRenderContext;
import ro.ainpc.gui.GuiScreen;

import java.util.ArrayList;
import java.util.List;

public class MainHubGui implements GuiScreen {

    @Override
    public GuiKey key() {
        return GuiKey.MAIN;
    }

    @Override
    public String title(Player player) {
        return "&0AINPC Hub";
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

        context.item(4, GuiItemFactory.item(
            Material.NETHER_STAR,
            "&6AINPC Hub",
            List.of(
                "&7Jucator: &f" + player.getName(),
                "&7NPC-uri incarcate: &f" + context.plugin().getNpcManager().getNPCCount(),
                "&7World mapping: &f" + worldAdmin.getRegionCount() + " regiuni, "
                    + worldAdmin.getPlaceCount() + " places, " + worldAdmin.getNodeCount() + " noduri",
                "&7Locatie: &f" + location.getWorld().getName() + " "
                    + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ()
            )
        ));

        openButton(context, 10, GuiKey.QUEST, Material.WRITABLE_BOOK, "&eQuesturi",
            List.of("&7Quest log, tracking si status rapid."));
        openButton(context, 11, GuiKey.INTERACT, Material.VILLAGER_SPAWN_EGG, "&aInteractiune NPC",
            List.of("&7NPC-uri apropiate si actiuni rapide."));
        openButton(context, 12, GuiKey.WORLD, Material.COMPASS, "&bWorld",
            List.of("&7Regiune, place, noduri si context local."));
        openButton(context, 13, GuiKey.STATS, Material.CLOCK, "&dStatistici",
            List.of("&7Snapshot personal si NPC-uri din apropiere."));
        openButton(context, 14, GuiKey.SHOP, Material.EMERALD, "&2Shop NPC",
            List.of("&7Intrare pregatita pentru economie/shop."));

        openButton(context, 28, GuiKey.MANAGER, Material.NAME_TAG, "&6Manager NPC",
            List.of("&7Lista NPC admin, info si teleport."));
        openButton(context, 29, GuiKey.AUDIT, Material.REDSTONE_TORCH, "&cAudit",
            List.of("&7Ruleaza audituri operationale."));
        openButton(context, 30, GuiKey.DEBUG, Material.SPYGLASS, "&9Debug",
            List.of("&7Debugdump si test OpenAI."));

        if (context.service().canOpen(player, GuiKey.DEBUG)) {
            context.button(32, GuiButton.enabled(
                GuiItemFactory.item(Material.ENDER_EYE, "&bTest OpenAI", "&7Ruleaza /ainpc test."),
                click -> click.service().runCommand(click.player(), "ainpc test")
            ));
        }

        context.button(49, GuiButton.enabled(
            GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", "&7Reincarca hub-ul."),
            click -> click.service().open(click.player(), GuiKey.MAIN)
        ));
        context.button(53, GuiButton.enabled(
            GuiItemFactory.item(Material.BARRIER, "&cInchide", "&7Inchide interfata."),
            click -> click.player().closeInventory()
        ));
        context.fillEmpty(GuiItemFactory.filler());
    }

    private void openButton(GuiRenderContext context,
                            int slot,
                            GuiKey target,
                            Material material,
                            String title,
                            List<String> lore) {
        if (context.service().canOpen(context.player(), target)) {
            context.button(slot, GuiButton.enabled(
                GuiItemFactory.item(material, title, lore),
                click -> click.service().open(click.player(), target)
            ));
            return;
        }

        List<String> lockedLore = new ArrayList<>(lore);
        lockedLore.add("&8Necesita permisiune pentru " + target.displayName() + ".");
        context.button(slot, GuiButton.disabled(GuiItemFactory.disabled(Material.BARRIER, title, lockedLore)));
    }
}
