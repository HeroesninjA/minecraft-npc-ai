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
import ro.ainpc.routine.RoutineScheduleEntry;
import ro.ainpc.routine.RoutineSlot;
import ro.ainpc.world.NpcWorldBinding;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class RoutineGui implements GuiScreen {

    private static final int[] NPC_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    @Override
    public GuiKey key() {
        return GuiKey.ROUTINE;
    }

    @Override
    public String title(Player player) {
        return "&0AINPC Rutine";
    }

    @Override
    public int size(Player player) {
        return 54;
    }

    @Override
    public void render(GuiRenderContext context) {
        Player player = context.player();
        long worldTime = player.getWorld().getTime();
        boolean adminView = player.hasPermission("ainpc.admin");
        boolean routineEnabled = context.plugin().getConfig().getBoolean("routine.enabled", true);
        List<AINPC> npcs = context.plugin().getNpcManager().getAllNPCs().stream()
            .sorted(Comparator.comparing(npc -> npc.getName().toLowerCase(Locale.ROOT)))
            .limit(NPC_SLOTS.length)
            .toList();

        context.item(4, GuiItemFactory.item(
            routineEnabled ? Material.CLOCK : Material.GRAY_DYE,
            routineEnabled ? "&eRutine NPC" : "&7Rutine dezactivate",
            List.of(
                "&7NPC-uri afisate: &f" + npcs.size() + "&7/&f" + context.plugin().getNpcManager().getNPCCount(),
                "&7Timp world: &f" + formatWorldTime(worldTime),
                "&7Click card: status rutina.",
                "&7Right click card: info NPC."
            )
        ));

        for (int index = 0; index < npcs.size(); index++) {
            AINPC npc = npcs.get(index);
            RoutineAssignment current = context.plugin().getRoutineService().preview(npc);
            List<RoutineScheduleEntry> dayPreview = context.plugin().getRoutineService().getRoutineEngine().previewDay(npc);
            NpcWorldBinding binding = loadNpcWorldBinding(context, npc);
            context.button(NPC_SLOTS[index], GuiButton.enabled(
                GuiItemFactory.item(slotMaterial(current.slot()), "&f" + npc.getName(), npcLore(npc, current, dayPreview, binding, adminView)),
                click -> {
                    if (adminView && click.clickType().isShiftClick()) {
                        click.service().runCommand(click.player(), "ainpc world bindings npc " + npc.getDatabaseId());
                    } else if (!adminView || click.clickType().isRightClick()) {
                        click.service().runCommand(click.player(), "ainpc info " + npc.getName());
                    } else {
                        click.service().runCommand(click.player(), "ainpc routine status " + npc.getName());
                    }
                }
            ));
        }

        if (npcs.isEmpty()) {
            context.item(22, GuiItemFactory.item(
                Material.LIGHT_GRAY_DYE,
                "&7Fara NPC-uri",
                "&7Nu exista NPC-uri incarcate pentru preview de rutina."
            ));
        }

        context.button(46, adminView
            ? GuiButton.enabled(
                GuiItemFactory.item(Material.COMPASS, "&eStatus nearest", "&7Ruleaza /ainpc routine status pentru cel mai apropiat NPC."),
                click -> click.service().runCommand(click.player(), "ainpc routine status")
            )
            : GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Status nearest",
                List.of("&7Necesita ainpc.admin.")))
        );
        context.button(47, adminView
            ? GuiButton.enabled(
                GuiItemFactory.item(Material.REPEATER, "&6Ruleaza tick rutina", "&7Ruleaza manual evaluarea rutinelor pentru NPC-urile active."),
                click -> click.service().runCommand(click.player(), "ainpc routine tick")
            )
            : GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Ruleaza tick rutina",
                List.of("&7Necesita ainpc.admin.")))
        );
        context.button(48, context.service().canOpen(player, GuiKey.MANAGER)
            ? GuiButton.enabled(
                GuiItemFactory.item(Material.NAME_TAG, "&6Manager NPC", "&7Deschide managerul NPC admin."),
                click -> click.service().open(click.player(), GuiKey.MANAGER)
            )
            : GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Manager NPC",
                List.of("&7Necesita permisiune pentru manager.")))
        );

        GuiNavigation.addStandardControls(context, key());
        context.fillEmpty(GuiItemFactory.filler());
    }

    private List<String> npcLore(AINPC npc,
                                 RoutineAssignment current,
                                 List<RoutineScheduleEntry> dayPreview,
                                 NpcWorldBinding binding,
                                 boolean adminView) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Slot curent: &f" + slotLabel(current.slot()));
        lore.add("&7Activitate: &f" + GuiItemFactory.compact(current.activity(), 32));
        lore.add("&7Goal: &f" + GuiItemFactory.compact(current.goal(), 32));
        lore.add("&7Stare tinta: &f" + current.targetState().name());
        lore.add("&7Tinta: &f" + formatOwnedLocation(current.targetAnchor()));
        addBindingLore(lore, binding);
        lore.add("&7Spawned: &f" + (npc.isSpawned() ? "da" : "nu"));
        lore.add("&7Locatie: &f" + formatLocation(npc.getLocation()));
        lore.add("&8Program zi:");
        for (RoutineScheduleEntry entry : dayPreview) {
            RoutineAssignment assignment = entry.assignment();
            lore.add("&8- &7" + entry.label() + ": &f" + slotLabel(assignment.slot())
                + " &8/ &7" + GuiItemFactory.compact(assignment.activity(), 22));
        }
        lore.add(adminView ? "&8Click: status rutina" : "&8Click: info NPC");
        lore.add("&8Right click: info NPC");
        if (adminView) {
            lore.add("&8Shift click: world bindings");
        }
        return lore;
    }

    private NpcWorldBinding loadNpcWorldBinding(GuiRenderContext context, AINPC npc) {
        if (context.plugin().getNpcWorldBindingService() == null || npc.getDatabaseId() <= 0) {
            return null;
        }
        try {
            return context.plugin().getNpcWorldBindingService()
                .getBinding(npc.getDatabaseId())
                .orElse(null);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private void addBindingLore(List<String> lore, NpcWorldBinding binding) {
        if (binding == null) {
            lore.add("&7Mapping place: &8nepersistat");
            return;
        }
        lore.add("&7Mapping place: &f" + compactBindingTriple(
            binding.homePlaceId(),
            binding.workPlaceId(),
            binding.socialPlaceId()
        ));
        lore.add("&7Mapping node: &f" + compactBindingTriple(
            binding.homeNodeId(),
            binding.workNodeId(),
            binding.socialNodeId()
        ));
    }

    private String compactBindingTriple(String home, String work, String social) {
        return GuiItemFactory.compact("H=" + shortValue(home)
            + " W=" + shortValue(work)
            + " S=" + shortValue(social), 36);
    }

    private String shortValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private Material slotMaterial(RoutineSlot slot) {
        return switch (slot) {
            case HOME -> Material.OAK_DOOR;
            case WORK -> Material.SMITHING_TABLE;
            case SOCIAL -> Material.BELL;
            case IDLE -> Material.LIGHT_GRAY_DYE;
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
        String label = location.label() == null || location.label().isBlank() ? location.type() : location.label();
        return GuiItemFactory.compact(label + " @ "
            + Math.round(location.x()) + ", " + Math.round(location.y()) + ", " + Math.round(location.z()), 34);
    }

    private String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "necunoscuta";
        }
        return location.getWorld().getName() + " "
            + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    private String formatWorldTime(long worldTime) {
        long normalized = ((worldTime % 24000L) + 24000L) % 24000L;
        long hours = (normalized / 1000L + 6L) % 24L;
        long minutes = Math.round((normalized % 1000L) * 60.0D / 1000.0D);
        if (minutes == 60L) {
            minutes = 0L;
            hours = (hours + 1L) % 24L;
        }
        return String.format(Locale.ROOT, "%02d:%02d (%d ticks)", hours, minutes, normalized);
    }
}
