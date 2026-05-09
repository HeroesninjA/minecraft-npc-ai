package ro.ainpc.gui.screens;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import ro.ainpc.gui.GuiButton;
import ro.ainpc.gui.GuiClickContext;
import ro.ainpc.gui.GuiItemFactory;
import ro.ainpc.gui.GuiKey;
import ro.ainpc.gui.QuestLogGuiFilter;
import ro.ainpc.gui.QuestLogGuiPage;
import ro.ainpc.gui.GuiRenderContext;
import ro.ainpc.gui.GuiScreen;
import ro.ainpc.progression.ProgressionGuiEntry;
import ro.ainpc.progression.ProgressionGuiSnapshot;
import ro.ainpc.progression.ProgressionObjectiveSnapshot;

import java.util.List;

public class QuestLogGui implements GuiScreen {

    private static final int[] LOG_SLOTS = {
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    @Override
    public GuiKey key() {
        return GuiKey.QUEST;
    }

    @Override
    public String title(Player player) {
        return "&0AINPC Progresii";
    }

    @Override
    public int size(Player player) {
        return 54;
    }

    @Override
    public void render(GuiRenderContext context) {
        boolean adminView = context.player().hasPermission("ainpc.admin");
        String activeFilter = context.service().getQuestLogFilter(context.player());
        ProgressionGuiSnapshot snapshot =
            context.plugin().getProgressionService().getProgressionGuiSnapshot(context.player(), activeFilter, adminView);

        context.item(4, GuiItemFactory.item(
            Material.WRITABLE_BOOK,
            "&eLog progresii",
            List.of(
                "&7Afiseaza snapshot-ul curent pentru jucator.",
                "&7Filtru: &f" + snapshot.filterLabel(),
                "&7Progresii curente: &f" + snapshot.currentEntries().size(),
                "&7Progresii arhivate vizibile: &f" + snapshot.archivedEntries().size()
            )
        ));
        renderFilters(context, activeFilter);

        List<ProgressionGuiEntry> entries = snapshot.allEntries();
        QuestLogGuiPage page = QuestLogGuiPage.fromEntries(
            entries,
            context.service().getQuestLogPage(context.player()),
            LOG_SLOTS.length
        );
        int limit = Math.min(LOG_SLOTS.length, page.rows().size());
        for (int index = 0; index < limit; index++) {
            QuestLogGuiPage.Row row = page.rows().get(index);
            if (row.header()) {
                context.item(LOG_SLOTS[index], GuiItemFactory.item(
                    groupMaterial(row.groupId()),
                    "&6" + GuiItemFactory.compact(row.groupLabel(), 32),
                    List.of(
                        "&7Grup mecanica",
                        "&7Intrari: &f" + row.groupSize()
                    )
                ));
            } else {
                ProgressionGuiEntry entry = row.entry();
                context.button(LOG_SLOTS[index], GuiButton.enabled(
                    GuiItemFactory.item(entryMaterial(entry), entryTitle(entry), entryLore(entry)),
                    click -> handleEntryClick(click, entry, activeFilter)
                ));
            }
        }

        if (entries.isEmpty()) {
            context.item(22, GuiItemFactory.item(Material.LIGHT_GRAY_DYE, "&7Fara progresii",
                snapshot.summaryLines()));
        } else if (page.pageCount() > 1) {
            context.item(44, GuiItemFactory.item(Material.HOPPER, "&eLista trunchiata",
                "&7Pagina &f" + page.displayPage() + "&7/&f" + page.pageCount(),
                "&7Randuri grupate: &f" + page.totalRows(),
                "&7Progresii: &f" + page.totalEntries(),
                snapshot.totalMatchingArchived() > snapshot.archivedEntries().size()
                    ? "&7Arhivate ascunse de limita snapshot: &f"
                        + (snapshot.totalMatchingArchived() - snapshot.archivedEntries().size())
                    : "&8Toate intrarile vizibile sunt incluse."));
        }

        ProgressionGuiEntry trackableEntry = entries.stream()
            .filter(ProgressionGuiEntry::active)
            .findFirst()
            .orElse(null);
        ProgressionGuiEntry trackedEntry = entries.stream()
            .filter(ProgressionGuiEntry::tracked)
            .findFirst()
            .orElse(null);
        renderControls(context, page, adminView, trackableEntry, trackedEntry);
        context.fillEmpty(GuiItemFactory.filler());
    }

    private void renderControls(GuiRenderContext context,
                                QuestLogGuiPage page,
                                boolean adminView,
                                ProgressionGuiEntry trackableEntry,
                                ProgressionGuiEntry trackedEntry) {
        context.button(45, GuiButton.enabled(
            GuiItemFactory.item(Material.ARROW, "&eInapoi", "&7Revine la hub-ul principal."),
            click -> click.service().open(click.player(), GuiKey.MAIN)
        ));
        context.button(46, page.hasPrevious()
            ? GuiButton.enabled(
                GuiItemFactory.item(Material.ARROW, "&ePagina anterioara",
                    "&7Pagina &f" + page.displayPage() + "&7/&f" + page.pageCount()),
                click -> click.service().openQuestLogPage(click.player(), page.pageIndex() - 1)
            )
            : GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Pagina anterioara",
                List.of("&7Esti deja la prima pagina.")))
        );
        context.button(47, page.hasNext()
            ? GuiButton.enabled(
                GuiItemFactory.item(Material.ARROW, "&ePagina urmatoare",
                    "&7Pagina &f" + page.displayPage() + "&7/&f" + page.pageCount()),
                click -> click.service().openQuestLogPage(click.player(), page.pageIndex() + 1)
            )
            : GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Pagina urmatoare",
                List.of("&7Esti deja la ultima pagina.")))
        );

        if (adminView) {
            context.button(48, GuiButton.enabled(
                GuiItemFactory.item(Material.MAP, "&6Ancore progresie",
                    "&7Listeaza ancorele persistate pentru progresii."),
                click -> click.service().runCommand(click.player(), "ainpc quest anchors all")
            ));
        }

        context.button(49, GuiButton.enabled(
            GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh",
                "&7Reincarca pagina curenta.",
                "&7Pagina: &f" + page.displayPage() + "&7/&f" + page.pageCount()),
            click -> click.service().openQuestLogPage(click.player(), page.pageIndex())
        ));
        context.button(50, trackableEntry != null
            ? GuiButton.enabled(
                GuiItemFactory.item(Material.COMPASS, "&aUrmareste progresie activa",
                    "&7Porneste tracking persistent pentru prima",
                    "&7intrare activa din filtrul curent.",
                    "&7Comanda: &f/" + trackableEntry.trackStartCommand()),
                click -> click.service().runCommand(click.player(), trackableEntry.trackStartCommand())
            )
            : GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Urmareste progresie activa",
                List.of("&7Nu exista progresie activa in filtrul curent.")))
        );
        String stopTrackingCommand = trackedEntry != null
            ? trackedEntry.trackStopCommand()
            : "ainpc progression track stop";
        context.button(51, GuiButton.enabled(
            GuiItemFactory.item(trackedEntry != null ? Material.COMPASS : Material.GRAY_DYE, "&eOpreste tracking",
                trackedEntry != null
                    ? List.of(
                        "&7Opreste progresia urmarita in filtrul curent.",
                        "&7Comanda: &f/" + stopTrackingCommand
                    )
                    : List.of(
                        "&7Opreste busola/actionbar/particule pentru progresie.",
                        "&8Nu exista progresie urmarita vizibila in filtrul curent."
                    )),
            click -> click.service().runCommand(click.player(), stopTrackingCommand)
        ));
        context.button(53, GuiButton.enabled(
            GuiItemFactory.item(Material.BARRIER, "&cInchide", "&7Inchide interfata."),
            click -> click.player().closeInventory()
        ));
    }

    private void handleEntryClick(GuiClickContext click, ProgressionGuiEntry entry, String activeFilter) {
        String selector = entry.guiDetailSelector();
        if (click.clickType().isShiftClick()) {
            click.service().runCommand(click.player(), entry.command("status"));
            return;
        }

        if (click.clickType().isRightClick()) {
            if (entry.active()) {
                click.service().runCommand(click.player(),
                    entry.tracked() ? entry.trackStopCommand() : entry.trackStartCommand());
            } else {
                click.service().runCommand(click.player(), entry.command("status"));
            }
            return;
        }

        click.service().openQuestDetail(click.player(), selector, activeFilter);
    }

    private void renderFilters(GuiRenderContext context, String activeFilter) {
        int slot = 9;
        for (QuestLogGuiFilter filter : QuestLogGuiFilter.primaryFilters()) {
            boolean selected = filter.matches(activeFilter);
            context.button(slot++, GuiButton.enabled(
                GuiItemFactory.item(
                    filterMaterial(filter, selected),
                    selected ? "&a" + filter.buttonLabel() : "&f" + filter.buttonLabel(),
                    List.of(
                        selected ? "&aFiltru curent." : "&7Click pentru filtrare.",
                        "&7Ruleaza view-ul: &f" + filter.displayLabel()
                    )
                ),
                click -> click.service().openQuestLog(click.player(), filter.filter())
            ));
        }
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

    private Material groupMaterial(String groupId) {
        return switch (groupId == null ? "" : groupId) {
            case "main_quests", "side_quests", "quest" -> Material.BOOKSHELF;
            case "village_contracts", "contract" -> Material.PAPER;
            case "npc_duties", "duty" -> Material.SHIELD;
            case "local_bounties", "bounty" -> Material.IRON_SWORD;
            case "village_events", "event" -> Material.BELL;
            case "onboarding", "tutorial" -> Material.COMPASS;
            case "village_rituals", "ritual" -> Material.AMETHYST_SHARD;
            default -> Material.NAME_TAG;
        };
    }

    private Material filterMaterial(QuestLogGuiFilter filter, boolean selected) {
        if (selected) {
            return Material.LIME_DYE;
        }
        return switch (filter) {
            case ALL -> Material.BOOKSHELF;
            case ACTIVE -> Material.EMERALD;
            case QUEST -> Material.WRITABLE_BOOK;
            case CONTRACT -> Material.PAPER;
            case DUTY -> Material.SHIELD;
            case BOUNTY -> Material.IRON_SWORD;
            case EVENT -> Material.BELL;
            case TUTORIAL -> Material.COMPASS;
            case RITUAL -> Material.AMETHYST_SHARD;
        };
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
        if (!entry.mechanicDisplay().isBlank()) {
            lore.add("&7Mecanica: &f" + entry.mechanicDisplay());
        }
        lore.add("&7Categorie: &f" + entry.categoryDisplay());
        if (!entry.currentStageLabel().isBlank()) {
            lore.add("&7Stage: &f" + entry.currentStageLabel());
        }
        if (entry.tracked()) {
            lore.add("&bProgresie urmarita");
        }
        if (!entry.actorName().isBlank()) {
            lore.add("&7NPC: &f" + entry.actorName());
        }
        if (!entry.objectives().isEmpty()) {
            lore.add("&7Obiective: &f" + completeObjectives + "&7/&f" + entry.objectives().size());
        }
        lore.add("&8Click: detalii progresie");
        if (entry.active()) {
            lore.add(entry.tracked() ? "&8Right click: opreste tracking" : "&8Right click: urmareste progresia");
        } else {
            lore.add("&8Right click: status in chat");
        }
        lore.add("&8Shift click: status in chat");
        return lore;
    }
}
