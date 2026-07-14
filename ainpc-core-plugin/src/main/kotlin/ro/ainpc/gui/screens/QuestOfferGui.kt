package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.progression.ProgressionGuiEntry

class QuestOfferGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.QUEST_OFFER

    override fun title(player: Player): String = "&0Oferta"

    override fun size(player: Player): Int = 36

    override fun render(context: GuiRenderContext) {
        val selector = context.service().getQuestOfferSelector(context.player())
        if (selector.isBlank()) {
            context.item(13, GuiItemFactory.item(Material.BARRIER, "&cNicio oferta", listOf("&7Nu exista o oferta de quest activa.")))
            return
        }

        val snapshot = context.plugin().progressionService.getProgressionGuiSnapshot(
            context.player(), "offered", context.player().hasPermission("ainpc.admin")
        )
        val entry = snapshot.findEntry(selector)
        if (entry == null || !entry.offered()) {
            context.item(13, GuiItemFactory.item(Material.BARRIER, "&cOferta indisponibila", listOf(
                "&7Questul oferit nu mai este disponibil.",
                "&7Poate a fost deja acceptat sau a expirat."
            )))
            return
        }

        context.item(4, GuiItemFactory.item(
            Material.WRITABLE_BOOK,
            "&e${GuiItemFactory.compact(entry.title(), 40)}",
            listOf(
                "&7Status: &f${entry.statusDisplay()}",
                "&7Mecanica: &f${entry.mechanicDisplay()}",
                "&7Categorie: &f${entry.categoryDisplay()}",
                if (entry.actorName().isNotBlank()) "&7NPC: &f${entry.actorName()}" else ""
            )
        ))

        val loc = context.player().location
        val worldAdmin = context.plugin().platform.worldAdmin
        val place = worldAdmin.findPlace(loc.world.name, loc.blockX, loc.blockY, loc.blockZ)
        val region = worldAdmin.findRegion(loc.world.name, loc.blockX, loc.blockY, loc.blockZ)
        if (region != null || place != null) {
            context.item(0, GuiItemFactory.item(
                if (place != null) Material.OAK_DOOR else Material.FILLED_MAP,
                "&bContext mapping",
                listOf(
                    "&7Regiune: &f${region?.id() ?: "<nemapata>"}",
                    "&7Place: &f${place?.id() ?: "<nemapat>"}"
                )
            ))
        }

        val objectives = entry.objectives()
        val objectiveSlots = listOf(10, 11, 12, 13, 14, 15, 16)
        for (i in 0 until minOf(objectiveSlots.size, objectives.size)) {
            val obj = objectives[i]
            context.item(
                objectiveSlots[i],
                GuiItemFactory.item(
                    Material.PAPER,
                    "&e${obj.label()}",
                    listOf(
                        "&7Tip: &f${obj.type()}",
                        if (obj.description().isNotBlank()) "&7${obj.description()}" else "",
                        "&7Progres: &f${obj.currentAmount()}&7/&f${obj.requiredAmount()}"
                    )
                )
            )
        }

        val rewardLines = entry.rewardLines()
        val rewardSlots = listOf(19, 20, 21, 22, 23)
        for (i in 0 until minOf(rewardSlots.size, rewardLines.size)) {
            context.item(
                rewardSlots[i],
                GuiItemFactory.item(
                    Material.EMERALD,
                    "&a${GuiItemFactory.compact(rewardLines[i], 32)}",
                    GuiItemFactory.wrapLore(rewardLines[i], "&7")
                )
            )
        }

        val root = entry.commandRoot()
        val npcName = entry.actorName()
        val acceptCmd = if (npcName.isNotBlank()) "ainpc $root accept $npcName" else "ainpc $root accept nearest"
        val declineCmd = if (npcName.isNotBlank()) "ainpc $root decline $npcName" else "ainpc $root decline nearest"

        context.button(
            27,
            GuiButton.enabled(
                GuiItemFactory.item(Material.LIME_DYE, "&aAccepta", listOf(
                    "&7Accepta aceasta oferta.",
                    "&8Comanda: /$acceptCmd"
                ))
            ) { click ->
                click.service().runCommand(click.player(), acceptCmd)
                click.player().closeInventory()
            }
        )

        context.button(
            28,
            GuiButton.enabled(
                GuiItemFactory.item(Material.REDSTONE_BLOCK, "&cRefuza", listOf(
                    "&7Refuza aceasta oferta.",
                    "&8Comanda: /$declineCmd"
                ))
            ) { click ->
                click.service().runCommand(click.player(), declineCmd)
                click.player().closeInventory()
            }
        )

        context.button(
            35,
            GuiButton.enabled(
                GuiItemFactory.item(Material.BARRIER, "&cInchide", listOf("&7Inchide oferta."))
            ) { click -> click.player().closeInventory() }
        )

        context.fillEmpty(GuiItemFactory.filler())
    }
}
