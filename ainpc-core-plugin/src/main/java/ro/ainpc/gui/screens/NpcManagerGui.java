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

public class NpcManagerGui implements GuiScreen {

    private static final int[] NPC_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    @Override
    public GuiKey key() {
        return GuiKey.MANAGER;
    }

    @Override
    public String title(Player player) {
        return "&0AINPC Manager NPC";
    }

    @Override
    public int size(Player player) {
        return 54;
    }

    @Override
    public void render(GuiRenderContext context) {
        List<AINPC> npcs = context.plugin().getNpcManager().getAllNPCs().stream()
            .sorted(Comparator.comparing(npc -> npc.getName().toLowerCase(Locale.ROOT)))
            .limit(NPC_SLOTS.length)
            .toList();

        context.item(4, GuiItemFactory.item(
            Material.NAME_TAG,
            "&6Manager NPC",
            List.of(
                "&7Total NPC-uri: &f" + context.plugin().getNpcManager().getNPCCount(),
                "&7Click: /ainpc info",
                "&7Right click: /ainpc tp"
            )
        ));

        for (int index = 0; index < npcs.size(); index++) {
            AINPC npc = npcs.get(index);
            Location location = npc.getLocation();
            context.button(NPC_SLOTS[index], GuiButton.enabled(
                GuiItemFactory.item(Material.PLAYER_HEAD, "&f" + npc.getName(), List.of(
                    "&7ID DB: &f" + npc.getDatabaseId(),
                    "&7Ocupatie: &f" + valueOrUnknown(npc.getOccupation()),
                    "&7Spawned: &f" + (npc.isSpawned() ? "da" : "nu"),
                    "&7Locatie: &f" + formatLocation(location),
                    "&8Click: info",
                    "&8Right click: teleport"
                )),
                click -> {
                    if (click.clickType().isRightClick()) {
                        click.service().runCommand(click.player(), "ainpc tp " + npc.getName());
                    } else {
                        click.service().runCommand(click.player(), "ainpc info " + npc.getName());
                    }
                }
            ));
        }

        context.button(46, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&aLista NPC", "&7Ruleaza /ainpc list."),
            click -> click.service().runCommand(click.player(), "ainpc list")
        ));
        context.button(47, GuiButton.enabled(
            GuiItemFactory.item(Material.CLOCK, "&eRoutine status", "&7Verifica rutina NPC-urilor."),
            click -> click.service().runCommand(click.player(), "ainpc routine status")
        ));
        context.button(48, GuiButton.enabled(
            GuiItemFactory.item(Material.REDSTONE_TORCH, "&cAudit NPC", "&7Ruleaza audit NPC."),
            click -> click.service().runCommand(click.player(), "ainpc audit npc")
        ));

        GuiNavigation.addStandardControls(context, key());
        context.fillEmpty(GuiItemFactory.filler());
    }

    private String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "necunoscuta";
        }
        return location.getWorld().getName() + " "
            + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "necunoscut" : value;
    }
}
