package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiAccessHelper
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen

class CreatorHubGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.CREATOR_HUB
    override fun title(player: Player): String = "&0Creator Tools"
    override fun size(player: Player): Int = 45

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        if (!GuiAccessHelper.adminOrCreator(player)) {
            context.item(4, GuiItemFactory.item(Material.BARRIER, "&cAcces restrictionat", listOf(
                "&7Doar creatorii si administratorii pot accesa acest panou."
            )))
            context.fillEmpty(GuiItemFactory.filler())
            return
        }

        context.item(4, GuiItemFactory.item(Material.CRAFTING_TABLE, "&6Creator Tools", listOf(
            "&7Creeaza, editeaza si testeaza questuri si mapping."
        )))

        context.item(9, GuiItemFactory.item(Material.WRITABLE_BOOK, "&bQuest Tools", listOf("&7Creeaza, editeaza si mapeaza questuri.")))
        context.button(10, GuiButton.enabled(GuiItemFactory.item(Material.CRAFTING_TABLE, "&eQuick Quest Wizard", listOf("&7Creeaza Un Quest Rapid In 5 Pasi.", "&8Alege NPC Giver, Obiective, Stage-uri Si Recompensa.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUICK_QUEST) }))
        context.button(11, GuiButton.enabled(GuiItemFactory.item(Material.SPYGLASS, "&6Quest AI", listOf("&7Deschide fluxul asistat pentru quest create.", "&7Comanda: &f/ainpc quest create ai")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest create ai") }))
        context.button(12, GuiButton.enabled(GuiItemFactory.item(Material.WRITABLE_BOOK, "&bQuest Creator", listOf("&7Formular complet cu toate optiunile.", "&8Stage-uri, dialoguri, story events.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_CREATE) }))
        context.button(13, GuiButton.enabled(GuiItemFactory.item(Material.COMPASS, "&bQuest Overview", listOf("&7Definitii, test, editor si authoring.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.CREATOR_QUEST) }))
        context.button(14, GuiButton.enabled(GuiItemFactory.item(Material.FILLED_MAP, "&eQuest Mapping", listOf("&7Leaga obiective de locatii in lume.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_MAP) }))
        context.button(15, GuiButton.enabled(GuiItemFactory.item(Material.ANVIL, "&7Ancore (Quest Anchors)", listOf("&7Listeaza toate ancorele persistate.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest anchors all") }))
        context.button(16, GuiButton.enabled(GuiItemFactory.item(Material.SPYGLASS, "&6Quest Admin", listOf("&7Panou admin: definitii, statistici, stocare.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.ADMIN_QUEST) }))
        context.button(17, GuiButton.enabled(GuiItemFactory.item(Material.ENCHANTED_BOOK, "&bQuest Authoring", listOf("&7Snapshot story si progresie.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.AUTHORING) }))

        context.item(18, GuiItemFactory.item(Material.GRASS_BLOCK, "&6Mapping Tools", listOf("&7Creeaza si gestioneaza harta semantica.")))
        context.button(19, GuiButton.enabled(GuiItemFactory.item(Material.GRASS_BLOCK, "&6Creator Mapping", listOf("&7Creeaza regiuni, locuri si noduri manual.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.MAPPING_CREATOR) }))
        context.item(20, GuiItemFactory.item(Material.SPYGLASS, "&6AI Quick Actions", listOf("&7Quest: create/refine.", "&7Mapping: create/refine.")))
        context.button(21, GuiButton.enabled(GuiItemFactory.item(Material.SPYGLASS, "&6Mapping AI create", listOf("&7Deschide fluxul asistat pentru world create ai.", "&7Comanda: &f/ainpc world create ai")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc world create ai") }))
        context.button(22, GuiButton.enabled(GuiItemFactory.item(Material.WRITABLE_BOOK, "&bMapping AI help", listOf("&7Arata utilizarea si exemplele pentru world create ai.", "&7Util cand vrei formatul exact.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc world create ai help") }))
        context.button(23, GuiButton.enabled(GuiItemFactory.item(Material.MAGENTA_DYE, "&dWorld AI refine", listOf("&7Deschide draftul AI de regiune cu presetul curent.", "&7Comanda: &f/ainpc world create ai region")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc world create ai region") }))
        context.button(24, GuiButton.enabled(GuiItemFactory.item(Material.MAGENTA_DYE, "&dQuest AI refine selection", listOf("&7Reface promptul AI din questul selectat.", "&7Comanda: &f/ainpc quest create ai from selection")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest create ai from selection") }))
        context.button(25, GuiButton.enabled(GuiItemFactory.item(Material.GRASS_BLOCK, "&6Demo mapping", listOf("&7Creeaza mapping demo la pozitia ta.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc world demo create") }))
        context.button(26, GuiButton.enabled(GuiItemFactory.item(Material.WRITABLE_BOOK, "&aSalveaza mapping", listOf("&7Persista modificarile in config.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc world save") }))
        context.button(27, GuiButton.enabled(GuiItemFactory.item(Material.CLOCK, "&6Quest Draft Preview", listOf("&7Previzualizeaza draftul questului curent.", "&7Comanda: &f/ainpc quest preview")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest preview") }))
        context.button(28, GuiButton.enabled(GuiItemFactory.item(Material.CLOCK, "&6Mapping AI preview", listOf("&7Previzualizeaza draftul AI de mapping.", "&7Comanda: &f/ainpc world create ai preview region")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc world create ai preview region") }))
        if (GuiAccessHelper.canAccess(player, GuiKey.QUEST_EDIT, context.service())) {
            context.button(29, GuiButton.enabled(GuiItemFactory.item(Material.CRAFTING_TABLE, "&6Quest Editor", listOf("&7Deschide editorul cu lista de definitii.", "&7Click: deschide quest editor.")),
                GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_EDIT) }))
        } else {
            context.button(29, GuiButton.disabled(GuiItemFactory.disabled(
                Material.GRAY_DYE,
                "&8Quest Editor",
                listOf("&8Necesita permisiune quest sau admin.")
            )))
        }

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }
}
