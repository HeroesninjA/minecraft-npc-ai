package ro.ainpc.engine

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.Location
import org.bukkit.World
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.NpcScenarioActorDefinition
import ro.ainpc.npc.NpcPersistenceMode
import java.lang.reflect.Proxy
import java.util.UUID

class ScenarioSessionSnapshotCodecTest {
    @Test
    fun `captures and round trips a versioned scenario snapshot`() {
        val template = ScenarioTemplate(ScenarioType.QUEST).apply {
            templateId = "pack:quest"
            addPhase("INTRO", "")
            addActor("guide", NpcScenarioActorDefinition("guide", "Guide"))
        }
        val world = Proxy.newProxyInstance(World::class.java.classLoader, arrayOf(World::class.java)) {
            _, method, _ -> if (method.name == "getName") "test_world" else null
        } as World
        val scenario = ScenarioSessionRegistry().start(template, Location(world, 2.0, 65.0, -4.0))!!
        val actorUuid = UUID.randomUUID()
        val expiresAt = scenario.startTime + 30_000L
        scenario.assignActor("guide", actorUuid, expiresAt)
        scenario.assignNPCRole(actorUuid, "GUIDE")
        scenario.assignPlayerRole(UUID.randomUUID(), "HERO")

        val captured = ScenarioSessionSnapshotCodec.capture(scenario) { if (it == actorUuid) 42 else null }
        assertNotNull(captured)
        val encoded = ScenarioSessionSnapshotCodec.encode(captured!!)
        val decoded = ScenarioSessionSnapshotCodec.decode(encoded)

        assertEquals(captured, decoded)
        assertEquals(ScenarioSessionSnapshotCodec.VERSION, decoded?.version)
        assertEquals(ScenarioTemplateRecoveryFingerprint.compute(template), decoded?.templateFingerprint)
        assertEquals("test_world", decoded?.anchor?.world)
        assertEquals(42, decoded?.actors?.get("guide")?.databaseId)
        assertEquals(expiresAt, decoded?.actors?.get("guide")?.expiresAt)
        assertTrue(ScenarioSessionSnapshotCodec.templateIssues(decoded!!, template).isEmpty())
    }

    @Test
    fun `rejects malformed or unsupported snapshots and reports template mismatches`() {
        val template = ScenarioTemplate(ScenarioType.THEFT).apply {
            templateId = "pack:theft"
            addPhase("START", "")
        }
        val snapshot = ScenarioSessionSnapshot(
            1, UUID.randomUUID(), "pack:theft", "MISSING", 100L,
            ScenarioSessionSnapshot.Anchor("world", 1.0, 2.0, 3.0, 0f, 0f),
            emptyMap(), emptyMap(),
            mapOf("thief" to ScenarioSessionSnapshot.Actor(UUID.randomUUID(), null)),
        )
        val encoded = ScenarioSessionSnapshotCodec.encode(snapshot)
        assertNull(ScenarioSessionSnapshotCodec.decode(encoded)?.actors?.get("thief")?.expiresAt)
        val issues = ScenarioSessionSnapshotCodec.templateIssues(snapshot, template)

        assertTrue(issues.contains("faza absenta din template"))
        assertTrue(issues.contains("actor absent din template"))
        assertTrue(issues.contains("actor fara rand DB"))
        assertNull(ScenarioSessionSnapshotCodec.decode("not-json"))
        assertNull(ScenarioSessionSnapshotCodec.decode(encoded.replace("\"version\":1", "\"version\":2")))
        val malformedUuid = JsonParser.parseString(encoded).asJsonObject.apply {
            addProperty("scenario_id", "not-a-uuid")
        }
        assertNull(ScenarioSessionSnapshotCodec.decode(malformedUuid.toString()))
        val invalidCoordinates = JsonParser.parseString(encoded).asJsonObject.apply {
            getAsJsonObject("anchor").addProperty("x", "NaN")
        }
        assertNull(ScenarioSessionSnapshotCodec.decode(invalidCoordinates.toString()))
        val invalidExpiration = JsonParser.parseString(encoded).asJsonObject.apply {
            getAsJsonObject("actors").getAsJsonObject("thief").addProperty("expires_at", 99L)
        }
        assertNull(ScenarioSessionSnapshotCodec.decode(invalidExpiration.toString()))
        val invalidFingerprint = JsonParser.parseString(encoded).asJsonObject.apply {
            addProperty("template_fingerprint", "not-a-sha256")
        }
        assertNull(ScenarioSessionSnapshotCodec.decode(invalidFingerprint.toString()))
        val changedTemplate = snapshot.copy(
            templateFingerprint = ScenarioTemplateRecoveryFingerprint.compute(template),
        )
        template.displayName = "Changed"
        assertTrue(ScenarioSessionSnapshotCodec.templateIssues(changedTemplate, template)
            .contains("amprenta template diferita"))
    }

