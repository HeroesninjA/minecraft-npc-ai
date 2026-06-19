package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.debug.DebugDumpAuthoringText
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
                    "&7Story signals: &f${storyContext.storySignals().size}",
                    "&7Warnings: &f${authoringSnapshot.warnings.size}"
                )
            )
        )

        context.item(
            5,
            GuiItemFactory.item(
                Material.NAME_TAG,
                "&bQuest selector",
                listOf(
                    "&7Curent: &f${valueOrUnknown(authoringSnapshot.requestedQuestSelector)}",
                    "&7Ciclareaza selectorul authoring.",
                    "&7Folosit la dump si analiza GUI."
                )
            )
        )

        context.item(
            6,
            GuiItemFactory.item(
                Material.COMPARATOR,
                "&dMechanic",
                listOf(
                    "&7Curent: &f${valueOrUnknown(authoringSnapshot.requestedMechanicId)}",
                    "&7Ciclareaza mecanica authoring.",
                    "&7Raman mecanicile active din progresie."
                )
            )
        )

        context.item(
            10,
            GuiItemFactory.item(
                Material.MAP,
                "&bStory context",
                storyLore(storyContext.toPromptBlock())
            )
        )

        context.item(
            11,
            GuiItemFactory.item(
                Material.BOOK,
                "&dDecision",
                compactLore(authoringSnapshot.summaryLines, "&7", 7)
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
                Material.STRUCTURE_BLOCK,
                "&6Decision detail",
                listOf(
                    "&7Runtime executable: &f${authoringSnapshot.decisionRuntimeExecutable()}",
                    "&7Matched signals: &f${valueOrUnknown(authoringSnapshot.decisionMatchedSignals().joinToString(", "))}",
                    "&7Candidate templates: &f${valueOrUnknown(authoringSnapshot.decisionCandidateTemplateIds().joinToString(", "))}",
                    "&7Blocked reasons: &f${valueOrUnknown(authoringSnapshot.decisionBlockedReasons().joinToString(", "))}"
                )
            )
        )

        context.item(
            13,
            GuiItemFactory.item(
                Material.ENCHANTED_BOOK,
                "&aSeed",
                seedLore(authoringSnapshot)
            )
        )

        context.item(
            14,
            GuiItemFactory.item(
                Material.FILLED_MAP,
                "&bProgressie",
                progressionLore(progressionSnapshot.summaryLines())
            )
        )

        context.item(
            15,
            GuiItemFactory.item(
                Material.REDSTONE_TORCH,
                "&eWarnings",
                warningLore(authoringSnapshot.warnings)
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
        lines.add("&7Story mode: &f${valueOrUnknown(snapshot.seedStoryMode())}")
        lines.add("&7Theme: &f${valueOrUnknown(snapshot.seedTheme())}")
        return lines
    }

    private fun warningLore(lines: List<String>): List<String> =
        if (lines.isEmpty()) {
            listOf("&8Fara warnings.")
        } else {
            compactLore(lines, "&e", 8)
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
