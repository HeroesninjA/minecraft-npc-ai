package ro.ainpc.story

import ro.ainpc.api.WorldAdminApi
import ro.ainpc.world.WorldContextSnapshot
import kotlin.math.floor

internal class NpcBindingStorySignalResolver {
    fun collect(bindings: List<WorldContextSnapshot.NpcBindingInfo>, worldAdmin: WorldAdminApi?): List<String> {
        if (worldAdmin == null || !worldAdmin.isEnabled) return emptyList()
        val signals = linkedSetOf<String>()
        for (binding in bindings) {
            val role = binding.role()?.takeIf { it in setOf("home", "work", "social") } ?: continue
            val worldName = binding.worldName()?.takeIf(String::isNotBlank) ?: continue
            if (!binding.x().isFinite() || !binding.y().isFinite() || !binding.z().isFinite()) continue
            val blockX = floor(binding.x())
            val blockY = floor(binding.y())
            val blockZ = floor(binding.z())
            if (sequenceOf(blockX, blockY, blockZ).any { it < Int.MIN_VALUE || it > Int.MAX_VALUE }) continue
            val place = worldAdmin.findPlace(worldName,
                blockX.toInt(), blockY.toInt(), blockZ.toInt()) ?: continue
            signals.add("npc_${role}_place_id=${place.id()}")
        }
        return signals.toList()
    }
}
