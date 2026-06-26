package ro.ainpc.routine

import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState

class RoutineEngine(private val plugin: AINPCPlugin? = null) {
    fun assign(npc: AINPC?, worldTime: Long): RoutineAssignment {
        if (npc == null) {
            return idle("nu exista NPC valid")
        }

        val time = normalizeWorldTime(worldTime)

        val occupation = npc.occupation
        val profile = if (plugin != null && occupation != null) {
            val loader = BehaviorProfileLoader(plugin)
            loader.loadAll()
            loader.findProfileForOccupation(occupation)
        } else null

        if (profile != null && profile.schedule.isNotEmpty()) {
            for (entry in profile.schedule) {
                if (time >= entry.startTick && time < entry.endTick) {
                    val slot = when (entry.slot.uppercase()) {
                        "HOME" -> RoutineSlot.HOME
                        "WORK" -> RoutineSlot.WORK
                        "SOCIAL" -> RoutineSlot.SOCIAL
                        else -> RoutineSlot.IDLE
                    }
                    val anchor = when (slot) {
                        RoutineSlot.HOME -> npc.homeAnchor
                        RoutineSlot.WORK -> npc.workAnchor
                        RoutineSlot.SOCIAL -> npc.socialAnchor
                        RoutineSlot.IDLE -> null
                    }
                    return RoutineAssignment(slot, entry.activity, entry.target.ifBlank { entry.label }, when (slot) {
                        RoutineSlot.HOME -> NPCState.RESTING
                        RoutineSlot.WORK -> zoneWorkState(npc)
                        RoutineSlot.SOCIAL -> NPCState.SOCIALIZING
                        RoutineSlot.IDLE -> NPCState.IDLE
                    }, anchor)
                }
            }
        }

        if (time >= 18000 || time < 2000) {
            return home(
                npc,
                if (time >= 18000) "doarme acasa" else "se trezeste si isi pregateste ziua"
            )
        }

        if (npc.energyLevel < 25 || npc.hungerLevel < 25 || npc.safetyLevel < 35) {
            return home(npc, "revine acasa pentru siguranta si refacere")
        }

        if (npc.socialNeedLevel < 35 && hasAnchor(npc.socialAnchor) && time >= 8000 && time < 18000) {
            return social(npc, "merge la punctul social pentru a vorbi cu localnicii")
        }

        if (time >= 2000 && time < 12000) {
            return if (hasAnchor(npc.workAnchor)) {
                work(npc, zoneWorkActivity(npc, "merge la lucru"))
            } else {
                fallbackDay(npc, "nu are loc de munca mapat")
            }
        }

        if (time >= 12000 && time < 16000) {
            return if (hasAnchor(npc.workAnchor)) {
                work(npc, zoneWorkActivity(npc, "inchide treburile principale ale zilei"))
            } else {
                fallbackDay(npc, "nu are loc de munca mapat pentru dupa-amiaza")
            }
        }

        if (time >= 16000 && time < 18000) {
            return if (hasAnchor(npc.socialAnchor)) {
                social(npc, "se intalneste cu localnicii seara")
            } else {
                home(npc, "se intoarce acasa seara")
            }
        }

        return idle("isi urmeaza rutina obisnuita")
    }

    private fun zoneWorkState(npc: AINPC): NPCState {
        val zone = npc.context.topologyCategory
        if (zone == null) return NPCState.WORKING
        return when (zone) {
            ro.ainpc.topology.TopologyCategory.FOREST,
            ro.ainpc.topology.TopologyCategory.DARK_FOREST,
            ro.ainpc.topology.TopologyCategory.JUNGLE -> NPCState.FARMING
            ro.ainpc.topology.TopologyCategory.MOUNTAIN,
            ro.ainpc.topology.TopologyCategory.UNDERGROUND -> NPCState.MINING
            ro.ainpc.topology.TopologyCategory.RIVER,
            ro.ainpc.topology.TopologyCategory.COAST,
            ro.ainpc.topology.TopologyCategory.OCEAN -> NPCState.FISHING
            else -> NPCState.WORKING
        }
    }

