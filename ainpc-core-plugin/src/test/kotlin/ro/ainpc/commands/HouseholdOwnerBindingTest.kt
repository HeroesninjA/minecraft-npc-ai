package ro.ainpc.commands

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.platform.PlatformProfile
import ro.ainpc.platform.RuntimeMode
import ro.ainpc.spawn.HouseAllocation
import ro.ainpc.spawn.HouseholdSpawnResult
import ro.ainpc.spawn.NpcSpawnResult
import ro.ainpc.world.StoryMode
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.WorldMode
import java.util.logging.Logger

class HouseholdOwnerBindingTest {
    private lateinit var service: WorldAdminService
    private val allocation = HouseAllocation.builder("demo_sat:house_1")
        .familyId("family_house_1")
        .primaryOwnerNpcKey("npc_gigel")
        .addResident(HouseAllocation.ResidentPlan.builder("npc_maria", "Maria").build())
        .addResident(HouseAllocation.ResidentPlan.builder("npc_gigel", "Gigel").build())
        .build()
    private val spawnResults = listOf(
        NpcSpawnResult.created(npc(42, "Maria"), emptyList()),
        NpcSpawnResult.reused(npc(43, "Gigel"), emptyList())
    )

    @BeforeEach
    fun setUp() {
        service = WorldAdminService({ }, Logger.getLogger("HouseholdOwnerBindingTest"))
        service.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
    }

    @Test
    fun resolvesThePrimaryOwnerToTheCreatedOrReusedNpcId() {
        val result = success(allocation, spawnResults)

        val binding = HouseholdOwnerBinding.resolve(allocation, result)

        assertEquals("demo_sat:house_1", binding.homePlaceId)
        assertEquals("npc_43", binding.npcId)
        assertEquals("Gigel", binding.npcName)
    }

    @Test
    fun primaryOwnerIsStableAcrossResidentOrderRerunAndConfigReload() {
        for (reversedOrder in listOf(false, true)) {
            val orderedAllocation = if (reversedOrder) allocation.copy(residentPlans = allocation.residentPlans().reversed()) else allocation
            val orderedResults = if (reversedOrder) spawnResults.reversed() else spawnResults
            val result = success(orderedAllocation, orderedResults)
            val owner = HouseholdOwnerBinding.resolve(orderedAllocation, result)
            for (spawnResult in orderedResults) {
                val npc = spawnResult.npc()!!
                service.addNpcResidentToHomePlace(allocation.placeId(), "npc_${npc.databaseId}", npc.name)
            }
            owner.apply(service)
            val configuration = YamlConfiguration()
            service.saveToConfig(configuration)
            val reloaded = WorldAdminService({ }, Logger.getLogger("HouseholdOwnerReloadTest"))
            reloaded.reloadFromConfig(configuration, PlatformProfile(RuntimeMode.STANDALONE, WorldMode.FINITE_DYNAMIC, StoryMode.EVOLUTIVE))
            reloaded.bindNpcToHomePlace(allocation.placeId(), "npc_42", "Maria")
            owner.apply(reloaded)

            val home = reloaded.getPlace(allocation.placeId())!!
            assertEquals("npc_43", home.ownerNpcId())
            assertEquals("Gigel", home.metadata()["owner_name"])
            assertEquals("npc_42,npc_43", home.metadata()["resident_npc_ids"])
            assertEquals("assigned", home.metadata()["owner_status"])
        }
    }

    @Test
    fun normalizationMatchesTheAllocationValidator() {
        val normalizedAllocation = allocation.copy(primaryOwnerNpcKey = " NPC-GIGEL ")

        val owner = HouseholdOwnerBinding.resolve(normalizedAllocation, success(normalizedAllocation, spawnResults))

        assertEquals("npc_43", owner.npcId)
    }

    @Test
    fun absentOrUnknownOwnerDoesNotFallBackToFirstOrLastResident() {
        for (ownerKey in listOf("", "npc_missing")) {
            val invalidAllocation = allocation.copy(primaryOwnerNpcKey = ownerKey)
            assertThrows(IllegalArgumentException::class.java) {
                HouseholdOwnerBinding.resolve(invalidAllocation, success(invalidAllocation, spawnResults))
            }
        }
        assertEquals("", service.getPlace(allocation.placeId())!!.ownerNpcId())
    }

    @Test
    fun duplicateNormalizedOwnerKeysAreRejected() {
        val duplicateAllocation = allocation.copy(residentPlans = listOf(
            HouseAllocation.ResidentPlan.builder("npc-gigel", "Gigel").build(),
            HouseAllocation.ResidentPlan.builder("npc_gigel", "Alt Gigel").build()
        ))

        assertThrows(IllegalArgumentException::class.java) {
            HouseholdOwnerBinding.resolve(duplicateAllocation, success(duplicateAllocation, spawnResults))
        }
    }

    @Test
    fun failedOrDryRunHouseholdIsNotEligibleForOwnership() {
        val plans = allocation.toNpcSpawnPlans()
        val ineligible = listOf(
            HouseholdSpawnResult.dryRunSuccess(plans, emptyList()),
            HouseholdSpawnResult.failedPersistence(plans, spawnResults, null, listOf("SQL failure"), emptyList())
        )

        for (result in ineligible) {
            assertThrows(IllegalArgumentException::class.java) {
                HouseholdOwnerBinding.resolve(allocation, result)
            }
        }
    }

