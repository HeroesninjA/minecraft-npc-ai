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
import ro.ainpc.progression.ProgressionGuiEntry
import ro.ainpc.progression.ProgressionGuiSnapshot
import java.util.ArrayList
import java.util.Comparator
import java.util.Locale
import java.util.Optional

class NpcInteractionGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.INTERACT

    override fun title(player: Player): String = "&0AINPC Interactiune"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        val location = player.location
        val adminView = player.hasPermission("ainpc.admin")
        val progressionSnapshot = context.plugin().progressionService
            .getProgressionGuiSnapshot(player, "all", adminView)
        val nearbyNpcs = context.plugin().npcManager.getNPCsNear(location, 32.0).stream()
            .sorted(Comparator.comparingDouble { npc: AINPC -> distanceSquared(location, npc.location) })
            .limit(NPC_SLOTS.size.toLong())
            .toList()
        val nearestProgression = nearestProgression(nearbyNpcs, progressionSnapshot)

        context.item(
            4,
            GuiItemFactory.item(
                Material.VILLAGER_SPAWN_EGG,
                "&aInteractiuni NPC",
                listOf(
                    "&7NPC-uri in raza 32: &f${nearbyNpcs.size}",
                    "&7Progresii vizibile: &f${progressionSnapshot.allEntries().size}",
                    "&7Click pe card: info NPC.",
                    "&7Right click pe card: status progresie.",
                    "&7Shift click pe card: detalii progresie.",
                    "&7Pentru dialog direct: click dreapta pe NPC in lume."
                )
            )
        )

        for (index in nearbyNpcs.indices) {
            val npc = nearbyNpcs[index]
            val distance = Math.sqrt(distanceSquared(location, npc.location))
            val progression = primaryProgressionForNpc(progressionSnapshot, npc).orElse(null)
            context.button(
                NPC_SLOTS[index],
                GuiButton.enabled(
                    GuiItemFactory.item(Material.PLAYER_HEAD, "&f${npc.name}", npcLore(npc, progression, distance)),
                    GuiAction { click ->
                        if (progression != null && click.clickType().isShiftClick) {
                            click.service().openQuestDetail(
                                click.player(),
                                progression.guiDetailSelector(),
                                progression.guiFilter()
                            )
                        } else if (click.clickType().isRightClick) {
                            click.service().runCommand(
                                click.player(),
                                if (progression != null) progression.command("status")
                                else "ainpc quest status ${npc.name}"
                            )
                        } else {
                            click.service().runCommand(click.player(), "ainpc info ${npc.name}")
                        }
                    }
                )
            )
        }

        val nearestRoot = if (nearestProgression.entryValue != null) {
            nearestProgression.entryValue.commandRoot()
        } else {
            "quest"
        }
        val nearestStatusCommand = if (nearestProgression.entryValue != null) {
            nearestProgression.entryValue.command("status")
        } else {
            "ainpc quest status nearest"
        }

        context.button(
            46,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.WRITABLE_BOOK,
                    "&eProgresie nearest",
                    "&7Declanseaza progresia relevanta a celui",
                    "&7mai apropiat NPC cu progres vizibil.",
                    "&7Comanda: &f/ainpc $nearestRoot nearest"
                ),
                GuiAction { click -> click.service().runCommand(click.player(), "ainpc $nearestRoot nearest") }
            )
        )
        context.button(
            47,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.LIME_DYE,
                    "&aAccepta nearest",
                    "&7Accepta oferta celui mai apropiat NPC",
                    "&7pentru mecanica relevanta.",
                    "&7Comanda: &f/ainpc $nearestRoot accept nearest"
                ),
                GuiAction { click -> click.service().runCommand(click.player(), "ainpc $nearestRoot accept nearest") }
            )
        )
        context.button(
            48,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.COMPASS,
                    "&aStatus nearest",
                    "&7Afiseaza statusul progresiei apropiate.",
                    "&7Comanda: &f/$nearestStatusCommand"
                ),
                GuiAction { click -> click.service().runCommand(click.player(), nearestStatusCommand) }
            )
        )
        context.button(
            50,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.MAP,
                    "&bStory nearest",
                    "&7Afiseaza context story pentru cel mai apropiat NPC."
                ),
                GuiAction { click ->
                    click.service().runCommand(click.player(), "ainpc story context ${click.player().name} nearest")
                }
            )
        )
        context.button(
            51,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.CLOCK,
                    "&eRutina nearest",
                    "&7Afiseaza programul celui mai apropiat NPC."
                ),
                GuiAction { click -> click.service().runCommand(click.player(), "ainpc routine status nearest") }
            )
        )

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun npcLore(npc: AINPC, progression: ProgressionGuiEntry?, distance: Double): List<String> {
        val lore = ArrayList<String>()
        lore.add("&7Ocupatie: &f${valueOrUnknown(npc.occupation)}")
        lore.add("&7Stare: &f${npc.currentState.displayName}")
        lore.add("&7Rutina: &f${valueOrUnknown(npc.plannedRoutineActivity)}")
        lore.add("&7Emotie: &f${npc.emotions.dominantEmotion}")
        lore.add("&7Distanta: &f${String.format(Locale.ROOT, "%.1f", distance)}")
        if (progression != null) {
            lore.add("&7Progresie: &f${GuiItemFactory.compact(progression.title(), 28)}")
            lore.add("&7Mecanica: &f${valueOrUnknown(progression.mechanicDisplay())}")
            lore.add("&7Status progresie: &f${valueOrUnknown(progression.statusDisplay())}")
        } else {
            lore.add("&8Nu exista progresie vizibila pentru acest NPC.")
        }
        lore.add("&8Click: info")
        lore.add(
            if (progression != null) "&8Right click: status ${progression.commandRoot()}"
            else "&8Right click: quest status fallback"
        )
        if (progression != null) {
            lore.add("&8Shift click: detalii progresie")
        }
        return lore
    }

    private fun nearestProgression(nearbyNpcs: List<AINPC>?, snapshot: ProgressionGuiSnapshot): NearbyProgression {
        if (nearbyNpcs.isNullOrEmpty()) {
            return NearbyProgression(null, null)
        }

        for (npc in nearbyNpcs) {
            val progression = primaryProgressionForNpc(snapshot, npc)
            if (progression.isPresent) {
                return NearbyProgression(npc, progression.get())
            }
        }
        return NearbyProgression(nearbyNpcs[0], null)
    }

    private fun primaryProgressionForNpc(snapshot: ProgressionGuiSnapshot?, npc: AINPC?): Optional<ProgressionGuiEntry> {
        if (snapshot == null || npc == null) {
            return Optional.empty()
        }

        return snapshot.allEntries().stream()
            .filter { entry -> actorMatchesNpc(entry, npc) }
            .min(
                Comparator
                    .comparingInt<ProgressionGuiEntry> { entry -> progressionPriority(entry) }
                    .thenComparing(ProgressionGuiEntry::updatedAt, Comparator.reverseOrder())
                    .thenComparing(ProgressionGuiEntry::title, String.CASE_INSENSITIVE_ORDER)
            )
    }

    private fun progressionPriority(entry: ProgressionGuiEntry): Int {
        if (entry.tracked()) {
            return 0
        }
        if (entry.current()) {
            return 1
        }
        if (entry.active()) {
            return 2
        }
        if (entry.offered()) {
            return 3
        }
        if (entry.archived()) {
            return 5
        }
        return 4
    }

    private fun actorMatchesNpc(entry: ProgressionGuiEntry, npc: AINPC): Boolean {
        val actorName = normalize(entry.actorName())
        if (actorName.isBlank()) {
            return false
        }
        return actorName == normalize(npc.name) || actorName == normalize(npc.displayName)
    }

    private fun distanceSquared(playerLocation: Location?, npcLocation: Location?): Double {
        if (playerLocation == null || npcLocation == null || playerLocation.world != npcLocation.world) {
            return Double.MAX_VALUE
        }
        return playerLocation.distanceSquared(npcLocation)
    }

    private fun valueOrUnknown(value: String?): String = if (value.isNullOrBlank()) "necunoscut" else value

    private fun normalize(value: String?): String = value?.trim()?.lowercase(Locale.ROOT).orEmpty()

    private class NearbyProgression(
        val npcValue: AINPC?,
        val entryValue: ProgressionGuiEntry?
    )

    companion object {
        private val NPC_SLOTS = intArrayOf(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
        )
    }
}
