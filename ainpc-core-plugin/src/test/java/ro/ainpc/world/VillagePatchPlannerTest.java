package ro.ainpc.world;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.ainpc.platform.PlatformProfile;
import ro.ainpc.platform.RuntimeMode;
import ro.ainpc.world.patch.GapReport;
import ro.ainpc.world.patch.PatchPlannerOptions;
import ro.ainpc.world.patch.PatchPlannerResult;
import ro.ainpc.world.patch.PatchType;
import ro.ainpc.world.patch.PatchValidationStatus;
import ro.ainpc.world.patch.VillageGapAnalyzer;
import ro.ainpc.world.patch.VillagePatchPlanner;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagePatchPlannerTest {

    private WorldAdminService service;

    @BeforeEach
    void setUp() {
        service = new WorldAdminService(message -> { }, Logger.getLogger("VillagePatchPlannerTest"));
    }

    @Test
    void analyzerFindsCapacityProfessionSocialAndQuestGaps() throws Exception {
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
            """), profile());
        PatchPlannerOptions options = PatchPlannerOptions.forTargetPopulation(4, List.of("blacksmith", "farmer"));

        GapReport report = new VillageGapAnalyzer().analyze(service, "demo_sat", options);

        assertTrue(report.success());
        assertEquals(1, report.currentCapacity());
        assertEquals(4, report.requiredCapacity());
        assertEquals(2, report.missingHomes());
        assertEquals(List.of("blacksmith"), report.missingWorkplaces());
        assertEquals(1, report.missingSocialPlaces());
        assertTrue(report.missingNodes().contains("quest_trigger"));
        assertTrue(report.gaps().stream().anyMatch(gap -> gap.type().name().equals("WORKPLACE_WITHOUT_WORK_NODE")));
    }

    @Test
    void plannerTurnsGapsIntoReadOnlyPatchPlans() throws Exception {
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
            """), profile());
        PatchPlannerOptions options = PatchPlannerOptions.forTargetPopulation(4, List.of("blacksmith"));
        GapReport report = new VillageGapAnalyzer().analyze(service, "demo_sat", options);

        PatchPlannerResult result = new VillagePatchPlanner().plan(report, options);

        assertTrue(result.success());
        assertTrue(result.patchPlans().stream().anyMatch(plan -> plan.type() == PatchType.ADD_HOUSE));
        assertTrue(result.patchPlans().stream().anyMatch(plan -> plan.type() == PatchType.ADD_WORKPLACE));
        assertTrue(result.patchPlans().stream().anyMatch(plan -> plan.type() == PatchType.ADD_SOCIAL_PLACE));
        assertTrue(result.patchPlans().stream().anyMatch(plan -> plan.type() == PatchType.ADD_NODE));
        assertTrue(result.patchPlans().stream().anyMatch(plan -> plan.validationStatus() == PatchValidationStatus.BLOCKED));
        assertFalse(result.patchPlans().stream()
            .filter(plan -> plan.type() == PatchType.ADD_NODE)
            .findFirst()
            .orElseThrow()
            .plannedNodes()
            .isEmpty());
    }

    @Test
    void demoMappingHasNoRequiredPatchForInitialPlayablePopulation() {
        service.createDemoSettlement("demo_sat", "world", 0, 64, 0, 0, 320);
        PatchPlannerOptions options = PatchPlannerOptions.forTargetPopulation(
            6,
            List.of("blacksmith", "farmer", "merchant", "innkeeper")
        );

        GapReport report = new VillageGapAnalyzer().analyze(service, "demo_sat", options);

        assertTrue(report.success());
        assertEquals(6, report.currentCapacity());
        assertEquals(0, report.missingHomes());
        assertTrue(report.missingWorkplaces().isEmpty());
        assertEquals(0, report.missingSocialPlaces());
        assertFalse(report.missingNodes().contains("quest_trigger"));
    }

    private PlatformProfile profile() {
        return new PlatformProfile(RuntimeMode.STANDALONE, WorldMode.FINITE_DYNAMIC, StoryMode.EVOLUTIVE);
    }

    private YamlConfiguration loadConfig(String content) throws InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(content);
        return configuration;
    }
}
