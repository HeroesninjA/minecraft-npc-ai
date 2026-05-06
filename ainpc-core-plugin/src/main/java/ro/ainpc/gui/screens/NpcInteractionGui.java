package ro.ainpc.gui.screens;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ro.ainpc.gui.GuiButton;
import ro.ainpc.gui.GuiItemFactory;
import ro.ainpc.gui.GuiKey;
import ro.ainpc.gui.GuiNavigation;
import ro.ainpc.gui.GuiRenderContext;
import ro.ainpc.gui.GuiScreen;
import ro.ainpc.npc.AINPC;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class NpcInteractionGui implements GuiScreen {

    private static final int[] NPC_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    @Override
    public GuiKey key() {
        return GuiKey.INTERACT;
    }

    @Override
    public String title(Player player) {
        return "&0AINPC Interactiune";
    }

    @Override
    public int size(Player player) {
        return 54;
    }

    @Override
    public void render(GuiRenderContext context) {
        Player player = context.player();
        Location location = player.getLocation();
        List<AINPC> nearbyNpcs = context.plugin().getNpcManager().getNPCsNear(location, 32.0).stream()
            .sorted(Comparator.comparingDouble(npc -> distanceSquared(location, npc.getLocation())))
            .limit(NPC_SLOTS.length)
            .toList();

        context.item(4, GuiItemFactory.item(
            Material.VILLAGER_SPAWN_EGG,
            "&aInteractiuni NPC",
            List.of(
                "&7NPC-uri in raza 32: &f" + nearbyNpcs.size(),
                "&7Click pe NPC: info.",
                "&7Right click pe NPC: quest status."
            )
        ));

        for (int index = 0; index < nearbyNpcs.size(); index++) {
            AINPC npc = nearbyNpcs.get(index);
            double distance = Math.sqrt(distanceSquared(location, npc.getLocation()));
            context.button(NPC_SLOTS[index], GuiButton.enabled(
                GuiItemFactory.item(Material.PLAYER_HEAD, "&f" + npc.getName(), List.of(
                    "&7Ocupatie: &f" + valueOrUnknown(npc.getOccupation()),
                    "&7Stare: &f" + npc.getCurrentState().getDisplayName(),
                    "&7Emotie: &f" + npc.getEmotions().getDominantEmotion(),
                    "&7Distanta: &f" + String.format(Locale.ROOT, "%.1f", distance),
                    "&8Click: info",
                    "&8Right click: quest status"
                )),
                click -> {
                    if (click.clickType().isRightClick()) {
                        click.service().runCommand(click.player(), "ainpc quest status " + npc.getName());
                    } else {
                        click.service().runCommand(click.player(), "ainpc info " + npc.getName());
                    }
                }
            ));
        }

        context.button(46, GuiButton.enabled(
            GuiItemFactory.item(Material.WRITABLE_BOOK, "&eQuest nearest", "&7Declanseaza quest-ul celui mai apropiat NPC."),
            click -> click.service().runCommand(click.player(), "ainpc quest nearest")
        ));
        context.button(47, GuiButton.enabled(
            GuiItemFactory.item(Material.MAP, "&bStory nearest", "&7Afiseaza context story pentru cel mai apropiat NPC."),
            click -> click.service().runCommand(click.player(), "ainpc story context " + click.player().getName() + " nearest")
        ));
        context.button(48, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPASS, "&aQuest status nearest", "&7Afiseaza statusul questului apropiat."),
            click -> click.service().runCommand(click.player(), "ainpc quest status nearest")
        ));

        GuiNavigation.addStandardControls(context, key());
        context.fillEmpty(GuiItemFactory.filler());
    }

    private double distanceSquared(Location playerLocation, Location npcLocation) {
        if (playerLocation == null || npcLocation == null || !playerLocation.getWorld().equals(npcLocation.getWorld())) {
            return Double.MAX_VALUE;
        }
        return playerLocation.distanceSquared(npcLocation);
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "necunoscut" : value;
    }
}
