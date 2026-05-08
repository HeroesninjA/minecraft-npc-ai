package ro.ainpc.gui.screens;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import ro.ainpc.gui.GuiButton;
import ro.ainpc.gui.GuiItemFactory;
import ro.ainpc.gui.GuiKey;
import ro.ainpc.gui.GuiNavigation;
import ro.ainpc.gui.GuiRenderContext;
import ro.ainpc.gui.GuiScreen;
import ro.ainpc.progression.ProgressionGuiEntry;
import ro.ainpc.progression.ProgressionGuiSnapshot;
import ro.ainpc.progression.ProgressionObjectiveSnapshot;

import java.util.List;

public class QuestLogGui implements GuiScreen {

    private static final int[] LOG_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    @Override
    public GuiKey key() {
        return GuiKey.QUEST;
    }

    @Override
    public String title(Player player) {
        return "&0AINPC Questuri";
    }

    @Override
    public int size(Player player) {
        return 54;
    }

    @Override
    public void render(GuiRenderContext context) {
        boolean adminView = context.player().hasPermission("ainpc.admin");
        ProgressionGuiSnapshot snapshot =
            context.plugin().getProgressionService().getProgressionGuiSnapshot(context.player(), "", adminView);

        context.item(4, GuiItemFactory.item(
            Material.WRITABLE_BOOK,
            "&eQuest log",
            List.of(
                "&7Afiseaza snapshot-ul curent pentru jucator.",
                "&7Questuri curente: &f" + snapshot.currentEntries().size(),
                "&7Questuri arhivate vizibile: &f" + snapshot.archivedEntries().size()
            )
        ));

        List<ProgressionGuiEntry> entries = snapshot.currentEntries().isEmpty()
            ? snapshot.archivedEntries()
            : snapshot.currentEntries();
        int limit = Math.min(LOG_SLOTS.length, entries.size());
        for (int index = 0; index < limit; index++) {
            ProgressionGuiEntry entry = entries.get(index);
            context.button(LOG_SLOTS[index], GuiButton.enabled(
                GuiItemFactory.item(entryMaterial(entry), entryTitle(entry), entryLore(entry)),
                click -> click.service().openQuestDetail(click.player(), entry.selector())
            ));
        }

        if (entries.isEmpty()) {
            context.item(22, GuiItemFactory.item(Material.LIGHT_GRAY_DYE, "&7Fara questuri",
                snapshot.summaryLines()));
        } else if (entries.size() > LOG_SLOTS.length) {
            context.item(34, GuiItemFactory.item(Material.HOPPER, "&eLista trunchiata",
                "&7Sunt afisate primele &f" + LOG_SLOTS.length + " &7questuri."));
        }

        context.button(46, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPASS, "&aUrmareste quest curent",
                "&7Porneste tracking persistent pentru questul selectabil."),
            click -> click.service().runCommand(click.player(), "ainpc quest track start")
        ));
        context.button(47, GuiButton.enabled(
            GuiItemFactory.item(Material.GRAY_DYE, "&eOpreste tracking",
                "&7Opreste busola/actionbar/particule pentru quest."),
            click -> click.service().runCommand(click.player(), "ainpc quest track stop")
        ));

        if (adminView) {
            context.button(48, GuiButton.enabled(
                GuiItemFactory.item(Material.MAP, "&6Quest anchors",
                    "&7Listeaza ancorele persistate pentru questuri."),
                click -> click.service().runCommand(click.player(), "ainpc quest anchors all")
            ));
        }

        GuiNavigation.addStandardControls(context, key());
        context.fillEmpty(GuiItemFactory.filler());
    }

    private Material entryMaterial(ProgressionGuiEntry entry) {
        if (entry.tracked()) {
            return Material.COMPASS;
        }
        if (entry.active()) {
            return Material.LIME_DYE;
        }
        if (entry.offered()) {
            return Material.WRITABLE_BOOK;
        }
        if (entry.archived()) {
            return Material.MAP;
        }
        return entry.missingTemplate() ? Material.BARRIER : Material.PAPER;
    }

    private String entryTitle(ProgressionGuiEntry entry) {
        String prefix = entry.tracked() ? "&b" : entry.active() ? "&a" : entry.offered() ? "&e" : "&f";
        return prefix + GuiItemFactory.compact(entry.title(), 36);
    }

    private List<String> entryLore(ProgressionGuiEntry entry) {
        long completeObjectives = entry.objectives().stream()
            .filter(ProgressionObjectiveSnapshot::complete)
            .count();
        List<String> lore = new java.util.ArrayList<>();
        lore.add("&7Status: &f" + entry.statusDisplay());
        lore.add("&7Categorie: &f" + entry.categoryDisplay());
        if (!entry.currentStageLabel().isBlank()) {
            lore.add("&7Stage: &f" + entry.currentStageLabel());
        }
        if (entry.tracked()) {
            lore.add("&bQuest urmarit");
        }
        if (!entry.actorName().isBlank()) {
            lore.add("&7NPC: &f" + entry.actorName());
        }
        if (!entry.objectives().isEmpty()) {
            lore.add("&7Obiective: &f" + completeObjectives + "&7/&f" + entry.objectives().size());
        }
        lore.add("&8Click: detalii quest");
        return lore;
    }
}
