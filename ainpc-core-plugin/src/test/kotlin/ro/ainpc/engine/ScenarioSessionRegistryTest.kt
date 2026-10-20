package ro.ainpc.engine

import org.bukkit.Location
import org.bukkit.World
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NpcLifecycleType
import ro.ainpc.npc.NpcPersistenceMode
import ro.ainpc.npc.NpcScenarioActorDefinition
import java.lang.reflect.Proxy
import java.util.UUID

class ScenarioSessionRegistryTest {
    @Test
    fun `start uses the initial phase and copies the anchor and phases`() {
        val template = ScenarioTemplate(ScenarioType.THEFT).apply {
            templateId = "demo:theft"
            addPhase("PLANNING", "")
            addPhase("EXECUTION", "")
            addQuestActorTrigger("on_stage_enter", listOf("thief"))
        }
        val location = Location(null, 10.0, 64.0, -5.0)
        val registry = ScenarioSessionRegistry()

        val scenario = registry.start(template, location)
        assertNotNull(scenario)
        location.x = 99.0
        template.addPhase("LATE", "")
        template.questActorTriggers["on_stage_enter"]?.add("witness")

        assertEquals("PLANNING", scenario!!.currentPhase)
        assertEquals(listOf("PLANNING", "EXECUTION"), scenario.phases)
        assertEquals(setOf("thief"), scenario.questActorTriggers["on_stage_enter"])
        assertEquals(10.0, scenario.anchorLocation?.x)
        assertEquals(scenario, registry.activeScenarios[scenario.id])
        assertNull(registry.start(ScenarioTemplate(ScenarioType.THEFT).apply {
            templateId = "DEMO:THEFT"
            addPhase("PLANNING", "")
        }, location))
    }

    @Test
    fun `start rejects templates without a usable initial phase`() {
        val registry = ScenarioSessionRegistry()
        val location = Location(null, 0.0, 64.0, 0.0)

        assertNull(registry.start(ScenarioTemplate(ScenarioType.CONFLICT), location))
        assertNull(registry.start(ScenarioTemplate(ScenarioType.CONFLICT).apply {
            addPhase(" ", "")
        }, location))
        assertEquals(0, registry.activeScenarios.size)
    }

    @Test
    fun `restores a safe actorless session with its original id phase and start time`() {
        val template = ScenarioTemplate(ScenarioType.THEFT).apply {
            templateId = "demo:theft"
            addPhase("PLANNING", "")
            addPhase("EXECUTION", "")
        }
        val world = Proxy.newProxyInstance(World::class.java.classLoader, arrayOf(World::class.java)) {
            _, method, _ -> if (method.name == "getName") "test_world" else null
        } as World
        val snapshot = ScenarioSessionSnapshot(
            1, UUID.randomUUID(), "demo:theft", "EXECUTION", 1_234L,
            ScenarioSessionSnapshot.Anchor("test_world", 4.0, 70.0, 8.0, 45f, 10f),
            emptyMap(), emptyMap(), emptyMap(),
        )
        val registry = ScenarioSessionRegistry()

        val restored = registry.restore(template, snapshot, world)

        assertNotNull(restored)
        assertEquals(snapshot.scenarioId, restored!!.id)
        assertEquals(snapshot.startedAt, restored.startTime)
        assertEquals("EXECUTION", restored.currentPhase)
        assertEquals(4.0, restored.anchorLocation?.x)
        assertNull(registry.restore(template, snapshot, world))
        assertNull(registry.start(template, Location(world, 0.0, 64.0, 0.0)))
    }

