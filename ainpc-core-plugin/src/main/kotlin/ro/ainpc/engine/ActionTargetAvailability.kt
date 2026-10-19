package ro.ainpc.engine

import org.bukkit.util.NumberConversions
import ro.ainpc.npc.AINPC

internal object ActionTargetAvailability {
    fun hasUsableAnchor(anchor: AINPC.OwnedLocation?, chunkAvailable: (String, Int, Int) -> Boolean): Boolean {
        if (anchor == null || anchor.worldName().isBlank()) return false
        if (!anchor.x().isFinite() || !anchor.y().isFinite() || !anchor.z().isFinite()) return false
        val chunkX = NumberConversions.floor(anchor.x()) shr 4
        val chunkZ = NumberConversions.floor(anchor.z()) shr 4
        return chunkAvailable(anchor.worldName(), chunkX, chunkZ)
    }
}
