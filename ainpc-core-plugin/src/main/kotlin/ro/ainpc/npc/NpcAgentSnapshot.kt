package ro.ainpc.npc

import java.util.UUID

data class NpcAgentSnapshot(
    val npcUuid: UUID,
    val runtimeIdentity: UUID,
    val decisionRevision: Long,
    val stateRevision: Long,
    val capturedAtMillis: Long,
    val cognition: NpcCognitiveState,
    val perception: NpcPerceptionSnapshot,
) {
    val schemaVersion: Int = 2

    companion object {
        fun capture(npc: AINPC, context: NPCContext = npc.context): NpcAgentSnapshot =
            capture(npc, context, System.currentTimeMillis())

        internal fun capture(npc: AINPC, context: NPCContext, now: Long): NpcAgentSnapshot = NpcAgentSnapshot(
            npcUuid = npc.uuid,
            runtimeIdentity = npc.runtimeIdentity,
            decisionRevision = npc.decisionRevision,
            stateRevision = npc.stateRevision,
            capturedAtMillis = now,
            cognition = NpcCognitiveState.capture(npc, context, now),
            perception = NpcPerceptionSnapshot.capture(context),
        )
    }
}
