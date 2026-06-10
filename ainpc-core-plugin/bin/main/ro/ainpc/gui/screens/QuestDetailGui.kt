package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.progression.ProgressionAnchorBinding
import ro.ainpc.progression.ProgressionGuiEntry
import ro.ainpc.progression.ProgressionGuiSnapshot
import ro.ainpc.progression.ProgressionObjectiveSnapshot
import ro.ainpc.progression.ProgressionStageSnapshot
import java.sql.SQLException

class QuestDetailGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.QUEST_DETAIL

    override fun title(player: Player): String = "&0AINPC Progresie"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val selector = context.service().getQuestDetailSelector(context.player())
        if (selector.isBlank()) {
            renderMissingSelection(context)
            return
        }

        val adminView = context.player().hasPermission("ainpc.admin")
        val detailFilter = context.service().getQuestDetailFilter(context.player())
        var snapshot = context.plugin().progressionService.getProgressionGuiSnapshot(context.player(), detailFilter, adminView)
        var optionalEntry = findEntry(snapshot, selector)
        if (optionalEntry == null && !detailFilter.equals("all", ignoreCase = true)) {
            snapshot = context.plugin().progressionService.getProgressionGuiSnapshot(context.player(), "all", adminView)
            optionalEntry = findEntry(snapshot, selector)
        }
        if (optionalEntry == null) {
            renderMissingQuest(context, selector, detailFilter)
            return
        }

        val entry = optionalEntry
        val anchors = loadAnchorBindings(context, entry)
        context.item(4, GuiItemFactory.item(headerMaterial(entry), "&e${GuiItemFactory.compact(entry.title(), 40)}", headerLore(entry)))

        renderDiagnosticCards(context, entry)
        renderAnchorDiagnostics(context, entry, anchors, adminView)
        renderObjectives(context, entry, anchors)
        renderStages(context, entry)
        renderRewards(context, entry)
        renderActions(context, entry, selector, detailFilter, adminView)
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun findEntry(snapshot: ProgressionGuiSnapshot?, selector: String?): ProgressionGuiEntry? {
        if (snapshot == null || selector.isNullOrBlank()) {
            return null
        }
        val normalized = selector.trim()
        return snapshot.allEntries().firstOrNull { entry ->
            matches(normalized, entry.guiDetailSelector()) ||
                matches(normalized, entry.commandSelector()) ||
                matches(normalized, entry.selector()) ||
                matches(normalized, entry.progressionId()) ||
                matches(normalized, "${entry.mechanicId()}:${entry.code()}") ||
                matches(normalized, "${entry.mechanicId()}:${entry.definitionId()}") ||
                matches(normalized, "${entry.kind()}:${entry.code()}") ||
                matches(normalized, "${entry.kind()}:${entry.definitionId()}") ||
                matches(normalized, entry.code()) ||
                matches(normalized, entry.templateId()) ||
                matches(normalized, entry.definitionId())
        }
    }

    private fun matches(left: String?, right: String?): Boolean =
        left != null && right != null && right.isNotBlank() && left.equals(right, ignoreCase = true)

    private fun renderObjectives(
        context: GuiRenderContext,
        entry: ProgressionGuiEntry,
        anchors: List<ProgressionAnchorBinding>?
    ) {
        val limit = minOf(OBJECTIVE_SLOTS.size, entry.objectives().size)
        for (index in 0 until limit) {
            val objective = entry.objectives()[index]
            context.item(
                OBJECTIVE_SLOTS[index],
                GuiItemFactory.item(
                    objectiveMaterial(objective),
                    if (objective.complete()) "&a${objective.label()}" else if (objective.active()) "&e${objective.label()}" else "&7${objective.label()}",
                    objectiveLore(objective, anchors)
                )
            )
        }
    }

    private fun renderStages(context: GuiRenderContext, entry: ProgressionGuiEntry) {
        val limit = minOf(STAGE_SLOTS.size, entry.stages().size)
        for (index in 0 until limit) {
            val stage = entry.stages()[index]
            context.item(
                STAGE_SLOTS[index],
                GuiItemFactory.item(
                    if (stage.active()) Material.AMETHYST_SHARD else if (stage.complete()) Material.EMERALD else Material.PAPER,
                    if (stage.active()) "&d${stage.label()}" else if (stage.complete()) "&a${stage.label()}" else "&f${stage.label()}",
                    stageLore(stage)
                )
            )
        }
    }

    private fun renderRewards(context: GuiRenderContext, entry: ProgressionGuiEntry) {
        val limit = minOf(REWARD_SLOTS.size, entry.rewardLines().size)
        for (index in 0 until limit) {
            val reward = entry.rewardLines()[index]
            context.item(
                REWARD_SLOTS[index],
                GuiItemFactory.item(Material.EMERALD, "&a${GuiItemFactory.compact(reward, 36)}", GuiItemFactory.wrapLore(reward, "&7"))
            )
        }
    }

    private fun renderDiagnosticCards(context: GuiRenderContext, entry: ProgressionGuiEntry) {
        context.item(
            DIAGNOSTIC_SLOT,
            GuiItemFactory.item(
                Material.BOOK,
                "&bStatus runtime",
                compactLore(entry.statusLines(), "&8Nu exista linii de status in snapshot.", 5)
            )
        )
        context.item(
            ACTION_HINT_SLOT,
            GuiItemFactory.item(
                Material.OAK_SIGN,
                "&aActiuni sugerate",
                compactLore(entry.actionLines(), "&8Nu exista actiuni sugerate in snapshot.", 5)
            )
        )
    }

    private fun loadAnchorBindings(context: GuiRenderContext, entry: ProgressionGuiEntry): List<ProgressionAnchorBinding>? {
        return try {
            context.plugin().progressionService.getAnchorBindingsForProgression(
                context.player().uniqueId.toString(),
                entry.templateId(),
                entry.code(),
                8
            )
        } catch (exception: SQLException) {
            context.plugin().logger.warning("Nu pot incarca ancorele pentru Quest Detail GUI: ${exception.message}")
            null
        }
    }

    private fun renderAnchorDiagnostics(
        context: GuiRenderContext,
        entry: ProgressionGuiEntry,
        anchors: List<ProgressionAnchorBinding>?,
        adminView: Boolean
    ) {
        if (anchors == null) {
            context.item(
                ANCHOR_SLOT,
                GuiItemFactory.item(
                    Material.BARRIER,
                    "&cAncore indisponibile",
                    "&7Nu am putut citi quest_anchor_bindings.",
                    "&7Verifica /ainpc audit quest."
                )
            )
            return
        }

        val lore = anchorLore(entry, anchors, adminView)
        if (adminView) {
            context.button(
                ANCHOR_SLOT,
                GuiButton.enabled(GuiItemFactory.item(Material.MAP, "&6Ancore persistate", lore)) { click ->
                    click.service().runCommand(click.player(), anchorCommand(click.player(), entry))
                }
            )
        } else {
            context.item(ANCHOR_SLOT, GuiItemFactory.item(Material.MAP, "&bAncore persistate", lore))
        }
    }

    private fun renderActions(
        context: GuiRenderContext,
        entry: ProgressionGuiEntry,
        selector: String,
        detailFilter: String,
        adminView: Boolean
    ) {
        context.button(
            45,
            GuiButton.enabled(GuiItemFactory.item(Material.ARROW, "&eInapoi", "&7Revine la log cu filtrul sursa.")) { click ->
                click.service().openQuestLog(click.player(), detailFilter)
            }
        )

        if (entry.active()) {
            context.button(
                46,
                GuiButton.enabled(
                    GuiItemFactory.item(
                        if (entry.tracked()) Material.GRAY_DYE else Material.COMPASS,
                        if (entry.tracked()) "&eOpreste tracking" else "&aUrmareste progresia",
                        if (entry.tracked()) "&7Ruleaza /${entry.trackStopCommand()}." else "&7Ruleaza /${entry.trackStartCommand()}."
                    )
                ) { click ->
                    click.service().runCommand(click.player(), if (entry.tracked()) entry.trackStopCommand() else entry.trackStartCommand())
                }
            )
        }

        context.button(
            47,
            GuiButton.enabled(GuiItemFactory.item(Material.WRITABLE_BOOK, "&bStatus in chat", "&7Ruleaza /${entry.command("status")}.")) { click ->
                click.service().runCommand(click.player(), entry.command("status"))
            }
        )

        context.button(
            51,
            GuiButton.enabled(GuiItemFactory.item(Material.FILLED_MAP, "&bProgres in chat", "&7Ruleaza /${entry.command("progress")}.")) { click ->
                click.service().runCommand(click.player(), entry.command("progress"))
            }
        )

        if (entry.active()) {
            context.button(
                48,
                GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.REDSTONE_BLOCK,
                        "&cAbandoneaza",
                        "&7Cere confirmare inainte de /${entry.command("abandon")}."
                    )
                ) { click ->
                    click.service().openConfirmCommand(
                        click.player(),
                        "Abandoneaza progresia",
                        entry.command("abandon"),
                        GuiKey.QUEST_DETAIL,
                        selector,
                        listOf(
                            "&cProgresie: &f${entry.title()}",
                            "&7Progresul curent va fi marcat ca esuat/abandonat."
                        )
                    )
                }
            )
        }

        context.button(
            49,
            GuiButton.enabled(GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", "&7Reincarca detaliile progresiei.")) { click ->
                click.service().openQuestDetail(click.player(), selector, detailFilter)
            }
        )

        if (adminView) {
            context.button(
                50,
                GuiButton.enabled(
                    GuiItemFactory.item(Material.SPYGLASS, "&6Debug progresie", "&7Ruleaza /${entry.command("debug")}.")
                ) { click ->
                    click.service().runCommand(click.player(), entry.command("debug"))
                }
            )
        }

        context.button(
            53,
            GuiButton.enabled(GuiItemFactory.item(Material.BARRIER, "&cInchide", "&7Inchide interfata.")) { click ->
                click.player().closeInventory()
            }
        )
    }

    private fun renderMissingSelection(context: GuiRenderContext) {
        context.item(
            22,
            GuiItemFactory.item(Material.BARRIER, "&cNicio progresie selectata", "&7Deschide log-ul si alege o progresie.")
        )
        context.button(
            45,
            GuiButton.enabled(GuiItemFactory.item(Material.ARROW, "&eInapoi", "&7Revine la log-ul de progresii.")) { click ->
                click.service().openQuestLog(click.player(), click.service().getQuestDetailFilter(click.player()))
            }
        )
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun renderMissingQuest(context: GuiRenderContext, selector: String, detailFilter: String) {
        context.item(
            22,
            GuiItemFactory.item(
                Material.BARRIER,
                "&cProgresie indisponibila",
                "&7Nu mai gasesc selectorul: &f$selector",
                "&7Progresia poate fi finalizata, abandonata sau reincarcata."
            )
        )
        context.button(
            45,
            GuiButton.enabled(GuiItemFactory.item(Material.ARROW, "&eInapoi", "&7Revine la log cu filtrul sursa.")) { click ->
                click.service().openQuestLog(click.player(), detailFilter)
            }
        )
        context.button(
            49,
            GuiButton.enabled(GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", "&7Reincarca log-ul cu filtrul sursa.")) { click ->
                click.service().openQuestLog(click.player(), detailFilter)
            }
        )
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun headerMaterial(entry: ProgressionGuiEntry): Material {
        if (entry.tracked()) {
            return Material.COMPASS
        }
        if (entry.active()) {
            return Material.LIME_DYE
        }
        if (entry.offered()) {
            return Material.WRITABLE_BOOK
        }
        return if (entry.archived()) Material.MAP else Material.PAPER
    }

    private fun headerLore(entry: ProgressionGuiEntry): List<String> {
        val lore = ArrayList<String>()
        lore.add("&7Status: &f${entry.statusDisplay()}")
        if (entry.mechanicDisplay().isNotBlank()) {
            lore.add("&7Mecanica: &f${entry.mechanicDisplay()}")
        }
        lore.add("&7Categorie: &f${entry.categoryDisplay()}")
        if (entry.currentStageLabel().isNotBlank()) {
            lore.add("&7Stage curent: &f${entry.currentStageLabel()}")
        }
        if (entry.tracked()) {
            lore.add("&bProgresie urmarita")
        }
        if (entry.actorName().isNotBlank()) {
            lore.add("&7NPC: &f${entry.actorName()}")
        }
        if (entry.missingTemplate()) {
            lore.add("&cDefinitia progresiei nu este incarcata.")
        }
        return lore
    }

    private fun anchorLore(
        entry: ProgressionGuiEntry,
        anchors: List<ProgressionAnchorBinding>?,
        adminView: Boolean
    ): List<String> {
        val safeAnchors = anchors ?: emptyList()
        val lore = ArrayList<String>()
        if (entry.templateId().isNotBlank()) {
            lore.add("&7Template: &f${GuiItemFactory.compact(entry.templateId(), 32)}")
        }
        if (entry.code().isNotBlank()) {
            lore.add("&7Cod: &f${entry.code()}")
        }
        lore.add("&7Binding-uri: &f${safeAnchors.size}")
        if (safeAnchors.isEmpty()) {
            lore.add("&8Nu exista ancore persistate pentru progresia curenta.")
            lore.add("&8Accepta/progreseaza continutul pe mapping verificat.")
        } else {
            safeAnchors.take(5).forEach { anchor ->
                lore.add(
                    "&7- &f${GuiItemFactory.compact(anchor.objectiveKey(), 14)} &8-> &f${GuiItemFactory.compact(anchor.anchorSelector(), 24)}"
                )
            }
        }
        lore.add(if (adminView) "&8Click: lista ancorele in chat" else "&8Read-only pentru progresia ta.")
        return lore
    }

    private fun anchorCommand(player: Player, entry: ProgressionGuiEntry): String {
        val template = firstNonBlank(entry.templateId(), entry.code(), entry.progressionId())
        return if (template.isBlank()) "ainpc quest anchors ${player.name}" else "ainpc quest anchors ${player.name} $template"
    }

    private fun objectiveMaterial(objective: ProgressionObjectiveSnapshot): Material {
        if (objective.stateId().equals("failed", ignoreCase = true)) {
            return Material.REDSTONE
        }
        if (objective.complete()) {
            return Material.LIME_DYE
        }
        if (objective.stateId().equals("in_progress", ignoreCase = true)) {
            return Material.ORANGE_DYE
        }
        return if (objective.active()) Material.YELLOW_DYE else Material.LIGHT_GRAY_DYE
    }

    private fun objectiveLore(
        objective: ProgressionObjectiveSnapshot,
        anchors: List<ProgressionAnchorBinding>?
    ): List<String> {
        val lore = ArrayList<String>()
        lore.add("&7Stare: &f${objective.stateDisplay()}")
        lore.add("&7Progres: &f${objective.currentAmount()}&7/&f${objective.requiredAmount()}")
        lore.add("&7Tip: &f${objective.type()}")
        if (objective.stageLabel().isNotBlank()) {
            lore.add("&7Stage: &f${objective.stageLabel()}")
        }
        val matchedAnchor = matchingAnchor(objective, anchors)
        if (matchedAnchor != null) {
            lore.add("&7Ancora: &f${GuiItemFactory.compact(matchedAnchor.anchorSelector(), 30)}")
        }
        lore.add(if (objective.active()) "&eActiv in etapa curenta." else "&8Inactiv in etapa curenta.")
        lore.addAll(GuiItemFactory.wrapLore(objective.description(), "&7"))
        return lore
    }

    private fun matchingAnchor(
        objective: ProgressionObjectiveSnapshot?,
        anchors: List<ProgressionAnchorBinding>?
    ): ProgressionAnchorBinding? {
        if (objective == null || anchors.isNullOrEmpty()) {
            return null
        }
        return anchors.firstOrNull { anchor ->
            anchor.objectiveKey().isNotBlank() && anchor.objectiveKey().equals(objective.key(), ignoreCase = true)
        }
    }

    private fun stageLore(stage: ProgressionStageSnapshot): List<String> {
        val lore = ArrayList<String>()
        lore.add(
            "&7Status: &f" + if (stage.active()) "curent" else if (stage.complete()) "complet" else "in asteptare"
        )
        lore.add("&7Completare: &f${stage.completionMode()}")
        if (stage.nextStageId().isNotBlank()) {
            lore.add("&7Next: &f${stage.nextStageId()}")
        }
        if (stage.objectiveIds().isNotEmpty()) {
            lore.add("&7Objectives: &f${stage.objectiveIds().joinToString(", ")}")
        }
        lore.addAll(GuiItemFactory.wrapLore(stage.description(), "&7"))
        return lore
    }

    private fun compactLore(lines: List<String>?, emptyLine: String, maxLines: Int): List<String> {
        if (lines.isNullOrEmpty()) {
            return listOf(emptyLine)
        }

        val lore = ArrayList<String>()
        val limit = minOf(maxOf(1, maxLines), lines.size)
        for (index in 0 until limit) {
            lore.add("&7${GuiItemFactory.compact(lines[index], 44)}")
        }
        if (lines.size > limit) {
            lore.add("&8+${lines.size - limit} linii in chat/status.")
        }
        return lore
    }

    private companion object {
        val OBJECTIVE_SLOTS = intArrayOf(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25)
        val STAGE_SLOTS = intArrayOf(28, 29, 30, 31, 32, 33, 34)
        val REWARD_SLOTS = intArrayOf(37, 38, 39, 40, 41, 42, 43)
        const val DIAGNOSTIC_SLOT = 2
        const val ANCHOR_SLOT = 5
        const val ACTION_HINT_SLOT = 6

        fun firstNonBlank(vararg values: String?): String {
            for (value in values) {
                val safeValue = value?.trim().orEmpty()
                if (safeValue.isNotBlank()) {
                    return safeValue
                }
            }
            return ""
        }
    }
}
