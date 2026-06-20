package ro.ainpc.world.exterior

import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ro.ainpc.platform.PlatformProfile
import ro.ainpc.platform.RuntimeMode
import ro.ainpc.world.StoryMode
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.WorldMode
import java.util.logging.Logger

class ExteriorStructureAnalyzerTest {
    private lateinit var service: WorldAdminService
    private lateinit var analyzer: ExteriorStructureAnalyzer

    @BeforeEach
    fun setUp() {
        service = WorldAdminService({ }, Logger.getLogger("ExteriorStructureAnalyzerTest"))
        analyzer = ExteriorStructureAnalyzer()
    }

    @Test
    fun blueprintCatalogCoversEveryCanonicalExteriorType() {
        val expectedTypes = ExteriorStructureType.entries
            .filter { type -> type != ExteriorStructureType.CUSTOM }
            .toSet()
        val actualTypes = ExteriorStructureBlueprintCatalog.all()
            .map { blueprint -> blueprint.type() }
            .toSet()

        assertEquals(expectedTypes, actualTypes)
        for (blueprint in ExteriorStructureBlueprintCatalog.all()) {
            assertTrue(blueprint.title().isNotBlank())
            assertTrue(blueprint.summary().isNotBlank())
            assertTrue(blueprint.tags().isNotEmpty())
            assertTrue(blueprint.requiredNodes().isNotEmpty())
        }
    }

    @Test
    fun barbarianBlueprintAliasStaysNeutral() {
        val blueprint = ExteriorStructureBlueprintCatalog.find("barbarian_village")

        assertNotNull(blueprint)
        assertEquals(ExteriorStructureType.FACTION_SETTLEMENT, blueprint!!.type())
        assertTrue(blueprint.summary().contains("neutra"))
    }

    @Test
    fun plannerBuildsReadOnlySkeletonFromBlueprint() {
        val plan = ExteriorStructurePlanner().plan("dungeon", "Cripta Lupilor!")

        assertEquals(ExteriorStructureType.DUNGEON, plan.type())
        assertEquals("cripta_lupilor", plan.regionId())
        assertEquals("Cripta Lupilor", plan.displayName())
        assertTrue(plan.plannedPlaces().contains("cripta_lupilor:entrance"))
        assertTrue(plan.plannedNodes().contains("cripta_lupilor:entrance [entrance]"))
        assertTrue(plan.warnings().any { warning -> warning.contains("read-only") })
    }

    @Test
    @Throws(Exception::class)
    fun dungeonRequiresSeparateExit() {
        service.reloadFromConfig(
            loadConfig(
                """
            world_admin:
              enabled: true
              regions:
                cripta_lupilor:
                  name: "Cripta Lupilor"
                  world: "world"
                  type: "dungeon"
                  min: { x: 0, y: 30, z: 0 }
                  max: { x: 80, y: 70, z: 80 }
                  tags: [danger_high, combat, loot]
                  nodes:
                    entrance:
                      type: "entrance"
                      x: 10
                      y: 50
                      z: 10
                      radius: 3.0
                    boss_spawn:
                      type: "boss"
                      x: 60
                      y: 45
                      z: 60
                      radius: 5.0
        """
            ), profile()
        )

        val report = analyzer.analyze(service, service.getRegion("cripta_lupilor")!!)

        assertEquals(ExteriorStructureType.DUNGEON, report.type())
        assertFalse(report.success())
        assertTrue(report.errors().any { error -> error.contains("iesire") })
    }

    @Test
    @Throws(Exception::class)
    fun barbarianAliasMapsToNeutralFactionSettlement() {
        service.reloadFromConfig(
            loadConfig(
                """
            world_admin:
              enabled: true
              regions:
                tabara_barbari:
                  name: "Tabara Barbari"
                  world: "world"
                  type: "settlement"
                  min: { x: 0, y: 50, z: 0 }
                  max: { x: 80, y: 90, z: 80 }
                  tags: [exterior, barbarian_village, faction, danger_medium, hostile_optional]
                  places:
                    center:
                      name: "Campfire Center"
                      type: "camp"
                      min: { x: 30, y: 60, z: 30 }
                      max: { x: 40, y: 70, z: 40 }
                      tags: [campfire, leader]
                      nodes:
                        campfire:
                          type: "meeting_point"
                          x: 35
                          y: 64
                          z: 35
                          radius: 4.0
                    storage:
                      name: "Loot Storage"
                      type: "custom"
                      min: { x: 50, y: 60, z: 30 }
                      max: { x: 58, y: 70, z: 38 }
                      tags: [storage, loot]
                  nodes:
                    gate:
                      type: "entrance"
                      x: 5
                      y: 64
                      z: 40
                      radius: 3.0
        """
            ), profile()
        )

        val report = analyzer.analyze(service, service.getRegion("tabara_barbari")!!)

        assertEquals(ExteriorStructureType.FACTION_SETTLEMENT, report.type())
        assertTrue(report.success())
        assertTrue(report.entryNodes().contains("tabara_barbari:gate"))
    }

    @Test
    @Throws(Exception::class)
    fun isolatedHouseUsesHousePlaceAndHomeNodes() {
        service.reloadFromConfig(
            loadConfig(
                """
            world_admin:
              enabled: true
              regions:
                casa_padurarului:
                  name: "Casa Padurarului"
                  world: "world"
                  type: "custom"
                  min: { x: 0, y: 60, z: 0 }
                  max: { x: 30, y: 85, z: 30 }
                  tags: [isolated_house, remote, danger_none]
                  places:
                    casa:
                      name: "Casa"
                      type: "house"
                      min: { x: 8, y: 64, z: 8 }
                      max: { x: 20, y: 75, z: 20 }
                      tags: [home, shelter]
                      nodes:
                        door:
                          type: "entrance"
                          x: 10
                          y: 65
                          z: 8
                          radius: 2.0
                        bed:
                          type: "bed"
                          x: 15
                          y: 65
                          z: 15
                          radius: 1.5
        """
            ), profile()
        )

        val report = analyzer.analyze(service, service.getRegion("casa_padurarului")!!)

        assertEquals(ExteriorStructureType.ISOLATED_HOUSE, report.type())
        assertTrue(report.success())
        assertTrue(report.warnings().isEmpty())
    }

    private fun profile(): PlatformProfile =
        PlatformProfile(RuntimeMode.STANDALONE, WorldMode.FINITE_DYNAMIC, StoryMode.EVOLUTIVE)

    @Throws(InvalidConfigurationException::class)
    private fun loadConfig(content: String): YamlConfiguration {
        val configuration = YamlConfiguration()
        configuration.loadFromString(content)
        return configuration
    }
}
