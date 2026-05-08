package ro.ainpc.gui.screens;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import ro.ainpc.gui.GuiButton;
import ro.ainpc.gui.GuiItemFactory;
import ro.ainpc.gui.GuiKey;
import ro.ainpc.gui.GuiRenderContext;
import ro.ainpc.gui.GuiScreen;
import ro.ainpc.progression.ProgressionGuiEntry;
import ro.ainpc.progression.ProgressionGuiSnapshot;
import ro.ainpc.progression.ProgressionObjectiveSnapshot;
import ro.ainpc.progression.ProgressionStageSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuestDetailGui implements GuiScreen {

    private static final int[] OBJECTIVE_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25
    };
    private static final int[] STAGE_SLOTS = {28, 29, 30, 31, 32, 33, 34};
    private static final int[] REWARD_SLOTS = {37, 38, 39, 40, 41, 42, 43};

    @Override
    public GuiKey key() {
        return GuiKey.QUEST_DETAIL;
    }

    @Override
    public String title(Player player) {
        return "&0AINPC Quest Detalii";
    }

    @Override
    public int size(Player player) {
        return 54;
    }

    @Override
    public void render(GuiRenderContext context) {
        String selector = context.service().getQuestDetailSelector(context.player());
        if (selector.isBlank()) {
            renderMissingSelection(context);
            return;
        }

        boolean adminView = context.player().hasPermission("ainpc.admin");
        ProgressionGuiSnapshot snapshot =
            context.plugin().getProgressionService().getProgressionGuiSnapshot(context.player(), "all", adminView);
        Optional<ProgressionGuiEntry> optionalEntry = findEntry(snapshot, selector);
        if (optionalEntry.isEmpty()) {
            renderMissingQuest(context, selector);
            return;
        }

        ProgressionGuiEntry entry = optionalEntry.get();
        context.item(4, GuiItemFactory.item(
            headerMaterial(entry),
            "&e" + GuiItemFactory.compact(entry.title(), 40),
            headerLore(entry)
        ));

        renderObjectives(context, entry);
        renderStages(context, entry);
        renderRewards(context, entry);
        renderActions(context, entry, selector, adminView);
        context.fillEmpty(GuiItemFactory.filler());
    }

    private Optional<ProgressionGuiEntry> findEntry(ProgressionGuiSnapshot snapshot, String selector) {
        if (snapshot == null || selector == null || selector.isBlank()) {
            return Optional.empty();
        }
        String normalized = selector.trim();
        return snapshot.allEntries().stream()
            .filter(entry -> matches(normalized, entry.selector())
                || matches(normalized, entry.progressionId())
                || matches(normalized, entry.mechanicId() + ":" + entry.code())
                || matches(normalized, entry.mechanicId() + ":" + entry.definitionId())
                || matches(normalized, entry.kind() + ":" + entry.code())
                || matches(normalized, entry.kind() + ":" + entry.definitionId())
                || matches(normalized, entry.code())
                || matches(normalized, entry.templateId())
                || matches(normalized, entry.definitionId()))
            .findFirst();
    }

    private boolean matches(String left, String right) {
        return left != null && right != null && !right.isBlank() && left.equalsIgnoreCase(right);
    }

    private void renderObjectives(GuiRenderContext context, ProgressionGuiEntry entry) {
        int limit = Math.min(OBJECTIVE_SLOTS.length, entry.objectives().size());
        for (int index = 0; index < limit; index++) {
            ProgressionObjectiveSnapshot objective = entry.objectives().get(index);
            context.item(OBJECTIVE_SLOTS[index], GuiItemFactory.item(
                objectiveMaterial(objective),
                objective.complete() ? "&a" + objective.label() : objective.active() ? "&e" + objective.label() : "&7" + objective.label(),
                objectiveLore(objective)
            ));
        }
    }

    private void renderStages(GuiRenderContext context, ProgressionGuiEntry entry) {
        int limit = Math.min(STAGE_SLOTS.length, entry.stages().size());
        for (int index = 0; index < limit; index++) {
            ProgressionStageSnapshot stage = entry.stages().get(index);
            context.item(STAGE_SLOTS[index], GuiItemFactory.item(
                stage.active() ? Material.AMETHYST_SHARD : stage.complete() ? Material.EMERALD : Material.PAPER,
                stage.active() ? "&d" + stage.label() : stage.complete() ? "&a" + stage.label() : "&f" + stage.label(),
                stageLore(stage)
            ));
        }
    }

    private void renderRewards(GuiRenderContext context, ProgressionGuiEntry entry) {
        int limit = Math.min(REWARD_SLOTS.length, entry.rewardLines().size());
        for (int index = 0; index < limit; index++) {
            String reward = entry.rewardLines().get(index);
            context.item(REWARD_SLOTS[index], GuiItemFactory.item(
                Material.EMERALD,
                "&a" + GuiItemFactory.compact(reward, 36),
                GuiItemFactory.wrapLore(reward, "&7")
            ));
        }
    }

    private void renderActions(GuiRenderContext context,
                               ProgressionGuiEntry entry,
                               String selector,
                               boolean adminView) {
        context.button(45, GuiButton.enabled(
            GuiItemFactory.item(Material.ARROW, "&eInapoi", "&7Revine la quest log."),
            click -> click.service().open(click.player(), GuiKey.QUEST)
        ));

        if (entry.active()) {
            context.button(46, GuiButton.enabled(
                GuiItemFactory.item(entry.tracked() ? Material.GRAY_DYE : Material.COMPASS,
                    entry.tracked() ? "&eOpreste tracking" : "&aUrmareste quest",
                    entry.tracked() ? "&7Ruleaza /ainpc quest track stop." : "&7Ruleaza /ainpc quest track start."),
                click -> click.service().runCommand(click.player(),
                    entry.tracked() ? "ainpc quest track stop" : "ainpc quest track start " + selector)
            ));
        }

        context.button(47, GuiButton.enabled(
            GuiItemFactory.item(Material.WRITABLE_BOOK, "&bStatus in chat", "&7Ruleaza /ainpc quest status."),
            click -> click.service().runCommand(click.player(), "ainpc quest status " + selector)
        ));

        if (entry.active()) {
            context.button(48, GuiButton.enabled(
                GuiItemFactory.item(Material.REDSTONE_BLOCK, "&cAbandoneaza",
                    "&7Cere confirmare inainte de /ainpc quest abandon."),
                click -> click.service().openConfirmCommand(
                    click.player(),
                    "Abandoneaza quest",
                    "ainpc quest abandon " + selector,
                    GuiKey.QUEST_DETAIL,
                    selector,
                    List.of(
                        "&cQuest: &f" + entry.title(),
                        "&7Progresul curent va fi marcat ca esuat/abandonat."
                    )
                )
            ));
        }

        context.button(49, GuiButton.enabled(
            GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", "&7Reincarca detaliile questului."),
            click -> click.service().openQuestDetail(click.player(), selector)
        ));

        if (adminView) {
            context.button(50, GuiButton.enabled(
                GuiItemFactory.item(Material.SPYGLASS, "&6Debug quest", "&7Ruleaza /ainpc quest debug."),
                click -> click.service().runCommand(click.player(), "ainpc quest debug " + selector)
            ));
        }

        context.button(53, GuiButton.enabled(
            GuiItemFactory.item(Material.BARRIER, "&cInchide", "&7Inchide interfata."),
            click -> click.player().closeInventory()
        ));
    }

    private void renderMissingSelection(GuiRenderContext context) {
        context.item(22, GuiItemFactory.item(Material.BARRIER, "&cNiciun quest selectat",
            "&7Deschide quest log-ul si alege un quest."));
        context.button(45, GuiButton.enabled(
            GuiItemFactory.item(Material.ARROW, "&eInapoi", "&7Revine la quest log."),
            click -> click.service().open(click.player(), GuiKey.QUEST)
        ));
        context.fillEmpty(GuiItemFactory.filler());
    }

    private void renderMissingQuest(GuiRenderContext context, String selector) {
        context.item(22, GuiItemFactory.item(Material.BARRIER, "&cQuest indisponibil",
            "&7Nu mai gasesc selectorul: &f" + selector,
            "&7Questul poate fi finalizat, abandonat sau reincarcat."));
        context.button(45, GuiButton.enabled(
            GuiItemFactory.item(Material.ARROW, "&eInapoi", "&7Revine la quest log."),
            click -> click.service().open(click.player(), GuiKey.QUEST)
        ));
        context.button(49, GuiButton.enabled(
            GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", "&7Reincarca quest log-ul."),
            click -> click.service().open(click.player(), GuiKey.QUEST)
        ));
        context.fillEmpty(GuiItemFactory.filler());
    }

    private Material headerMaterial(ProgressionGuiEntry entry) {
        if (entry.tracked()) {
            return Material.COMPASS;
        }
        if (entry.active()) {
            return Material.LIME_DYE;
        }
        if (entry.offered()) {
            return Material.WRITABLE_BOOK;
        }
        return entry.archived() ? Material.MAP : Material.PAPER;
    }

    private List<String> headerLore(ProgressionGuiEntry entry) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Status: &f" + entry.statusDisplay());
        if (!entry.mechanicDisplay().isBlank()) {
            lore.add("&7Mecanica: &f" + entry.mechanicDisplay());
        }
        lore.add("&7Categorie: &f" + entry.categoryDisplay());
        if (!entry.currentStageLabel().isBlank()) {
            lore.add("&7Stage curent: &f" + entry.currentStageLabel());
        }
        if (entry.tracked()) {
            lore.add("&bQuest urmarit");
        }
        if (!entry.actorName().isBlank()) {
            lore.add("&7NPC: &f" + entry.actorName());
        }
        if (entry.missingTemplate()) {
            lore.add("&cTemplate-ul questului nu este incarcat.");
        }
        return lore;
    }

    private Material objectiveMaterial(ProgressionObjectiveSnapshot objective) {
        if ("failed".equalsIgnoreCase(objective.stateId())) {
            return Material.REDSTONE;
        }
        if (objective.complete()) {
            return Material.LIME_DYE;
        }
        if ("in_progress".equalsIgnoreCase(objective.stateId())) {
            return Material.ORANGE_DYE;
        }
        return objective.active() ? Material.YELLOW_DYE : Material.LIGHT_GRAY_DYE;
    }

    private List<String> objectiveLore(ProgressionObjectiveSnapshot objective) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Stare: &f" + objective.stateDisplay());
        lore.add("&7Progres: &f" + objective.currentAmount() + "&7/&f" + objective.requiredAmount());
        lore.add("&7Tip: &f" + objective.type());
        if (!objective.stageLabel().isBlank()) {
            lore.add("&7Stage: &f" + objective.stageLabel());
        }
        lore.add(objective.active() ? "&eActiv in etapa curenta." : "&8Inactiv in etapa curenta.");
        lore.addAll(GuiItemFactory.wrapLore(objective.description(), "&7"));
        return lore;
    }

    private List<String> stageLore(ProgressionStageSnapshot stage) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Status: &f" + (stage.active() ? "curent" : stage.complete() ? "complet" : "in asteptare"));
        lore.add("&7Completare: &f" + stage.completionMode());
        if (!stage.nextStageId().isBlank()) {
            lore.add("&7Next: &f" + stage.nextStageId());
        }
        if (!stage.objectiveIds().isEmpty()) {
            lore.add("&7Objectives: &f" + String.join(", ", stage.objectiveIds()));
        }
        lore.addAll(GuiItemFactory.wrapLore(stage.description(), "&7"));
        return lore;
    }
}
