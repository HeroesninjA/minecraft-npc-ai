package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.progression.ProgressionDefinition
import ro.ainpc.progression.ProgressionFormatUtil
import ro.ainpc.progression.StoredProgression
import ro.ainpc.progression.StoredProgressionSummary

class AdminQuestGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.ADMIN_QUEST

    override fun title(player: Player): String = "&0Admin Quest"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        val definitions = context.plugin().progressionService.getDefinitions()
        val snapshot = context.plugin().progressionService.getProgressionGuiSnapshot(player, "all", true)

        val storedSummary: StoredProgressionSummary? = runCatching {
            context.plugin().progressionService.getStoredProgressionSummary("", "")
        }.getOrNull()

        val duplicates = context.plugin().progressionService.findDuplicateDefinitions()
        val duplicateCodes = context.plugin().progressionService.findDuplicateCodes()
        val unresolvedCount = runCatching {
            context.plugin().progressionService.findUnresolvedProgressions("", 0).size
        }.getOrDefault(-1)

        val byMechanic = definitions.groupBy { it.mechanicId().ifBlank { "other" } }
        val mechanics = byMechanic.keys.sorted()

        context.item(4, GuiItemFactory.item(
            Material.KNOWLEDGE_BOOK,
            "&6Admin Quest",
            listOf(
                "&7Definitii: &f${definitions.size}",
                "&7Mecanici: &f${mechanics.size}",
                "&7Progresii curente: &f${snapshot.allEntries().size}",
                "&7Active: &f${snapshot.currentEntries().count { it.active() }}",
                "&7Tracked: &f${snapshot.currentEntries().count { it.tracked() }}",
                if (storedSummary != null) "&7Stored DB: &f${storedSummary.rowCount()}" else "&7Stored DB: &cN/A"
            )
        ))

        context.item(5, GuiItemFactory.item(
            Material.WRITABLE_BOOK,
            "&bDefinitii",
            listOf(
                "&7Total: &f${definitions.size}",
                "&7Enabled: &f${definitions.count { it.enabled() }}",
                "&7Repeatable: &f${definitions.count { it.repeatable() }}",
                "&7Cu stagii: &f${definitions.count { it.stageCount() > 0 }}",
                "&7Cu obiective: &f${definitions.count { it.objectiveCount() > 0 }}",
                "&7Cu recompense: &f${definitions.count { it.rewardCount() > 0 }}"
            )
        ))

        context.item(6, GuiItemFactory.item(
            Material.STRUCTURE_BLOCK,
            "&dDiagnostic",
            listOf(
                "&7Def. duplicat (id): &f${duplicates.size}",
                "&7Def. duplicat (cod): &f${duplicateCodes.size}",
                if (unresolvedCount >= 0) "&7Nerezolvate in DB: &f$unresolvedCount" else "&7Nerezolvate: &cDB indisponibil",
                if (storedSummary != null) "&7Stored: &f${storedSummary.rowCount()} (current: &f${storedSummary.currentCount()}&7, archived: &f${storedSummary.archivedCount()}&7)" else "&7Stored DB: &cN/A"
            )
        ))

        context.button(10, GuiButton.enabled(
            GuiItemFactory.item(Material.WRITABLE_BOOK, "&eQuest log", "&7Deschide log-ul de progresii."),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST) }
        ))

        context.button(11, GuiButton.enabled(
            GuiItemFactory.item(Material.ENCHANTED_BOOK, "&bAuthoring", "&7Deschide authoring GUI."),
            GuiAction { click -> click.service().open(click.player(), GuiKey.AUTHORING) }
        ))

        context.button(12, GuiButton.enabled(
            GuiItemFactory.item(Material.MAP, "&6Ancore", "&7Listeaza ancorele persistate."),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest anchors all") }
        ))

        context.button(13, GuiButton.enabled(
            GuiItemFactory.item(Material.SHIELD, "&cNerezolvate", "&7Raporteaza progresii fara definitie."),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc progression unresolved") }
        ))

        context.button(14, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&aQuest dump", "&7Debug dump quest in chat."),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc debugdump quest") }
        ))

        context.button(15, GuiButton.enabled(
            GuiItemFactory.item(Material.SPYGLASS, "&dDebug tracked", "&7Debug progresia curenta."),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest debug tracked") }
        ))

        context.button(16, GuiButton.enabled(
            GuiItemFactory.item(Material.ENDER_EYE, "&6Test", "&7Ruleaza /ainpc test."),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc test") }
        ))

        context.button(17, GuiButton.enabled(
            GuiItemFactory.item(Material.BOOK, "&eDefinitii chat", "&7Listeaza definitii in chat."),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc progression definitions") }
        ))

        context.button(18, GuiButton.enabled(
            GuiItemFactory.item(Material.WRITABLE_BOOK, "&eProgresii stored", "&7Listeaza stored progressions in chat."),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc progression stored all") }
        ))

        var slot = 19
        for (mechanic in mechanics.take(14)) {
            val entries = byMechanic[mechanic] ?: emptyList()
            val enabled = entries.count { it.enabled() }
            context.button(slot++, GuiButton.enabled(
                GuiItemFactory.item(mechanicMaterial(mechanic), "&e$mechanic", listOf(
                    "&7Definitii: &f${entries.size}",
                    "&7Active: &f$enabled",
                    "&7Obiective total: &f${entries.sumOf { it.objectiveCount() }}",
                    "&7Stagii total: &f${entries.sumOf { it.stageCount() }}",
                    "&7Click: filtreaza quest log"
                )),
                GuiAction { click -> click.service().openQuestLog(click.player(), mechanic) }
            ))
        }

        if (definitions.isEmpty()) {
            context.item(22, GuiItemFactory.item(Material.BARRIER, "&cNicio definitie", listOf(
                "&7Nu exista definitii de progresie.",
                "&7Asigura-te ca feature pack-urile sunt configurate."
            )))
        }

        context.button(28, GuiButton.enabled(
            GuiItemFactory.item(Material.BOOKSHELF, "&eToate progresiile", "&7Deschide log cu toate."),
            GuiAction { click -> click.service().openQuestLog(click.player(), "all") }
        ))

        context.button(29, GuiButton.enabled(
            GuiItemFactory.item(Material.EMERALD, "&aActive", "&7Doar progresiile active."),
            GuiAction { click -> click.service().openQuestLog(click.player(), "active") }
        ))

        context.button(30, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPASS, "&6Admin Mapping", "&7Deschide admin mapping."),
            GuiAction { click -> click.service().open(click.player(), GuiKey.ADMIN_MAPPING) }
        ))

        context.button(31, GuiButton.enabled(
            GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh definitii", "&7Reincarca cache-ul de definitii."),
            GuiAction { click ->
                context.plugin().progressionService.invalidateDefinitionCache()
                click.service().open(click.player(), GuiKey.ADMIN_QUEST)
            }
        ))

        if (duplicates.isNotEmpty()) {
            context.button(32, GuiButton.enabled(
                GuiItemFactory.item(Material.BARRIER, "&cDuplicates: ${duplicates.size}", listOf(
                    "&7Exista definitii cu acelasi mechanic:definitionId.",
                    "&7Click: raporteaza in chat."
                )),
                GuiAction { click ->
                    click.service().runCommand(click.player(), "ainpc progression duplicates")
                }
            ))
        }

        if (storedSummary != null && storedSummary.rowCount() > 0) {
            val statusLines = mutableListOf<String>()
            statusLines.add("&7Total: &f${storedSummary.rowCount()} | players: &f${storedSummary.playerCount()}")
            statusLines.add("&7Current: &f${storedSummary.currentCount()} archived: &f${storedSummary.archivedCount()} tracked: &f${storedSummary.trackedCount()}")
            val byStatus = storedSummary.byStatus()
            if (byStatus.isNotEmpty()) {
                val parts = byStatus.entries.joinToString(" ") { (k, v) -> "&7$k:&f$v" }
                statusLines.add("&7By status: $parts")
            }
            val byMechanic = storedSummary.byMechanic()
            if (byMechanic.isNotEmpty()) {
                val parts = byMechanic.entries.joinToString(" ") { (k, v) -> "&7$k:&f$v" }
                statusLines.add("&7By mechanic: $parts")
            }
            context.item(33, GuiItemFactory.item(
                Material.BOOK,
                "&7Stored DB &8(${storedSummary.rowCount()})",
                statusLines
            ))
        }

        if (storedSummary != null && storedSummary.byStatus().isNotEmpty()) {
            val total = storedSummary.rowCount()
            val byStatus = storedSummary.byStatus()
            val lore = mutableListOf("&7Total: &f$total")
            for ((status, count) in byStatus.entries.sortedByDescending { it.value }) {
                lore.add("&7- &f$status: &e$count")
            }
            if (storedSummary.byKind().isNotEmpty()) {
                lore.add("&8By kind:")
                for ((kind, count) in storedSummary.byKind().entries.sortedByDescending { it.value }) {
                    lore.add("&8  $kind: &f$count")
                }
            }
            context.item(34, GuiItemFactory.item(
                Material.BOOKSHELF,
                "&7Status & kind",
                lore
            ))
        }

        if (definitions.isNotEmpty()) {
            val byPack = definitions.groupBy { it.packId().ifBlank { "unknown" } }
            val packLore = mutableListOf<String>()
            for ((pack, entries) in byPack.entries.sortedByDescending { it.value.size }) {
                packLore.add("&7- &f${pack}: &e${entries.size}")
            }
            context.item(35, GuiItemFactory.item(
                Material.BOOK,
                "&7By pack",
                packLore
            ))
        }

        val recentStored: List<StoredProgression> = runCatching {
            context.plugin().progressionService.getStoredProgressions("", "", 6)
        }.getOrDefault(emptyList())

        if (recentStored.isNotEmpty()) {
            var spSlot = 36
            for (sp in recentStored.take(6)) {
                val statusColor = when (sp.status().lowercase()) {
                    "active", "current" -> "&a"
                    "completed" -> "&e"
                    "failed" -> "&c"
                    "archived" -> "&7"
                    else -> "&f"
                }
                val playerUuid = sp.playerUuid()
                context.button(spSlot++, GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.MAP,
                        "$statusColor${GuiItemFactory.compact(sp.templateId(), 20)}",
                        listOf(
                            "&7Player: &f${ProgressionFormatUtil.compactUuid(playerUuid)}",
                            "&7Status: $statusColor${sp.status()}",
                            "&7Stage: &f${ProgressionFormatUtil.formatOptional(sp.currentStageId())}",
                            "&7Code: &f${sp.code().ifBlank { "-" }}",
                            "&8Click: status in chat",
                            "&8Shift click: debug in chat"
                        )
                    ),
                    GuiAction { click ->
                        val selector = sp.templateId().ifBlank { sp.code() }
                        click.service().runCommand(click.player(), "ainpc quest status $selector $playerUuid")
                    }
                ))
            }
        }

        val anchorCount = runCatching {
            context.plugin().progressionService.getAnchorBindings("", "", 0).size
        }.getOrDefault(-1)

        if (anchorCount >= 0) {
            context.item(43, GuiItemFactory.item(
                Material.ANVIL,
                "&7Ancore: &f$anchorCount",
                listOf(
                    "&7Total anchor bindings in DB.",
                    "&7Click: listeaza in chat"
                )
            ))
        }

        context.button(44, GuiButton.enabled(
            GuiItemFactory.item(Material.REDSTONE_TORCH, "&cReset progresie", listOf(
                "&7Reseteaza progresiile tale.",
                "&7Click: /ainpc progression reset"
            )),
            GuiAction { click ->
                click.service().openConfirmCommand(
                    click.player(),
                    "Reset progresii",
                    "ainpc progression reset ${player.name}",
                    GuiKey.ADMIN_QUEST,
                    "",
                    listOf("&7Reseteaza toate progresiile persistate pentru tine.")
                )
            }
        ))

        context.button(46, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&dDump definitii", listOf(
                "&7Debug dump definitii in JSON.",
                "&7Click: /ainpc debugdump questconfig"
            )),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc debugdump questconfig") }
        ))

        context.button(47, GuiButton.enabled(
            GuiItemFactory.item(Material.AMETHYST_SHARD, "&dStory snapshot", listOf(
                "&7Deschide story snapshot.",
                "&7Click: deschide Story GUI"
            )),
            GuiAction { click -> click.service().open(click.player(), GuiKey.STORY) }
        ))

        context.button(48, GuiButton.enabled(
            GuiItemFactory.item(Material.CLOCK, "&eRutine NPC", listOf(
                "&7Deschide GUI-ul de rutine.",
                "&7Click: deschide Routine GUI"
            )),
            GuiAction { click -> click.service().open(click.player(), GuiKey.ROUTINE) }
        ))

        context.button(50, GuiButton.enabled(
            GuiItemFactory.item(Material.NAME_TAG, "&6Manager NPC", listOf(
                "&7Deschide manager-ul de NPC-uri.",
                "&7Click: deschide Manager GUI"
            )),
            GuiAction { click -> click.service().open(click.player(), GuiKey.MANAGER) }
        ))

        context.button(52, GuiButton.enabled(
            GuiItemFactory.item(Material.FILLED_MAP, "&eQuest Mapping", listOf(
                "&7Creaza/editeaza ancore pentru obiective.",
                "&7Pensat pentru creatori de continut."
            )),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_MAP) }
        ))

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun mechanicMaterial(mechanicId: String): Material = when (mechanicId) {
        "quest" -> Material.WRITABLE_BOOK
        "contract" -> Material.PAPER
        "duty" -> Material.SHIELD
        "bounty" -> Material.IRON_SWORD
        "event" -> Material.BELL
        "tutorial", "onboarding" -> Material.COMPASS
        "ritual" -> Material.AMETHYST_SHARD
        else -> Material.NAME_TAG
    }
}
