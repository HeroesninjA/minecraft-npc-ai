package ro.ainpc.context

import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC

class ContextService(private val plugin: AINPCPlugin?) {

    fun buildPlayerContext(playerName: String): ContextSnapshot {
        if (plugin == null) return ContextSnapshot(playerName, 0, 0.0, 0)
        return ContextSnapshot.build(plugin, playerName)
    }

    fun buildNpcContext(playerName: String, npc: AINPC): ContextSnapshot {
        if (plugin == null) return ContextSnapshot(playerName, 0, 0.0, 0)
        return ContextSnapshot.build(plugin, playerName, npc)
    }

    fun buildCompactPromptBlock(playerName: String, npc: AINPC? = null): String {
        val snapshot = if (npc != null) buildNpcContext(playerName, npc) else buildPlayerContext(playerName)
        return snapshot.toPromptBlock()
    }

    fun buildShortSummary(playerName: String): String {
        val snap = buildPlayerContext(playerName)
        return buildString {
            append("${snap.playerName} | Lv${snap.playerLevel} | ${snap.currentRegion}")
            if (snap.currentPlace.isNotBlank()) append(" @ ${snap.currentPlace}")
            append(" | ${snap.economyBalance} coins | ${snap.activeQuestCount} quests")
        }
    }
}
