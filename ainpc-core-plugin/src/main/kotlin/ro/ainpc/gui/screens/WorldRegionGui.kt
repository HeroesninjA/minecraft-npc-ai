package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.gui.EnvironmentUi
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.world.RegionIdentityProvider
import ro.ainpc.world.RegionType
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo

class WorldRegionGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.REGION

    override fun title(player: Player): String = "&0Regiune"

    override fun size(player: Player): Int = 45

    override fun render(context: GuiRenderContext) {
        val worldAdmin: WorldAdminApi = context.plugin().platform.worldAdmin
        val regionId = context.service().getRegionDetailId(context.player())
        val region = if (regionId.isNotBlank()) worldAdmin.getRegion(regionId) else null
        val places = region?.let { worldAdmin.getPlaces(it.id()).sortedBy { p -> p.id() } } ?: emptyList()
        val adminView = context.player().hasPermission("ainpc.admin")

        context.item(4, GuiItemFactory.item(
            if (region != null) Material.FILLED_MAP else Material.BARRIER,
            if (region != null) "&e${region.name()}" else "&cRegiune neselectata",
            regionLore(region)
        ))

        if (region != null) {
            val regionType = RegionType.fromId(region.typeId())
            val identity = RegionIdentityProvider.identity(regionType)
            context.item(9, GuiItemFactory.item(
                Material.AMETHYST_SHARD, "&dIdentitate: ${identity.displayName}",
                listOf(
                    "&7Tip: &f${identity.displayName}",
                    "&7Descriere: &f${identity.description}",
                    "&7Poveste: &f${identity.defaultStoryKey} &8(${identity.mood})",
                    "&7Threat: &f${identity.threatLevel}",
                    "&7Atmosfera: &f${identity.ambiance}"
                )
            ))
            context.item(10, GuiItemFactory.item(
                Material.NAME_TAG, "&bDetalii",
                listOf(
                    "&7ID: &f${region.id()}",
                    "&7Tip: &f${region.typeId()}",
                    "&7Lume: &f${region.worldName()}"
                )
            ))
            context.item(11, GuiItemFactory.item(
                Material.NAME_TAG, "&eTags",
                listOf("&7${region.tags().joinToString(", ").ifBlank { "<fara tags>" }}")
            ))
            context.item(12, GuiItemFactory.item(
                Material.COMPASS, "&7Delimitare",
                listOf(
                    "&7X: &f${region.minX()} - ${region.maxX()}",
                    "&7Y: &f${region.minY()} - ${region.maxY()}",
                    "&7Z: &f${region.minZ()} - ${region.maxZ()}"
                )
            ))
            context.item(13, GuiItemFactory.item(
                Material.AMETHYST_SHARD, "&dStory",
                listOf(
                    "&7Mod: &f${region.storyMode().id}",
                    "&7State: &f${region.storyStateKey()}"
                )
            ))

            val env = context.plugin().environmentEngine.getContext(region.worldName())
            context.item(7, GuiItemFactory.item(EnvironmentUi.icon(env), EnvironmentUi.title(env), EnvironmentUi.lore(env)))

            context.item(14, GuiItemFactory.item(
                Material.OAK_DOOR, "&aPlaces (${
                    if (places.size > 7) "afișate 7/${places.size}" else places.size
                })",
                places.take(7).map { "&7- &f${it.displayName()} &8(${it.id()})" }.ifEmpty { listOf("&8<fara places>") }
            ))

            var slot = 19
            for (place in places.take(7)) {
                context.button(slot++, GuiButton.enabled(
                    GuiItemFactory.item(Material.OAK_DOOR, "&f${place.displayName()}", placeLore(place)),
                    action = { click -> click.service().openPlaceDetail(click.player(), place.id()) }
                ))
            }

            context.button(28, if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.ENDER_PEARL, "&6Teleport",
                        "&7Teleporteaza-te la centrul regiunii."),
                    action = { click -> click.service().runCommand(click.player(), "ainpc world region info ${region.id()}") }
                )
            } else GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Teleport", listOf("&7Necesita admin."))))

            context.button(29, if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.SPYGLASS, "&6Audit",
                        "&7Ruleaza audit pentru aceasta regiune."),
                    action = { click -> click.service().runCommand(click.player(), "ainpc audit world") }
                )
            } else GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Audit", listOf("&7Necesita admin."))))

            context.button(30, if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.ANVIL, "&eEdit region",
                        listOf("&7Recentreaza bounds-urile pe pozitia curenta.", "&7Editare rapida din GUI.")),
                    action = { click -> click.service().runCommand(click.player(), "ainpc world region edit ${region.id()}") }
                )
            } else GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Edit region", listOf("&7Necesita admin."))))

            context.button(31, if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.BARRIER, "&cDelete region",
                        listOf("&7Sterge regiunea curenta.", "&cActiune destructiva cu confirmare.")),
                    action = { click ->
                        click.service().openConfirmCommand(
                            click.player(),
                            "Sterge regiune",
                            "ainpc world region remove ${region.id()}",
                            GuiKey.REGION,
                            region.id(),
                            listOf("&7ID: &f${region.id()}", "&cSterge regiunea si datele dependente.")
                        )
                    }
                )
            } else GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Delete region", listOf("&7Necesita admin."))))
        }

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun regionLore(region: WorldRegionInfo?): List<String> {
        if (region == null) return listOf("&7Nicio regiune selectata.")
        return listOf(
            "&7ID: &f${region.id()}",
            "&7Tip: &f${region.typeId()}",
            "&7Tags: &f${region.tags().joinToString(", ").ifBlank { "<fara>" }}",
            "&7Lume: &f${region.worldName()}"
        )
    }

    private fun placeLore(place: WorldPlaceInfo): List<String> {
        return listOf(
            "&7Tip: &f${place.placeType().id}",
            "&7Owner: &f${place.ownerNpcId().ifBlank { "<niciunul>" }}",
            "&7Click: detalii place"
        )
    }
}
