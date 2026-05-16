package ro.ainpc.world

import ro.ainpc.npc.AINPC
import ro.ainpc.spawn.NpcSpawnPlan

class NpcWorldBinding(
    npcId: Int,
    npcUuid: String?,
    npcName: String?,
    homePlaceId: String?,
    workPlaceId: String?,
    socialPlaceId: String?,
    homeNodeId: String?,
    workNodeId: String?,
    socialNodeId: String?,
    familyId: String?,
    source: String?,
    private val createdAt: Long,
    private val updatedAt: Long
) {
    private val npcId: Int = npcId
    private val npcUuid: String = clean(npcUuid)
    private val npcName: String = clean(npcName)
    private val homePlaceId: String = clean(homePlaceId)
    private val workPlaceId: String = clean(workPlaceId)
    private val socialPlaceId: String = clean(socialPlaceId)
    private val homeNodeId: String = clean(homeNodeId)
    private val workNodeId: String = clean(workNodeId)
    private val socialNodeId: String = clean(socialNodeId)
    private val familyId: String = clean(familyId)
    private val source: String = clean(source)

    fun npcId(): Int = npcId
    fun npcUuid(): String = npcUuid
    fun npcName(): String = npcName
    fun homePlaceId(): String = homePlaceId
    fun workPlaceId(): String = workPlaceId
    fun socialPlaceId(): String = socialPlaceId
    fun homeNodeId(): String = homeNodeId
    fun workNodeId(): String = workNodeId
    fun socialNodeId(): String = socialNodeId
    fun familyId(): String = familyId
    fun source(): String = source
    fun createdAt(): Long = createdAt
    fun updatedAt(): Long = updatedAt

    fun mergeMissingFrom(previous: NpcWorldBinding?): NpcWorldBinding {
        if (previous == null) {
            return this
        }

        return NpcWorldBinding(
            npcId,
            firstNonBlank(npcUuid, previous.npcUuid()),
            firstNonBlank(npcName, previous.npcName()),
            firstNonBlank(homePlaceId, previous.homePlaceId()),
            firstNonBlank(workPlaceId, previous.workPlaceId()),
            firstNonBlank(socialPlaceId, previous.socialPlaceId()),
            firstNonBlank(homeNodeId, previous.homeNodeId()),
            firstNonBlank(workNodeId, previous.workNodeId()),
            firstNonBlank(socialNodeId, previous.socialNodeId()),
            firstNonBlank(familyId, previous.familyId()),
            firstNonBlank(source, previous.source()),
            previous.createdAt(),
            updatedAt
        )
    }

    fun hasAnyPlaceBinding(): Boolean {
        return homePlaceId.isNotBlank() || workPlaceId.isNotBlank() || socialPlaceId.isNotBlank()
    }

    companion object {
        @JvmStatic
        fun fromSpawnPlan(npc: AINPC?, plan: NpcSpawnPlan?, source: String?): NpcWorldBinding {
            if (npc == null || plan == null) {
                throw IllegalArgumentException("NPC-ul si planul de spawn sunt obligatorii pentru binding.")
            }

            return NpcWorldBinding(
                npc.databaseId,
                npc.uuid?.toString().orEmpty(),
                npc.name,
                plan.homePlaceId(),
                plan.workPlaceId(),
                plan.socialPlaceId(),
                plan.homeNodeId(),
                plan.workNodeId(),
                plan.socialNodeId(),
                plan.familyId(),
                source,
                0L,
                0L
            )
        }

        private fun firstNonBlank(preferred: String?, fallback: String?): String {
            val cleanPreferred = clean(preferred)
            return if (cleanPreferred.isBlank()) clean(fallback) else cleanPreferred
        }

        private fun clean(value: String?): String = value?.trim().orEmpty()
    }
}
