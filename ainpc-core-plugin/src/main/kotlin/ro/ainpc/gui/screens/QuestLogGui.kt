package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.debug.DebugDumpQuestText
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
import java.text.SimpleDateFormat
import java.util.Date

class QuestLogGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.QUEST

    override fun title(player: Player): String = "&0AINPC Progresii"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val adminView = context.player().hasPermission("ainpc.admin")
        val activeFilter = context.service().getQuestLogFilter(context.player())
        val snapshot: ProgressionGuiSnapshot =
            context.plugin().progressionService.getProgressionGuiSnapshot(context.player(), activeFilter, adminView)

        val snapshotTime = SimpleDateFormat("HH:mm:ss").format(Date(System.currentTimeMillis()))
        context.item(
            4,
            GuiItemFactory.item(
                Material.WRITABLE_BOOK,
                "&eLog progresii",
                buildQuestLogStatusLines(snapshot, activeFilter, adminView) + listOf("&8Actualizat: $snapshotTime")
            )
        )
        renderProgressSummary(context, snapshot)
        renderAuthoringSummary(context, snapshot)
        renderQuestDiagnostics(context)
        renderBaseFilters(context, activeFilter)
        renderAdvancedFilters(context, activeFilter)

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
                        GuiItemFactory.item(entryMaterial(entry), entryTitle(entry), entryLore(entry))
                    ) { click -> handleEntryClick(click, entry, activeFilter) }
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

        val trackableEntry = entries.firstOrNull { it.active() }
        val trackedEntry = entries.firstOrNull { it.tracked() }
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
                GuiItemFactory.item(Material.ARROW, "&eInapoi", "&7Revine la hub-ul principal.")
            ) { click -> click.service().open(click.player(), GuiKey.MAIN) }
        )
        context.button(
            46,
            if (page.hasPrevious()) {
                GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.ARROW,
                        "&ePagina anterioara",
                        "&7Pagina &f${page.displayPage()}&7/&f${page.pageCount()}"
                    )
                ) { click -> click.service().openQuestLogPage(click.player(), page.pageIndex() - 1) }
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
                    )
                ) { click -> click.service().openQuestLogPage(click.player(), page.pageIndex() + 1) }
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
                    )
                ) { click -> click.service().runCommand(click.player(), "ainpc quest anchors all") }
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
                )
            ) { click -> click.service().openQuestLogPage(click.player(), page.pageIndex()) }
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
                    )
                ) { click -> click.service().runCommand(click.player(), trackableEntry.trackStartCommand()) }
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
                )
            ) { click -> click.service().runCommand(click.player(), stopTrackingCommand) }
        )
        context.button(
            53,
            GuiButton.enabled(
                GuiItemFactory.item(Material.BARRIER, "&cInchide", "&7Inchide interfata."),
            ) { click -> click.player().closeInventory() }
        )
    }

    private fun renderAuthoringSummary(context: GuiRenderContext, snapshot: ProgressionGuiSnapshot) {
        val selectedEntry = snapshot.currentEntries().firstOrNull() ?: snapshot.allEntries().firstOrNull()
        val storyContext = context.plugin().storyContextService.buildForPlayer(context.player())
        val authoringSnapshot = context.plugin().authoringService.analyze(
            storyContext,
            context.plugin().progressionService.getDefinitions(),
            selectedEntry?.selector(),
            selectedEntry?.mechanicId(),
            storyContext.worldContext().currentRegion()?.id(),
            storyContext.worldContext().currentPlace()?.id(),
            true,
            emptyList()
        )
        context.button(
            7,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.ENCHANTED_BOOK,
                    "&bAuthoring",
                    listOf(
                        "&7Decision: &f${authoringSnapshot.decisionStatus()}",
                        "&7Reason: &f${valueOrUnknown(authoringSnapshot.decisionReason())}",
                        cooldownLine(authoringSnapshot, storyContext.storySignals()),
                        "&7Selector: &f${valueOrUnknown(authoringSnapshot.requestedQuestSelector)}",
                        "&7Mechanic: &f${valueOrUnknown(authoringSnapshot.requestedMechanicId)}",
                        "&7Warnings: &f${authoringSnapshot.warnings.size}",
                        "&8Click: deschide authoring GUI."
                    )
                )
            ) { click ->
                click.service().openAuthoring(click.player(), selectedEntry?.selector(), selectedEntry?.mechanicId())
            }
        )
    }

    private fun renderProgressSummary(context: GuiRenderContext, snapshot: ProgressionGuiSnapshot) {
        val allEntries = snapshot.allEntries()
        val currentEntries = snapshot.currentEntries()
        val totalObjectives = allEntries.sumOf { it.objectives().size }
        val completedObjectives = allEntries.sumOf { it.objectives().count { obj -> obj.complete() } }
        val activeCount = currentEntries.count { it.active() }
        val trackedCount = currentEntries.count { it.tracked() }
        val completionPct = if (totalObjectives > 0) (completedObjectives * 100 / totalObjectives) else 0

        context.item(
            2,
            GuiItemFactory.item(
                Material.EXPERIENCE_BOTTLE,
                "&bSumar progres",
                listOf(
                    "&7Active: &f$activeCount &7| Tracked: &f$trackedCount",
                    "&7Obiective: &f$completedObjectives&7/&f$totalObjectives &7($completionPct%)",
                    "&7Curente: &f${currentEntries.size} &7| Arhivate: &f${snapshot.archivedEntries().size}",
                    "&8Include toate intrarile din snapshot-ul curent."
                )
            )
        )
        renderStoryContextCard(context, snapshot)
    }

    private fun renderStoryContextCard(context: GuiRenderContext, snapshot: ProgressionGuiSnapshot) {
        val storyContext = context.plugin().storyContextService.buildForPlayer(context.player())
        val currentRegion = storyContext.worldContext().currentRegion()
        val currentPlace = storyContext.worldContext().currentPlace()
        val recentQuestEvents = storyContext.recentStoryEvents().filter { it.eventType().startsWith("quest_") }
        val questSignals = storyContext.storySignals().filter {
            it.startsWith("recent_quest_") || it.startsWith("last_quest_")
        }
        val regionState = try {
            val service = context.plugin().storyStateService
            if (currentRegion != null) {
                service.getRegionState(currentRegion.id()).orElse(null)
            } else null
        } catch (_: Exception) { null }
        context.item(
            6,
            GuiItemFactory.item(
                if (regionState != null) Material.AMETHYST_SHARD else Material.GRAY_DYE,
                "&dContext poveste",
                listOf(
                    "&7Quest Entries Active: &f${snapshot.currentEntries().size}",
                    "&7Regiune: &f${currentRegion?.id() ?: "<nemapata>"}",
                    "&7Story State: &f${regionState?.stateKey() ?: "<nepersistat>"}",
                    "&7Place: &f${currentPlace?.id() ?: "<nemapat>"}",
                    "&7Ancore active: &f${storyContext.activeQuestAnchors().size}",
                    "&7Quest Events Recente: &f${recentQuestEvents.size}",
                    "&7Quest Signals: &f${questSignals.size}",
                    if (questSignals.isNotEmpty()) "&7Ultimul quest signal: &f${GuiItemFactory.compact(questSignals.first(), 32)}" else "&7Ultimul quest signal: &f<none>",
                    if (recentQuestEvents.isNotEmpty()) "&7Ultimul quest event: &f${recentQuestEvents.first().eventType()} ${valueOrUnknown(recentQuestEvents.first().eventKey())}" else "&7Ultimul quest event: &f<none>",
                    "&7Warnings: &f${storyContext.warnings().size}"
                )
            )
        )
    }

    private fun renderQuestDiagnostics(context: GuiRenderContext) {
        context.button(
            8,
            if (context.player().hasPermission("ainpc.admin")) {
                GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.PAPER,
                        "&dQuest Diagnostics",
                        questDiagnosticsLore(context.plugin())
                    ),
                ) { click -> click.service().runCommand(click.player(), "ainpc debugdump quest") }
            } else {
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&7Quest Diagnostics",
                        questDiagnosticsLore(context.plugin())
                    )
                )
            }
        )
    }

    private fun questDiagnosticsLore(plugin: AINPCPlugin): List<String> {
        return DebugDumpQuestText.buildQuestText(plugin)
            .lineSequence()
            .filter { it.isNotBlank() }
            .take(6)
            .map { "&7$it" }
            .toList()
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

    private fun renderBaseFilters(context: GuiRenderContext, activeFilter: String) {
        var slot = 9
        val baseFilters = listOf(
            QuestLogGuiFilter.ALL,
            QuestLogGuiFilter.ACTIVE,
            QuestLogGuiFilter.QUEST,
            QuestLogGuiFilter.CONTRACT
        )
        for (filter in baseFilters) {
            val selected = filter.matches(activeFilter)
            context.button(
                slot++,
                GuiButton.enabled(
                    GuiItemFactory.item(
                        filterMaterial(filter, selected),
                        if (selected) "&a${filter.buttonLabel()}" else "&f${filter.buttonLabel()}",
                        listOf(
                            if (selected) "&aFiltru curent." else "&7Click pentru filtrare.",
                            "&7Filtru de baza: &f${filter.displayLabel()}"
                        )
                    )
                ) { click -> click.service().openQuestLog(click.player(), filter.filter()) }
            )
        }
    }

    private fun renderAdvancedFilters(context: GuiRenderContext, activeFilter: String) {
        val advancedFilters = listOf(
            QuestLogGuiFilter.DUTY,
            QuestLogGuiFilter.BOUNTY,
            QuestLogGuiFilter.EVENT,
            QuestLogGuiFilter.TUTORIAL,
            QuestLogGuiFilter.RITUAL
        )
        var slot = 14
        for (filter in advancedFilters) {
            val selected = filter.matches(activeFilter)
            context.button(
                slot++,
                GuiButton.enabled(
                    GuiItemFactory.item(
                        filterMaterial(filter, selected),
                        if (selected) "&d${filter.buttonLabel()}" else "&f${filter.buttonLabel()}",
                        listOf(
                            if (selected) "&dFiltru curent." else "&7Click pentru filtrare.",
                            "&7Filtru avansat: &f${filter.displayLabel()}"
                        )
                    )
                ) { click -> click.service().openQuestLog(click.player(), filter.filter()) }
            )
        }
    }

    private fun entryMaterial(entry: ProgressionGuiEntry): Material {
        if (entry.tracked()) return Material.COMPASS
        if (entry.active()) {
            return when (entry.commandRoot()) {
                "quest" -> Material.WRITABLE_BOOK
                "contract" -> Material.PAPER
                "duty" -> Material.SHIELD
                "bounty" -> Material.IRON_SWORD
                "event" -> Material.BELL
                "tutorial" -> Material.COMPASS
                "ritual" -> Material.AMETHYST_SHARD
                else -> Material.LIME_DYE
            }
        }
        if (entry.offered()) return Material.BOOK
        if (entry.archived()) return Material.MAP
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
        val totalObjectives = entry.objectives().size
        val completeObjectives = entry.objectives().count { it.complete() }
        val lore = mutableListOf<String>()
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
        if (totalObjectives > 0) {
            val pct = (completeObjectives * 100) / totalObjectives
            val bar = progressBar(pct)
            lore.add("&7Obiective: &f$completeObjectives&7/&f$totalObjectives &8$pct%")
            lore.add(" &8$bar")
        }
        val rewards = entry.rewardLines()
        if (rewards.isNotEmpty()) {
            lore.add("&6Recompense:")
            for (reward in rewards.take(3)) {
                lore.add("&7- &f${GuiItemFactory.compact(reward, 32)}")
            }
            if (rewards.size > 3) {
                lore.add("  &8... si ${rewards.size - 3} altele")
            }
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

    private fun progressBar(pct: Int): String {
        val segments = 16
        val filled = (pct * segments) / 100
        val empty = segments - filled
        val sb = StringBuilder("&a")
        repeat(filled) { sb.append('\u2588') }
        sb.append("&7")
        repeat(empty) { sb.append('\u2588') }
        return sb.toString()
    }

    private fun valueOrUnknown(value: String): String = value.ifBlank { "unknown" }

    private fun cooldownLine(authoringSnapshot: ro.ainpc.engine.QuestAuthoringSnapshot, storySignals: List<String>): String {
        return if (authoringSnapshot.decisionReason() == "structure_quest_generation_cooldown") {
            val category = signalValue(storySignals, "quest_generation_cooldown_category")
            val remaining = signalValue(storySignals, "quest_generation_cooldown_remaining_ms").toLongOrNull()
            val detail = buildList {
                if (category.isNotBlank()) add(category)
                if (remaining != null) add(formatDuration(remaining))
            }.joinToString(", ")
            if (detail.isBlank()) {
                "&7Quest Generation: &ein cooldown dupa o structura recenta"
            } else {
                "&7Quest Generation: &ein cooldown ($detail)"
            }
        } else {
            "&7Quest Generation: &aactiv"
        }
    }

    private fun signalValue(signals: List<String>, prefix: String): String {
        return signals.firstOrNull { it.startsWith("$prefix=") }
            ?.substringAfter("=")
            .orEmpty()
    }

    private fun formatDuration(milliseconds: Long): String {
        val seconds = (milliseconds / 1000).coerceAtLeast(0)
        return "${seconds}s"
    }

    private fun buildQuestLogStatusLines(
        snapshot: ProgressionGuiSnapshot,
        activeFilter: String,
        adminView: Boolean
    ): List<String> {
        val currentCount = snapshot.currentEntries().size
        val archivedVisible = snapshot.archivedEntries().size
        val trackedCount = snapshot.currentEntries().count { it.tracked() }
        val activeCount = snapshot.currentEntries().count { it.active() }
        val summary = snapshot.summaryLines().take(2)

        return buildList {
            val offeredCount = snapshot.currentEntries().count { it.offered() }
            add("&7Afiseaza snapshot-ul curent pentru jucator.")
            add("&7Filtru: &f${snapshot.filterLabel()}")
            add("&7Curente: &f$currentCount &8($activeCount active, $offeredCount oferite)")
            add("&7Urmarite: &f$trackedCount")
            add("&7Progresii arhivate vizibile: &f$archivedVisible")
            if (snapshot.totalMatchingArchived() > archivedVisible) {
                add("&7Arhivate ascunse: &f${snapshot.totalMatchingArchived() - archivedVisible}")
            }
            if (summary.isNotEmpty()) {
                add("&8Rezumat:")
                summary.forEach { add("&8- $it") }
            }
            add(if (adminView) "&8Actiuni principale: detalii / track / refresh / anchors" else "&8Actiuni principale: detalii / track / refresh")
            add("&7Filtru curent: &f${activeFilter.ifBlank { "all" }}")
        }
    }

    companion object {
        private val LOG_SLOTS = intArrayOf(
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        )
    }
}
