package ro.ainpc.engine

import org.bukkit.World
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NpcLifecycleType
import ro.ainpc.npc.NpcPersistenceMode
import ro.ainpc.npc.NpcScenarioActorDefinition
import java.lang.reflect.Proxy
import java.util.UUID

class ScenarioTransientRecoveryTest {
    private val now = System.currentTimeMillis()
    private val definition = NpcScenarioActorDefinition("guide", "Guide",
        lifecycleType = NpcLifecycleType.TEMPORARY, persistenceMode = NpcPersistenceMode.LIGHT,
        durationSeconds = 120L)
    private val template = ScenarioTemplate(ScenarioType.THEFT).apply {
        templateId = "demo:temporary"
        addPhase("START", "")
        addActor("guide", definition)
    }
    private val snapshot = ScenarioSessionSnapshot(
        1, UUID.randomUUID(), template.templateId, "START", now - 90_000L,
        ScenarioSessionSnapshot.Anchor("world", 0.0, 64.0, 0.0, 0f, 0f),
        emptyMap(), emptyMap(),
        mapOf("guide" to ScenarioSessionSnapshot.Actor(UUID.randomUUID(), 42, now + 60_000L)),
        ScenarioTemplateRecoveryFingerprint.compute(template),
    )
    private val npc = AINPC(null).apply {
        applyScenarioDefinition(definition)
        databaseId = 42
        uuid = snapshot.actors.getValue("guide").uuid
        worldName = "world"
        profileSource = "scenario_actor"
        scenarioSessionId = snapshot.scenarioId
    }
    private val world = Proxy.newProxyInstance(World::class.java.classLoader, arrayOf(World::class.java)) {
        _, method, _ -> if (method.name == "getName") "world" else throw UnsupportedOperationException(method.name)
    } as World
    private val registry = ScenarioSessionRegistry()
    private val registered = mutableMapOf<Int, AINPC>()

    @Test
    fun `transient restore requires live identity verification in addition to the saved metadata`() {
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(snapshot, template, { npc }, now))
        assertTrue(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(
            snapshot, template, { npc }, now, transientActorReady = { true }))

