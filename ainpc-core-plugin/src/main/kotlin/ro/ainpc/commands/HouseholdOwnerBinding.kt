package ro.ainpc.commands

import ro.ainpc.spawn.HouseAllocation
import ro.ainpc.spawn.HouseholdSpawnResult
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.WorldPlaceInfo
import java.util.Locale

internal data class HouseholdOwnerBinding(
    val homePlaceId: String,
    val npcId: String,
    val npcName: String
) {
    fun apply(worldAdmin: WorldAdminService): WorldPlaceInfo =
        worldAdmin.assignHomeOwner(homePlaceId, npcId, npcName)

    fun complete(worldAdmin: WorldAdminService, progress: PostSpawnBindingResult): PostSpawnBindingResult {
        if (progress.status != PostSpawnBindingStatus.COMPLETE) return progress
        return try {
            apply(worldAdmin)
            progress.copy(mappingWritesApplied = progress.mappingWritesApplied + 1)
        } catch (exception: RuntimeException) {
            progress.copy(
                householdsCompleted = 0,
                failures = progress.failures + "Proprietar home=$homePlaceId: ${exception.message ?: "eroare runtime fara mesaj"}"
            )
        }
    }

    companion object {
        fun resolve(allocation: HouseAllocation, result: HouseholdSpawnResult): HouseholdOwnerBinding {
            require(result.success() && !result.dryRun()) { "Proprietarul necesita un spawn household reusit, nu dry-run." }
            val plans = result.spawnPlans()
            val spawnResults = result.spawnResults()
            require(plans.size == spawnResults.size) {
                "Planurile spawn (${plans.size}) nu corespund rezultatelor (${spawnResults.size})."
            }
            val homePlaceId = allocation.placeId()
            require(homePlaceId.isNotBlank() && plans.all { it.homePlaceId().equals(homePlaceId, ignoreCase = true) }) {
                "Planurile household-ului nu apartin aceleiasi case: $homePlaceId."
            }
            val ownerKey = normalizeKey(allocation.primaryOwnerNpcKey())
            require(ownerKey.isNotBlank()) { "Household-ul $homePlaceId nu are primaryOwnerNpcKey." }
            require(allocation.residentPlans().count { normalizeKey(it.npcKey()) == ownerKey } == 1) {
                "Proprietarul '${allocation.primaryOwnerNpcKey()}' nu identifica un rezident unic in $homePlaceId."
            }
            val ownerIndexes = plans.indices.filter { normalizeKey(plans[it].npcKey()) == ownerKey }
            require(ownerIndexes.size == 1) {
                "Proprietarul '${allocation.primaryOwnerNpcKey()}' nu identifica un plan spawn unic in $homePlaceId."
            }
            val ownerResult = spawnResults[ownerIndexes.single()]
            val ownerNpc = ownerResult.npc()
            require(ownerResult.success() && ownerNpc != null && ownerNpc.databaseId > 0) {
                "Proprietarul '${allocation.primaryOwnerNpcKey()}' nu are un NPC persistent creat/reutilizat valid."
            }
            return HouseholdOwnerBinding(homePlaceId, "npc_${ownerNpc.databaseId}", ownerNpc.name)
        }

        private fun normalizeKey(value: String): String =
            value.trim().lowercase(Locale.ROOT).replace(' ', '_').replace('-', '_')
    }
}
