package ro.ainpc.spawn

import java.sql.SQLException

internal object HouseholdSpawnPersistence {
    fun complete(
        allocation: HouseAllocation?,
        spawnPlans: List<NpcSpawnPlan>,
        spawnResults: List<NpcSpawnResult>,
        familyBindingResult: FamilyBindingResult?,
        warnings: List<String>,
        saveHousehold: () -> Int
    ): HouseholdSpawnResult {
        val completionWarnings = warnings.toMutableList()
        val errors = mutableListOf<String>()
        if (allocation == null) {
            errors.add("HouseAllocation lipseste; household-ul nu poate fi persistat.")
        } else {
            try {
                val residents = saveHousehold()
                if (residents == spawnPlans.size) {
                    completionWarnings.add("Household persistent actualizat: ${allocation.householdId()} rezidenti=$residents.")
                    return HouseholdSpawnResult.success(spawnPlans, spawnResults, familyBindingResult, completionWarnings)
                }
                errors.add(
                    "Salvare household incompleta ${allocation.householdId()}: rezidenti=$residents, asteptati=${spawnPlans.size}."
                )
            } catch (exception: SQLException) {
                errors.add("Nu am putut salva household-ul persistent ${allocation.householdId()}: ${exception.message}")
            }
        }
        completionWarnings.add(
            "NPC-urile create/reutilizate si relatiile deja legate sunt pastrate pentru recuperare. " +
                "Persistenta poate fi partiala; verificati datele inainte de reluare."
        )
        return HouseholdSpawnResult.failedPersistence(spawnPlans, spawnResults, familyBindingResult, errors, completionWarnings)
    }
}
