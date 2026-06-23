package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen

class QuestCreatorGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.CREATOR_QUEST
    override fun title(player: Player): String = "&0Creator Quest"
    override fun size(player: Player): Int = 36

    override fun render(context: GuiRenderContext) {
        val defs = context.plugin().progressionService.getDefinitions()
        val mapTarget = context.service().getCreatorFormValue(context.player(), "creator_quest_map_target").ifBlank { "-" }
        val logFilter = context.service().getCreatorFormValue(context.player(), "creator_quest_log_filter").ifBlank { "all" }
        val editQuery = context.service().getCreatorFormValue(context.player(), "quest_edit_query").ifBlank { "-" }

        context.item(4, GuiItemFactory.item(Material.WRITABLE_BOOK, "&6Quest Creator", listOf(
            "&7Definitii: &f${defs.size}",
            "&7Creeaza, testeaza si administreaza questuri."
        )))

        context.button(10, GuiButton.enabled(
            GuiItemFactory.item(Material.BOOKSHELF, "&bDefinitii", listOf("&7Listeaza toate definitiile de progresie.", "&7Click: vezi template-uri si mecanici.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.CREATOR_QUEST_DEFS) }
        ))
        context.button(11, GuiButton.enabled(
            GuiItemFactory.item(Material.ENCHANTED_BOOK, "&bAuthoring", listOf("&7Quest design read-only (seed, draft, validare).", "&7Click: deschide authoring.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.AUTHORING) }
        ))
        context.button(12, GuiButton.enabled(
            GuiItemFactory.item(Material.MAP, "&6Ancore", listOf("&7Listeaza ancorele persistate pentru questuri.", "&7Click: ancore.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest anchors") }
        ))
        context.button(13, GuiButton.enabled(
            GuiItemFactory.item(Material.SPYGLASS, "&6Test Quest", listOf("&7Testeaza questuri: debug, status, accept.", "&7Click: deschide test panel.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.CREATOR_QUEST_TEST) }
        ))
        context.button(14, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPASS, "&eQuest Map", listOf(
                "&7Harta vizuala intre definitii si ancore.",
                "&7Target curent: &f$mapTarget",
                "&7Click: scrie mechanic:<id>, template:<id> sau objective:<key>."
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "creator_quest_map_target",
                    "creator_quest_map_target",
                    GuiKey.CREATOR_QUEST,
                    promptLines = listOf(
                        "&7Ex: mechanic:side_quests",
                        "&7Ex: template:Q08",
                        "&7Ex: objective:kill_zombie"
                    )
                )
            }
        ))
        context.button(15, GuiButton.enabled(
            GuiItemFactory.item(Material.WRITABLE_BOOK, "&eQuest Log", listOf(
                "&7Log-ul complet al progresiilor.",
                "&7Filtru curent: &f$logFilter",
                "&7Click: scrie un filtru sau all."
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "creator_quest_log_filter",
                    "creator_quest_log_filter",
                    GuiKey.CREATOR_QUEST,
                    promptLines = listOf("&7Ex: all, active, completed, questCode", "&7Scrie clear pentru all.")
                )
            }
        ))
        context.button(16, GuiButton.enabled(
            GuiItemFactory.item(Material.CRAFTING_TABLE, "&6Quest Editor", listOf(
                "&7Editeaza quest: selecteaza definitie, NPC giver, testeaza.",
                "&7Query curent: &f$editQuery",
                "&7Click: scrie ID sau nume pentru cautare directa."
            )),
            GuiAction { click ->
                click.service().openTextInput(
                    click.player(),
                    "quest_edit_query",
                    "quest_edit_query",
                    GuiKey.QUEST_EDIT,
                    promptLines = listOf("&7Ex: Q08, Castelul lui Dagon", "&7Scrie clear pentru a reveni la selectie.")
                )
            }
        ))
        context.button(17, GuiButton.enabled(
            GuiItemFactory.item(Material.EMERALD, "&aCreeaza Quest Nou", listOf("&7Formular pentru quest nou: ID, nume, mecanica, obiective.", "&7Click: deschide formularul.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_CREATE) }
        ))

        context.button(49, GuiButton.enabled(GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", ""),
            GuiAction { click -> click.service().open(click.player(), GuiKey.CREATOR_QUEST) }))
        context.fillEmpty(GuiItemFactory.filler())
    }
}
