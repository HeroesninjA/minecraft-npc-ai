package ro.ainpc.npc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NpcDefinitionsTest {
    @Test
    fun scenarioDefinitionDefaultsToTemporaryLightNpc() {
        val definition = NpcScenarioActorDefinition(id = "scene_guard", name = "Garda Scenei")

        assertEquals(NpcLifecycleType.TEMPORARY, definition.lifecycleType)
        assertEquals(NpcPersistenceMode.LIGHT, definition.persistenceMode)
        assertEquals(NpcSimulationMode.LIGHT, definition.simulationMode)
        assertEquals(NpcInteractionProfile.MINIMAL, definition.interactionProfile)
        assertEquals(NpcEntityKind.VILLAGER, definition.entityKind)
        assertFalse(definition.isRuntimeOnly())
    }

    @Test
    fun applyScenarioDefinitionCopiesScenarioFields() {
        val npc = AINPC(null)
        val definition = NpcScenarioActorDefinition(
            id = "dungeon_spirit",
            name = "Spiritul Din Cripta",
            lifecycleType = NpcLifecycleType.SCENE_ONLY,
            persistenceMode = NpcPersistenceMode.RUNTIME_ONLY,
            simulationMode = NpcSimulationMode.NONE,
            interactionProfile = NpcInteractionProfile.SCENE_ONLY,
            entityKind = NpcEntityKind.SPIRIT,
            entityArchetype = "wraith",
            ownerScenarioId = "medieval",
            ownerQuestId = "q07",
            spawnSource = "dungeon_intro",
            despawnRule = "on_stage_complete",
            durationSeconds = 120,
            temporaryTags = setOf("undead", "dungeon", "scene")
        )

        npc.applyScenarioDefinition(definition)

        assertEquals("dungeon_spirit", npc.sourceKey)
        assertEquals("Spiritul Din Cripta", npc.name)
        assertEquals(NpcLifecycleType.SCENE_ONLY, npc.lifecycleType)
        assertEquals(NpcPersistenceMode.RUNTIME_ONLY, npc.persistenceMode)
        assertEquals(NpcSimulationMode.NONE, npc.simulationMode)
        assertEquals(NpcInteractionProfile.SCENE_ONLY, npc.interactionProfile)
        assertEquals(NpcEntityKind.SPIRIT, npc.entityKind)
        assertEquals("wraith", npc.entityArchetype)
        assertEquals("medieval", npc.ownerScenarioId)
        assertEquals("q07", npc.ownerQuestId)
        assertEquals("dungeon_intro", npc.spawnSource)
        assertEquals("on_stage_complete", npc.despawnRule)
        assertEquals(120L, npc.durationSeconds)
        assertTrue(npc.temporaryTags.containsAll(setOf("undead", "dungeon", "scene")))
    }

    @Test
    fun spiritKindsResolveToArmorStandAdapter() {
        val npc = AINPC(null)
        npc.entityKind = NpcEntityKind.SPIRIT

        val adapter = NpcEntityAdapters.resolve(npc)

        assertTrue(adapter is ArmorStandNpcEntityAdapter)
    }
}
