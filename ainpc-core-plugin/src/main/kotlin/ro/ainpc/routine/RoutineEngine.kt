package ro.ainpc.routine

import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState

class RoutineEngine {
    fun assign(npc: AINPC?, worldTime: Long): RoutineAssignment {
        if (npc == null) {
            return idle("nu exista NPC valid")
        }

        val time = normalizeWorldTime(worldTime)
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
                work(npc, "merge la lucru")
            } else {
                fallbackDay(npc, "nu are loc de munca mapat")
            }
        }

        if (time >= 12000 && time < 16000) {
            return if (hasAnchor(npc.workAnchor)) {
                work(npc, "inchide treburile principale ale zilei")
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
            workStateFor(npc.occupation),
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
