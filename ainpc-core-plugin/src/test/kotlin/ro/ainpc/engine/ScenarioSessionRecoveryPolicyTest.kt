package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NpcLifecycleType
import ro.ainpc.npc.NpcPersistenceMode
import ro.ainpc.npc.NpcScenarioActorDefinition
import java.util.UUID

class ScenarioSessionRecoveryPolicyTest {
    @Test
    fun `automatic restore accepts only structurally valid actorless narrative sessions`() {
        val template = ScenarioTemplate(ScenarioType.THEFT).apply {
            templateId = "demo:theft"
            addPhase("PLANNING", "")
        }
        val snapshot = ScenarioSessionSnapshot(
            1, UUID.randomUUID(), "demo:theft", "PLANNING", 1_000L,
            ScenarioSessionSnapshot.Anchor("world", 0.0, 64.0, 0.0, 0f, 0f),
            emptyMap(), emptyMap(), emptyMap(),
        )

        assertTrue(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(snapshot, template))
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(snapshot.copy(version = 2), template))
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(snapshot.copy(phase = "MISSING"), template))
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(
            snapshot.copy(startedAt = 301_001L), template, now = 1_000L))
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(
            snapshot.copy(playerRoles = mapOf(UUID.randomUUID() to "HERO")), template))
        assertTrue(ScenarioSessionRecoveryPolicy.automaticRestoreBlockers(
            snapshot.copy(playerRoles = mapOf(UUID.randomUUID() to "HERO")), template)
            .contains("rolurile participantilor nu sunt restaurate"))
        template.requiresPlayer = true
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(snapshot, template))
        template.requiresPlayer = false
        template.addActor("guide", NpcScenarioActorDefinition("guide", "Guide"))
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(snapshot, template))
        assertTrue(ScenarioSessionRecoveryPolicy.automaticRestoreBlockers(snapshot, template)
            .contains("actorii snapshot-ului difera de template"))
    }

    @Test
    fun `progression templates never auto restore`() {
        val template = ScenarioTemplate(ScenarioType.QUEST).apply {
            templateId = "demo:quest"
            addPhase("START", "")
        }
        val snapshot = ScenarioSessionSnapshot(
            1, UUID.randomUUID(), "demo:quest", "START", 1_000L,
            ScenarioSessionSnapshot.Anchor("world", 0.0, 64.0, 0.0, 0f, 0f),
            emptyMap(), emptyMap(), emptyMap(),
        )

        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(snapshot, template))
        assertTrue(ScenarioSessionRecoveryPolicy.automaticRestoreBlockers(snapshot, template)
            .contains("progresia sau cerintele de quest nu sunt restaurate"))
    }

    @Test
    fun `permanent actor is eligible only when loaded identity and definition match`() {
        val definition = NpcScenarioActorDefinition(
            "guide", "Guide", lifecycleType = NpcLifecycleType.PERMANENT,
            persistenceMode = NpcPersistenceMode.LIGHT,
        )
        val template = ScenarioTemplate(ScenarioType.THEFT).apply {
            templateId = "demo:theft"
            addPhase("PLANNING", "")
            addActor("guide", definition)
        }
        val actorUuid = UUID.randomUUID()
        val snapshot = ScenarioSessionSnapshot(
            1, UUID.randomUUID(), "demo:theft", "PLANNING", 1_000L,
            ScenarioSessionSnapshot.Anchor("world", 0.0, 64.0, 0.0, 0f, 0f),
            emptyMap(), emptyMap(),
            mapOf("guide" to ScenarioSessionSnapshot.Actor(actorUuid, 42)),
            ScenarioTemplateRecoveryFingerprint.compute(template),
        )
        val npc = AINPC(null).apply {
            applyScenarioDefinition(definition)
            databaseId = 42
            uuid = actorUuid
            worldName = "world"
            profileSource = "scenario_actor"
            scenarioSessionId = snapshot.scenarioId
        }
        val lookup: (Int) -> AINPC? = { id -> npc.takeIf { it.databaseId == id } }

        assertTrue(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(snapshot, template, lookup))
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(
            snapshot.copy(actors = mapOf("guide" to ScenarioSessionSnapshot.Actor(actorUuid, 42, 31_000L))),
            template, lookup))
        assertTrue(ScenarioSessionRecoveryPolicy.automaticRestoreBlockers(
            snapshot.copy(actors = mapOf("guide" to ScenarioSessionSnapshot.Actor(actorUuid, 42, 31_000L))),
            template, lookup).contains("actorul guide are un deadline neasteptat"))
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(
            snapshot.copy(templateFingerprint = null), template, lookup))
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(snapshot.copy(actors = emptyMap()), template, lookup))
        npc.sourceKey = "another_actor"
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(snapshot, template, lookup))
        assertTrue(ScenarioSessionRecoveryPolicy.automaticRestoreBlockers(snapshot, template, lookup)
            .contains("actorul guide nu corespunde identitatii si definitiei persistente"))
        npc.sourceKey = "guide"
        npc.uuid = UUID.randomUUID()
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(snapshot, template, lookup))
        npc.uuid = actorUuid
        npc.scenarioSessionId = UUID.randomUUID()
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(snapshot, template, lookup))
        npc.scenarioSessionId = snapshot.scenarioId
        template.actors["guide"] = definition.copy(durationSeconds = 30)
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(snapshot, template, lookup))
        template.actors["guide"] = definition
        npc.persistenceMode = NpcPersistenceMode.FULL
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(snapshot, template, lookup))
    }

    @Test
    fun `unexpired permanent actor timer can restore only with a consistent deadline`() {
        val definition = NpcScenarioActorDefinition(
            "guide", "Guide", lifecycleType = NpcLifecycleType.PERMANENT,
            persistenceMode = NpcPersistenceMode.LIGHT, durationSeconds = 30L,
        )
        val template = ScenarioTemplate(ScenarioType.THEFT).apply {
            templateId = "demo:timed"
            addPhase("START", "")
            addActor("guide", definition)
        }
        val actorUuid = UUID.randomUUID()
        val snapshot = ScenarioSessionSnapshot(
            1, UUID.randomUUID(), "demo:timed", "START", 1_000L,
            ScenarioSessionSnapshot.Anchor("world", 0.0, 64.0, 0.0, 0f, 0f),
            emptyMap(), emptyMap(),
            mapOf("guide" to ScenarioSessionSnapshot.Actor(actorUuid, 42, 120_000L)),
            ScenarioTemplateRecoveryFingerprint.compute(template),
        )
        val npc = AINPC(null).apply {
            applyScenarioDefinition(definition)
            databaseId = 42
            uuid = actorUuid
            worldName = "world"
            profileSource = "scenario_actor"
            scenarioSessionId = snapshot.scenarioId
        }
        val lookup: (Int) -> AINPC? = { id -> npc.takeIf { it.databaseId == id } }

        assertTrue(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(snapshot, template, lookup, now = 100_000L))
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(snapshot, template, lookup, now = 120_000L))
        assertTrue(ScenarioSessionRecoveryPolicy.automaticRestoreBlockers(snapshot, template, lookup, now = 120_000L)
            .contains("deadline-ul actorului guide a expirat"))
        npc.sourceKey = "another_actor"
        val expiredWithWrongIdentity = ScenarioSessionRecoveryPolicy.automaticRestoreBlockers(
            snapshot, template, lookup, now = 120_000L)
        assertTrue(expiredWithWrongIdentity.contains("deadline-ul actorului guide a expirat"))
        assertTrue(expiredWithWrongIdentity.contains(
            "actorul guide nu corespunde identitatii si definitiei persistente"))
        npc.sourceKey = "guide"
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(
            snapshot.copy(actors = mapOf("guide" to ScenarioSessionSnapshot.Actor(actorUuid, 42))),
            template, lookup, now = 100_000L))
        assertTrue(ScenarioSessionRecoveryPolicy.automaticRestoreBlockers(
            snapshot.copy(actors = mapOf("guide" to ScenarioSessionSnapshot.Actor(actorUuid, 42))),
            template, lookup, now = 100_000L).contains("deadline-ul actorului guide lipseste din snapshot"))
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(
            snapshot.copy(actors = mapOf("guide" to ScenarioSessionSnapshot.Actor(actorUuid, 42, 20_000L))),
            template, lookup, now = 10_000L))
        assertTrue(ScenarioSessionRecoveryPolicy.automaticRestoreBlockers(
            snapshot.copy(actors = mapOf("guide" to ScenarioSessionSnapshot.Actor(actorUuid, 42, 20_000L))),
            template, lookup, now = 10_000L).contains("deadline-ul actorului guide este inconsistent"))
        val inconsistentWithMissingRow = ScenarioSessionRecoveryPolicy.automaticRestoreBlockers(
            snapshot.copy(actors = mapOf("guide" to ScenarioSessionSnapshot.Actor(actorUuid, 42, 20_000L))),
            template, actorById = { null }, now = 10_000L)
        assertTrue(inconsistentWithMissingRow.contains("deadline-ul actorului guide este inconsistent"))
        assertTrue(inconsistentWithMissingRow.contains("actorul guide nu este incarcat"))
    }

    @Test
    fun `retired actor restores only with explicit marker and matching fingerprint`() {
        val template = ScenarioTemplate(ScenarioType.THEFT).apply {
            templateId = "demo:retired"
            addPhase("START", "")
            addActor("guide", NpcScenarioActorDefinition("guide", "Guide"))
        }
        val snapshot = ScenarioSessionSnapshot(
            1, UUID.randomUUID(), "demo:retired", "START", 1_000L,
            ScenarioSessionSnapshot.Anchor("world", 0.0, 64.0, 0.0, 0f, 0f),
            emptyMap(), emptyMap(), emptyMap(),
            ScenarioTemplateRecoveryFingerprint.compute(template), setOf("guide"),
        )

        assertTrue(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(snapshot, template))
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(
            snapshot.copy(retiredActorIds = emptySet()), template))
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(
            snapshot.copy(templateFingerprint = null), template))
        assertFalse(ScenarioSessionRecoveryPolicy.canRestoreAutomatically(
            snapshot.copy(retiredActorIds = setOf("guide", "unknown")), template))
    }
}
