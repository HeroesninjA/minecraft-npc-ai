package ro.ainpc.world.fixture

import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.platform.PlatformProfile
import ro.ainpc.platform.RuntimeMode
import ro.ainpc.world.PlaceType
import ro.ainpc.world.RegionType
import ro.ainpc.world.StoryMode
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.WorldMode
import ro.ainpc.world.WorldNodeType
import java.util.logging.Logger

class ControlledTestWorldFixturePlannerTest {
    @Test
    fun defaultPlanBuildsMappingOnlyControlledFixture() {
        val plan = ControlledTestWorldFixturePlanner().plan()

        assertEquals("test_", plan.prefix())
        assertEquals("test_world_fixture_01", plan.fixtureId())
        assertEquals("test_sat_central", plan.villageRegion().id())
        assertEquals(8, plan.regionCount())
        assertEquals(14, plan.villageRegion().plannedPlaces().size)
        assertEquals(7, plan.exteriorRegions().size)
        assertTrue(plan.mappingOnly())
        assertFalse(plan.usesWorldEdit())
        assertFalse(plan.autoBuildBlocks())
        assertFalse(plan.autoSpawnNpcs())
        assertFalse(plan.autoSpawnMobs())
        assertFalse(plan.autoGrantQuestProgress())
        assertTrue(plan.warnings().any { warning -> warning.contains("read-only") })
        assertTrue(plan.warnings().any { warning -> warning.contains("WorldEdit") })
    }

    @Test
    fun defaultPlanContainsRequiredExteriorStructures() {
        val plan = ControlledTestWorldFixturePlanner().plan()
        val exteriorIds = plan.exteriorRegions().map { region -> region.id() }

        assertTrue(exteriorIds.contains("test_padure_veche"))
        assertTrue(exteriorIds.contains("test_fantana_uitata"))
        assertTrue(exteriorIds.contains("test_casa_izolata"))
        assertTrue(exteriorIds.contains("test_cripta_lupilor"))
        assertTrue(exteriorIds.contains("test_tabara_factiune"))
        assertTrue(exteriorIds.contains("test_turn_paza"))
        assertTrue(exteriorIds.contains("test_ruine_vechi"))
        assertTrue(
            plan.exteriorRegions()
                .first { region -> region.id() == "test_cripta_lupilor" }
                .requiredNodes()
                .containsAll(listOf("entrance", "exit", "evidence"))
        )
    }

    @Test
    fun customPrefixIsNormalizedAndApplied() {
        val plan = ControlledTestWorldFixturePlanner().plan("Demo Run!")

        assertEquals("demo_run_", plan.prefix())
        assertEquals("demo_run_world_fixture_01", plan.fixtureId())
        assertEquals("demo_run_sat_central", plan.villageRegion().id())
        assertTrue(plan.exteriorRegions().all { region -> region.id().startsWith("demo_run_") })
    }

    @Test
    @Throws(Exception::class)
    fun validatorReportsMissingMappingWithoutWritingAnything() {
        val service = enabledWorldAdmin()
        val plan = ControlledTestWorldFixturePlanner().plan()

        val report = ControlledTestWorldFixtureValidator().validate(service, plan)

        assertFalse(report.success())
        assertEquals(plan.regionCount(), report.expectedRegions())
        assertEquals(0, report.foundRegions())
        assertTrue(report.errors().any { error -> error.contains("test_sat_central") })
        assertEquals(0, service.regionCount)
        assertFalse(service.hasUnsavedChanges())
    }

    @Test
    @Throws(Exception::class)
    fun validatorAcceptsCompleteInMemoryMapping() {
        val service = enabledWorldAdmin()
        val plan = ControlledTestWorldFixturePlanner().plan()
        createMapping(service, plan)

        val report = ControlledTestWorldFixtureValidator().validate(service, plan)

        assertTrue(report.success())
        assertEquals(plan.regionCount(), report.foundRegions())
        assertEquals(plan.placeCount(), report.foundPlaces())
        assertEquals(plan.nodeCount(), report.foundNodes())
        assertTrue(report.errors().isEmpty())
    }

    @Throws(Exception::class)
    private fun enabledWorldAdmin(): WorldAdminService {
        val service = WorldAdminService({ }, Logger.getLogger("ControlledTestWorldFixturePlannerTest"))
        service.reloadFromConfig(
            loadConfig(
                """
                world_admin:
                  enabled: true
                """.trimIndent()
            ),
            PlatformProfile(RuntimeMode.STANDALONE, WorldMode.FINITE_DYNAMIC, StoryMode.EVOLUTIVE)
        )
        return service
    }

    private fun createMapping(service: WorldAdminService, plan: ControlledTestWorldFixturePlan) {
        for (region in listOf(plan.villageRegion()) + plan.exteriorRegions()) {
            val minX = region.offsetX() * 10
            val minZ = region.offsetZ() * 10
            service.createRegion(
                region.id(),
                region.displayName(),
                "world",
                RegionType.fromId(region.type()),
                minX,
                50,
                minZ,
                minX + 120,
                90,
                minZ + 120
            )

            region.requiredNodes().forEachIndexed { index, node ->
                service.createNode(
                    region.id(),
                    null,
                    node,
                    WorldNodeType.fromId(node),
                    "world",
                    minX + 4.0 + index,
                    60.0,
                    minZ + 4.0 + index,
                    2.0
                )
            }

            region.plannedPlaces().forEachIndexed { index, place ->
                val localPlaceId = place.id().substringAfter(':')
                val placeMinX = minX + 5 + (index % 5) * 20
                val placeMinZ = minZ + 20 + (index / 5) * 20
                service.createPlace(
                    region.id(),
                    localPlaceId,
                    localPlaceId,
                    "world",
                    PlaceType.fromId(place.type()),
                    placeMinX,
                    55,
                    placeMinZ,
                    placeMinX + 8,
                    70,
                    placeMinZ + 8
                )
                place.requiredNodes().forEachIndexed { nodeIndex, node ->
                    service.createNode(
                        region.id(),
                        place.id(),
                        node,
                        WorldNodeType.fromId(node),
                        "world",
                        placeMinX + 1.0 + (nodeIndex % 4),
                        60.0,
                        placeMinZ + 1.0 + (nodeIndex / 4),
                        1.5
                    )
                }
            }
        }
    }

    @Throws(InvalidConfigurationException::class)
    private fun loadConfig(content: String): YamlConfiguration {
        val configuration = YamlConfiguration()
        configuration.loadFromString(content)
        return configuration
    }
}