    @Test
    fun `retired actors round trip without active actor identity`() {
        val template = ScenarioTemplate(ScenarioType.THEFT).apply {
            templateId = "pack:retired"
            addPhase("START", "")
            addActor("guide", NpcScenarioActorDefinition("guide", "Guide"))
        }
        val world = Proxy.newProxyInstance(World::class.java.classLoader, arrayOf(World::class.java)) {
            _, method, _ -> if (method.name == "getName") "test_world" else null
        } as World
        val scenario = ScenarioSessionRegistry().start(template, Location(world, 0.0, 64.0, 0.0))!!
        scenario.assignActor("guide", UUID.randomUUID())
        scenario.removeActor("guide")

        val snapshot = ScenarioSessionSnapshotCodec.capture(scenario) { null }!!
        val encoded = ScenarioSessionSnapshotCodec.encode(snapshot)

        assertEquals(setOf("guide"), snapshot.retiredActorIds)
        assertEquals(snapshot, ScenarioSessionSnapshotCodec.decode(encoded))
        assertTrue(ScenarioSessionSnapshotCodec.templateIssues(snapshot, template).isEmpty())
        val duplicate = JsonParser.parseString(encoded).asJsonObject.apply {
            getAsJsonArray("retired_actor_ids").add("guide")
        }
        assertNull(ScenarioSessionSnapshotCodec.decode(duplicate.toString()))
        val unknown = snapshot.copy(retiredActorIds = setOf("unknown"))
        assertTrue(ScenarioSessionSnapshotCodec.templateIssues(unknown, template)
            .contains("actor retras absent din template"))
        scenario.assignActor("guide", UUID.randomUUID())
        assertTrue(scenario.retiredActorIds.isEmpty())
        assertTrue(ScenarioSessionSnapshotCodec.capture(scenario) { 42 }!!.retiredActorIds.isEmpty())
        val overlapping = JsonParser.parseString(encoded).asJsonObject.apply {
            getAsJsonObject("actors").add("guide", JsonObject().apply {
                addProperty("uuid", UUID.randomUUID().toString())
            })
        }
        assertNull(ScenarioSessionSnapshotCodec.decode(overlapping.toString()))
    }

    @Test
    fun `capture refuses to replace a persistent actor database identity with null`() {
        val template = ScenarioTemplate(ScenarioType.THEFT).apply {
            templateId = "pack:identity"
            addPhase("START", "")
            addActor("guide", NpcScenarioActorDefinition("guide", "Guide"))
        }
        val world = Proxy.newProxyInstance(World::class.java.classLoader, arrayOf(World::class.java)) {
            _, method, _ -> if (method.name == "getName") "test_world" else null
        } as World
        val scenario = ScenarioSessionRegistry().start(template, Location(world, 0.0, 64.0, 0.0))!!
        scenario.assignActor("guide", UUID.randomUUID())

        assertNull(ScenarioSessionSnapshotCodec.capture(scenario) { null })
        scenario.actors["guide"] = scenario.actors.getValue("guide").copy(
            persistenceMode = NpcPersistenceMode.RUNTIME_ONLY,
        )
        assertNotNull(ScenarioSessionSnapshotCodec.capture(scenario) { null })
    }
}
