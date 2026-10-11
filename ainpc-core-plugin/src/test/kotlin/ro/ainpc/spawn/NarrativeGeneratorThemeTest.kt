package ro.ainpc.spawn

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.world.WorldAdminService
import java.util.logging.Logger

class NarrativeGeneratorThemeTest {
    @Test
    fun socialCapacityLimitsNarrativeAssignmentsAcrossHouseholds() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("NarrativeSocialCapacityTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        for (placeId in listOf("piata", "taverna", "altar")) {
            worldAdmin.getPlaceModels("demo_sat").single { it.id == "demo_sat:$placeId" }
                .putMetadata("max_social_associations", "1")
        }
        worldAdmin.bindNpcToSocialPlace("demo_sat:piata", "npc_existing", "Existing")

        val result = NarrativeGenerator().generatePopulationPlan(worldAdmin, "demo_sat", 4, "social-capacity")

        assertTrue(result.success()) { result.errors().toString() }
        val assignments = result.plan()!!.households.flatMap { it.residents }.map { it.socialPlaceId }
        assertEquals(2, assignments.count { it.isNotBlank() })
        assertEquals(2, assignments.count { it.isBlank() })
        assertEquals(2, assignments.filter { it.isNotBlank() }.toSet().size)
        assertTrue(result.warnings().any { it.contains("Capacitate sociala insuficienta") })
    }

    @Test
    fun invalidSocialCapacityStopsNarrativePlanning() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("NarrativeInvalidSocialCapacityTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        worldAdmin.getPlaceModels("demo_sat").single { it.id == "demo_sat:piata" }
            .putMetadata("max_social_associations", "many")

        val result = NarrativeGenerator().generatePopulationPlan(worldAdmin, "demo_sat", 1, "social-capacity")

        assertTrue(!result.success())
        assertTrue(result.errors().any { it.contains("max_social_associations") })
    }

    @Test
    fun skipsPopulatedHousesAndUsesDistinctPhysicalSpawnNodes() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("NarrativeAvailableHousesTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        worldAdmin.bindNpcToHomePlace("demo_sat:house_1", "npc_existing", "Existing")

        val result = NarrativeGenerator().generatePopulationPlan(worldAdmin, "demo_sat", 4, "stable-seed")

        assertTrue(result.success()) { result.errors().toString() }
        val households = result.plan()!!.households
        assertEquals(listOf("demo_sat:house_2", "demo_sat:house_3", "demo_sat:house_4"), households.map { it.homePlaceId })
        assertEquals(listOf(1, 1, 1), households.map { it.residents.size })
        assertEquals(3, households.flatMap { it.residents }.map { it.spawnNodeId }.toSet().size)
        assertTrue(result.warnings().any { it.contains("house_1") && it.contains("omisa") })
        assertTrue(result.warnings().any { it.contains("rezidenti neplanificati: 1") })
    }

    @Test
    fun refusesNarrativePlanWhenAllHousesAlreadyHaveResidents() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("NarrativeFullHousesTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        (1..4).forEach { houseNumber ->
            worldAdmin.bindNpcToHomePlace("demo_sat:house_$houseNumber", "npc_$houseNumber", "Existing $houseNumber")
        }

        val result = NarrativeGenerator().generatePopulationPlan(worldAdmin, "demo_sat", 1, "stable-seed")

        assertTrue(!result.success())
        assertTrue(result.errors().any { it.contains("nu are case fara rezidenti") })
    }

    @Test
    fun respectsExistingWorkersAndCapacityAcrossNarrativeHouseholds() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("NarrativeWorkCapacityTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        worldAdmin.bindNpcToWorkPlace("demo_sat:fierarie", "npc_existing", "Existing")
        worldAdmin.getPlaceModels("demo_sat").filter { it.id == "demo_sat:fierarie" }
            .single().putMetadata("max_workers", "2")
        worldAdmin.getPlaceModels("demo_sat").filter { it.id in setOf("demo_sat:ferma", "demo_sat:piata", "demo_sat:taverna") }
            .forEach { it.putMetadata("max_workers", "0") }
        val before = worldAdmin.getPlaces("demo_sat").associate { it.id() to it.metadata() }

        val result = NarrativeGenerator().generatePopulationPlan(worldAdmin, "demo_sat", 4, "stable-seed")

        assertTrue(result.success()) { result.errors().toString() }
        val residents = result.plan()!!.households.flatMap { it.residents }
        assertEquals(listOf("demo_sat:fierarie", "", "", ""), residents.map { it.workPlaceId })
        assertTrue(result.warnings().any { it.contains("3 rezidenti raman fara loc de munca") })
        assertEquals(before, worldAdmin.getPlaces("demo_sat").associate { it.id() to it.metadata() })
    }

    @Test
    fun rejectsInvalidNarrativeWorkplaceCapacity() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("NarrativeInvalidWorkCapacityTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        worldAdmin.getPlaceModels("demo_sat").single { it.id == "demo_sat:fierarie" }
            .putMetadata("max_workers", "-1")

        val result = NarrativeGenerator().generatePopulationPlan(worldAdmin, "demo_sat", 1, "stable-seed")

        assertTrue(!result.success())
        assertTrue(result.errors().any { it.contains("max_workers invalid") })
    }

    @Test
    fun reservesSocialAssignmentsAcrossHouseholdsInOnePreview() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("NarrativeSocialPreviewTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)

        val result = NarrativeGenerator().generatePopulationPlan(worldAdmin, "demo_sat", 4, "stable-seed")

        assertTrue(result.success()) { result.errors().toString() }
        val assignments = result.plan()!!.households.flatMap { it.residents }
            .groupingBy { it.socialPlaceId }
            .eachCount()
        assertEquals(4, assignments.values.sum())
        assertEquals(3, assignments.size)
        assertTrue(assignments.values.max() - assignments.values.min() <= 1)
    }

    @Test
    fun balancesSocialPlacesAgainstExistingBindingsWithoutChangingMapping() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("NarrativeSocialBindingsTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        worldAdmin.bindNpcToSocialPlace("demo_sat:piata", "npc_market", "Market")
        worldAdmin.bindNpcToSocialPlace("demo_sat:taverna", "npc_tavern", "Tavern")
        val before = worldAdmin.getPlaces("demo_sat").associate { it.id() to it.metadata() }

        val first = NarrativeGenerator().generatePopulationPlan(worldAdmin, "demo_sat", 1, "stable-seed")
        val repeated = NarrativeGenerator().generatePopulationPlan(worldAdmin, "demo_sat", 1, "stable-seed")

        assertTrue(first.success()) { first.errors().toString() }
        assertEquals("demo_sat:altar", first.plan()!!.households.single().residents.single().socialPlaceId)
        assertEquals(first.plan(), repeated.plan())
        assertEquals(before, worldAdmin.getPlaces("demo_sat").associate { it.id() to it.metadata() })
    }

    @Test
    fun resolvesThemeTagCaseInsensitivelyAndNormalizesId() {
        assertEquals(
            "dark_fantasy",
            resolveTaggedPopulationTheme(listOf("village", " Theme: Dark Fantasy "))
        )
        assertEquals("generic", resolveTaggedPopulationTheme(listOf("theme:generic")))
    }

    @Test
    fun ignoresMalformedThemeTags() {
        assertNull(
            resolveTaggedPopulationTheme(
                listOf("biome:plains", "theme", "theme: ", "theme: !!!")
            )
        )
    }

    @Test
    fun selectsTheSameThemeRegardlessOfTagOrder() {
        val tags = listOf("theme:rustic", "THEME: Alpine", "theme:rustic")

        assertEquals("alpine", resolveTaggedPopulationTheme(tags))
        assertEquals("alpine", resolveTaggedPopulationTheme(tags.reversed()))
    }
}