    @Test
    fun failedOrUnpersistedOwnerNpcCannotOwnTheHouse() {
        val invalidOwners = listOf(
            NpcSpawnResult.failed(listOf("spawn failed"), emptyList()),
            NpcSpawnResult.created(npc(0, "Gigel"), emptyList())
        )
        for (owner in invalidOwners) {
            assertThrows(IllegalArgumentException::class.java) {
                HouseholdOwnerBinding.resolve(allocation, success(allocation, listOf(spawnResults.first(), owner)))
            }
        }
    }

    @Test
    fun mismatchedPlanAndResultCountsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            HouseholdOwnerBinding.resolve(allocation, success(allocation, spawnResults.take(1)))
        }
    }

    @Test
    fun ownerCannotBeResolvedFromADifferentHome() {
        val differentHome = allocation.copy(placeId = "demo_sat:house_2")

        assertThrows(IllegalArgumentException::class.java) {
            HouseholdOwnerBinding.resolve(allocation, success(differentHome, spawnResults))
        }
    }

    @Test
    fun missingOrAmbiguousOwnerSpawnPlanIsRejected() {
        val plans = allocation.toNpcSpawnPlans()
        val invalidResults = listOf(
            HouseholdSpawnResult.success(plans.take(1), spawnResults.take(1), null, emptyList()),
            HouseholdSpawnResult.success(listOf(plans.last(), plans.last()), spawnResults, null, emptyList())
        )

        for (result in invalidResults) {
            assertThrows(IllegalArgumentException::class.java) {
                HouseholdOwnerBinding.resolve(allocation, result)
            }
        }
    }

    @Test
    fun completeBindingAssignsTheOwnerAndCountsOneAdditionalMappingWrite() {
        service.addNpcResidentToHomePlace(allocation.placeId(), "npc_42", "Maria")
        service.addNpcResidentToHomePlace(allocation.placeId(), "npc_43", "Gigel")
        val owner = HouseholdOwnerBinding.resolve(allocation, success(allocation, spawnResults))
        val progress = PostSpawnBindingResult(1, 1, 2, 2, 2, 2, emptyList())

        val result = owner.complete(service, progress)

        assertEquals(PostSpawnBindingStatus.COMPLETE, result.status)
        assertEquals(3, result.mappingWritesApplied)
        assertEquals(2, result.persistentBindingsSaved)
        assertEquals(1, result.householdsCompleted)
        assertEquals("npc_43", service.getPlace(allocation.placeId())!!.ownerNpcId())
    }

    @Test
    fun ownerConflictProducesPartialBindingRatherThanCompleteSuccess() {
        service.bindNpcToHomePlace(allocation.placeId(), "npc_42", "Maria")
        service.addNpcResidentToHomePlace(allocation.placeId(), "npc_43", "Gigel")
        val owner = HouseholdOwnerBinding.resolve(allocation, success(allocation, spawnResults))
        val progress = PostSpawnBindingResult(1, 1, 2, 2, 2, 2, emptyList())

        val result = owner.complete(service, progress)

        assertEquals(PostSpawnBindingStatus.PARTIAL, result.status)
        assertEquals(0, result.householdsCompleted)
        assertEquals(2, result.npcsCompleted)
        assertEquals(2, result.mappingWritesApplied)
        assertEquals(2, result.persistentBindingsSaved)
        assertEquals(1, result.failures.size)
        assertEquals("npc_42", service.getPlace(allocation.placeId())!!.ownerNpcId())
    }

    @Test
    fun incompleteResidentBindingDoesNotAssignAnOwnerOrHideEarlierFailures() {
        service.addNpcResidentToHomePlace(allocation.placeId(), "npc_43", "Gigel")
        val owner = HouseholdOwnerBinding.resolve(allocation, success(allocation, spawnResults))
        val incomplete = listOf(
            PostSpawnBindingResult(1, 0, 2, 1, 2, 1, listOf("npc_world_bindings failed")),
            PostSpawnBindingResult(1, 0, 2, 0, 0, 0, listOf("mapping failed")),
            PostSpawnBindingResult.EMPTY
        )

        for (progress in incomplete) {
            val result = owner.complete(service, progress)
            assertEquals(progress, result)
            assertEquals("", service.getPlace(allocation.placeId())!!.ownerNpcId())
        }
    }

    @Test
    fun existingDifferentOwnerRequiresExplicitRecoveryInsteadOfSilentReplacement() {
        service.bindNpcToHomePlace(allocation.placeId(), "npc_42", "Maria")
        service.addNpcResidentToHomePlace(allocation.placeId(), "npc_43", "Gigel")
        val before = service.getPlace(allocation.placeId())!!
        val owner = HouseholdOwnerBinding.resolve(allocation, success(allocation, spawnResults))

        assertThrows(IllegalArgumentException::class.java) {
            owner.apply(service)
        }

        val after = service.getPlace(allocation.placeId())!!
        assertEquals(before.ownerNpcId(), after.ownerNpcId())
        assertEquals(before.metadata(), after.metadata())
    }

    private fun success(allocation: HouseAllocation, results: List<NpcSpawnResult>): HouseholdSpawnResult =
        HouseholdSpawnResult.success(allocation.toNpcSpawnPlans(), results, null, emptyList())

    private fun npc(id: Int, name: String): AINPC = AINPC(null).apply {
        databaseId = id
        this.name = name
    }
}
