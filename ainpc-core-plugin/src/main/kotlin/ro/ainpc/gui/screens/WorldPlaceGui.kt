package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.util.Locale

class WorldPlaceGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.PLACE

    override fun title(player: Player): String = "&0Place"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val plugin = context.plugin()
        val worldAdmin: WorldAdminApi = plugin.platform.worldAdmin
        val placeId = context.service().getPlaceDetailId(context.player())
        val place = if (placeId.isNotBlank()) worldAdmin.getPlace(placeId) else null
        val region = place?.let { worldAdmin.getRegion(it.regionId()) }
        val nodes = place?.let { worldAdmin.getNodesForPlace(it.id()).sortedBy { n -> n.id() } } ?: emptyList()
        val adminView = context.player().hasPermission("ainpc.admin")

        context.item(4, GuiItemFactory.item(
            if (place != null) Material.OAK_DOOR else Material.BARRIER,
            if (place != null) "&a${place.displayName()}" else "&cPlace neselectat",
            placeLore(place)
        ))
        context.item(10, GuiItemFactory.item(Material.FILLED_MAP, "&eRegiune parinte", regionLore(region)))

        if (place != null) {
            context.item(11, GuiItemFactory.item(
                Material.NAME_TAG,
                "&bTip place",
                listOf("&7Tip: &f${place.placeType().id}", "&7Owner: &f${place.ownerNpcId().ifBlank { "<niciunul>" }}")
            ))
            context.item(12, GuiItemFactory.item(
                Material.NAME_TAG,
                "&eTags",
                listOf("&7${place.tags().joinToString(", ").ifBlank { "<fara tags>" }}")
            ))
            context.item(13, GuiItemFactory.item(
                Material.BOOK,
                "&dMetadata",
                place.metadata().map { (k, v) -> "&7$k: &f$v" }.ifEmpty { listOf("&7<fara metadata>") }
            ))
            context.item(14, GuiItemFactory.item(
                Material.LEVER,
                "&7Acces",
                listOf("&7Public: &f${if (place.publicAccess()) "da" else "nu"}")
            ))
            context.item(15, GuiItemFactory.item(
                Material.COMPASS,
                "&7Delimitare",
                listOf(
                    "&7X: &f${place.minX()} - ${place.maxX()}",
                    "&7Y: &f${place.minY()} - ${place.maxY()}",
                    "&7Z: &f${place.minZ()} - ${place.maxZ()}"
                )
            ))

            var slot = 19
            for (node in nodes.take(7)) {
                context.item(slot++, GuiItemFactory.item(
                    Material.LODESTONE,
                    "&f${node.id()}",
                    nodeLore(node)
                ))
            }

            val residentsCount = plugin.npcManager.getAllNPCs().count { npc ->
                runCatching { plugin.npcWorldBindingService.getBinding(npc.databaseId) }
                    .getOrNull()?.homePlaceId().equals(place.id(), ignoreCase = true)
            }
            context.button(28, GuiButton.enabled(
                GuiItemFactory.item(
                    Material.VILLAGER_SPAWN_EGG,
                    "&bLocuitori: $residentsCount",
                    listOf("&7Click: detalii in chat despre locuitorii acestui place.")
                ),
                GuiAction { click ->
                    click.service().runCommand(click.player(), "ainpc world place info ${place.id()}")
                }
            ))

            context.button(29, GuiButton.enabled(
                GuiItemFactory.item(
                    Material.TRIPWIRE_HOOK,
                    "&bNPC Bindings",
                    listOf("&7Click: vezi NPC-urile legate de acest place (home/work/social).")
                ),
                GuiAction { click ->
                    click.service().runCommand(click.player(), "ainpc world place info ${place.id()}")
                }
            ))

            context.button(30, GuiButton.enabled(
                GuiItemFactory.item(
                    Material.ENDER_EYE,
                    "&bAncore quest",
                    listOf("&7Click: vezi ancorele de quest pentru acest place.")
                ),
                GuiAction { click ->
                    click.service().runCommand(click.player(), "ainpc quest anchors ${place.id()}")
                }
            ))

            context.button(31, GuiButton.enabled(
                GuiItemFactory.item(Material.AMETHYST_SHARD, "&dStory state", "&7Vezi starea povestii pentru acest place."),
                GuiAction { click ->
                    click.service().runCommand(click.player(), "ainpc story place ${place.id()}")
                }
            ))

            context.button(32, if (adminView) {
                GuiButton.enabled(GuiItemFactory.item(Material.ENDER_PEARL, "&6Teleport", "&7Teleporteaza-te la acest place."),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc tp ${place.id()}") })
            } else GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Teleport", listOf("&7Necesita admin."))))

            context.button(33, if (adminView) {
                GuiButton.enabled(GuiItemFactory.item(Material.PAPER, "&6Inspectie", "&7Inspecteaza place in chat."),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc world place info ${place.id()}") })
            } else GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Inspectie", listOf("&7Necesita admin."))))

            context.button(34, if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.ANVIL, "&eEdit place",
                        listOf("&7Recentreaza bounds-urile pe pozitia curenta.", "&7Pastrat prin GUI.")),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc world place edit ${place.id()}") }
                )
            } else GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Edit place", listOf("&7Necesita admin."))))

            context.button(35, if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.LEAD, "&eBind NPC",
                        listOf("&7Leaga un NPC de acest place (home/work/social).")),
                    GuiAction { click ->
                        click.service().runCommand(click.player(), "ainpc world place bind ${place.id()}")
                    }
                )
            } else GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Bind NPC", listOf("&7Necesita admin."))))

            context.button(36, if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.BARRIER, "&cDelete place",
                        listOf("&7Sterge place-ul curent.", "&cSterge si node-urile asociate.")),
                    GuiAction { click ->
                        click.service().openConfirmCommand(
                            click.player(),
                            "Sterge place",
                            "ainpc world place remove ${place.id()}",
                            GuiKey.PLACE,
                            place.id(),
                            listOf("&7ID: &f${place.id()}", "&cSterge place-ul si node-urile dependente.")
                        )
                    }
                )
            } else GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Delete place", listOf("&7Necesita admin."))))

            context.button(37, if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.LODESTONE, "&aCreate node",
                        listOf("&7Creeaza un node la pozitia ta.", "&7Node-ul se leaga de place-ul curent.")),
                    GuiAction { click ->
                        val loc = click.player().location
                        click.service().runCommand(
                            click.player(),
                            "ainpc world node create ${place.regionId()} ${place.id()} ${click.player().uniqueId.toString().take(8)} meeting_point ${loc.x} ${loc.y} ${loc.z} 2.5"
                        )
                    }
                )
            } else GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Create node", listOf("&7Necesita admin."))))

            if (nodes.isNotEmpty()) {
                val nearest = nodes.first()
                context.button(38, if (adminView) {
                    GuiButton.enabled(
                        GuiItemFactory.item(Material.COMPASS, "&eEdit node",
                            listOf("&7Mută node-ul cel mai apropiat la poziția curentă.")),
                        GuiAction { click -> click.service().runCommand(click.player(), "ainpc world node edit ${nearest.id()}") }
                    )
                } else GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Edit node", listOf("&7Necesita admin."))))

                context.button(39, if (adminView) {
                    GuiButton.enabled(
                        GuiItemFactory.item(Material.BARRIER, "&cDelete node",
                            listOf("&7Sterge node-ul cel mai apropiat.")),
                        GuiAction { click ->
                            click.service().openConfirmCommand(
                                click.player(),
                                "Sterge node",
                                "ainpc world node remove ${nearest.id()}",
                                GuiKey.PLACE,
                                place.id(),
                                listOf("&7Node: &f${nearest.id()}", "&cSterge node-ul selectat.")
                            )
                        }
                    )
                } else GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Delete node", listOf("&7Necesita admin."))))
            }
        }

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun placeLore(place: WorldPlaceInfo?): List<String> {
        if (place == null) return listOf("&7Niciun place selectat.")
        return listOf(
            "&7ID: &f${place.id()}",
            "&7Regiune: &f${place.regionId()}",
            "&7Nume: &f${place.displayName()}",
            "&7Noduri: &f0"
        )
    }

    private fun regionLore(region: WorldRegionInfo?): List<String> {
        if (region == null) return listOf("&7Regiune negasita.")
        return listOf("&7ID: &f${region.id()}", "&7Nume: &f${region.name()}", "&7Tip: &f${region.typeId()}")
    }

    private fun nodeLore(node: WorldNodeInfo): List<String> {
        return listOf(
            "&7Tip: &f${node.typeId()}",
            "&7Coordonate: &f${node.x().toInt()}, ${node.y().toInt()}, ${node.z().toInt()}",
            "&7Raza: &f${String.format(Locale.ROOT, "%.1f", node.radius())}"
        )
    }


}