    @Test
    fun `restores a verified permanent actor without spawning another NPC`() {
        val definition = NpcScenarioActorDefinition("guide", "Guide",
            lifecycleType = NpcLifecycleType.PERMANENT, persistenceMode = NpcPersistenceMode.LIGHT)
        val template = ScenarioTemplate(ScenarioType.THEFT).apply {
            templateId = "demo:theft"
            addPhase("PLANNING", "")
            addActor("guide", definition)
        }
        val world = Proxy.newProxyInstance(World::class.java.classLoader, arrayOf(World::class.java)) {
            _, method, _ -> if (method.name == "getName") "test_world" else null
        } as World
        val actorUuid = UUID.randomUUID()
        val snapshot = ScenarioSessionSnapshot(
            1, UUID.randomUUID(), "demo:theft", "PLANNING", 1_234L,
            ScenarioSessionSnapshot.Anchor("test_world", 4.0, 70.0, 8.0, 45f, 10f),
            emptyMap(), emptyMap(), mapOf("guide" to ScenarioSessionSnapshot.Actor(actorUuid, 42)),
            ScenarioTemplateRecoveryFingerprint.compute(template),
        )
        val npc = AINPC(null).apply {
            applyScenarioDefinition(definition)
            databaseId = 42
            uuid = actorUuid
            worldName = "test_world"
            profileSource = "scenario_actor"
            scenarioSessionId = snapshot.scenarioId
        }
        val registry = ScenarioSessionRegistry()

        val restored = registry.restore(template, snapshot, world) { id -> npc.takeIf { it.databaseId == id } }

        assertNotNull(restored)
        assertEquals(actorUuid, restored?.spawnedActors?.get("guide"))
        assertEquals(1, restored?.spawnedActors?.size)
        assertEquals(emptyMap<String, Long>(), restored?.actorExpirationTimes)
        assertNull(registry.restore(template, snapshot, world) { npc })
    }

    @Test
    fun `restores an unexpired permanent actor deadline without changing its identity`() {
        val definition = NpcScenarioActorDefinition("guide", "Guide",
            lifecycleType = NpcLifecycleType.PERMANENT, persistenceMode = NpcPersistenceMode.LIGHT,
            durationSeconds = 30L)
        val template = ScenarioTemplate(ScenarioType.THEFT).apply {
            templateId = "demo:timed"
            addPhase("START", "")
            addActor("guide", definition)
        }
        val world = Proxy.newProxyInstance(World::class.java.classLoader, arrayOf(World::class.java)) {
            _, method, _ -> if (method.name == "getName") "test_world" else null
        } as World
        val now = System.currentTimeMillis()
        val actorUuid = UUID.randomUUID()
        val snapshot = ScenarioSessionSnapshot(
            1, UUID.randomUUID(), "demo:timed", "START", now - 1_000L,
            ScenarioSessionSnapshot.Anchor("test_world", 4.0, 70.0, 8.0, 0f, 0f),
            emptyMap(), emptyMap(), mapOf("guide" to ScenarioSessionSnapshot.Actor(actorUuid, 42, now + 30_000L)),
            ScenarioTemplateRecoveryFingerprint.compute(template),
        )
        val npc = AINPC(null).apply {
            applyScenarioDefinition(definition)
            databaseId = 42
            uuid = actorUuid
            worldName = "test_world"
            profileSource = "scenario_actor"
            scenarioSessionId = snapshot.scenarioId
        }

        val restored = ScenarioSessionRegistry().restore(template, snapshot, world) { id ->
            npc.takeIf { it.databaseId == id }
        }

        assertNotNull(restored)
        assertEquals(actorUuid, restored?.spawnedActors?.get("guide"))
        assertEquals(now + 30_000L, restored?.actorExpirationTimes?.get("guide"))
    }

    @Test
    fun `restores retired actor state without respawning it`() {
        val template = ScenarioTemplate(ScenarioType.THEFT).apply {
            templateId = "demo:retired"
            addPhase("START", "")
            addActor("guide", NpcScenarioActorDefinition("guide", "Guide"))
        }
        val world = Proxy.newProxyInstance(World::class.java.classLoader, arrayOf(World::class.java)) {
            _, method, _ -> if (method.name == "getName") "test_world" else null
        } as World
        val snapshot = ScenarioSessionSnapshot(
            1, UUID.randomUUID(), "demo:retired", "START", 1_234L,
            ScenarioSessionSnapshot.Anchor("test_world", 4.0, 70.0, 8.0, 0f, 0f),
            emptyMap(), emptyMap(), emptyMap(),
            ScenarioTemplateRecoveryFingerprint.compute(template), setOf("guide"),
        )

        val restored = ScenarioSessionRegistry().restore(template, snapshot, world)

        assertNotNull(restored)
        assertEquals(emptyMap<String, UUID>(), restored?.spawnedActors)
        assertEquals(setOf("guide"), restored?.retiredActorIds)
    }
}
