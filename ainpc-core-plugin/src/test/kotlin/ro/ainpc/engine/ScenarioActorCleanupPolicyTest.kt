package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.NpcLifecycleType
import ro.ainpc.npc.NpcPersistenceMode
import java.util.UUID

class ScenarioActorCleanupPolicyTest {
    @Test
    fun `persistent transient actors are deleted while permanent actors remain stored`() {
        for (lifecycleType in listOf(
            NpcLifecycleType.TEMPORARY,
            NpcLifecycleType.EPISODIC,
            NpcLifecycleType.SCENE_ONLY,
        )) {
            for (persistenceMode in listOf(NpcPersistenceMode.FULL, NpcPersistenceMode.LIGHT)) {
                assertEquals(
                    ScenarioActorCleanupPolicy.Action.DELETE_PERSISTED,
                    ScenarioActorCleanupPolicy.decide(lifecycleType, persistenceMode, 42),
                )
            }
        }
        assertEquals(
            ScenarioActorCleanupPolicy.Action.DESPAWN,
            ScenarioActorCleanupPolicy.decide(NpcLifecycleType.PERMANENT, NpcPersistenceMode.FULL, 42),
        )
    }

    @Test
    fun `runtime only and unsaved actors are unregistered regardless of lifecycle`() {
        assertEquals(
            ScenarioActorCleanupPolicy.Action.UNREGISTER,
            ScenarioActorCleanupPolicy.decide(NpcLifecycleType.PERMANENT, NpcPersistenceMode.RUNTIME_ONLY, 0),
        )
        assertEquals(
            ScenarioActorCleanupPolicy.Action.UNREGISTER,
            ScenarioActorCleanupPolicy.decide(NpcLifecycleType.TEMPORARY, NpcPersistenceMode.LIGHT, 0),
        )
    }

    @Test
    fun `old expiration cannot target a replacement actor`() {
        val scenario = ActiveScenario(UUID.randomUUID(), ScenarioTemplate(ScenarioType.QUEST))
        val firstNpcUuid = UUID.randomUUID()
        val replacementNpcUuid = UUID.randomUUID()

        scenario.assignActor("guard", firstNpcUuid, 31_000L)
        assertTrue(scenario.isCurrentActor("guard", firstNpcUuid))
        assertEquals(31_000L, scenario.actorExpirationTimes["guard"])

        scenario.removeActor("guard")
        assertFalse(scenario.actorExpirationTimes.containsKey("guard"))
        scenario.assignActor("guard", replacementNpcUuid)
        assertFalse(scenario.isCurrentActor("guard", firstNpcUuid))
        assertTrue(scenario.isCurrentActor("guard", replacementNpcUuid))
    }
}
