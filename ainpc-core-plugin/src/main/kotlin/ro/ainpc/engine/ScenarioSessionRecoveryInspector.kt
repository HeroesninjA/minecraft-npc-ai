package ro.ainpc.engine

import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NpcPersistenceMode
import java.util.UUID

data class ScenarioRecoveryPreview(
    val scenarioId: String,
    val templateId: String,
    val phase: String?,
    val actorCount: Int,
    val updatedAt: Long,
    val issues: List<String>,
    val automaticRestoreEligible: Boolean,
    val activeInRuntime: Boolean,
    val automaticRestoreBlockers: List<String> = emptyList(),
)

data class ScenarioRecoveryPage(val total: Long, val entries: List<ScenarioRecoveryPreview>)

internal object ScenarioSessionRecoveryInspector {
    fun inspect(
        stored: StoredScenarioSession,
        template: ScenarioTemplate?,
        worldAvailable: (String) -> Boolean,
        npcById: (Int) -> AINPC?,
        npcByUuid: (UUID) -> AINPC?,
        duplicateTemplateCount: Long,
        templateAlreadyActive: Boolean,
        scenarioAlreadyActive: Boolean = false,
        now: Long = System.currentTimeMillis(),
        transientActorReady: (AINPC) -> Boolean = { false },
    ): ScenarioRecoveryPreview {
        val snapshot = stored.snapshot
        val issues = buildList {
            stored.issue?.let(::add)
            if (snapshot != null) {
                if (template == null) add("template indisponibil")
                else addAll(ScenarioSessionSnapshotCodec.templateIssues(snapshot, template))
                if ((snapshot.actors.isNotEmpty() || snapshot.retiredActorIds.isNotEmpty()) &&
                    snapshot.templateFingerprint == null
                ) {
                    add("amprenta template lipsa pentru actori")
                }
                if (!worldAvailable(snapshot.anchor.world)) add("lumea ancorei nu este incarcata")
                if (snapshot.startedAt > now + 300_000L) add("timpul de inceput este in viitor")
                if (duplicateTemplateCount > 1) add("mai multe snapshot-uri pentru acelasi template")
                if (templateAlreadyActive) add("template deja activ in runtime")
                for (actor in snapshot.actors.values) {
                    val databaseId = actor.databaseId ?: continue
                    val npc = npcById(databaseId)
                    when {
                        npc == null -> add("actorul persistent nu este incarcat")
                        npc.uuid != actor.uuid -> add("UUID-ul actorului nu corespunde randului DB")
                        npc.profileSource != "scenario_actor" -> add("provenienta actorului nu este scenario_actor")
                        npc.scenarioSessionId != snapshot.scenarioId -> add("ownership-ul sesiunii nu este verificabil")
                        npc.persistenceMode == NpcPersistenceMode.RUNTIME_ONLY ->
                            add("actorul nu este persistent")
                    }
                }
                for (npcId in snapshot.npcRoles.keys) {
                    if (npcByUuid(npcId) == null) add("participantul NPC nu este incarcat")
                }
            }
        }.distinct()
        val automaticRestoreBlockers = if (snapshot != null && template != null) {
            ScenarioSessionRecoveryPolicy.automaticRestoreBlockers(snapshot, template, npcById, now, transientActorReady)
        } else emptyList()
        return ScenarioRecoveryPreview(
            stored.scenarioId,
            stored.templateId,
            snapshot?.phase,
            snapshot?.actors?.size ?: 0,
            stored.updatedAt,
            issues,
            issues.isEmpty() && snapshot != null && template != null && automaticRestoreBlockers.isEmpty(),
            scenarioAlreadyActive,
            automaticRestoreBlockers,
        )
    }
}
