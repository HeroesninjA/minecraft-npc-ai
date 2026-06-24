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

class CreatorHubGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.CREATOR_HUB
    override fun title(player: Player): String = "&0Creator Tools"
    override fun size(player: Player): Int = 45

    override fun render(context: GuiRenderContext) {
        context.item(4, GuiItemFactory.item(Material.CRAFTING_TABLE, "&6Creator Tools", listOf(
            "&7Creeaza, editezi si testezi questuri si mapping."
        )))

        context.item(9, GuiItemFactory.item(Material.WRITABLE_BOOK, "&bQuest Tools", listOf("&7Creeaza, editeaza si mappeaza questuri.")))
        context.button(10, GuiButton.enabled(GuiItemFactory.item(Material.CRAFTING_TABLE, "&eQuick Quest Wizard", listOf("&7Creeaza un quest rapid in 5 pasi.", "&8Alege NPC giver, obiective, stage-uri si recompensa.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUICK_QUEST) }))
        context.button(11, GuiButton.enabled(GuiItemFactory.item(Material.WRITABLE_BOOK, "&bQuest Creator (avansat)", listOf("&7Formular complet cu toate optiunile.", "&8Stage-uri, dialoguri, story events.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_CREATE) }))
        context.button(12, GuiButton.enabled(GuiItemFactory.item(Material.COMPASS, "&bQuest Explorer", listOf("&7Definitii, test, editor si authoring.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.CREATOR_QUEST) }))
        context.button(13, GuiButton.enabled(GuiItemFactory.item(Material.FILLED_MAP, "&eQuest Mapping", listOf("&7Leaga obiective de locatii in lume.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_MAP) }))
        context.button(14, GuiButton.enabled(GuiItemFactory.item(Material.ANVIL, "&7Ancore (Quest Anchors)", listOf("&7Listeaza toate ancorele persistate.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest anchors all") }))
        context.button(15, GuiButton.enabled(GuiItemFactory.item(Material.SPYGLASS, "&6Quest Admin", listOf("&7Panou admin: definitii, statistici, stocare.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.ADMIN_QUEST) }))
        context.button(16, GuiButton.enabled(GuiItemFactory.item(Material.ENCHANTED_BOOK, "&bAuthoring", listOf("&7Snapshot story si progresie.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.AUTHORING) }))

        context.item(18, GuiItemFactory.item(Material.GRASS_BLOCK, "&6Mapping Tools", listOf("&7Creaza si gestioneaza harta semantica.")))
        context.button(19, GuiButton.enabled(GuiItemFactory.item(Material.GRASS_BLOCK, "&6Creator Mapping", listOf("&7Creaza regiuni, places si noduri manual.")),
            GuiAction { click -> click.service().open(click.player(), GuiKey.MAPPING_CREATOR) }))
        context.button(20, GuiButton.enabled(GuiItemFactory.item(Material.GRASS_BLOCK, "&6Demo mapping", listOf("&7Creeaza mapping demo la pozitia ta.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc world demo create") }))
        context.button(21, GuiButton.enabled(GuiItemFactory.item(Material.WRITABLE_BOOK, "&aSalveaza mapping", listOf("&7Persista modificarile in config.")),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc world save") }))

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }
}
