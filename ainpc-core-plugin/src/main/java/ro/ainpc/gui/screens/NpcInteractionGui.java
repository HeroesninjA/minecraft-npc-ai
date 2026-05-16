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
import ro.ainpc.progression.ProgressionGuiEntry;
import ro.ainpc.progression.ProgressionGuiSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
        boolean adminView = player.hasPermission("ainpc.admin");
        ProgressionGuiSnapshot progressionSnapshot =
            context.plugin().getProgressionService().getProgressionGuiSnapshot(player, "all", adminView);
        List<AINPC> nearbyNpcs = context.plugin().getNpcManager().getNPCsNear(location, 32.0).stream()
            .sorted(Comparator.comparingDouble(npc -> distanceSquared(location, npc.getLocation())))
            .limit(NPC_SLOTS.length)
            .toList();
        NearbyProgression nearestProgression = nearestProgression(nearbyNpcs, progressionSnapshot);

        context.item(4, GuiItemFactory.item(
            Material.VILLAGER_SPAWN_EGG,
            "&aInteractiuni NPC",
            List.of(
                "&7NPC-uri in raza 32: &f" + nearbyNpcs.size(),
                "&7Progresii vizibile: &f" + progressionSnapshot.allEntries().size(),
                "&7Click pe card: info NPC.",
                "&7Right click pe card: status progresie.",
                "&7Shift click pe card: detalii progresie.",
                "&7Pentru dialog direct: click dreapta pe NPC in lume."
            )
        ));

        for (int index = 0; index < nearbyNpcs.size(); index++) {
            AINPC npc = nearbyNpcs.get(index);
            double distance = Math.sqrt(distanceSquared(location, npc.getLocation()));
            ProgressionGuiEntry progression = primaryProgressionForNpc(progressionSnapshot, npc).orElse(null);
            context.button(NPC_SLOTS[index], GuiButton.enabled(
                GuiItemFactory.item(Material.PLAYER_HEAD, "&f" + npc.getName(), npcLore(npc, progression, distance)),
                click -> {
                    if (progression != null && click.clickType().isShiftClick()) {
                        click.service().openQuestDetail(
                            click.player(),
                            progression.guiDetailSelector(),
                            progression.guiFilter()
                        );
                    } else if (click.clickType().isRightClick()) {
                        click.service().runCommand(click.player(), progression != null
                            ? progression.command("status")
                            : "ainpc quest status " + npc.getName());
                    } else {
                        click.service().runCommand(click.player(), "ainpc info " + npc.getName());
                    }
                }
            ));
        }

        String nearestRoot = nearestProgression.entry() != null
            ? nearestProgression.entry().commandRoot()
            : "quest";
        String nearestStatusCommand = nearestProgression.entry() != null
            ? nearestProgression.entry().command("status")
            : "ainpc quest status nearest";

        context.button(46, GuiButton.enabled(
            GuiItemFactory.item(Material.WRITABLE_BOOK, "&eProgresie nearest",
                "&7Declanseaza progresia relevanta a celui",
                "&7mai apropiat NPC cu progres vizibil.",
                "&7Comanda: &f/ainpc " + nearestRoot + " nearest"),
            click -> click.service().runCommand(click.player(), "ainpc " + nearestRoot + " nearest")
        ));
        context.button(47, GuiButton.enabled(
            GuiItemFactory.item(Material.LIME_DYE, "&aAccepta nearest",
                "&7Accepta oferta celui mai apropiat NPC",
                "&7pentru mecanica relevanta.",
                "&7Comanda: &f/ainpc " + nearestRoot + " accept nearest"),
            click -> click.service().runCommand(click.player(), "ainpc " + nearestRoot + " accept nearest")
        ));
        context.button(48, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPASS, "&aStatus nearest",
                "&7Afiseaza statusul progresiei apropiate.",
                "&7Comanda: &f/" + nearestStatusCommand),
            click -> click.service().runCommand(click.player(), nearestStatusCommand)
        ));
        context.button(50, GuiButton.enabled(
            GuiItemFactory.item(Material.MAP, "&bStory nearest", "&7Afiseaza context story pentru cel mai apropiat NPC."),
            click -> click.service().runCommand(click.player(), "ainpc story context " + click.player().getName() + " nearest")
        ));
        context.button(51, GuiButton.enabled(
            GuiItemFactory.item(Material.CLOCK, "&eRutina nearest", "&7Afiseaza programul celui mai apropiat NPC."),
            click -> click.service().runCommand(click.player(), "ainpc routine status")
        ));

        GuiNavigation.addStandardControls(context, key());
        context.fillEmpty(GuiItemFactory.filler());
    }

    private List<String> npcLore(AINPC npc, ProgressionGuiEntry progression, double distance) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Ocupatie: &f" + valueOrUnknown(npc.getOccupation()));
        lore.add("&7Stare: &f" + npc.getCurrentState().getDisplayName());
        lore.add("&7Rutina: &f" + valueOrUnknown(npc.getPlannedRoutineActivity()));
        lore.add("&7Emotie: &f" + npc.getEmotions().getDominantEmotion());
        lore.add("&7Distanta: &f" + String.format(Locale.ROOT, "%.1f", distance));
        if (progression != null) {
            lore.add("&7Progresie: &f" + GuiItemFactory.compact(progression.title(), 28));
            lore.add("&7Mecanica: &f" + valueOrUnknown(progression.mechanicDisplay()));
            lore.add("&7Status progresie: &f" + valueOrUnknown(progression.statusDisplay()));
        } else {
            lore.add("&8Nu exista progresie vizibila pentru acest NPC.");
        }
        lore.add("&8Click: info");
        lore.add(progression != null
            ? "&8Right click: status " + progression.commandRoot()
            : "&8Right click: quest status fallback");
        if (progression != null) {
            lore.add("&8Shift click: detalii progresie");
        }
        return lore;
    }

    private NearbyProgression nearestProgression(List<AINPC> nearbyNpcs, ProgressionGuiSnapshot snapshot) {
        if (nearbyNpcs == null || nearbyNpcs.isEmpty()) {
            return new NearbyProgression(null, null);
        }

        for (AINPC npc : nearbyNpcs) {
            Optional<ProgressionGuiEntry> progression = primaryProgressionForNpc(snapshot, npc);
            if (progression.isPresent()) {
                return new NearbyProgression(npc, progression.get());
            }
        }
        return new NearbyProgression(nearbyNpcs.get(0), null);
    }

    private Optional<ProgressionGuiEntry> primaryProgressionForNpc(ProgressionGuiSnapshot snapshot, AINPC npc) {
        if (snapshot == null || npc == null) {
            return Optional.empty();
        }

        return snapshot.allEntries().stream()
            .filter(entry -> actorMatchesNpc(entry, npc))
            .min(Comparator
                .comparingInt(this::progressionPriority)
                .thenComparing(ProgressionGuiEntry::updatedAt, Comparator.reverseOrder())
                .thenComparing(ProgressionGuiEntry::title, String.CASE_INSENSITIVE_ORDER));
    }

    private int progressionPriority(ProgressionGuiEntry entry) {
        if (entry.tracked()) {
            return 0;
        }
        if (entry.current()) {
            return 1;
        }
        if (entry.active()) {
            return 2;
        }
        if (entry.offered()) {
            return 3;
        }
        if (entry.archived()) {
            return 5;
        }
        return 4;
    }

    private boolean actorMatchesNpc(ProgressionGuiEntry entry, AINPC npc) {
        String actorName = normalize(entry.actorName());
        if (actorName.isBlank()) {
            return false;
        }
        return actorName.equals(normalize(npc.getName()))
            || actorName.equals(normalize(npc.getDisplayName()));
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

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record NearbyProgression(AINPC npc, ProgressionGuiEntry entry) {
    }
}