    private fun zoneWorkActivity(npc: AINPC, base: String): String {
        val zone = npc.context.topologyCategory
        if (zone == null) return base
        return when (zone) {
            ro.ainpc.topology.TopologyCategory.FOREST,
            ro.ainpc.topology.TopologyCategory.DARK_FOREST -> "$base in padure"
            ro.ainpc.topology.TopologyCategory.PLAINS -> "$base pe camp"
            ro.ainpc.topology.TopologyCategory.MOUNTAIN -> "$base pe munte"
            ro.ainpc.topology.TopologyCategory.DESERT -> "$base in desert"
            ro.ainpc.topology.TopologyCategory.JUNGLE -> "$base in jungla"
            ro.ainpc.topology.TopologyCategory.UNDERGROUND -> "$base in subteran"
            ro.ainpc.topology.TopologyCategory.RIVER -> "$base la rau"
            ro.ainpc.topology.TopologyCategory.COAST -> "$base pe coasta"
            ro.ainpc.topology.TopologyCategory.INTERIOR -> "$base in interior"
            else -> base
        }
    }

    fun previewDay(npc: AINPC?): List<RoutineScheduleEntry> {
        return DAY_PREVIEW_POINTS.map { point ->
            RoutineScheduleEntry(point.label, point.worldTime, assign(npc, point.worldTime))
        }
    }

    private fun fallbackDay(npc: AINPC, reason: String): RoutineAssignment {
        if (hasAnchor(npc.socialAnchor)) {
            return social(npc, "$reason; foloseste punctul social")
        }
        if (hasAnchor(npc.homeAnchor)) {
            return home(npc, "$reason; ramane aproape de casa")
        }
        return idle(reason)
    }

    private fun home(npc: AINPC, activity: String): RoutineAssignment {
        val state = if (activity.contains("doarme")) NPCState.SLEEPING else NPCState.RESTING
        return RoutineAssignment(
            RoutineSlot.HOME,
            activity,
            "sa fie acasa",
            state,
            npc.homeAnchor
        )
    }

    private fun work(npc: AINPC, activity: String): RoutineAssignment {
        return RoutineAssignment(
            RoutineSlot.WORK,
            activity,
            "sa lucreze la " + describeAnchor(npc.workAnchor, "locul de munca"),
            workStateForNpc(npc),
            npc.workAnchor
        )
    }

    private fun social(npc: AINPC, activity: String): RoutineAssignment {
        return RoutineAssignment(
            RoutineSlot.SOCIAL,
            activity,
            "sa socializeze la " + describeAnchor(npc.socialAnchor, "punctul social"),
            NPCState.SOCIALIZING,
            npc.socialAnchor
        )
    }

    private fun idle(reason: String): RoutineAssignment {
        return RoutineAssignment(
            RoutineSlot.IDLE,
            reason,
            "sa astepte pana exista o ancora utila",
            NPCState.IDLE,
            null
        )
    }

    private fun workStateFor(occupation: String?): NPCState {
        return NPCState.WORKING
    }

    private fun workStateForNpc(npc: AINPC): NPCState = zoneWorkState(npc)

    private fun hasAnchor(anchor: AINPC.OwnedLocation?): Boolean {
        return anchor != null && !anchor.worldName().isNullOrBlank()
    }

    private fun describeAnchor(anchor: AINPC.OwnedLocation?, fallback: String): String {
        val label = anchor?.label()
        if (label.isNullOrBlank()) {
            return fallback
        }
        return label
    }

    private fun normalizeWorldTime(worldTime: Long): Long {
        val normalized = worldTime % 24000L
        return if (normalized < 0) normalized + 24000L else normalized
    }

    private data class SchedulePoint(val label: String, val worldTime: Long)

    companion object {
        private val DAY_PREVIEW_POINTS = listOf(
            SchedulePoint("Noapte", 19000L),
            SchedulePoint("Dimineata", 6000L),
            SchedulePoint("Pranz", 13000L),
            SchedulePoint("Seara", 17000L)
        )
    }
}
