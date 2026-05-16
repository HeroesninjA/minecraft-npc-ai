package ro.ainpc.gui.screens

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.npc.AINPC
import java.util.Comparator
import java.util.Locale

class StatsGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.STATS

    override fun title(player: Player): String = "&0AINPC Statistici"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        val location = player.location
        val nearbyNpcs = context.plugin().npcManager.getNPCsNear(location, 24.0).stream()
            .sorted(Comparator.comparing { npc: AINPC -> npc.name.lowercase(Locale.ROOT) })
            .toList()

        context.item(
            4,
            GuiItemFactory.item(
                Material.CLOCK,
                "&dSnapshot jucator",
                listOf(
                    "&7Jucator: &f${player.name}",
                    "&7World: &f${location.world.name}",
                    "&7Coordonate: &f${location.blockX}, ${location.blockY}, ${location.blockZ}",
                    "&7Level: &f${player.level}",
                    "&7Health: &f${String.format(Locale.ROOT, "%.1f", player.health)}",
                    "&7Food: &f${player.foodLevel}"
                )
            )
        )

        context.item(
            10,
            GuiItemFactory.item(
                Material.VILLAGER_SPAWN_EGG,
                "&aNPC-uri",
                listOf(
                    "&7Total incarcat: &f${context.plugin().npcManager.npcCount}",
                    "&7In apropiere: &f${nearbyNpcs.size}",
                    "&7Raza snapshot: &f24 block-uri"
                )
            )
        )
        context.button(
            11,
            GuiButton.enabled(
                GuiItemFactory.item(Material.WRITABLE_BOOK, "&eProgresii", "&7Deschide log-ul de progresii."),
                GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST) }
            )
        )
        context.button(
            12,
            GuiButton.enabled(
                GuiItemFactory.item(Material.COMPASS, "&bWorld", "&7Deschide contextul world."),
                GuiAction { click -> click.service().open(click.player(), GuiKey.WORLD) }
            )
        )

        var slot = 19
        for (npc in nearbyNpcs.stream().limit(14).toList()) {
            val npcLocation: Location? = npc.location
            val distance = if (npcLocation != null) npcLocation.distance(location) else -1.0
            context.button(
                slot++,
                GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.PLAYER_HEAD,
                        "&f${npc.name}",
                        listOf(
                            "&7Ocupatie: &f${valueOrUnknown(npc.occupation)}",
                            "&7Varsta: &f${npc.age}",
                            "&7Spawned: &f${if (npc.isSpawned) "da" else "nu"}",
                            "&7Emotie: &f${npc.emotions.dominantEmotion}",
                            "&7Distanta: &f${if (distance >= 0) String.format(Locale.ROOT, "%.1f", distance) else "necunoscuta"}",
                            "&8Click: /ainpc info"
                        )
                    ),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc info ${npc.name}") }
                )
            )
        }

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun valueOrUnknown(value: String?): String = if (value.isNullOrBlank()) "necunoscut" else value
}
