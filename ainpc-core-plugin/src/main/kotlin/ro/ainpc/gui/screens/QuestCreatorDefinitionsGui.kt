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

class QuestCreatorDefinitionsGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.CREATOR_QUEST_DEFS
    override fun title(player: Player): String = "&0Definitii"
    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val filter = context.service().getCreatorFormValue(context.player(), "creator_defs_filter").trim()
        val defs = context.plugin().progressionService.getDefinitions()
            .sortedBy { it.progressionId() }
            .filter {
                filter.isBlank() ||
                    it.progressionId().contains(filter, ignoreCase = true) ||
                    it.displayName().contains(filter, ignoreCase = true) ||
                    it.mechanicId().contains(filter, ignoreCase = true) ||
                    it.kind().contains(filter, ignoreCase = true)
            }
        val pageSize = 16
        val pageCount = maxOf(1, (defs.size + pageSize - 1) / pageSize)
        val requestedPage = context.service().getCreatorFormValue(context.player(), "creator_defs_page").toIntOrNull() ?: 0
        val currentPage = minOf(maxOf(0, requestedPage), pageCount - 1)
        val pageStart = currentPage * pageSize
        val visibleDefs = defs.drop(pageStart).take(pageSize)

        context.item(4, GuiItemFactory.item(Material.BOOKSHELF, "&6Definitii Progresie", listOf(
            "&7Total: &f${defs.size}",
            "&7Filtru: &f${if (filter.isBlank()) "-" else filter}",
            "&7Pagina: &f${currentPage + 1}&7/&f$pageCount",
            "&7Afisate: &f${visibleDefs.size}"
        )))

        context.button(26, GuiButton.enabled(
            GuiItemFactory.item(Material.NAME_TAG, "&eFiltru definitii", listOf(
                "&7Click: scrie un ID, nume sau mecanica.",
                "&7Filtreaza toate definitiile afisate."
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "creator_defs_filter",
                    "creator_defs_filter",
                    GuiKey.CREATOR_QUEST_DEFS,
                    promptLines = listOf("&7Ex: Q08, side_quests, Dagon", "&7Scrie clear pentru reset.")
                )
            }
        ))
        context.button(34, GuiButton.enabled(
            GuiItemFactory.item(Material.BARRIER, "&cCurata filtru", listOf("&7Sterge filtrul curent.", "&7Revine la lista completa.")),
            GuiAction { click ->
                context.service().setCreatorFormValue(click.player(), "creator_defs_filter", null)
                context.service().setCreatorFormValue(click.player(), "creator_defs_page", null)
                click.service().open(click.player(), GuiKey.CREATOR_QUEST_DEFS)
            }
        ))

        var slot = 9
        for (def in visibleDefs) {
            val cmd = "/ainpc progression definitions ${def.progressionId()}"
            context.button(slot++, GuiButton.enabled(
                GuiItemFactory.item(Material.PAPER, "&f${def.progressionId()}", listOf(
                    "&7Mecanica: &f${def.mechanicId().ifBlank { "?" }}",
                    "&7Kind: &f${def.kind().ifBlank { "?" }}",
                    "&7Obiective: &f${def.objectiveCount()}",
                    "&7Click: detalii in chat"
                )),
                GuiAction { click -> click.service().runCommand(click.player(), cmd) }
            ))
        }

        if (pageCount > 1) {
            context.button(46, GuiButton.enabled(
                GuiItemFactory.item(Material.ARROW, "&eAnterioara", listOf("&7Pagina &f${currentPage + 1}&7/&f$pageCount")),
                GuiAction { click ->
                    context.service().setCreatorFormValue(click.player(), "creator_defs_page", (currentPage - 1).toString())
                    click.service().open(click.player(), GuiKey.CREATOR_QUEST_DEFS)
                }
            ))
            context.button(52, GuiButton.enabled(
                GuiItemFactory.item(Material.ARROW, "&eUrmatoarea", listOf("&7Pagina &f${currentPage + 1}&7/&f$pageCount")),
                GuiAction { click ->
                    context.service().setCreatorFormValue(click.player(), "creator_defs_page", (currentPage + 1).toString())
                    click.service().open(click.player(), GuiKey.CREATOR_QUEST_DEFS)
                }
            ))
        }

        if (defs.isEmpty()) {
            context.item(22, GuiItemFactory.item(Material.BARRIER, "&cNicio definitie", listOf(
                "&7Nu exista definitii de progresie.",
                if (filter.isBlank()) "&7Adauga un filtru daca vrei cautare mai precisa." else "&7Incearca un filtru mai larg."
            )))
        }

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }
}
