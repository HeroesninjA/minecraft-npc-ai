package ro.ainpc.routine

import ro.ainpc.npc.AINPC

class RoutineTimeResolver {
    fun routineTimeFor(npc: AINPC, worldTime: Long, routineBiasTicks: Long): Long {
        val adjusted = (normalizeWorldTime(worldTime) + stableRoutineOffset(npc) + routineBiasTicks) % 24000L
        return if (adjusted < 0) adjusted + 24000L else adjusted
    }

    fun normalizeWorldTime(worldTime: Long): Long {
        val normalized = worldTime % 24000L
        return if (normalized < 0) normalized + 24000L else normalized
    }

    private fun stableRoutineOffset(npc: AINPC): Long {
        val key = buildString {
            append(npc.uuid.toString())
            append('|')
            append(npc.name)
        }
        return Math.floorMod(key.hashCode().toLong(), 1800L)
    }
}
