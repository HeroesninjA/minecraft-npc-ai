package ro.ainpc.spawn

import ro.ainpc.AINPCPlugin
import ro.ainpc.managers.NPCManager

class NpcPopulationService(
    private val plugin: AINPCPlugin,
    private val npcManager: NPCManager
) {
    fun getPopulationStats(): PopulationStats {
        val all = npcManager.getAllNPCs()
        val byWorld = mutableMapOf<String, Int>()
        val byProfession = mutableMapOf<String, Int>()
        var spawned = 0
        var despawned = 0
        for (npc in all) {
            val world = npc.worldName?.ifBlank { "unknown" } ?: "unknown"
            byWorld[world] = (byWorld[world] ?: 0) + 1
            val prof = npc.occupation?.ifBlank { "none" } ?: "none"
            byProfession[prof] = (byProfession[prof] ?: 0) + 1
            if (npc.isSpawned()) spawned++ else despawned++
        }
        return PopulationStats(all.size, spawned, despawned, byWorld.toMap(), byProfession.toMap())
    }

    fun getPopulationStats(worldName: String?): PopulationStats {
        if (worldName.isNullOrBlank()) return getPopulationStats()
        val all = npcManager.getAllNPCs().filter { npc ->
            npc.worldName?.equals(worldName, ignoreCase = true) == true
        }
        val byProfession = mutableMapOf<String, Int>()
        var spawned = 0
        var despawned = 0
        for (npc in all) {
            val prof = npc.occupation?.ifBlank { "none" } ?: "none"
            byProfession[prof] = (byProfession[prof] ?: 0) + 1
            if (npc.isSpawned()) spawned++ else despawned++
        }
        return PopulationStats(all.size, spawned, despawned, mapOf(worldName to all.size), byProfession.toMap())
    }

    data class PopulationStats(
        val total: Int,
        val spawned: Int,
        val despawned: Int,
        val byWorld: Map<String, Int>,
        val byProfession: Map<String, Int>
    )
}
