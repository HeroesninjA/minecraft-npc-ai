package ro.ainpc.engine

import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NpcLifecycleType
import ro.ainpc.npc.NpcPersistenceMode
import ro.ainpc.npc.NpcSpawnPolicy

internal object ScenarioSessionRecoveryPolicy {
    fun canRestoreAutomatically(
        snapshot: ScenarioSessionSnapshot,
        template: ScenarioTemplate,
        actorById: (Int) -> AINPC? = { null },
        now: Long = System.currentTimeMillis(),
        transientActorReady: (AINPC) -> Boolean = { false },
    ): Boolean = automaticRestoreBlockers(snapshot, template, actorById, now, transientActorReady).isEmpty()

    fun automaticRestoreBlockers(
        snapshot: ScenarioSessionSnapshot,
        template: ScenarioTemplate,
        actorById: (Int) -> AINPC? = { null },
        now: Long = System.currentTimeMillis(),
        transientActorReady: (AINPC) -> Boolean = { false },
    ): List<String> = buildList {
        if (!ScenarioSessionSnapshotCodec.isStructurallyValid(snapshot)) add("snapshot invalid")
        if (snapshot.startedAt > now + 300_000L) add("timpul de inceput este in viitor")
        addAll(ScenarioSessionSnapshotCodec.templateIssues(snapshot, template))
        if (snapshot.npcRoles.isNotEmpty() || snapshot.playerRoles.isNotEmpty()) {
            add("rolurile participantilor nu sunt restaurate")
        }
        if ((snapshot.actors.isNotEmpty() || snapshot.retiredActorIds.isNotEmpty()) && snapshot.templateFingerprint == null) {
            add("amprenta template lipsa pentru actori")
        }
        if (template.questActorTriggers.isNotEmpty() || template.questStages.isNotEmpty() ||
            template.objectives.isNotEmpty() || template.rewards.isNotEmpty() ||
            template.questCode.isNotBlank() || template.questGiverProfession.isNotBlank() ||
            template.questPrerequisites.isNotEmpty() || template.nextQuest.isNotBlank() ||
            template.questDialogues.isNotEmpty() || template.progressionEnabled || template.requiresPlayer
        ) add("progresia sau cerintele de quest nu sunt restaurate")
        if (template.runtimeConditionDefs.isNotEmpty() || template.runtimeTriggerDefs.isNotEmpty() ||
            template.runtimeActionDefs.isNotEmpty()
        ) add("conditiile, trigger-ele sau actiunile runtime nu sunt restaurate")
        if (snapshot.actors.keys + snapshot.retiredActorIds != template.actors.keys) {
            add("actorii snapshot-ului difera de template")
        }
        snapshot.actors.forEach { (actorId, actor) ->
            val definition = template.actors[actorId] ?: return@forEach
            val databaseId = actor.databaseId
            val deadlineStatus = if ((definition.durationSeconds ?: 0) > 0 && actor.expiresAt != null) {
                ScenarioActorExpiration.recoveryStatus(
                    now, snapshot.startedAt, definition.durationSeconds, actor.expiresAt,
                )
            } else null
            if (definition.persistenceMode == NpcPersistenceMode.RUNTIME_ONLY) {
                add("actorul $actorId nu este persistent")
            }
            if (definition.spawnPolicy != NpcSpawnPolicy.AUTO) {
                add("actorul $actorId nu are spawn AUTO")
            }
            if (definition.despawnRule.isNotBlank()) {
                add("regula de despawn pentru actorul $actorId nu este restaurata")
            }
            when {
                (definition.durationSeconds ?: 0) > 0 && actor.expiresAt == null ->
                    add("deadline-ul actorului $actorId lipseste din snapshot")
                deadlineStatus == ScenarioActorExpiration.RecoveryStatus.EXPIRED ->
                    add("deadline-ul actorului $actorId a expirat")
                deadlineStatus == ScenarioActorExpiration.RecoveryStatus.INCONSISTENT ->
                    add("deadline-ul actorului $actorId este inconsistent")
                (definition.durationSeconds ?: 0) <= 0 && actor.expiresAt != null ->
                    add("actorul $actorId are un deadline neasteptat")
            }
            if (databaseId == null) {
                add("actorul $actorId nu are rand DB")
            } else {
                val npc = actorById(databaseId)
                if (npc == null) add("actorul $actorId nu este incarcat")
                else if (!actorMatches(snapshot, template, actorId, actor, databaseId, npc)) {
                    add("actorul $actorId nu corespunde identitatii si definitiei persistente")
                } else if (npc.lifecycleType != NpcLifecycleType.PERMANENT && !transientActorReady(npc)) {
                    add("entitatea originala a actorului tranzitoriu $actorId nu este verificata")
                }
            }
        }
    }.distinct()

    private fun actorMatches(
        snapshot: ScenarioSessionSnapshot,
        template: ScenarioTemplate,
        actorId: String,
        actor: ScenarioSessionSnapshot.Actor,
        databaseId: Int,
        npc: AINPC,
    ): Boolean {
        val definition = template.actors[actorId] ?: return false
        val expectedSourceKey = definition.id.takeIf(String::isNotBlank)
            ?: "${template.templateId}:$actorId"
        return npc.databaseId == databaseId && npc.uuid == actor.uuid &&
            npc.profileSource == "scenario_actor" && npc.scenarioSessionId == snapshot.scenarioId &&
            npc.lifecycleType == definition.lifecycleType &&
            npc.persistenceMode == definition.persistenceMode &&
            npc.simulationMode == definition.simulationMode &&
            npc.interactionProfile == definition.interactionProfile &&
            npc.entityKind == definition.entityKind &&
            (definition.entityArchetype.isBlank() || npc.entityArchetype == definition.entityArchetype) &&
            npc.sourceKey == expectedSourceKey && npc.worldName == snapshot.anchor.world &&
            npc.ownerScenarioId.orEmpty() == definition.ownerScenarioId &&
            npc.ownerQuestId.orEmpty() == definition.ownerQuestId &&
            npc.spawnSource.orEmpty() == definition.spawnSource &&
            npc.despawnRule.orEmpty() == definition.despawnRule &&
            npc.durationSeconds == definition.durationSeconds &&
            npc.temporaryTags == definition.temporaryTags
    }
}
