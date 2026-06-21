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
            GuiItemFactory.item(Material.COMPASS, "&eQuest Map", listOf("&7Harta vizuala intre definitii si ancore.", "&7Click: deschide quest map.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_MAP) }
        ))
        context.button(15, GuiButton.enabled(
            GuiItemFactory.item(Material.WRITABLE_BOOK, "&eQuest Log", listOf("&7Log-ul complet al progresiilor.", "&7Click: deschide log.")),
            GuiAction { click -> click.service().openQuestLog(click.player(), "all") }
        ))
        context.button(16, GuiButton.enabled(
            GuiItemFactory.item(Material.CRAFTING_TABLE, "&6Quest Editor", listOf("&7Editeaza quest: selecteaza definitie, NPC giver, testeaza.", "&7Click: deschide editorul.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_EDIT) }
        ))

        context.button(49, GuiButton.enabled(GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", ""),
            GuiAction { click -> click.service().open(click.player(), GuiKey.CREATOR_QUEST) }))
        context.fillEmpty(GuiItemFactory.filler())
    }
}
