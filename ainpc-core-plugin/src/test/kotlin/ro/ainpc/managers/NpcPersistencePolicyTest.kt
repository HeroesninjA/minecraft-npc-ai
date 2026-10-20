package ro.ainpc.managers

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NpcPersistenceMode

class NpcPersistencePolicyTest {
    @Test
    fun `runtime only NPCs are never selected for a database save`() {
        val npc = AINPC(null)

        npc.persistenceMode = NpcPersistenceMode.RUNTIME_ONLY
        assertFalse(NpcPersistencePolicy.shouldSave(npc))
        npc.persistenceMode = NpcPersistenceMode.LIGHT
        assertTrue(NpcPersistencePolicy.shouldSave(npc))
        npc.persistenceMode = NpcPersistenceMode.FULL
        assertTrue(NpcPersistencePolicy.shouldSave(npc))
    }
}
