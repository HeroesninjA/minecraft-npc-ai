package ro.ainpc.managers

import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NpcPersistenceMode

internal object NpcPersistencePolicy {
    fun shouldSave(npc: AINPC): Boolean = npc.persistenceMode != NpcPersistenceMode.RUNTIME_ONLY
}
