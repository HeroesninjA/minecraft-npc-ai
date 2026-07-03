package ro.ainpc.routine

import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC

class RoutineEngine(
    private val plugin: AINPCPlugin? = null,
    private val routineBiasResolver: ((String?, BehaviorProfile?) -> Long)? = null
) {
    private val profileResolver = RoutineProfileResolver(plugin)
    private val decisionResolver = RoutineDecisionResolver()
    private val timeResolver = RoutineTimeResolver()
    private val previewResolver = RoutinePreviewResolver(profileResolver, this::assign)

    fun assign(npc: AINPC?, worldTime: Long): RoutineAssignment {
        if (npc == null) {
            return decisionResolver.assign(null, null, null, 0L)
        }

        val time = timeResolver.normalizeWorldTime(worldTime)
        val occupation = npc.occupation
        val profile = profileResolver.resolveProfile(npc)
        val defaultProfile = profileResolver.defaultProfile()
        val routineBiasTicks = routineBiasResolver?.invoke(occupation, profile) ?: profile?.routineBiasTicks ?: 0L
        val routineTime = timeResolver.routineTimeFor(npc, time, routineBiasTicks)
        return decisionResolver.assign(npc, profile, defaultProfile, routineTime)
    }

    fun previewDay(npc: AINPC?): List<RoutineScheduleEntry> = previewResolver.previewDay(npc)
}
