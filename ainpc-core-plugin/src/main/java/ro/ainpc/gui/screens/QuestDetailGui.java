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
    private static final int DIAGNOSTIC_SLOT = 2;
    private static final int ACTION_HINT_SLOT = 6;

    @Override
    public GuiKey key() {
        return GuiKey.QUEST_DETAIL;
    }

    @Override
    public String title(Player player) {
        return "&0AINPC Progresie";
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
        String detailFilter = context.service().getQuestDetailFilter(context.player());
        ProgressionGuiSnapshot snapshot =
            context.plugin().getProgressionService().getProgressionGuiSnapshot(context.player(), detailFilter, adminView);
        Optional<ProgressionGuiEntry> optionalEntry = findEntry(snapshot, selector);
        if (optionalEntry.isEmpty() && !"all".equalsIgnoreCase(detailFilter)) {
            snapshot = context.plugin().getProgressionService().getProgressionGuiSnapshot(context.player(), "all", adminView);
            optionalEntry = findEntry(snapshot, selector);
        }
        if (optionalEntry.isEmpty()) {
            renderMissingQuest(context, selector, detailFilter);
            return;
        }

        ProgressionGuiEntry entry = optionalEntry.get();
        context.item(4, GuiItemFactory.item(
            headerMaterial(entry),
            "&e" + GuiItemFactory.compact(entry.title(), 40),
            headerLore(entry)
        ));

        renderDiagnosticCards(context, entry);
        renderObjectives(context, entry);
        renderStages(context, entry);
        renderRewards(context, entry);
        renderActions(context, entry, selector, detailFilter, adminView);
        context.fillEmpty(GuiItemFactory.filler());
    }

    private Optional<ProgressionGuiEntry> findEntry(ProgressionGuiSnapshot snapshot, String selector) {
        if (snapshot == null || selector == null || selector.isBlank()) {
            return Optional.empty();
        }
        String normalized = selector.trim();
        return snapshot.allEntries().stream()
            .filter(entry -> matches(normalized, entry.guiDetailSelector())
                || matches(normalized, entry.commandSelector())
                || matches(normalized, entry.selector())
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

    private void renderDiagnosticCards(GuiRenderContext context, ProgressionGuiEntry entry) {
        context.item(DIAGNOSTIC_SLOT, GuiItemFactory.item(
            Material.BOOK,
            "&bStatus runtime",
            compactLore(entry.statusLines(), "&8Nu exista linii de status in snapshot.", 5)
        ));
        context.item(ACTION_HINT_SLOT, GuiItemFactory.item(
            Material.OAK_SIGN,
            "&aActiuni sugerate",
            compactLore(entry.actionLines(), "&8Nu exista actiuni sugerate in snapshot.", 5)
        ));
    }

    private void renderActions(GuiRenderContext context,
                               ProgressionGuiEntry entry,
                               String selector,
                               String detailFilter,
                               boolean adminView) {
        context.button(45, GuiButton.enabled(
            GuiItemFactory.item(Material.ARROW, "&eInapoi", "&7Revine la log cu filtrul sursa."),
            click -> click.service().openQuestLog(click.player(), detailFilter)
        ));

        if (entry.active()) {
            context.button(46, GuiButton.enabled(
                GuiItemFactory.item(entry.tracked() ? Material.GRAY_DYE : Material.COMPASS,
                    entry.tracked() ? "&eOpreste tracking" : "&aUrmareste progresia",
                    entry.tracked()
                        ? "&7Ruleaza /" + entry.trackStopCommand() + "."
                        : "&7Ruleaza /" + entry.trackStartCommand() + "."),
                click -> click.service().runCommand(click.player(),
                    entry.tracked() ? entry.trackStopCommand() : entry.trackStartCommand())
            ));
        }

        context.button(47, GuiButton.enabled(
            GuiItemFactory.item(Material.WRITABLE_BOOK, "&bStatus in chat",
                "&7Ruleaza /" + entry.command("status") + "."),
            click -> click.service().runCommand(click.player(), entry.command("status"))
        ));

        context.button(51, GuiButton.enabled(
            GuiItemFactory.item(Material.FILLED_MAP, "&bProgres in chat",
                "&7Ruleaza /" + entry.command("progress") + "."),
            click -> click.service().runCommand(click.player(), entry.command("progress"))
        ));

        if (entry.active()) {
            context.button(48, GuiButton.enabled(
                GuiItemFactory.item(Material.REDSTONE_BLOCK, "&cAbandoneaza",
                    "&7Cere confirmare inainte de /" + entry.command("abandon") + "."),
                click -> click.service().openConfirmCommand(
                    click.player(),
                    "Abandoneaza progresia",
                    entry.command("abandon"),
                    GuiKey.QUEST_DETAIL,
                    selector,
                    List.of(
                        "&cProgresie: &f" + entry.title(),
                        "&7Progresul curent va fi marcat ca esuat/abandonat."
                    )
                )
            ));
        }

        context.button(49, GuiButton.enabled(
            GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", "&7Reincarca detaliile progresiei."),
            click -> click.service().openQuestDetail(click.player(), selector, detailFilter)
        ));

        if (adminView) {
            context.button(50, GuiButton.enabled(
                GuiItemFactory.item(Material.SPYGLASS, "&6Debug progresie",
                    "&7Ruleaza /" + entry.command("debug") + "."),
                click -> click.service().runCommand(click.player(), entry.command("debug"))
            ));
        }

        context.button(53, GuiButton.enabled(
            GuiItemFactory.item(Material.BARRIER, "&cInchide", "&7Inchide interfata."),
            click -> click.player().closeInventory()
        ));
    }

    private void renderMissingSelection(GuiRenderContext context) {
        context.item(22, GuiItemFactory.item(Material.BARRIER, "&cNicio progresie selectata",
            "&7Deschide log-ul si alege o progresie."));
        context.button(45, GuiButton.enabled(
            GuiItemFactory.item(Material.ARROW, "&eInapoi", "&7Revine la log-ul de progresii."),
            click -> click.service().openQuestLog(click.player(), click.service().getQuestDetailFilter(click.player()))
        ));
        context.fillEmpty(GuiItemFactory.filler());
    }

    private void renderMissingQuest(GuiRenderContext context, String selector, String detailFilter) {
        context.item(22, GuiItemFactory.item(Material.BARRIER, "&cProgresie indisponibila",
            "&7Nu mai gasesc selectorul: &f" + selector,
            "&7Progresia poate fi finalizata, abandonata sau reincarcata."));
        context.button(45, GuiButton.enabled(
            GuiItemFactory.item(Material.ARROW, "&eInapoi", "&7Revine la log cu filtrul sursa."),
            click -> click.service().openQuestLog(click.player(), detailFilter)
        ));
        context.button(49, GuiButton.enabled(
            GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", "&7Reincarca log-ul cu filtrul sursa."),
            click -> click.service().openQuestLog(click.player(), detailFilter)
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
            lore.add("&bProgresie urmarita");
        }
        if (!entry.actorName().isBlank()) {
            lore.add("&7NPC: &f" + entry.actorName());
        }
        if (entry.missingTemplate()) {
            lore.add("&cDefinitia progresiei nu este incarcata.");
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

    private List<String> compactLore(List<String> lines, String emptyLine, int maxLines) {
        if (lines == null || lines.isEmpty()) {
            return List.of(emptyLine);
        }

        List<String> lore = new ArrayList<>();
        int limit = Math.min(Math.max(1, maxLines), lines.size());
        for (int index = 0; index < limit; index++) {
            lore.add("&7" + GuiItemFactory.compact(lines.get(index), 44));
        }
        if (lines.size() > limit) {
            lore.add("&8+" + (lines.size() - limit) + " linii in chat/status.");
        }
        return lore;
    }
}
