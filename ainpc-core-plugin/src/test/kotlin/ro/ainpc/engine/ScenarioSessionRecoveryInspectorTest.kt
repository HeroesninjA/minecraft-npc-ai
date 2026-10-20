package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NpcPersistenceMode
import ro.ainpc.npc.NpcLifecycleType
import ro.ainpc.npc.NpcScenarioActorDefinition
import java.util.UUID

class ScenarioSessionRecoveryInspectorTest {
    @Test
    fun `accepts a candidate only when its actor belongs to the saved session`() {
        val template = template()
        val snapshot = snapshot().copy(templateFingerprint = ScenarioTemplateRecoveryFingerprint.compute(template))
        val actor = actor(snapshot)
        val preview = inspect(snapshot, template, actor)

        assertTrue(preview.issues.isEmpty())
        assertTrue(!preview.automaticRestoreEligible)
        assertTrue(preview.automaticRestoreBlockers.contains("progresia sau cerintele de quest nu sunt restaurate"))
    }

    @Test
    fun `blocks missing ownership duplicate snapshots and unavailable worlds`() {
        val snapshot = snapshot()
        val actor = actor(snapshot).apply { scenarioSessionId = UUID.randomUUID() }
        val preview = inspect(snapshot, template(), actor, worldAvailable = false, duplicateCount = 2)

        assertTrue(preview.issues.contains("ownership-ul sesiunii nu este verificabil"))
        assertTrue(preview.issues.contains("mai multe snapshot-uri pentru acelasi template"))
        assertTrue(preview.issues.contains("lumea ancorei nu este incarcata"))
    }

    @Test
    fun `marks a verified permanent actor as automatic recovery eligible`() {
        val definition = NpcScenarioActorDefinition("guide", "Guide",
            lifecycleType = NpcLifecycleType.PERMANENT, persistenceMode = NpcPersistenceMode.LIGHT)
        val template = ScenarioTemplate(ScenarioType.THEFT).apply {
            templateId = "pack:scenario"
            addPhase("START", "")
            addActor("guide", definition)
        }
        val snapshot = snapshot().copy(templateFingerprint = ScenarioTemplateRecoveryFingerprint.compute(template))
        val actor = actor(snapshot).apply {
            applyScenarioDefinition(definition)
            worldName = "world"
        }

        assertTrue(inspect(snapshot, template, actor).automaticRestoreEligible)
        assertTrue(inspect(snapshot, template, actor).automaticRestoreBlockers.isEmpty())
    }

    @Test
    fun `reports missing fingerprint for legacy actor snapshot`() {
        val snapshot = snapshot()
        val preview = inspect(snapshot, template(), actor(snapshot))

        assertTrue(preview.issues.contains("amprenta template lipsa pentru actori"))
        assertTrue(!preview.automaticRestoreEligible)
    }

    @Test
    fun `retired actor preview requires fingerprint but not an active NPC`() {
        val template = ScenarioTemplate(ScenarioType.THEFT).apply {
            templateId = "pack:scenario"
            addPhase("START", "")
            addActor("guide", NpcScenarioActorDefinition("guide", "Guide"))
        }
        val snapshot = snapshot().copy(actors = emptyMap(), retiredActorIds = setOf("guide"))
        fun preview(candidate: ScenarioSessionSnapshot) = ScenarioSessionRecoveryInspector.inspect(
            StoredScenarioSession(candidate.scenarioId.toString(), candidate.templateId, 1, 1L, candidate, null),
            template, { true }, { null }, { null }, 1, false,
        )

        assertTrue(preview(snapshot).issues.contains("amprenta template lipsa pentru actori"))
        assertTrue(preview(snapshot.copy(templateFingerprint = ScenarioTemplateRecoveryFingerprint.compute(template)))
            .automaticRestoreEligible)
    }

