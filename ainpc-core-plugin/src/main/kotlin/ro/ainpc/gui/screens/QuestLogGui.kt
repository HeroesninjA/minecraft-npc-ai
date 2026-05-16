package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiClickContext
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.gui.QuestLogGuiFilter
import ro.ainpc.gui.QuestLogGuiPage
import ro.ainpc.progression.ProgressionGuiEntry
import ro.ainpc.progression.ProgressionGuiSnapshot
import ro.ainpc.progression.ProgressionObjectiveSnapshot

class QuestLogGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.QUEST

    override fun title(player: Player): String = "&0AINPC Progresii"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val adminView = context.player().hasPermission("ainpc.admin")
        val activeFilter = context.service().getQuestLogFilter(context.player())
        val snapshot: ProgressionGuiSnapshot =
            context.plugin().progressionService.getProgressionGuiSnapshot(context.player(), activeFilter, adminView)

        context.item(
            4,
            GuiItemFactory.item(
                Material.WRITABLE_BOOK,
                "&eLog progresii",
                listOf(
                    "&7Afiseaza snapshot-ul curent pentru jucator.",
                    "&7Filtru: &f${snapshot.filterLabel()}",
                    "&7Progresii curente: &f${snapshot.currentEntries().size}",
                    "&7Progresii arhivate vizibile: &f${snapshot.archivedEntries().size}"
                )
            )
        )
        renderFilters(context, activeFilter)

        val entries = snapshot.allEntries()
        val page = QuestLogGuiPage.fromEntries(
            entries,
            context.service().getQuestLogPage(context.player()),
            LOG_SLOTS.size
        )
        val limit = minOf(LOG_SLOTS.size, page.rows().size)
        for (index in 0 until limit) {
            val row = page.rows()[index]
            if (row.header()) {
                context.item(
                    LOG_SLOTS[index],
                    GuiItemFactory.item(
                        groupMaterial(row.groupId()),
                        "&6${GuiItemFactory.compact(row.groupLabel(), 32)}",
                        listOf(
                            "&7Grup mecanica",
                            "&7Intrari: &f${row.groupSize()}"
                        )
                    )
                )
            } else {
                val entry = row.entry() ?: continue
                context.button(
                    LOG_SLOTS[index],
                    GuiButton.enabled(
                        GuiItemFactory.item(entryMaterial(entry), entryTitle(entry), entryLore(entry)),
                        GuiAction { click -> handleEntryClick(click, entry, activeFilter) }
                    )
                )
            }
        }

        if (entries.isEmpty()) {
            context.item(22, GuiItemFactory.item(Material.LIGHT_GRAY_DYE, "&7Fara progresii", snapshot.summaryLines()))
        } else if (page.pageCount() > 1) {
            context.item(
                44,
                GuiItemFactory.item(
                    Material.HOPPER,
                    "&eLista trunchiata",
                    "&7Pagina &f${page.displayPage()}&7/&f${page.pageCount()}",
                    "&7Randuri grupate: &f${page.totalRows()}",
                    "&7Progresii: &f${page.totalEntries()}",
                    if (snapshot.totalMatchingArchived() > snapshot.archivedEntries().size)
                        "&7Arhivate ascunse de limita snapshot: &f" +
                            (snapshot.totalMatchingArchived() - snapshot.archivedEntries().size)
                    else
                        "&8Toate intrarile vizibile sunt incluse."
                )
            )
        }

        val trackableEntry = entries.stream().filter { e -> e.active() }.findFirst().orElse(null)
        val trackedEntry = entries.stream().filter { e -> e.tracked() }.findFirst().orElse(null)
        renderControls(context, page, adminView, trackableEntry, trackedEntry)
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun renderControls(
        context: GuiRenderContext,
        page: QuestLogGuiPage,
        adminView: Boolean,
        trackableEntry: ProgressionGuiEntry?,
        trackedEntry: ProgressionGuiEntry?
    ) {
        context.button(
            45,
            GuiButton.enabled(
                GuiItemFactory.item(Material.ARROW, "&eInapoi", "&7Revine la hub-ul principal."),
                GuiAction { click -> click.service().open(click.player(), GuiKey.MAIN) }
            )
        )
        context.button(
            46,
            if (page.hasPrevious()) {
                GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.ARROW,
                        "&ePagina anterioara",
                        "&7Pagina &f${page.displayPage()}&7/&f${page.pageCount()}"
                    ),
                    GuiAction { click -> click.service().openQuestLogPage(click.player(), page.pageIndex() - 1) }
                )
            } else {
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&7Pagina anterioara",
                        listOf("&7Esti deja la prima pagina.")
                    )
                )
            }
        )
        context.button(
            47,
            if (page.hasNext()) {
                GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.ARROW,
                        "&ePagina urmatoare",
                        "&7Pagina &f${page.displayPage()}&7/&f${page.pageCount()}"
                    ),
                    GuiAction { click -> click.service().openQuestLogPage(click.player(), page.pageIndex() + 1) }
                )
            } else {
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&7Pagina urmatoare",
                        listOf("&7Esti deja la ultima pagina.")
                    )
                )
            }
        )

        if (adminView) {
            context.button(
                48,
                GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.MAP,
                        "&6Ancore progresie",
                        "&7Listeaza ancorele persistate pentru progresii."
                    ),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest anchors all") }
                )
            )
        }

        context.button(
            49,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.SUNFLOWER,
                    "&aRefresh",
                    "&7Reincarca pagina curenta.",
                    "&7Pagina: &f${page.displayPage()}&7/&f${page.pageCount()}"
                ),
                GuiAction { click -> click.service().openQuestLogPage(click.player(), page.pageIndex()) }
            )
        )
        context.button(
            50,
            if (trackableEntry != null) {
                GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.COMPASS,
                        "&aUrmareste progresie activa",
                        "&7Porneste tracking persistent pentru prima",
                        "&7intrare activa din filtrul curent.",
                        "&7Comanda: &f/${trackableEntry.trackStartCommand()}"
                    ),
                    GuiAction { click -> click.service().runCommand(click.player(), trackableEntry.trackStartCommand()) }
                )
            } else {
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&7Urmareste progresie activa",
                        listOf("&7Nu exista progresie activa in filtrul curent.")
                    )
                )
            }
        )
        val stopTrackingCommand = trackedEntry?.trackStopCommand() ?: "ainpc progression track stop"
        context.button(
            51,
            GuiButton.enabled(
                GuiItemFactory.item(
                    if (trackedEntry != null) Material.COMPASS else Material.GRAY_DYE,
                    "&eOpreste tracking",
                    if (trackedEntry != null) {
                        listOf(
                            "&7Opreste progresia urmarita in filtrul curent.",
                            "&7Comanda: &f/$stopTrackingCommand"
                        )
                    } else {
                        listOf(
                            "&7Opreste busola/actionbar/particule pentru progresie.",
                            "&8Nu exista progresie urmarita vizibila in filtrul curent."
                        )
                    }
                ),
                GuiAction { click -> click.service().runCommand(click.player(), stopTrackingCommand) }
            )
        )
        context.button(
            53,
            GuiButton.enabled(
                GuiItemFactory.item(Material.BARRIER, "&cInchide", "&7Inchide interfata."),
                GuiAction { click -> click.player().closeInventory() }
            )
        )
    }

    private fun handleEntryClick(click: GuiClickContext, entry: ProgressionGuiEntry, activeFilter: String) {
        val selector = entry.guiDetailSelector()
        if (click.clickType().isShiftClick) {
            click.service().runCommand(click.player(), entry.command("status"))
            return
        }

        if (click.clickType().isRightClick) {
            if (entry.active()) {
                click.service().runCommand(
                    click.player(),
                    if (entry.tracked()) entry.trackStopCommand() else entry.trackStartCommand()
                )
            } else {
                click.service().runCommand(click.player(), entry.command("status"))
            }
            return
        }

        click.service().openQuestDetail(click.player(), selector, activeFilter)
    }

    private fun renderFilters(context: GuiRenderContext, activeFilter: String) {
        var slot = 9
        for (filter in QuestLogGuiFilter.primaryFilters()) {
            val selected = filter.matches(activeFilter)
            context.button(
                slot++,
                GuiButton.enabled(
                    GuiItemFactory.item(
                        filterMaterial(filter, selected),
                        if (selected) "&a${filter.buttonLabel()}" else "&f${filter.buttonLabel()}",
                        listOf(
                            if (selected) "&aFiltru curent." else "&7Click pentru filtrare.",
                            "&7Ruleaza view-ul: &f${filter.displayLabel()}"
                        )
                    ),
                    GuiAction { click -> click.service().openQuestLog(click.player(), filter.filter()) }
                )
            )
        }
    }

    private fun entryMaterial(entry: ProgressionGuiEntry): Material {
        if (entry.tracked()) {
            return Material.COMPASS
        }
        if (entry.active()) {
            return Material.LIME_DYE
        }
        if (entry.offered()) {
            return Material.WRITABLE_BOOK
        }
        if (entry.archived()) {
            return Material.MAP
        }
        return if (entry.missingTemplate()) Material.BARRIER else Material.PAPER
    }

    private fun groupMaterial(groupId: String?): Material =
        when (groupId ?: "") {
            "main_quests", "side_quests", "quest" -> Material.BOOKSHELF
            "village_contracts", "contract" -> Material.PAPER
            "npc_duties", "duty" -> Material.SHIELD
            "local_bounties", "bounty" -> Material.IRON_SWORD
            "village_events", "event" -> Material.BELL
            "onboarding", "tutorial" -> Material.COMPASS
            "village_rituals", "ritual" -> Material.AMETHYST_SHARD
            else -> Material.NAME_TAG
        }

    private fun filterMaterial(filter: QuestLogGuiFilter, selected: Boolean): Material {
        if (selected) {
            return Material.LIME_DYE
        }
        return when (filter) {
            QuestLogGuiFilter.ALL -> Material.BOOKSHELF
            QuestLogGuiFilter.ACTIVE -> Material.EMERALD
            QuestLogGuiFilter.QUEST -> Material.WRITABLE_BOOK
            QuestLogGuiFilter.CONTRACT -> Material.PAPER
            QuestLogGuiFilter.DUTY -> Material.SHIELD
            QuestLogGuiFilter.BOUNTY -> Material.IRON_SWORD
            QuestLogGuiFilter.EVENT -> Material.BELL
            QuestLogGuiFilter.TUTORIAL -> Material.COMPASS
            QuestLogGuiFilter.RITUAL -> Material.AMETHYST_SHARD
        }
    }

    private fun entryTitle(entry: ProgressionGuiEntry): String {
        val prefix = when {
            entry.tracked() -> "&b"
            entry.active() -> "&a"
            entry.offered() -> "&e"
            else -> "&f"
        }
        return prefix + GuiItemFactory.compact(entry.title(), 36)
    }

    private fun entryLore(entry: ProgressionGuiEntry): List<String> {
        val completeObjectives = entry.objectives().stream()
            .filter { objective: ProgressionObjectiveSnapshot -> objective.complete() }
            .count()
        val lore = java.util.ArrayList<String>()
        lore.add("&7Status: &f${entry.statusDisplay()}")
        if (entry.mechanicDisplay().isNotBlank()) {
            lore.add("&7Mecanica: &f${entry.mechanicDisplay()}")
        }
        lore.add("&7Categorie: &f${entry.categoryDisplay()}")
        if (entry.currentStageLabel().isNotBlank()) {
            lore.add("&7Stage: &f${entry.currentStageLabel()}")
        }
        if (entry.tracked()) {
            lore.add("&bProgresie urmarita")
        }
        if (entry.actorName().isNotBlank()) {
            lore.add("&7NPC: &f${entry.actorName()}")
        }
        if (entry.objectives().isNotEmpty()) {
            lore.add("&7Obiective: &f$completeObjectives&7/&f${entry.objectives().size}")
        }
        lore.add("&8Click: detalii progresie")
        if (entry.active()) {
            lore.add(if (entry.tracked()) "&8Right click: opreste tracking" else "&8Right click: urmareste progresia")
        } else {
            lore.add("&8Right click: status in chat")
        }
        lore.add("&8Shift click: status in chat")
        return lore
    }

    companion object {
        private val LOG_SLOTS = intArrayOf(
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        )
    }
}
