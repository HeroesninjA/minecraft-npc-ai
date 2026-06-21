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

class MappingCreatePlaceGui : GuiScreen {
    private val placeTypes = listOf("house", "forge", "farm", "market", "tavern", "shop", "camp", "temple", "custom")

    override fun key(): GuiKey = GuiKey.MAPPING_CREATE_PLACE
    override fun title(player: Player): String = "&0Creaza Place"
    override fun size(player: Player): Int = 45

    override fun render(context: GuiRenderContext) {
        val wa: WorldAdminApi = context.plugin().platform.worldAdmin
        val loc = context.player().location
        val regions = wa.regions.sortedBy { it.id() }

        val selectedRegion = context.service().getCreatorFormValue(context.player(), "place_region")
            .ifBlank { regions.firstOrNull()?.id() ?: "" }
        val placeName = context.service().getCreatorFormValue(context.player(), "place_name")
            .ifBlank { "place_${loc.blockX}_${loc.blockZ}" }
        var placeType = context.service().getCreatorFormValue(context.player(), "place_type")
            .ifBlank { "house" }
        val size = context.service().getCreatorFormValue(context.player(), "place_size").ifBlank { "10" }

        context.item(4, GuiItemFactory.item(Material.OAK_DOOR, "&6Creaza Place", listOf("&7Completeaza si apasa Creaza.")))

        if (regions.isNotEmpty()) {
            context.button(10, GuiButton.enabled(
                GuiItemFactory.item(Material.FILLED_MAP, "&eRegiune: &f$selectedRegion", listOf("&7Click: urmatoarea regiune.")),
                GuiAction { click ->
                    val ids = regions.map { it.id() }
                    val idx = ids.indexOf(selectedRegion)
                    val next = ids[(idx + 1) % ids.size]
                    context.service().setCreatorFormValue(click.player(), "place_region", next)
                    click.service().open(click.player(), GuiKey.MAPPING_CREATE_PLACE)
                }
            ))
        }

        context.button(11, GuiButton.enabled(
            GuiItemFactory.item(Material.NAME_TAG, "&eNume: &f$placeName", listOf("&7Click: schimba prefixul numelui.")),
            GuiAction { click ->
                val newName = if (placeName.startsWith("place_")) "loc_${placeName.removePrefix("place_")}" else "place_${loc.blockX}_${loc.blockZ}"
                context.service().setCreatorFormValue(click.player(), "place_name", newName)
                click.service().open(click.player(), GuiKey.MAPPING_CREATE_PLACE)
            }
        ))

        context.button(12, GuiButton.enabled(
            GuiItemFactory.item(Material.SPYGLASS, "&eTip: &f$placeType", listOf("&7Click: schimba tipul.")),
            GuiAction { click ->
                val idx = placeTypes.indexOf(placeType)
                placeType = placeTypes[(idx + 1) % placeTypes.size]
                context.service().setCreatorFormValue(click.player(), "place_type", placeType)
                click.service().open(click.player(), GuiKey.MAPPING_CREATE_PLACE)
            }
        ))

        context.button(13, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPASS, "&eMarime: &f$size", listOf("&7Click: mareste cu 5.")),
            GuiAction { click ->
                val s = (size.toIntOrNull() ?: 10) + 5
                context.service().setCreatorFormValue(click.player(), "place_size", if (s <= 50) s.toString() else "5")
                click.service().open(click.player(), GuiKey.MAPPING_CREATE_PLACE)
            }
        ))

        context.button(15, GuiButton.enabled(
            GuiItemFactory.item(Material.LIME_DYE, "&aCreaza Place", listOf(
                "&7Place: $placeName ($placeType)",
                "&7Regiune: $selectedRegion",
                "&7Click: creeaza."
            )),
            GuiAction { click ->
                click.service().runCommand(click.player(),
                    "ainpc world wand mode place")
                click.service().runCommand(click.player(),
                    "ainpc map place $placeName de tip $placeType in $selectedRegion")
            }
        ))

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }
}
