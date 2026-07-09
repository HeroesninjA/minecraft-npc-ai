package ro.ainpc.routine

import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import java.util.UUID

class RoutineEngine(
    private val plugin: AINPCPlugin? = null,
    private val routineBiasResolver: ((String?, BehaviorProfile?) -> Long)? = null
) {
    private val profileResolver = RoutineProfileResolver(plugin)
    private val decisionResolver = RoutineDecisionResolver()
    val timeResolver = RoutineTimeResolver()
    private val previewResolver = RoutinePreviewResolver(profileResolver, this::assign)

    private var externalBiasSupplier: ((UUID) -> Long)? = null

    fun setExternalBiasSupplier(supplier: (UUID) -> Long) {
        externalBiasSupplier = supplier
    }

    fun assign(npc: AINPC?, worldTime: Long): RoutineAssignment {
        if (npc == null) {
            return decisionResolver.assign(null, null, null, 0L)
        }

        val time = timeResolver.normalizeWorldTime(worldTime)
        val occupation = npc.occupation
        val profile = profileResolver.resolveProfile(npc)
        val defaultProfile = profileResolver.defaultProfile()
        val profileBias = routineBiasResolver?.invoke(occupation, profile) ?: profile?.routineBiasTicks ?: 0L
        val externalBias = npc.uuid?.let { externalBiasSupplier?.invoke(it) } ?: 0L
        val routineBiasTicks = profileBias + externalBias
        val routineTime = timeResolver.routineTimeFor(npc, time, routineBiasTicks)
        return decisionResolver.assign(npc, profile, defaultProfile, routineTime)
    }

    fun previewDay(npc: AINPC?): List<RoutineScheduleEntry> = previewResolver.previewDay(npc)
}
