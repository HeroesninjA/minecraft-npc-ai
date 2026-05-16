package ro.ainpc.gui.screens

import org.bukkit.Location
import org.bukkit.Material
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.npc.AINPC
import ro.ainpc.routine.RoutineAssignment
import ro.ainpc.routine.RoutineSlot
import java.util.ArrayList
import java.util.Comparator
import java.util.Locale

class NpcManagerGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.MANAGER

    override fun title(player: org.bukkit.entity.Player): String = "&0AINPC Manager NPC"

    override fun size(player: org.bukkit.entity.Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val npcs = context.plugin().npcManager.getAllNPCs().stream()
            .sorted(Comparator.comparing { npc: AINPC -> npc.getName().lowercase(Locale.ROOT) })
            .limit(NPC_SLOTS.size.toLong())
            .toList()

        context.item(
            4,
            GuiItemFactory.item(
                Material.NAME_TAG,
                "&6Manager NPC",
                listOf(
                    "&7Total NPC-uri: &f${context.plugin().npcManager.npcCount}",
                    "&7Click: /ainpc info",
                    "&7Right click: /ainpc tp",
                    "&7Shift click: routine/family"
                )
            )
        )

        for (index in npcs.indices) {
            val npc = npcs[index]
            val routine = context.plugin().routineService.preview(npc)
            context.button(
                NPC_SLOTS[index],
                GuiButton.enabled(
                    GuiItemFactory.item(cardMaterial(npc, routine.slot()), "&f${npc.getName()}", npcLore(npc, routine)),
                    GuiAction { click ->
                        if (click.clickType().isShiftClick && click.clickType().isRightClick) {
                            click.service().runCommand(click.player(), "ainpc family ${npc.getName()}")
                        } else if (click.clickType().isShiftClick) {
                            click.service().runCommand(click.player(), "ainpc routine status ${npc.getName()}")
                        } else if (click.clickType().isRightClick) {
                            click.service().runCommand(click.player(), "ainpc tp ${npc.getName()}")
                        } else {
                            click.service().runCommand(click.player(), "ainpc info ${npc.getName()}")
                        }
                    }
                )
            )
        }

        context.button(
            46,
            GuiButton.enabled(
                GuiItemFactory.item(Material.PAPER, "&aLista NPC", "&7Ruleaza /ainpc list."),
                GuiAction { click -> click.service().runCommand(click.player(), "ainpc list") }
            )
        )
        context.button(
            47,
            GuiButton.enabled(
                GuiItemFactory.item(Material.CLOCK, "&eRutine NPC", "&7Deschide preview-ul vizual al rutinelor."),
                GuiAction { click -> click.service().open(click.player(), GuiKey.ROUTINE) }
            )
        )
        context.button(
            48,
            GuiButton.enabled(
                GuiItemFactory.item(Material.REDSTONE_TORCH, "&cAudit NPC", "&7Ruleaza audit NPC."),
                GuiAction { click -> click.service().runCommand(click.player(), "ainpc audit npc") }
            )
        )
        context.button(
            50,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.REPEATER,
                    "&6Ruleaza tick rutina",
                    "&7Evalueaza manual rutinele NPC active."
                ),
                GuiAction { click -> click.service().runCommand(click.player(), "ainpc routine tick") }
            )
        )
        context.button(
            51,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.SPYGLASS,
                    "&9Debugdump NPC",
                    "&7Genereaza debugdump pentru runtime-ul NPC."
                ),
                GuiAction { click -> click.service().runCommand(click.player(), "ainpc debugdump npc") }
            )
        )

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun npcLore(npc: AINPC, routine: RoutineAssignment): List<String> {
        val lore = ArrayList<String>()
        lore.add("&7ID DB: &f${npc.getDatabaseId()}")
        lore.add("&7Ocupatie: &f${valueOrUnknown(npc.getOccupation())} &8/ &7varsta &f${npc.getAge()}")
        lore.add(
            "&7Spawned: &f${if (npc.isSpawned) "da" else "nu"}" +
                " &8/ &7stare &f${npc.getCurrentState().displayName}"
        )
        lore.add("&7Emotie: &f${npc.getEmotions().dominantEmotion}")
        lore.add("&7Rutina: &f${slotLabel(routine.slot())} &8/ &7${GuiItemFactory.compact(routine.activity(), 28)}")
        lore.add("&7Goal: &f${GuiItemFactory.compact(firstNonBlank(npc.getCurrentGoal(), routine.goal()), 32)}")
        lore.add("&7Tinta rutina: &f${formatOwnedLocation(routine.targetAnchor())}")
        lore.add(
            "&7Nevoi: &fsat ${npc.getHungerLevel()} &7en &f${npc.getEnergyLevel()}" +
                " &7sig &f${npc.getSafetyLevel()} &7conf &f${npc.getComfortLevel()}"
        )
        lore.add("&7Locatie: &f${formatLocation(npc.getLocation())}")
        lore.add(
            "&8Ancore: home=${shortAnchor(npc.getHomeAnchor())} " +
                "work=${shortAnchor(npc.getWorkAnchor())} social=${shortAnchor(npc.getSocialAnchor())}"
        )
        lore.add("&8Click: info")
        lore.add("&8Right click: teleport")
        lore.add("&8Shift click: rutina")
        lore.add("&8Shift right click: familie")
        return lore
    }

    private fun cardMaterial(npc: AINPC, slot: RoutineSlot?): Material {
        if (!npc.isSpawned) {
            return Material.GRAY_DYE
        }
        return when (slot ?: RoutineSlot.IDLE) {
            RoutineSlot.HOME -> Material.OAK_DOOR
            RoutineSlot.WORK -> Material.SMITHING_TABLE
            RoutineSlot.SOCIAL -> Material.BELL
            RoutineSlot.IDLE -> Material.PLAYER_HEAD
        }
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
        return GuiItemFactory.compact(
            "${firstNonBlank(location.label(), location.type())} @ " +
                "${Math.round(location.x())}, ${Math.round(location.y())}, ${Math.round(location.z())}",
            32
        )
    }

    private fun shortAnchor(location: AINPC.OwnedLocation?): String {
        if (location == null) {
            return "-"
        }
        return GuiItemFactory.compact(firstNonBlank(location.label(), location.type()), 10)
    }

    private fun formatLocation(location: Location?): String {
        if (location == null || location.world == null) {
            return "necunoscuta"
        }
        return "${location.world.name} ${location.blockX}, ${location.blockY}, ${location.blockZ}"
    }

    private fun valueOrUnknown(value: String?): String = if (value.isNullOrBlank()) "necunoscut" else value

    private fun firstNonBlank(vararg values: String?): String {
        for (value in values) {
            if (!value.isNullOrBlank()) {
                return value.trim()
            }
        }
        return ""
    }

    companion object {
        private val NPC_SLOTS = intArrayOf(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
        )
    }
}
