package ro.ainpc.engine

import ro.ainpc.npc.NpcLifecycleType
import ro.ainpc.npc.NpcPersistenceMode

internal object ScenarioActorCleanupPolicy {
    enum class Action {
        DESPAWN,
        UNREGISTER,
        DELETE_PERSISTED,
    }

    fun decide(
        lifecycleType: NpcLifecycleType,
        persistenceMode: NpcPersistenceMode,
        databaseId: Int,
    ): Action = when {
        persistenceMode == NpcPersistenceMode.RUNTIME_ONLY || databaseId <= 0 -> Action.UNREGISTER
        lifecycleType == NpcLifecycleType.PERMANENT -> Action.DESPAWN
        else -> Action.DELETE_PERSISTED
    }
}
