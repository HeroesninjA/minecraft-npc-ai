package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.debug.DebugDumpAuthoringText
import ro.ainpc.engine.QuestAuthoringSnapshot
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen

class QuestAuthoringGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.AUTHORING

    override fun title(player: Player): String = "&0AINPC Authoring"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        val adminView = player.hasPermission("ainpc.admin")
        val storyContext = context.plugin().storyContextService.buildForPlayer(player)
        val progressionSnapshot = context.plugin().progressionService.getProgressionGuiSnapshot(player, "all", adminView)
        val selectedEntry = progressionSnapshot.currentEntries().firstOrNull()
        val requestedQuestSelector = context.service().getAuthoringQuestSelector(player)
        val requestedMechanicId = context.service().getAuthoringMechanicId(player)
        val aiMode = context.service().getCreatorFormValue(player, "qa_ai_mode").ifBlank { "" }
        val aiSelector = context.service().getCreatorFormValue(player, "qa_ai_selector").ifBlank { "" }
        val aiMechanic = context.service().getCreatorFormValue(player, "qa_ai_mechanic").ifBlank { "" }
        val aiSummary = context.service().getCreatorFormValue(player, "qa_ai_summary").ifBlank { "" }
        val authoringSnapshot = context.plugin().authoringService.analyze(
            storyContext,
            context.plugin().progressionService.getDefinitions(),
            requestedQuestSelector.ifBlank { selectedEntry?.selector().orEmpty() },
            requestedMechanicId.ifBlank { selectedEntry?.mechanicId().orEmpty() },
            storyContext.worldContext().currentRegion()?.id(),
            storyContext.worldContext().currentPlace()?.id(),
            true,
            emptyList()
        )

        context.item(
            4,
            GuiItemFactory.item(
                Material.WRITABLE_BOOK,
                "&6Quest Authoring",
                listOf(
                    "&7Read-only bridge intre story, mapping si progresie.",
                    "&7Jucator: &f${player.name}",
                    "&7Selector: &f${valueOrUnknown(authoringSnapshot.requestedQuestSelector)}",
                    "&7Mechanic: &f${valueOrUnknown(authoringSnapshot.requestedMechanicId)}",
                    "&7Entries progresie: &f${progressionSnapshot.allEntries().size}",
                    "&7Story Signals: &f${storyContext.storySignals().size}",
                    "&7Warnings: &f${authoringSnapshot.warnings.size}",
                    cooldownLore(authoringSnapshot, storyContext.storySignals()),
                    if (aiMode.isBlank()) "&7AI preset: &f(nu)" else "&7AI preset: &f$aiMode",
                    if (aiSelector.isBlank()) "&7AI selector: &f(nu)" else "&7AI selector: &f$aiSelector",
                    if (aiMechanic.isBlank()) "&7AI mechanic: &f(nu)" else "&7AI mechanic: &f$aiMechanic",
                    if (aiSummary.isBlank()) "&7AI summary: &f(nu)" else "&7AI summary: &f$aiSummary",
                    "&8Actiuni principale: next / prev / reset / dump"
                )
            )
        )
        if (ro.ainpc.commands.isRuntimeReadOnly(context.plugin())) {
            context.item(5, GuiItemFactory.item(Material.BARRIER, "&cRead-only activ", listOf(
                "&7MCP ruleaza in mod read-only.",
                "&7Authoring writes sunt blocate.",
                "&8Inspectia si preview-ul raman disponibile."
            )))
        }

        context.item(
            10,
            GuiItemFactory.item(
                Material.MAP,
                "&bStory Context",
                storyLore(storyContext.toPromptBlock())
            )
        )

        context.item(
            13,
            GuiItemFactory.item(
                Material.WRITTEN_BOOK,
                "&bQuest Semantic",
                questSemanticLore(authoringSnapshot, storyContext)
            )
        )

        context.item(
            14,
            GuiItemFactory.item(
                Material.PAPER,
                "&aQuest Authoring Summary",
                questAuthoringSummaryLore(authoringSnapshot)
            )
        )

        context.item(
            11,
            GuiItemFactory.item(
                Material.BOOK,
                "&dDecision detail",
                listOf(
                    "&7Decision: &f${valueOrUnknown(authoringSnapshot.decisionStatus())}",
                    "&7Reason: &f${valueOrUnknown(authoringSnapshot.decisionReason())}",
                    "&7Runtime executable: &f${authoringSnapshot.decisionRuntimeExecutable()}",
                    "&7Matched signals: &f${valueOrUnknown(authoringSnapshot.decisionMatchedSignals().joinToString(", "))}",
                    "&7Candidate templates: &f${valueOrUnknown(authoringSnapshot.decisionCandidateTemplateIds().joinToString(", "))}",
                    "&7Blocked reasons: &f${valueOrUnknown(authoringSnapshot.decisionBlockedReasons().joinToString(", "))}"
                )
            )
        )

        context.item(
            8,
            GuiItemFactory.item(
                Material.PAPER,
                "&dAuthoring diagnostics",
                authoringDiagnosticsLore(context.plugin(), player, authoringSnapshot.requestedQuestSelector, authoringSnapshot.requestedMechanicId)
            )
        )

        context.item(
            12,
            GuiItemFactory.item(
                Material.REDSTONE_TORCH,
                "&eWarnings",
                warningLore(authoringSnapshot.warnings) +
                    listOf("&7Progressie: &f${progressionSnapshot.allEntries().size}", "&7Seed: &f${authoringSnapshot.seedKind()}")
            )
        )

        context.button(
            16,
            GuiButton.enabled(
                GuiItemFactory.item(Material.COMPASS, "&aRefresh", "&7Recalculeaza authoring snapshot-ul."),
                GuiAction { click -> click.service().open(click.player(), GuiKey.AUTHORING) }
            )
        )

        context.button(
            17,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.ARROW,
                    "&eSelector anterior",
                    "&7Curent: &f${valueOrUnknown(authoringSnapshot.requestedQuestSelector)}",
                    "&7Schimba selectorul authoring inapoi."
                ),
                GuiAction { click -> click.service().cycleAuthoringQuestSelector(click.player(), -1) }
            )
        )

        context.button(
            18,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.ARROW,
                    "&eSelector urmator",
                    "&7Curent: &f${valueOrUnknown(authoringSnapshot.requestedQuestSelector)}",
                    "&7Schimba selectorul authoring inainte."
                ),
                GuiAction { click -> click.service().cycleAuthoringQuestSelector(click.player(), 1) }
            )
        )

        context.button(
            19,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.ARROW,
                    "&dMechanic anterior",
                    "&7Curent: &f${valueOrUnknown(authoringSnapshot.requestedMechanicId)}",
                    "&7Schimba mecanica authoring inapoi."
                ),
                GuiAction { click -> click.service().cycleAuthoringMechanicId(click.player(), -1) }
            )
        )

        context.button(
            20,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.ARROW,
                    "&dMechanic urmator",
                    "&7Curent: &f${valueOrUnknown(authoringSnapshot.requestedMechanicId)}",
                    "&7Schimba mecanica authoring inainte."
                ),
                GuiAction { click -> click.service().cycleAuthoringMechanicId(click.player(), 1) }
            )
        )

        context.button(
            21,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.BARRIER,
                    "&cReset authoring",
                    "&7Sterge selectorul si mecanica stocate.",
                    "&7Revine la selecția automata."
                ),
                GuiAction { click -> click.service().clearAuthoringSelection(click.player()) }
            )
        )

        context.button(
            22,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.KNOWLEDGE_BOOK,
                    "&6Dump authoring",
                    "&7Deschide dump-ul text pentru selectia curenta.",
                    "&7Foloseste aceeasi selectie ca GUI-ul."
                ),
                GuiAction { click -> click.service().runCommand(click.player(), "/ainpc authoring dump") }
            )
        )

        val detailSelector = authoringSnapshot.requestedQuestSelector
        context.button(
            23,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.BOOK,
                    "&bDetalii progresie",
                    "&7Revine la detaliile progresiei selectate.",
                    "&7Selector: &f${valueOrUnknown(detailSelector)}"
                ),
                GuiAction { click ->
                    if (detailSelector.isBlank()) {
                        click.service().openQuestLog(click.player(), "all")
                    } else {
                        click.service().openQuestDetail(click.player(), detailSelector, "all")
                    }
                }
            )
        )

        if (adminView) {
            context.button(
                24,
                GuiButton.enabled(
                    GuiItemFactory.item(Material.CLOCK, "&dBuild status", "&7Inspecteaza rapid build mode."),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc build mode status") }
                )
            )
            context.button(
                25,
                GuiButton.enabled(
                    GuiItemFactory.item(Material.WRITABLE_BOOK, "&bBuild history", "&7Ultimele schimbari build mode."),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc build mode history") }
                )
            )
            context.button(
                26,
                GuiButton.enabled(
                    GuiItemFactory.item(Material.PAPER, "&dBuild export", "&7Export compact al build mode."),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc build mode export") }
                )
            )
            context.button(
                27,
                GuiButton.enabled(
                    GuiItemFactory.item(Material.BARRIER, "&cClear build history", "&7Curata istoricul local build mode."),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc build mode clear-history") }
                )
            )
        }

        context.item(
            28,
            GuiItemFactory.item(
                Material.SPYGLASS,
                "&6AI preset",
                listOf(
                    "&7Mode: &f${aiMode.ifBlank { "(none)" }}",
                    "&7Selector: &f${aiSelector.ifBlank { "(auto)" }}",
                    "&7Mechanic: &f${aiMechanic.ifBlank { "(auto)" }}",
                    "&7Summary: &f${aiSummary.ifBlank { "(none)" }}"
                )
            )
        )
        context.button(
            29,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.ARROW,
                    "&eSelector anterior",
                    "&7Curent: &f${valueOrUnknown(authoringSnapshot.requestedQuestSelector)}",
                    "&7Schimba presetul AI inapoi."
                ),
                GuiAction { click -> click.service().cycleAuthoringQuestSelector(click.player(), -1) }
            )
        )
        context.button(
            30,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.ARROW,
                    "&eSelector urmator",
                    "&7Curent: &f${valueOrUnknown(authoringSnapshot.requestedQuestSelector)}",
                    "&7Schimba presetul AI inainte."
                ),
                GuiAction { click -> click.service().cycleAuthoringQuestSelector(click.player(), 1) }
            )
        )
        context.button(
            31,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.ARROW,
                    "&dMechanic anterior",
                    "&7Curent: &f${valueOrUnknown(authoringSnapshot.requestedMechanicId)}",
                    "&7Schimba mecanica AI inapoi."
                ),
                GuiAction { click -> click.service().cycleAuthoringMechanicId(click.player(), -1) }
            )
        )
        context.button(
            32,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.ARROW,
                    "&dMechanic urmator",
                    "&7Curent: &f${valueOrUnknown(authoringSnapshot.requestedMechanicId)}",
                    "&7Schimba mecanica AI inainte."
                ),
                GuiAction { click -> click.service().cycleAuthoringMechanicId(click.player(), 1) }
            )
        )
        context.button(
            33,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.BARRIER,
                    "&cReset AI preset",
                    "&7Sterge selectorul si mecanica stocate.",
                    "&7Revine la selectia automata."
                ),
                GuiAction { click -> click.service().clearAuthoringSelection(click.player()) }
            )
        )
        if (context.service().canOpen(player, GuiKey.QUEST_CREATE)) {
            context.button(
                34,
                GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.WRITABLE_BOOK,
                        "&bQuest Creator",
                        listOf(
                            "&7Deschide formularul complet pentru quest.",
                            "&7Click: deschide quest creator."
                        )
                    ),
                    GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_CREATE) }
                )
            )
        } else {
            context.button(
                34,
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&8Quest Creator",
                        listOf("&8Necesita permisiune quest sau admin.")
                    )
                )
            )
        }
        if (context.service().canOpen(player, GuiKey.MAPPING_CREATOR)) {
            context.button(
                35,
                GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.FILLED_MAP,
                        "&aMapping Creator",
                        listOf(
                            "&7Deschide formularul pentru mapping.",
                            "&7Click: deschide mapping creator."
                        )
                    ),
                    GuiAction { click -> click.service().open(click.player(), GuiKey.MAPPING_CREATOR) }
                )
            )
        } else {
            context.button(
                35,
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&8Mapping Creator",
                        listOf("&8Necesita permisiune admin sau creator.")
                    )
                )
            )
        }
        context.button(
            38,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.FILLED_MAP,
                    "&6Quest Map",
                    listOf("&7Leaga obiectivele de locatii.", "&7Click: deschide Quest Map.")
                ),
                GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_MAP) }
            )
        )
        context.button(
            36,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.CLOCK,
                    "&dQuest Draft Preview",
                    listOf(
                        "&7Previzualizeaza draftul curent fara salvare.",
                        "&7Click: ruleaza /ainpc quest preview."
                    )
                ),
                GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest preview") }
            )
        )
        if (context.service().canOpen(player, GuiKey.QUEST_EDIT)) {
            context.button(
                37,
                GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.CRAFTING_TABLE,
                        "&6Quest Editor",
                        listOf(
                            "&7Deschide editorul cu lista de definitii.",
                            "&7Click: deschide quest editor."
                        )
                    ),
                    GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_EDIT) }
                )
            )
        } else {
            context.button(
                37,
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&8Quest Editor",
                        listOf("&8Necesita permisiune quest sau admin.")
                    )
                )
            )
        }

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun storyLore(block: String): List<String> =
        if (block.isBlank()) {
            listOf("&8Niciun context story disponibil.")
        } else {
            GuiItemFactory.wrapLore(block, "&7", 36)
        }

    private fun authoringDiagnosticsLore(
        plugin: ro.ainpc.AINPCPlugin,
        player: Player,
        preferredQuestSelector: String,
        preferredMechanicId: String
    ): List<String> {
        return DebugDumpAuthoringText.buildAuthoringText(plugin, player, preferredQuestSelector, preferredMechanicId)
            .lineSequence()
            .filter { it.isNotBlank() }
            .take(6)
            .map { "&7$it" }
            .toList()
    }

    private fun questSemanticLore(
        snapshot: QuestAuthoringSnapshot,
        storyContext: ro.ainpc.story.StoryContextSnapshot
    ): List<String> {
        val lore = ArrayList<String>()
        lore.add("&7QUEST_LORE:")
        lore.add("&8- selector: &f${valueOrUnknown(snapshot.requestedQuestSelector)}")
        lore.add("&8- mechanic: &f${valueOrUnknown(snapshot.requestedMechanicId)}")
        lore.add("&8- seed: &f${valueOrUnknown(snapshot.seedStoryMode())}/${valueOrUnknown(snapshot.seedKind())}")
        lore.add("&7QUEST_HISTORY:")
        lore.add("&8- decision: &f${valueOrUnknown(snapshot.decisionStatus())}")
        lore.add("&8- reason: &f${valueOrUnknown(snapshot.decisionReason())}")
        lore.add("&8- matched signals: &f${valueOrUnknown(snapshot.decisionMatchedSignals().joinToString(", "))}")
        lore.add("&7QUEST_SIGNALS:")
        lore.add("&8- allowed objectives: &f${valueOrUnknown(snapshot.seed?.allowedObjectiveTypes().orEmpty().joinToString(", "))}")
        lore.add("&8- allowed rewards: &f${valueOrUnknown(snapshot.seed?.allowedRewardTypes().orEmpty().joinToString(", "))}")
        lore.add("&8- limits: &f${valueOrUnknown(snapshot.seed?.limits().orEmpty().joinToString(", "))}")
        lore.add("&8- story signals: &f${valueOrUnknown(snapshot.seed?.storySignals().orEmpty().joinToString(", "))}")
        lore.add("&8- story warnings: &f${storyContext.warnings().size}")
        return lore
    }

    private fun questAuthoringSummaryLore(snapshot: QuestAuthoringSnapshot): List<String> {
        val lore = ArrayList<String>()
        lore.add("&7QUEST_AUTHORING_SUMMARY:")
        lore.add("&8- decision: &f${valueOrUnknown(snapshot.decisionStatus())}")
        lore.add("&8- reason: &f${valueOrUnknown(snapshot.decisionReason())}")
        lore.add("&8- selector: &f${valueOrUnknown(snapshot.requestedQuestSelector)}")
        lore.add("&8- mechanic: &f${valueOrUnknown(snapshot.requestedMechanicId)}")
        lore.add("&8- seed: &f${valueOrUnknown(snapshot.seedRegionId())}/${valueOrUnknown(snapshot.seedPlaceId())}/${valueOrUnknown(snapshot.seedStoryMode())}")
        lore.add("&8- template: &f${valueOrUnknown(snapshot.selectedTemplateId())}")
        lore.add("&8- definition: &f${valueOrUnknown(snapshot.selectedDefinitionId())}")
        return lore
    }

    private fun progressionLore(lines: List<String>): List<String> =
        if (lines.isEmpty()) {
            listOf("&8Nicio progresie vizibila in snapshot.")
        } else {
            compactLore(lines, "&7", 6)
        }

    private fun seedLore(snapshot: ro.ainpc.engine.QuestAuthoringSnapshot): List<String> {
        val lines = mutableListOf<String>()
        lines.add("&7Region: &f${valueOrUnknown(snapshot.seedRegionId())}")
        lines.add("&7Place: &f${valueOrUnknown(snapshot.seedPlaceId())}")
        lines.add("&7Mechanic: &f${valueOrUnknown(snapshot.seedMechanicId())}")
        lines.add("&7Kind: &f${valueOrUnknown(snapshot.seedKind())}")
        lines.add("&7Story Mode: &f${valueOrUnknown(snapshot.seedStoryMode())}")
        lines.add("&7Theme: &f${valueOrUnknown(snapshot.seedTheme())}")
        return lines
    }

    private fun warningLore(lines: List<String>): List<String> =
        if (lines.isEmpty()) {
            listOf("&8Fara warnings.")
        } else {
            compactLore(lines, "&e", 8)
        }

    private fun cooldownLore(snapshot: ro.ainpc.engine.QuestAuthoringSnapshot, storySignals: List<String>): String {
        return if (snapshot.decisionReason() == "structure_quest_generation_cooldown") {
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

    private fun compactLore(lines: List<String>, color: String, maxLines: Int): List<String> {
        if (lines.isEmpty()) {
            return listOf(color + "Fara date.")
        }
        val result = ArrayList<String>()
        val limit = minOf(maxLines, lines.size)
        for (index in 0 until limit) {
            result.add(color + GuiItemFactory.stripLegacy(lines[index]))
        }
        if (lines.size > limit) {
            result.add("&8... +${lines.size - limit} lines")
        }
        return result
    }

    private fun valueOrUnknown(value: String): String = value.ifBlank { "unknown" }
}
