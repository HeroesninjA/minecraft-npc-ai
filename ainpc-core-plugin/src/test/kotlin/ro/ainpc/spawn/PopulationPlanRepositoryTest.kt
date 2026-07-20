package ro.ainpc.spawn

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class PopulationPlanRepositoryTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun saveRoundTripsEveryNarrativeFieldAndPreservesCreationTime() {
        var now = 100L
        val repository = PopulationPlanRepository(
            temporaryDirectory.resolve("plans"),
            clock = { now }
        )
        val original = plan("village_pop_101", "village", "resident_one")

        val first = repository.save(original)
        now = 250L
        val replacement = original.copy(warnings = listOf("updated warning"))
        val second = repository.save(replacement)
        val reloaded = PopulationPlanRepository(temporaryDirectory.resolve("plans")).find(original.planId)

        assertEquals(100L, first.createdAt)
        assertEquals(100L, second.createdAt)
        assertEquals(250L, second.updatedAt)
        assertEquals(replacement, reloaded?.plan)
        assertEquals(100L, reloaded?.createdAt)
        assertEquals(250L, reloaded?.updatedAt)
        val persisted = Files.readString(temporaryDirectory.resolve("plans/village_pop_101.plan.json"))
        assertTrue(persisted.contains("\"schema_version\": 1"))
        assertTrue(persisted.contains("\"backstorySeed\": \"backstory_resident_one\""))
    }

    @Test
    fun selectionPersistsPerRegionAndReplacesOnlyThatRegionsChoice() {
        var now = 10L
        val directory = temporaryDirectory.resolve("plans")
        val repository = PopulationPlanRepository(directory, clock = { now++ })
        val firstVillagePlan = plan("village_pop_101", "village", "resident_one")
        val secondVillagePlan = plan("village_pop_202", "village", "resident_two")
        val harborPlan = plan("harbor_pop_303", "harbor", "resident_three")
        repository.save(firstVillagePlan)
        repository.save(secondVillagePlan)
        repository.save(harborPlan)

        repository.select(firstVillagePlan.planId)
        repository.select(harborPlan.planId)
        repository.select(secondVillagePlan.planId)

        val reloaded = PopulationPlanRepository(directory)
        assertEquals(secondVillagePlan.planId, reloaded.selectedPlanId("village"))
        assertEquals(harborPlan.planId, reloaded.selectedPlanId("harbor"))
        assertNull(reloaded.select("missing_plan"))
        assertEquals(secondVillagePlan.planId, reloaded.selectedPlanId("village"))
    }

    @Test
    fun listFiltersByRegionAndOrdersMostRecentlyUpdatedFirst() {
        var now = 1L
        val repository = PopulationPlanRepository(
            temporaryDirectory.resolve("plans"),
            clock = { now++ }
        )
        repository.save(plan("village_pop_101", "village", "resident_one"))
        repository.save(plan("harbor_pop_202", "harbor", "resident_two"))
        repository.save(plan("village_pop_303", "village", "resident_three"))

        assertEquals(
            listOf("village_pop_303", "village_pop_101"),
            repository.list("village").map { stored -> stored.plan.planId }
        )
        assertEquals(
            listOf("village_pop_303", "harbor_pop_202", "village_pop_101"),
            repository.list().map { stored -> stored.plan.planId }
        )
    }

    @Test
    fun unsafeOrInvalidPlansAreRejectedWithoutWritingFiles() {
        val directory = temporaryDirectory.resolve("plans")
        val repository = PopulationPlanRepository(directory)

        assertThrows(IllegalArgumentException::class.java) {
            repository.save(plan("../outside", "village", "resident_one"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            repository.save(plan("empty_pop", "village", "resident_one").copy(households = emptyList()))
        }
        assertFalse(Files.exists(directory))
    }

    @Test
    fun differentSeedCannotSilentlyOverwriteTheSamePlanId() {
        val repository = PopulationPlanRepository(temporaryDirectory.resolve("plans"))
        val original = plan("village_pop_101", "village", "resident_one")
        repository.save(original)

        assertThrows(IllegalArgumentException::class.java) {
            repository.save(original.copy(seed = "colliding_seed"))
        }
        assertEquals(original, repository.find(original.planId)?.plan)
    }

    private fun plan(planId: String, regionId: String, residentKey: String): PopulationPlan {
        val resident = ResidentNarrativePlan(
            npcKey = residentKey,
            displayName = "Resident $residentKey",
            familyName = "Family $residentKey",
            relationRole = "owner",
            socialRole = "artisan",
            profession = "builder",
            ageGroup = "adult",
            age = 34,
            gender = "unspecified",
            homePlaceId = "${regionId}_house",
            bedNodeId = "${regionId}_bed",
            spawnNodeId = "${regionId}_spawn",
            workPlaceId = "${regionId}_work",
            workNodeId = "${regionId}_work_node",
            socialPlaceId = "${regionId}_square",
            socialNodeId = "${regionId}_social_node",
            routineProfile = "day_worker",
            questRole = "giver",
            personalitySeed = "personality_$residentKey",
            backstorySeed = "backstory_$residentKey"
        )
        return PopulationPlan(
            planId = planId,
            regionId = regionId,
            themeId = "test_theme",
            seed = "seed_$residentKey",
            targetPopulation = 1,
            households = listOf(
                HouseholdPlan(
                    householdKey = "household_$residentKey",
                    familyId = "family_$residentKey",
                    homePlaceId = "${regionId}_house",
                    capacity = 2,
                    primaryOwnerNpcKey = residentKey,
                    residents = listOf(resident),
                    familyType = "single"
                )
            ),
            unassignedWorkplaces = listOf("${regionId}_unused_work"),
            warnings = listOf("test warning")
        )
    }
}