        npc.scenarioSessionId = UUID.randomUUID()
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(
            snapshot, template, { npc }, now, transientActorReady = { true }))
        npc.scenarioSessionId = snapshot.scenarioId
        npc.uuid = UUID.randomUUID()
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(
            snapshot, template, { npc }, now, transientActorReady = { true }))
    }

    @Test
    fun `live verification does not bypass expiration fingerprint or runtime only persistence`() {
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(
            snapshot, template, { npc }, now + 60_000L, transientActorReady = { true }))
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(
            snapshot.copy(templateFingerprint = null), template, { npc }, now, transientActorReady = { true }))
        template.actors["guide"] = definition.copy(persistenceMode = NpcPersistenceMode.RUNTIME_ONLY)
        npc.persistenceMode = NpcPersistenceMode.RUNTIME_ONLY
        val runtimeSnapshot = snapshot.copy(templateFingerprint = ScenarioTemplateRecoveryFingerprint.compute(template))
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(
            runtimeSnapshot, template, { npc }, now, transientActorReady = { true }))
    }

    @Test
    fun `verified temporary episodic and scene actors retain identity deadline and start time`() {
        for (lifecycle in listOf(NpcLifecycleType.TEMPORARY, NpcLifecycleType.EPISODIC, NpcLifecycleType.SCENE_ONLY)) {
            template.actors["guide"] = definition.copy(lifecycleType = lifecycle)
            npc.lifecycleType = lifecycle
            val updated = snapshot.copy(templateFingerprint = ScenarioTemplateRecoveryFingerprint.compute(template))
            registered[npc.databaseId] = npc
            val restored = registry.restore(template, updated, world, { true }, registered::get)

            assertNotNull(restored)
            assertEquals(snapshot.scenarioId, restored!!.id)
            assertEquals(snapshot.startedAt, restored.startTime)
            assertEquals(npc.uuid, restored.spawnedActors["guide"])
            assertEquals(now + 60_000L, restored.actorExpirationTimes["guide"])
            registry.activeScenarios.clear()
        }
    }

    @Test
    fun `mixed permanent and transient actors restore from the same snapshot`() {
        val permanentDefinition = NpcScenarioActorDefinition("elder", "Elder",
            lifecycleType = NpcLifecycleType.PERMANENT, persistenceMode = NpcPersistenceMode.LIGHT)
        template.addActor("elder", permanentDefinition)
        val permanent = AINPC(null).apply {
            applyScenarioDefinition(permanentDefinition)
            databaseId = 43
            worldName = "world"
            profileSource = "scenario_actor"
            scenarioSessionId = snapshot.scenarioId
        }
        val mixedSnapshot = snapshot.copy(
            actors = snapshot.actors + ("elder" to ScenarioSessionSnapshot.Actor(permanent.uuid, 43)),
            templateFingerprint = ScenarioTemplateRecoveryFingerprint.compute(template),
        )
        registered[42] = npc
        registered[43] = permanent
        var transientChecks = 0

        val restored = registry.restore(template, mixedSnapshot, world,
            transientActorReady = { transientChecks++; it === npc }, actorById = registered::get)

        assertNotNull(restored)
        assertEquals(2, restored!!.spawnedActors.size)
        assertEquals(npc.uuid, restored.spawnedActors["guide"])
        assertEquals(permanent.uuid, restored.spawnedActors["elder"])
        assertEquals(1, transientChecks)
    }

    @Test
    fun `preview can recognize a verified transient candidate without activating it`() {
        val preview = ScenarioSessionRecoveryInspector.inspect(
            StoredScenarioSession(snapshot.scenarioId.toString(), snapshot.templateId, 1, now, snapshot, null),
            template, { true }, { npc }, { null }, 1, false,
            now = now, transientActorReady = { true },
        )

        assertTrue(preview.automaticRestoreEligible)
        assertFalse(preview.activeInRuntime)
        assertTrue(registry.activeScenarios.isEmpty())
        assertTrue(registered.isEmpty())
    }

    @Test
    fun `successful activation commits the session and newly registered actors`() {
        var timerResumed = false
        val result = activate {
            timerResumed = true
            assertEquals(now + 60_000L, it.actorExpirationTimes["guide"])
            true
        }

        assertNotNull(result)
        assertTrue(timerResumed)
        assertEquals(npc, registered[42])
        assertEquals(result, registry.activeScenarios[snapshot.scenarioId])
    }

    @Test
    fun `timer failure or exception rolls back both the new session and actors`() {
        assertNull(activate { false })
        assertTrue(registered.isEmpty())
        assertTrue(registry.activeScenarios.isEmpty())

        assertThrows(IllegalStateException::class.java) { activate { error("scheduler unavailable") } }
        assertTrue(registered.isEmpty())
        assertTrue(registry.activeScenarios.isEmpty())
    }

    @Test
    fun `failed registration and late validation failure leave no partial activation`() {
        val result = ScenarioRecoveryActivation.activate(
            registerActors = { registered[42] = npc; false },
            releaseActors = { registered.clear() },
            restoreSession = { error("must not restore") },
            removeSession = { error("no session exists") },
            resumeTimers = { error("must not schedule") },
        )
        assertNull(result)
        assertTrue(registered.isEmpty())

        template.progressionEnabled = true
        assertNull(activate { error("incompatible session must not schedule") })
        assertTrue(registered.isEmpty())
        assertTrue(registry.activeScenarios.isEmpty())
    }

    private fun activate(resumeTimers: (ActiveScenario) -> Boolean): ActiveScenario? = ScenarioRecoveryActivation.activate(
        registerActors = { registered[42] = npc; true },
        releaseActors = { registered.remove(42, npc) },
        restoreSession = { registry.restore(template, snapshot, world, { true }, registered::get) },
        removeSession = { registry.activeScenarios.remove(it.id, it) },
        resumeTimers = resumeTimers,
    )
}
