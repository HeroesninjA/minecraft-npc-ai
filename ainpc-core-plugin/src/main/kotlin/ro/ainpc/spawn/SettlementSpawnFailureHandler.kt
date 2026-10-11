package ro.ainpc.spawn

internal object SettlementSpawnFailureHandler {
    fun finish(
        dryRun: Boolean,
        allocations: List<HouseAllocation>,
        householdResults: List<HouseholdSpawnResult>,
        errors: List<String>,
        warnings: List<String>,
        rollbackCreatedNpcs: (MutableList<String>) -> Boolean,
        removeHousehold: (HouseAllocation) -> Unit
    ): SettlementSpawnResult {
        val failedHousehold = householdResults.last()
        val allocation = allocations[householdResults.lastIndex]
        val failureErrors = errors + failedHousehold.errors().map { "${allocation.placeId()}: $it" }
        val failureWarnings = warnings.toMutableList()
        if (failedHousehold.rolledBack()) {
            failureWarnings.add("${allocation.placeId()}: rollback local executat pentru household-ul esuat.")
        }
        val globallyRolledBack = !dryRun && !failedHousehold.persistenceFailed() && rollbackCreatedNpcs(failureWarnings)
        if (globallyRolledBack) {
            var cleanedUp = 0
            for (previousAllocation in allocations.take(householdResults.size)) {
                try {
                    removeHousehold(previousAllocation)
                    cleanedUp++
                } catch (exception: Exception) {
                    failureWarnings.add("Nu am putut curata datele DB pentru ${previousAllocation.placeId()}: ${exception.message}")
                }
            }
            if (cleanedUp > 0) failureWarnings.add("Curatare DB pentru $cleanedUp household-uri persistate anterior.")
        }
        failureWarnings.add(
            when {
                dryRun -> "Spawn batch dry-run settlement finalizat cu erori; nu s-au creat NPC-uri."
                failedHousehold.persistenceFailed() ->
                    "Settlement oprit la persistenta: fara rollback global sau stergere de household-uri. " +
                        "NPC-urile si datele existente sunt pastrate pentru recuperare; operatia este incompleta."
                globallyRolledBack -> "Rollback global settlement executat pentru household-urile create anterior."
                else -> "Rollback global settlement incomplet pentru household-urile create anterior."
            }
        )
        return SettlementSpawnResult.failed(dryRun, globallyRolledBack, allocations, householdResults, failureErrors, failureWarnings)
    }
}
