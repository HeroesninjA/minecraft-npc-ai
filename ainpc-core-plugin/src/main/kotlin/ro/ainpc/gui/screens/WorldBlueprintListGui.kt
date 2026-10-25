package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.commands.WorldBlueprintWizard
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.world.WorldBlueprint
import ro.ainpc.world.WorldBlueprintStore

internal data class WorldBlueprintListPage(
    val index: Int,
    val count: Int,
    val entries: List<WorldBlueprintListEntry>,
)

internal data class WorldBlueprintListEntry(
    val worldName: String,
    val blueprint: WorldBlueprint?,
    val loaded: Boolean,
)

internal object WorldBlueprintListSelection {
    private const val PAGE_SIZE = 36

    fun select(
        drafts: List<WorldBlueprint>, loadedWorldNames: Collection<String>, requestedPage: Int,
    ): WorldBlueprintListPage {
        val draftsByName = drafts.associateBy { it.worldName }
        val loadedNames = loadedWorldNames.toSet()
        val entries = (draftsByName.keys + loadedNames).distinct().sorted()
            .map { WorldBlueprintListEntry(it, draftsByName[it], it in loadedNames) }
        val pageCount = maxOf(1, (entries.size + PAGE_SIZE - 1) / PAGE_SIZE)
        val page = requestedPage.coerceIn(0, pageCount - 1)
        return WorldBlueprintListPage(page, pageCount, entries.drop(page * PAGE_SIZE).take(PAGE_SIZE))
    }
}

class WorldBlueprintListGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.BLUEPRINT_LIST

    override fun title(player: Player): String = "&0Lumi Blueprint"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        if (!player.hasPermission("ainpc.admin")) {
            context.item(4, GuiItemFactory.item(Material.BARRIER, "&cAcces restrictionat"))
            context.fillEmpty(GuiItemFactory.filler())
            return
        }
        val store = WorldBlueprintStore(context.plugin().dataFolder.toPath().resolve("world-blueprints.json"))
        val drafts = runCatching { store.listWorlds() }.getOrElse {
            context.item(4, GuiItemFactory.item(Material.BARRIER, "&cNu pot citi drafturile",
                "&7Verifica fisierul world-blueprints.json."))
            context.button(45, GuiButton.enabled(GuiItemFactory.item(Material.ARROW, "&7World Hub"),
                action = { click -> click.service().runNextTickIfCurrent(click) {
                    click.service().open(click.player(), GuiKey.WORLD)
                } }))
            context.fillEmpty(GuiItemFactory.filler())
            return
        }
        val loadedWorldNames = context.plugin().server.worlds.map { it.name }
        val page = WorldBlueprintListSelection.select(drafts, loadedWorldNames,
            context.service().getBlueprintListPage(player))
        context.item(4, GuiItemFactory.item(Material.WRITABLE_BOOK, "&6Lumi si drafturi", listOf(
            "&7${drafts.size} drafturi salvate; ${loadedWorldNames.size} lumi incarcate.",
            "&7Click pe o lume pentru revizie sau creare draft.",
        )))
        if (page.entries.isEmpty()) {
            context.item(22, GuiItemFactory.item(Material.PAPER, "&eNicio lume sau draft",
                "&7Alege numele unei lumi pentru a incepe."))
        }
        page.entries.forEachIndexed { index, entry ->
            val review = WorldBlueprintReviewSelection.draft(entry.blueprint, store::fingerprint)
            val status = when {
                review == null -> "&eFara draft"
                review.completionIssues.isNotEmpty() -> "&eIncomplet"
                review.approved -> "&aConfirmat"
                else -> "&6Neconfirmat"
            }
            val material = when {
                review == null -> Material.WRITABLE_BOOK
                review.approved -> Material.ENCHANTED_BOOK
                else -> Material.BOOK
            }
            context.button(9 + index, GuiButton.enabled(GuiItemFactory.item(material,
                "&b${entry.worldName}", buildList {
                    add(status)
                    add(if (entry.loaded) "&aLume incarcata" else "&7Lume neincarcata")
                    review?.let { add("&7Amprenta: ${it.fingerprint.take(12)}...") }
                    add("&7Click: deschide revizia.")
                }), action = { click ->
                click.service().runNextTickIfCurrent(click) {
                    click.service().openBlueprintReview(click.player(), entry.worldName)
                }
            }))
        }
        context.button(45, GuiButton.enabled(GuiItemFactory.item(Material.ARROW, "&7World Hub"),
            action = { click -> click.service().runNextTickIfCurrent(click) {
                click.service().open(click.player(), GuiKey.WORLD)
            } }))
        if (page.index > 0) {
            context.button(47, GuiButton.enabled(GuiItemFactory.item(Material.ARROW, "&ePagina precedenta"),
                action = { click -> click.service().runNextTickIfCurrent(click) {
                    click.service().openBlueprintList(click.player(), page.index - 1)
                } }))
        }
        context.item(49, GuiItemFactory.item(Material.MAP, "&fPagina ${page.index + 1}/${page.count}"))
        if (page.index + 1 < page.count) {
            context.button(51, GuiButton.enabled(GuiItemFactory.item(Material.ARROW, "&ePagina urmatoare"),
                action = { click -> click.service().runNextTickIfCurrent(click) {
                    click.service().openBlueprintList(click.player(), page.index + 1)
                } }))
        }
        context.button(53, GuiButton.enabled(GuiItemFactory.item(Material.NAME_TAG,
            "&aAlta lume", "&7Introdu numele exact al unei lumi existente sau noi."),
            action = { click -> click.service().runNextTickIfCurrent(click) {
                val selectedPlayer = click.player()
                if (WorldBlueprintWizard.hasSession(selectedPlayer.uniqueId)) {
                    click.plugin().messageUtils.send(selectedPlayer, "&eOpreste mai intai wizard-ul activ.")
                } else {
                    click.service().openTextInput(selectedPlayer, "Lume Scenario Builder",
                        "blueprint_select_world", GuiKey.BLUEPRINT_LIST,
                        promptLines = listOf("&7Scrie numele exact al unei lumi existente sau noi."))
                }
            } }))
        context.fillEmpty(GuiItemFactory.filler())
    }
}
