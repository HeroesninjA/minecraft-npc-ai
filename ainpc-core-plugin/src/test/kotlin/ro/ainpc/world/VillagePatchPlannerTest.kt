package ro.ainpc.world

import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ro.ainpc.platform.PlatformProfile
import ro.ainpc.platform.RuntimeMode
import ro.ainpc.world.patch.PatchPlannerOptions
import ro.ainpc.world.patch.PatchType
import ro.ainpc.world.patch.PatchValidationStatus
import ro.ainpc.world.patch.VillageGapAnalyzer
import ro.ainpc.world.patch.VillagePatchPlanner
import java.util.logging.Logger

class VillagePatchPlannerTest {
    private lateinit var service: WorldAdminService

    @BeforeEach
    fun setUp() {
        service = WorldAdminService({ }, Logger.getLogger("VillagePatchPlannerTest"))
    }

    @Test
    @Throws(Exception::class)
    fun analyzerFindsCapacityProfessionSocialAndQuestGaps() {
        service.reloadFromConfig(loadConfig("""
            world_admin:
              enabled: true
              regions:
                demo_sat:
                  name: "Demo Sat"
                  world: "world"
                  type: "settlement"
                  min: { x: 0, y: 50, z: 0 }
                  max: { x: 100, y: 90, z: 100 }
                  places:
                    house_1:
                      name: "Casa 1"
                      type: "house"
                      min: { x: 10, y: 60, z: 10 }
                      max: { x: 20, y: 70, z: 20 }
                      metadata:
                        max_residents: "1"
                      nodes:
                        bed_1:
                          type: "bed"
                          x: 15
                          y: 62
                          z: 14
                          radius: 1.5
                    farm:
                      name: "Ferma"
                      type: "farm"
                      min: { x: 35, y: 60, z: 10 }
                      max: { x: 55, y: 70, z: 30 }
                      tags: [workplace]
                      metadata:
                        profession: "farmer"
            """), profile())
        val options = PatchPlannerOptions.forTargetPopulation(4, listOf("blacksmith", "farmer"))

        val report = VillageGapAnalyzer().analyze(service, "demo_sat", options)

        assertTrue(report.success())
        assertEquals(1, report.currentCapacity())
        assertEquals(4, report.requiredCapacity())
        assertEquals(2, report.missingHomes())
        assertEquals(listOf("blacksmith"), report.missingWorkplaces())
        assertEquals(1, report.missingSocialPlaces())
        assertTrue(report.missingNodes().contains("quest_trigger"))
        assertTrue(report.gaps().any { gap -> gap.type().name == "WORKPLACE_WITHOUT_WORK_NODE" })
    }

    @Test
    @Throws(Exception::class)
    fun plannerTurnsGapsIntoReadOnlyPatchPlans() {
        service.reloadFromConfig(loadConfig("""
            world_admin:
              enabled: true
              regions:
                demo_sat:
                  name: "Demo Sat"
                  world: "world"
                  type: "settlement"
                  min: { x: 0, y: 50, z: 0 }
                  max: { x: 100, y: 90, z: 100 }
                  places:
                    house_1:
                      name: "Casa 1"
                      type: "house"
                      min: { x: 10, y: 60, z: 10 }
                      max: { x: 20, y: 70, z: 20 }
                      metadata:
                        max_residents: "1"
                      nodes:
                        bed_1:
                          type: "bed"
                          x: 15
                          y: 62
                          z: 14
                          radius: 1.5
            """), profile())
        val options = PatchPlannerOptions.forTargetPopulation(4, listOf("blacksmith"))
        val report = VillageGapAnalyzer().analyze(service, "demo_sat", options)

        val result = VillagePatchPlanner().plan(report, options)

        assertTrue(result.success())
        assertTrue(result.patchPlans().any { plan -> plan.type() == PatchType.ADD_HOUSE })
        assertTrue(result.patchPlans().any { plan -> plan.type() == PatchType.ADD_WORKPLACE })
        assertTrue(result.patchPlans().any { plan -> plan.type() == PatchType.ADD_SOCIAL_PLACE })
        assertTrue(result.patchPlans().any { plan -> plan.type() == PatchType.ADD_NODE })
        assertTrue(result.patchPlans().any { plan -> plan.validationStatus() == PatchValidationStatus.BLOCKED })
        assertFalse(
            result.patchPlans()
                .first { plan -> plan.type() == PatchType.ADD_NODE }
                .plannedNodes()
                .isEmpty()
        )
    }

    @Test
    fun demoMappingHasNoRequiredPatchForInitialPlayablePopulation() {
        service.createDemoSettlement("demo_sat", "world", 0, 64, 0, 0, 320)
        val options = PatchPlannerOptions.forTargetPopulation(
            6,
            listOf("blacksmith", "farmer", "merchant", "innkeeper")
        )

        val report = VillageGapAnalyzer().analyze(service, "demo_sat", options)

        assertTrue(report.success())
        assertEquals(6, report.currentCapacity())
        assertEquals(0, report.missingHomes())
        assertTrue(report.missingWorkplaces().isEmpty())
        assertEquals(0, report.missingSocialPlaces())
        assertFalse(report.missingNodes().contains("quest_trigger"))
    }

    private fun profile(): PlatformProfile {
        return PlatformProfile(RuntimeMode.STANDALONE, WorldMode.FINITE_DYNAMIC, StoryMode.EVOLUTIVE)
    }

    @Throws(InvalidConfigurationException::class)
    private fun loadConfig(content: String): YamlConfiguration {
        val configuration = YamlConfiguration()
        configuration.loadFromString(content)
        return configuration
    }
}