    @Test
    fun `preview distinguishes expired deadline from inconsistent deadline`() {
        val definition = NpcScenarioActorDefinition("guide", "Guide",
            lifecycleType = NpcLifecycleType.PERMANENT, persistenceMode = NpcPersistenceMode.LIGHT,
            durationSeconds = 30L)
        val template = ScenarioTemplate(ScenarioType.THEFT).apply {
            templateId = "pack:scenario"
            addPhase("START", "")
            addActor("guide", definition)
        }
        val original = snapshot().copy(
            actors = mapOf("guide" to ScenarioSessionSnapshot.Actor(UUID.randomUUID(), 42, 30_100L)),
            templateFingerprint = ScenarioTemplateRecoveryFingerprint.compute(template),
        )
        val actor = actor(original).apply {
            applyScenarioDefinition(definition)
            worldName = "world"
        }

        val expired = inspect(original, template, actor, now = 40_000L)
        val inconsistent = inspect(original.copy(
            actors = mapOf("guide" to original.actors.getValue("guide").copy(expiresAt = 20_000L)),
        ), template, actor, now = 40_000L)

        assertTrue(expired.automaticRestoreBlockers.contains("deadline-ul actorului guide a expirat"))
        assertTrue(inconsistent.automaticRestoreBlockers.contains("deadline-ul actorului guide este inconsistent"))
        assertTrue(!expired.automaticRestoreEligible)
        assertTrue(!inconsistent.automaticRestoreEligible)
        actor.sourceKey = "different"
        val mismatched = inspect(original, template, actor, now = 40_000L)
        assertTrue(mismatched.automaticRestoreBlockers.contains("deadline-ul actorului guide a expirat"))
        assertTrue(mismatched.automaticRestoreBlockers.contains(
            "actorul guide nu corespunde identitatii si definitiei persistente"))
    }

    @Test
    fun `blocks runtime only actor and invalid snapshot row`() {
        val snapshot = snapshot()
        val runtimeActor = actor(snapshot).apply { persistenceMode = NpcPersistenceMode.RUNTIME_ONLY }
        val actorPreview = inspect(snapshot, template(), runtimeActor)
        val invalidPreview = ScenarioSessionRecoveryInspector.inspect(
            StoredScenarioSession(snapshot.scenarioId.toString(), snapshot.templateId, 1, 1L, null, "payload invalid"),
            template(), { true }, { runtimeActor }, { runtimeActor }, 1, false,
        )

        assertTrue(actorPreview.issues.contains("actorul nu este persistent"))
        assertTrue(invalidPreview.issues.contains("payload invalid"))
    }

    private fun inspect(
        snapshot: ScenarioSessionSnapshot,
        template: ScenarioTemplate,
        actor: AINPC,
        worldAvailable: Boolean = true,
        duplicateCount: Long = 1,
        now: Long = System.currentTimeMillis(),
    ): ScenarioRecoveryPreview = ScenarioSessionRecoveryInspector.inspect(
        StoredScenarioSession(snapshot.scenarioId.toString(), snapshot.templateId, 1, 1L, snapshot, null),
        template,
        { worldAvailable },
        { id -> actor.takeIf { it.databaseId == id } },
        { uuid -> actor.takeIf { it.uuid == uuid } },
        duplicateCount,
        false,
        now = now,
    )

    private fun template(): ScenarioTemplate = ScenarioTemplate(ScenarioType.QUEST).apply {
        templateId = "pack:scenario"
        addPhase("START", "")
        addActor("guide", NpcScenarioActorDefinition("guide", "Guide"))
    }

    private fun snapshot(): ScenarioSessionSnapshot = ScenarioSessionSnapshot(
        1, UUID.randomUUID(), "pack:scenario", "START", 100L,
        ScenarioSessionSnapshot.Anchor("world", 1.0, 64.0, 2.0, 0f, 0f),
        emptyMap(), emptyMap(),
        mapOf("guide" to ScenarioSessionSnapshot.Actor(UUID.randomUUID(), 42)),
    )

    private fun actor(snapshot: ScenarioSessionSnapshot): AINPC = AINPC(null).apply {
        databaseId = 42
        uuid = snapshot.actors.getValue("guide").uuid
        profileSource = "scenario_actor"
        scenarioSessionId = snapshot.scenarioId
    }
}
