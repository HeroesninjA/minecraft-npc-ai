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
import ro.ainpc.routine.RoutineAssignment
import ro.ainpc.routine.RoutineScheduleEntry
import ro.ainpc.routine.RoutineSlot
import ro.ainpc.world.NpcWorldBinding
import java.sql.SQLException
import java.util.ArrayList
import java.util.Comparator
import java.util.Locale

class RoutineGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.ROUTINE

    override fun title(player: Player): String = "&0AINPC Rutine"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        val worldTime = player.world.time
        val adminView = player.hasPermission("ainpc.admin")
        val routineEnabled = context.plugin().config.getBoolean("routine.enabled", false)
        val npcs = context.plugin().npcManager.getAllNPCs().stream()
            .sorted(Comparator.comparing { npc: AINPC -> npc.name.lowercase(Locale.ROOT) })
            .limit(NPC_SLOTS.size.toLong())
            .toList()

        context.item(
            4,
            GuiItemFactory.item(
                if (routineEnabled) Material.CLOCK else Material.GRAY_DYE,
                if (routineEnabled) "&eRutine NPC" else "&7Rutine dezactivate",
                listOf(
                    "&7NPC-uri afisate: &f${npcs.size}&7/&f${context.plugin().npcManager.npcCount}",
                    "&7Timp world: &f${formatWorldTime(worldTime)}",
                    "&7Click card: status rutina.",
                    "&7Right click card: info NPC."
                )
            )
        )

        for (index in npcs.indices) {
            val npc = npcs[index]
            val current = context.plugin().routineService.preview(npc)
            val dayPreview = context.plugin().routineService.routineEngine.previewDay(npc)
            val binding = loadNpcWorldBinding(context, npc)
            context.button(
                NPC_SLOTS[index],
                GuiButton.enabled(
                    GuiItemFactory.item(slotMaterial(current.slot()), "&f${npc.name}", npcLore(npc, current, dayPreview, binding, adminView)),
                    GuiAction { click ->
                        if (adminView && click.clickType().isShiftClick) {
                            click.service().runCommand(click.player(), "ainpc world bindings npc ${npc.databaseId}")
                        } else if (!adminView || click.clickType().isRightClick) {
                            click.service().runCommand(click.player(), "ainpc info ${npc.name}")
                        } else {
                            click.service().runCommand(click.player(), "ainpc routine status ${npc.name}")
                        }
                    }
                )
            )
        }

        if (npcs.isEmpty()) {
            context.item(
                22,
                GuiItemFactory.item(
                    Material.LIGHT_GRAY_DYE,
                    "&7Fara NPC-uri",
                    "&7Nu exista NPC-uri incarcate pentru preview de rutina."
                )
            )
        }

        context.button(
            46,
            if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.COMPASS,
                        "&eStatus nearest",
                        "&7Ruleaza /ainpc routine status nearest pentru cel mai apropiat NPC."
                    ),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc routine status nearest") }
                )
            } else {
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&7Status nearest",
                        listOf("&7Necesita ainpc.admin.")
                    )
                )
            }
        )
        context.button(
            47,
            if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.REPEATER,
                        "&6Ruleaza tick rutina",
                        "&7Ruleaza manual evaluarea rutinelor pentru NPC-urile active."
                    ),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc routine tick") }
                )
            } else {
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&7Ruleaza tick rutina",
                        listOf("&7Necesita ainpc.admin.")
                    )
                )
            }
        )
        context.button(
            48,
            if (context.service().canOpen(player, GuiKey.MANAGER)) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.NAME_TAG, "&6Manager NPC", "&7Deschide managerul NPC admin."),
                    GuiAction { click -> click.service().open(click.player(), GuiKey.MANAGER) }
                )
            } else {
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&7Manager NPC",
                        listOf("&7Necesita permisiune pentru manager.")
                    )
                )
            }
        )

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun npcLore(
        npc: AINPC,
        current: RoutineAssignment,
        dayPreview: List<RoutineScheduleEntry>,
        binding: NpcWorldBinding?,
        adminView: Boolean
    ): List<String> {
        val lore = ArrayList<String>()
        lore.add("&7Slot curent: &f${slotLabel(current.slot())}")
        lore.add("&7Activitate: &f${GuiItemFactory.compact(current.activity(), 32)}")
        lore.add("&7Goal: &f${GuiItemFactory.compact(current.goal(), 32)}")
        lore.add("&7Stare tinta: &f${current.targetState()?.name ?: "UNKNOWN"}")
        lore.add("&7Tinta: &f${formatOwnedLocation(current.targetAnchor())}")
        addBindingLore(lore, binding)
        lore.add("&7Spawned: &f${if (npc.isSpawned()) "da" else "nu"}")
        lore.add("&7Locatie: &f${formatLocation(npc.location)}")
        lore.add("&8Program zi:")
        for (entry in dayPreview) {
            val assignment = entry.assignment() ?: continue
            lore.add(
                "&8- &7${entry.label()}: &f${slotLabel(assignment.slot())}" +
                    " &8/ &7${GuiItemFactory.compact(assignment.activity(), 22)}"
            )
        }
        lore.add(if (adminView) "&8Click: status rutina" else "&8Click: info NPC")
        lore.add("&8Right click: info NPC")
        if (adminView) {
            lore.add("&8Shift click: world bindings")
        }
        return lore
    }

    private fun loadNpcWorldBinding(context: GuiRenderContext, npc: AINPC): NpcWorldBinding? {
        if (context.plugin().npcWorldBindingService == null || npc.databaseId <= 0) {
            return null
        }
        return try {
            context.plugin().npcWorldBindingService.getBinding(npc.databaseId).orElse(null)
        } catch (_: SQLException) {
            null
        }
    }

    private fun addBindingLore(lore: MutableList<String>, binding: NpcWorldBinding?) {
        if (binding == null) {
            lore.add("&7Mapping place: &8nepersistat")
            return
        }
        lore.add(
            "&7Mapping place: &f" + compactBindingTriple(
                binding.homePlaceId(),
                binding.workPlaceId(),
                binding.socialPlaceId()
            )
        )
        lore.add(
            "&7Mapping node: &f" + compactBindingTriple(
                binding.homeNodeId(),
                binding.workNodeId(),
                binding.socialNodeId()
            )
        )
    }

    private fun compactBindingTriple(home: String?, work: String?, social: String?): String =
        GuiItemFactory.compact("H=${shortValue(home)} W=${shortValue(work)} S=${shortValue(social)}", 36)

    private fun shortValue(value: String?): String = if (value.isNullOrBlank()) "-" else value

    private fun slotMaterial(slot: RoutineSlot?): Material =
        when (slot ?: RoutineSlot.IDLE) {
            RoutineSlot.HOME -> Material.OAK_DOOR
            RoutineSlot.WORK -> Material.SMITHING_TABLE
            RoutineSlot.SOCIAL -> Material.BELL
            RoutineSlot.IDLE -> Material.LIGHT_GRAY_DYE
        }

    private fun slotLabel(slot: RoutineSlot?): String =
        when (slot ?: RoutineSlot.IDLE) {
            RoutineSlot.HOME -> "acasa"
            RoutineSlot.WORK -> "lucru"
            RoutineSlot.SOCIAL -> "social"
            RoutineSlot.IDLE -> "idle"
        }

    private fun formatOwnedLocation(location: AINPC.OwnedLocation?): String {
        if (location == null) {
            return "fara tinta"
        }
        val label = if (location.label().isNullOrBlank()) location.type() else location.label()
        return GuiItemFactory.compact(
            "$label @ ${Math.round(location.x())}, ${Math.round(location.y())}, ${Math.round(location.z())}",
            34
        )
    }

    private fun formatLocation(location: Location?): String {
        if (location == null || location.world == null) {
            return "necunoscuta"
        }
        return "${location.world.name} ${location.blockX}, ${location.blockY}, ${location.blockZ}"
    }

    private fun formatWorldTime(worldTime: Long): String {
        val normalized = ((worldTime % 24000L) + 24000L) % 24000L
        var hours = (normalized / 1000L + 6L) % 24L
        var minutes = Math.round((normalized % 1000L) * 60.0 / 1000.0)
        if (minutes == 60L) {
            minutes = 0L
            hours = (hours + 1L) % 24L
        }
        return String.format(Locale.ROOT, "%02d:%02d (%d ticks)", hours, minutes, normalized)
    }

    companion object {
        private val NPC_SLOTS = intArrayOf(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        )
    }
}
