package ro.ainpc.routine

import ro.ainpc.npc.AINPC

class RoutinePreviewResolver(
    private val profileResolver: RoutineProfileResolver,
    private val assignmentProvider: (AINPC?, Long) -> RoutineAssignment
) {
    fun previewDay(npc: AINPC?): List<RoutineScheduleEntry> {
        return profileResolver.previewPoints(npc).map { point ->
            RoutineScheduleEntry(point.label, point.worldTime, assignmentProvider(npc, point.worldTime))
        }
    }
}
