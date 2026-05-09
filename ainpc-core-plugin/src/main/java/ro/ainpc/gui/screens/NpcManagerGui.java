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
import ro.ainpc.routine.RoutineAssignment;
import ro.ainpc.routine.RoutineSlot;

import java.util.ArrayList;
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
                    "&7Right click: /ainpc tp",
                    "&7Shift click: routine/family"
                )
        ));

        for (int index = 0; index < npcs.size(); index++) {
            AINPC npc = npcs.get(index);
            RoutineAssignment routine = context.plugin().getRoutineService().preview(npc);
            context.button(NPC_SLOTS[index], GuiButton.enabled(
                GuiItemFactory.item(cardMaterial(npc, routine.slot()), "&f" + npc.getName(), npcLore(npc, routine)),
                click -> {
                    if (click.clickType().isShiftClick() && click.clickType().isRightClick()) {
                        click.service().runCommand(click.player(), "ainpc family " + npc.getName());
                    } else if (click.clickType().isShiftClick()) {
                        click.service().runCommand(click.player(), "ainpc routine status " + npc.getName());
                    } else if (click.clickType().isRightClick()) {
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
            GuiItemFactory.item(Material.CLOCK, "&eRutine NPC", "&7Deschide preview-ul vizual al rutinelor."),
            click -> click.service().open(click.player(), GuiKey.ROUTINE)
        ));
        context.button(48, GuiButton.enabled(
            GuiItemFactory.item(Material.REDSTONE_TORCH, "&cAudit NPC", "&7Ruleaza audit NPC."),
            click -> click.service().runCommand(click.player(), "ainpc audit npc")
        ));
        context.button(50, GuiButton.enabled(
            GuiItemFactory.item(Material.REPEATER, "&6Ruleaza tick rutina", "&7Evalueaza manual rutinele NPC active."),
            click -> click.service().runCommand(click.player(), "ainpc routine tick")
        ));
        context.button(51, GuiButton.enabled(
            GuiItemFactory.item(Material.SPYGLASS, "&9Debugdump NPC", "&7Genereaza debugdump pentru runtime-ul NPC."),
            click -> click.service().runCommand(click.player(), "ainpc debugdump npc")
        ));

        GuiNavigation.addStandardControls(context, key());
        context.fillEmpty(GuiItemFactory.filler());
    }

    private List<String> npcLore(AINPC npc, RoutineAssignment routine) {
        List<String> lore = new ArrayList<>();
        lore.add("&7ID DB: &f" + npc.getDatabaseId());
        lore.add("&7Ocupatie: &f" + valueOrUnknown(npc.getOccupation()) + " &8/ &7varsta &f" + npc.getAge());
        lore.add("&7Spawned: &f" + (npc.isSpawned() ? "da" : "nu")
            + " &8/ &7stare &f" + npc.getCurrentState().getDisplayName());
        lore.add("&7Emotie: &f" + npc.getEmotions().getDominantEmotion());
        lore.add("&7Rutina: &f" + slotLabel(routine.slot())
            + " &8/ &7" + GuiItemFactory.compact(routine.activity(), 28));
        lore.add("&7Goal: &f" + GuiItemFactory.compact(firstNonBlank(npc.getCurrentGoal(), routine.goal()), 32));
        lore.add("&7Tinta rutina: &f" + formatOwnedLocation(routine.targetAnchor()));
        lore.add("&7Nevoi: &fsat " + npc.getHungerLevel()
            + " &7en &f" + npc.getEnergyLevel()
            + " &7sig &f" + npc.getSafetyLevel()
            + " &7conf &f" + npc.getComfortLevel());
        lore.add("&7Locatie: &f" + formatLocation(npc.getLocation()));
        lore.add("&8Ancore: home=" + shortAnchor(npc.getHomeAnchor())
            + " work=" + shortAnchor(npc.getWorkAnchor())
            + " social=" + shortAnchor(npc.getSocialAnchor()));
        lore.add("&8Click: info");
        lore.add("&8Right click: teleport");
        lore.add("&8Shift click: rutina");
        lore.add("&8Shift right click: familie");
        return lore;
    }

    private Material cardMaterial(AINPC npc, RoutineSlot slot) {
        if (!npc.isSpawned()) {
            return Material.GRAY_DYE;
        }
        return switch (slot) {
            case HOME -> Material.OAK_DOOR;
            case WORK -> Material.SMITHING_TABLE;
            case SOCIAL -> Material.BELL;
            case IDLE -> Material.PLAYER_HEAD;
        };
    }

    private String slotLabel(RoutineSlot slot) {
        return switch (slot) {
            case HOME -> "acasa";
            case WORK -> "lucru";
            case SOCIAL -> "social";
            case IDLE -> "idle";
        };
    }

    private String formatOwnedLocation(AINPC.OwnedLocation location) {
        if (location == null) {
            return "fara tinta";
        }
        return GuiItemFactory.compact(firstNonBlank(location.label(), location.type()) + " @ "
            + Math.round(location.x()) + ", " + Math.round(location.y()) + ", " + Math.round(location.z()), 32);
    }

    private String shortAnchor(AINPC.OwnedLocation location) {
        if (location == null) {
            return "-";
        }
        return GuiItemFactory.compact(firstNonBlank(location.label(), location.type()), 10);
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

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
