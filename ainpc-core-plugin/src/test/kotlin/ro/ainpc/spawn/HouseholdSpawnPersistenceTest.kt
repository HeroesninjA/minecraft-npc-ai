package ro.ainpc.spawn

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import java.sql.SQLException

class HouseholdSpawnPersistenceTest {
    private val allocation = HouseAllocation.builder("sat_a:house_1")
        .familyId("family_a")
        .addResident(HouseAllocation.ResidentPlan.builder("npc_ion", "Ion").build())
        .addResident(HouseAllocation.ResidentPlan.builder("npc_maria", "Maria").build())
        .build()
    private val plans = allocation.toNpcSpawnPlans()
    private val spawnResults = listOf(
        NpcSpawnResult.created(AINPC(null), emptyList()),
        NpcSpawnResult.reused(AINPC(null), emptyList())
    )
    private val familyBinding = FamilyBindingResult(true, "family_a", 2, emptyList(), emptyList())

    @Test
    fun successfulSaveCompletesHouseholdAndPreservesDetails() {
        val warnings = mutableListOf("existing warning")
        var calls = 0

        val result = HouseholdSpawnPersistence.complete(allocation, plans, spawnResults, familyBinding, warnings) {
            calls++
            2
        }

        assertTrue(result.success())
        assertFalse(result.persistenceFailed())
        assertFalse(result.rolledBack())
        assertFalse(result.dryRun())
        assertTrue(result.errors().isEmpty())
        assertEquals(1, calls)
        assertEquals(spawnResults, result.spawnResults())
        assertSame(familyBinding, result.familyBindingResult())
        assertTrue(result.warnings().contains("existing warning"))
        assertTrue(result.warnings().any { it.contains("rezidenti=2") })
        assertEquals(listOf("existing warning"), warnings)
    }

    @Test
    fun sqlFailureIsNotSuccessAndRetainsCreatedAndReusedNpcs() {
        val result = HouseholdSpawnPersistence.complete(allocation, plans, spawnResults, familyBinding, listOf("existing warning")) {
            throw SQLException("injected database failure")
        }

        assertFalse(result.success())
        assertTrue(result.persistenceFailed())
        assertFalse(result.rolledBack())
        assertFalse(result.dryRun())
        assertEquals(plans, result.spawnPlans())
        assertEquals(spawnResults, result.spawnResults())
        assertSame(familyBinding, result.familyBindingResult())
        assertTrue(result.errors().any { it.contains("family_a") && it.contains("injected database failure") })
        assertTrue(result.warnings().contains("existing warning"))
        assertTrue(result.warnings().any { it.contains("pastrate pentru recuperare") })
        assertFalse(result.warnings().any { it.contains("persistent actualizat") })
    }

    @Test
    fun incompleteResidentCountRequiresRecovery() {
        for (savedResidents in listOf(0, 1, 3)) {
            val result = HouseholdSpawnPersistence.complete(allocation, plans, spawnResults, familyBinding, emptyList()) {
                savedResidents
            }

            assertFalse(result.success())
            assertTrue(result.persistenceFailed())
            assertFalse(result.rolledBack())
            assertEquals(spawnResults, result.spawnResults())
            assertTrue(result.errors().any { it.contains("rezidenti=$savedResidents, asteptati=2") })
            assertFalse(result.warnings().any { it.contains("persistent actualizat") })
        }
    }

    @Test
    fun missingAllocationDoesNotInvokePersistenceOrReportSuccess() {
        val result = HouseholdSpawnPersistence.complete(null, plans, spawnResults, familyBinding, emptyList()) {
            error("Persistence must not run without an allocation")
        }

        assertFalse(result.success())
        assertTrue(result.persistenceFailed())
        assertFalse(result.rolledBack())
        assertTrue(result.errors().any { it.contains("HouseAllocation lipseste") })
    }

    @Test
    fun legacyConstructorRemainsAvailableAndDefaultsToNonPersistenceFailure() {
        val constructor = HouseholdSpawnResult::class.java.getConstructor(
            Boolean::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
            List::class.java,
            List::class.java,
            FamilyBindingResult::class.java,
            List::class.java,
            List::class.java
        )
        val result = constructor.newInstance(false, false, true, plans, spawnResults, familyBinding, listOf("spawn failure"), emptyList<String>())

        assertFalse(result.persistenceFailed())
        assertTrue(result.rolledBack())
        assertEquals(
            HouseholdSpawnResult.failed(false, true, plans, spawnResults, familyBinding, listOf("spawn failure"), emptyList()),
            result
        )
    }

    @Test
    fun persistenceFailureParticipatesInValueSemantics() {
        val errors = mutableListOf("save failed")
        val warnings = mutableListOf("recovery needed")
        val first = HouseholdSpawnResult.failedPersistence(plans, spawnResults, familyBinding, errors, warnings)
        val second = HouseholdSpawnResult.failedPersistence(plans, spawnResults, familyBinding, errors, warnings)
        val ordinaryFailure = HouseholdSpawnResult.failed(false, false, plans, spawnResults, familyBinding, errors, warnings)
        errors.clear()
        warnings.clear()

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, ordinaryFailure)
        assertEquals(listOf("save failed"), first.errors())
        assertEquals(listOf("recovery needed"), first.warnings())
        assertTrue(first.toString().contains("persistenceFailed=true"))
    }
}
