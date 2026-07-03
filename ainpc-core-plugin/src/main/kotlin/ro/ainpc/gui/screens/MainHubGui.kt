package ro.ainpc.gui.screens

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.version.BuildVersionInfo
import ro.ainpc.gui.GuiAccessHelper
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen


class MainHubGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.MAIN

    override fun title(player: Player): String = "&0AINPC Hub"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        val worldAdmin: WorldAdminApi = context.plugin().platform.worldAdmin
        val location: Location = player.location
        val versionSnapshot = BuildVersionInfo.capture(context.plugin())

        context.item(
            4,
            GuiItemFactory.item(
                Material.NETHER_STAR,
                "&6AINPC Hub",
                listOf(
                    "&7Jucator: &f${player.name}",
                    "&7NPC-uri incarcate: &f${context.plugin().npcManager.getNPCCount()}",
                    "&7World mapping: &f${worldAdmin.regionCount} regiuni, " +
                        "${worldAdmin.placeCount} places, ${worldAdmin.nodeCount} noduri",
                    "&7Locatie: &f${location.world.name} ${location.blockX}, ${location.blockY}, ${location.blockZ}",
                    "&8Actiuni principale: progresii / interactiune / world"
                )
            )
        )
        context.item(
            5,
            GuiItemFactory.item(
                Material.PAPER,
                "&aVersion snapshot",
                listOf(
                    "&7Versiune: &f${versionSnapshot.version}",
                    "&7Build: &f${versionSnapshot.buildHash}",
                    "&8Snapshot de runtime pentru suport si debug."
                )
            )
        )
        if (ro.ainpc.commands.isRuntimeReadOnly(context.plugin())) {
            context.item(
                6,
                GuiItemFactory.item(
                    Material.BARRIER,
                    "&cRead-only activ",
                    listOf(
                        "&7MCP raporteaza modul read-only.",
                        "&7Operatiile de scriere in mapping sunt blocate.",
                        "&8Inspectia ramanen disponibila: status / history / export."
                    )
                )
            )
        }

        openButton(
            context,
            10,
            GuiKey.QUEST,
            Material.WRITABLE_BOOK,
            "&eProgresii",
            listOf("&7Questuri si progresii active.", "&8Actiuni principale.")
        )
        openButton(
            context,
            11,
            GuiKey.INTERACT,
            Material.VILLAGER_SPAWN_EGG,
            "&aInteractiune NPC",
            listOf("&7NPC-uri apropiate si actiuni rapide.", "&8Vorbit / quest / routine.")
        )
        openButton(
            context,
            12,
            GuiKey.WORLD,
            Material.COMPASS,
            "&bWorld",
            listOf("&7Regiune, place, noduri si context local.", "&8Context si navigare.")
        )
        val currentRegion = worldAdmin.findRegion(location.world.name, location.blockX, location.blockY, location.blockZ)
        val currentPlace = worldAdmin.findPlace(location.world.name, location.blockX, location.blockY, location.blockZ)
        if (currentRegion != null && context.service().canOpen(player, GuiKey.REGION)) {
            context.button(
                17,
                GuiButton.enabled(
                    GuiItemFactory.item(Material.FILLED_MAP, "&eRegiune curenta", listOf("&7${currentRegion.name()}", "&7Click: detalii regiune")),
                    GuiAction { click -> click.service().openRegionDetail(click.player(), currentRegion.id()) }
                )
            )
        }
        if (currentPlace != null && context.service().canOpen(player, GuiKey.PLACE)) {
            context.button(
                18,
                GuiButton.enabled(
                    GuiItemFactory.item(Material.OAK_DOOR, "&aPlace curent", listOf("&7${currentPlace.displayName()}", "&7Click: detalii place")),
                    GuiAction { click -> click.service().openPlaceDetail(click.player(), currentPlace.id()) }
                )
            )
        }
        openButton(
            context,
            13,
            GuiKey.STATS,
            Material.CLOCK,
            "&dStatistici",
            listOf("&7Snapshot personal si NPC-uri din apropiere.", "&8Informatii scurte.")
        )
        openButton(
            context,
            14,
            GuiKey.SHOP,
            Material.EMERALD,
            "&2Shop NPC",
            listOf("&7Intrare pregatita pentru economie/shop.", "&8Actiuni secundare.")
        )
        openButton(
            context,
            15,
            GuiKey.ROUTINE,
            Material.CLOCK,
            "&eRutine NPC",
            listOf("&7Preview program zilnic si status rutina.", "&8Inspectie separata.")
        )
        openButton(
            context,
            16,
            GuiKey.STORY,
            Material.AMETHYST_SHARD,
            "&dStory",
            listOf("&7State narativ local si evenimente recente.", "&8Context narativ.")
        )

        openButton(
            context,
            28,
            GuiKey.MANAGER,
            Material.NAME_TAG,
            "&6Manager NPC",
            listOf("&7Lista NPC admin, info si teleport.", "&8Admin separat.")
        )
        openButton(
            context,
            29,
            GuiKey.AUDIT,
            Material.REDSTONE_TORCH,
            "&cAudit",
            listOf("&7Ruleaza audituri operationale.", "&8Inspectie separata.")
        )
        openButton(
            context,
            30,
            GuiKey.DEBUG,
            Material.SPYGLASS,
            "&9Debug",
            listOf("&7Debugdump si test OpenAI.", "&8Tooling separat.")
        )
        openButton(
            context,
            31,
            GuiKey.AUTHORING,
            Material.ENCHANTED_BOOK,
            "&bAuthoring",
            listOf("&7Snapshot story, mapping si progresie pentru quest design.", "&8Authoring compact.")
        )
        openButton(
            context,
            39,
            GuiKey.MCP,
            Material.ENDER_EYE,
            "&bMCP",
            listOf("&7Status MCP, summary-uri si routing semantic.", "&8Acces admin.")
        )
        context.button(
            32,
            GuiButton.enabled(
                GuiItemFactory.item(Material.NAME_TAG, "&eVersion", "&7Ruleaza /npc version."),
                GuiAction { click -> click.service().runCommand(click.player(), "npc version") }
            )
        )

        if (GuiAccessHelper.isAdmin(player)) {
            context.button(
                33,
                GuiButton.enabled(
                    GuiItemFactory.item(Material.KNOWLEDGE_BOOK, "&6Admin Quest", "&7Panou admin quest."),
                    GuiAction { click -> click.service().open(click.player(), GuiKey.ADMIN_QUEST) }
                )
            )
            context.button(
                34,
                GuiButton.enabled(
                    GuiItemFactory.item(Material.COMMAND_BLOCK, "&6Admin Mapping", "&7Panou admin mapping."),
                    GuiAction { click -> click.service().open(click.player(), GuiKey.ADMIN_MAPPING) }
                )
            )
            context.button(
                35,
                GuiButton.enabled(
                    GuiItemFactory.item(Material.SPYGLASS, "&6Quest debug", "&7Debug progresie curenta."),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest debug tracked") }
                )
            )
            context.button(
                36,
                GuiButton.enabled(
                    GuiItemFactory.item(Material.MAP, "&6Quest anchors", "&7Listeaza ancore persistate."),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest anchors") }
                )
            )
            context.button(
                37,
                GuiButton.enabled(
                    GuiItemFactory.item(Material.FILLED_MAP, "&eQuest Mapping", "&7Creaza/editeaza/stergere ancore."),
                    GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_MAP) }
                )
            )
            context.button(
                40,
                GuiButton.enabled(
                    GuiItemFactory.item(Material.CLOCK, "&dBuild mode", listOf("&7Inspectie rapida build mode.", "&7Status, history, export.")),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc build mode status") }
                )
            )
            context.button(
                41,
                GuiButton.enabled(
                    GuiItemFactory.item(Material.WRITABLE_BOOK, "&bBuild history", listOf("&7Ultimele schimbari build mode.")),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc build mode history") }
                )
            )
            context.button(
                42,
                GuiButton.enabled(
                    GuiItemFactory.item(Material.PAPER, "&dBuild export", listOf("&7Export compact al build mode.")),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc build mode export") }
                )
            )
            context.button(
                43,
                GuiButton.enabled(
                    GuiItemFactory.item(Material.BARRIER, "&cClear build history", listOf("&7Curata istoricul local build mode.")),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc build mode clear-history") }
                )
            )
        }

        if (GuiAccessHelper.adminOrCreator(player)) {
            context.button(
                38,
                GuiButton.enabled(
                    GuiItemFactory.item(Material.CRAFTING_TABLE, "&bQuick Quest", listOf("&7Creeaza un quest rapid in 5 pasi.", "&8Wizard pas cu pas.")),
                    GuiAction { click -> click.service().open(click.player(), GuiKey.QUICK_QUEST) }
                )
            )
        }

        context.button(
            49,
            GuiButton.enabled(
                GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", "&7Reincarca hub-ul."),
                GuiAction { click -> click.service().open(click.player(), GuiKey.MAIN) }
            )
        )
        context.button(
            53,
            GuiButton.enabled(
                GuiItemFactory.item(Material.BARRIER, "&cInchide", "&7Inchide interfata."),
                GuiAction { click -> click.player().closeInventory() }
            )
        )
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun openButton(
        context: GuiRenderContext,
        slot: Int,
        target: GuiKey,
        material: Material,
        title: String,
        lore: List<String>
    ) {
        if (context.service().canOpen(context.player(), target)) {
            context.button(
                slot,
                GuiButton.enabled(
                    GuiItemFactory.item(material, title, lore),
                    GuiAction { click -> click.service().open(click.player(), target) }
                )
            )
            return
        }

        val lockedLore = lore.toMutableList()
        lockedLore.add("&8Necesita permisiune pentru ${target.displayName()}.")
        context.button(slot, GuiButton.disabled(GuiItemFactory.disabled(Material.BARRIER, title, lockedLore)))
    }
}
