package ro.ainpc.world

import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.engine.QuestAnchorResolver
import ro.ainpc.engine.ScenarioEngine
import ro.ainpc.platform.PlatformProfile
import ro.ainpc.platform.RuntimeMode
import java.util.logging.Logger

class QuestAnchorResolverTest {
    private lateinit var service: WorldAdminService

    @BeforeEach
    fun setUp() {
        service = WorldAdminService({ }, Logger.getLogger("QuestAnchorResolverTest"))
    }

    @Test
    @Throws(Exception::class)
    fun resolvesPlaceAndNodeAnchorsFromQuestObjectives() {
        service.reloadFromConfig(loadConfig("""
            world_admin:
              enabled: true
              regions:
                satul_central:
                  name: "Satul Central"
                  world: "world"
                  type: "settlement"
                  min: { x: 0, y: 50, z: 0 }
                  max: { x: 100, y: 90, z: 100 }
                  places:
                    fierarie:
                      name: "Fierarie"
                      type: "forge"
                      min: { x: 20, y: 60, z: 20 }
                      max: { x: 40, y: 75, z: 40 }
                      tags: [blacksmith, workplace]
                      nodes:
                        anvil:
                          type: "interaction"
                          x: 30
                          y: 65
                          z: 30
                          radius: 2.0
                          metadata:
                            role: "inspect"
            """), profile())

        val template = ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.QUEST)
        template.setObjectives(
            listOf(
                FeaturePackLoader.QuestEntryDefinition("visit_place", "tag:blacksmith", 1, ""),
                FeaturePackLoader.QuestEntryDefinition("inspect_node", "node:satul_central:fierarie:anvil", 1, "")
            )
        )

        val result = QuestAnchorResolver(service, listOf()).resolve(template, null, null)

        assertTrue(result.valid())
        val variables = result.toQuestVariables()
        assertEquals("2", variables["quest_anchor_count"])
        assertEquals("satul_central:fierarie", variables["anchor.visit_place:tag:blacksmith:0.id"])
        assertEquals("satul_central:fierarie:anvil", variables["anchor.inspect_node:node:satul_central:fierarie:anvil:1.id"])
    }

    @Test
    @Throws(Exception::class)
    fun usesStableYamlEntryIdsForAnchorKeys() {
        service.reloadFromConfig(loadConfig("""
            world_admin:
              enabled: true
              regions:
                satul_central:
                  name: "Satul Central"
                  world: "world"
                  type: "settlement"
                  min: { x: 0, y: 50, z: 0 }
                  max: { x: 100, y: 90, z: 100 }
                  places:
                    fierarie:
                      name: "Fierarie"
                      type: "forge"
                      min: { x: 20, y: 60, z: 20 }
                      max: { x: 40, y: 75, z: 40 }
                      tags: [blacksmith]
            """), profile())

        val template = ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.QUEST)
        template.setObjectives(
            listOf(
                FeaturePackLoader.QuestEntryDefinition(
                    "visit_place",
                    "tag:blacksmith",
                    1,
                    "",
                    mapOf("entry_id" to "visit_forge"),
                    mapOf(),
                    mapOf()
                )
            )
        )

        val result = QuestAnchorResolver(service, listOf()).resolve(template, null, null)

        assertTrue(result.valid())
        val variables = result.toQuestVariables()
        assertEquals("satul_central:fierarie", variables["anchor.visit_forge.id"])
        assertFalse(variables.containsKey("anchor.visit_place:tag:blacksmith:0.id"))
    }

    @Test
    @Throws(Exception::class)
    fun resolvesStableRegionAnchorFromTypeReference() {
        service.reloadFromConfig(loadConfig("""
            world_admin:
              enabled: true
              regions:
                satul_central:
                  name: "Satul Central"
                  world: "world"
                  type: "settlement"
                  min: { x: 0, y: 50, z: 0 }
                  max: { x: 100, y: 90, z: 100 }
                  tags: [demo, medieval]
            """), profile())

        val template = ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.QUEST)
        template.setObjectives(
            listOf(
                FeaturePackLoader.QuestEntryDefinition(
                    "visit_region",
                    "type:settlement",
                    1,
                    "",
                    mapOf("entry_id" to "patrol_region"),
                    mapOf(),
                    mapOf()
                )
            )
        )

        val result = QuestAnchorResolver(service, listOf()).resolve(template, null, null)

        assertTrue(result.valid())
        val variables = result.toQuestVariables()
        assertEquals("satul_central", variables["anchor.patrol_region.id"])
        assertFalse(variables.containsKey("anchor.visit_region:type:settlement:0.id"))
    }

    @Test
    @Throws(Exception::class)
    fun resolvesMarketContractAnchorsFromTagsAndNodeSemantic() {
        service.reloadFromConfig(loadConfig("""
            world_admin:
              enabled: true
              regions:
                demo_sat:
                  name: "Demo Village"
                  world: "world"
                  type: "settlement"
                  min: { x: 0, y: 50, z: 0 }
                  max: { x: 100, y: 90, z: 100 }
                  places:
                    piata:
                      name: "Piata"
                      type: "market"
                      min: { x: 20, y: 60, z: 20 }
                      max: { x: 40, y: 75, z: 40 }
                      tags: [market, public, social]
                      nodes:
                        quest_board:
                          type: "quest_trigger"
                          x: 30
                          y: 65
                          z: 30
                          radius: 2.0
                          metadata:
                            semantic: "quest_board"
            """), profile())

        val template = ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.TRADE_DEAL)
        template.setObjectives(
            listOf(
                FeaturePackLoader.QuestEntryDefinition(
                    "visit_place",
                    "tag:market",
                    1,
                    "",
                    mapOf("entry_id" to "visit_market"),
                    mapOf(),
                    mapOf()
                ),
                FeaturePackLoader.QuestEntryDefinition(
                    "inspect_node",
                    "quest_board",
                    1,
                    "",
                    mapOf("entry_id" to "check_market_board"),
                    mapOf(),
                    mapOf()
                )
            )
        )

        val result = QuestAnchorResolver(service, listOf()).resolve(template, null, null)

        assertTrue(result.valid())
        val variables = result.toQuestVariables()
        assertEquals("demo_sat:piata", variables["anchor.visit_market.id"])
        assertEquals("demo_sat:piata:quest_board", variables["anchor.check_market_board.id"])
    }

    @Test
    @Throws(Exception::class)
    fun resolvesBountyFarmAnchorFromPlaceTag() {
        service.reloadFromConfig(loadConfig("""
            world_admin:
              enabled: true
              regions:
                demo_sat:
                  name: "Demo Village"
                  world: "world"
                  type: "settlement"
                  min: { x: 0, y: 50, z: 0 }
                  max: { x: 100, y: 90, z: 100 }
                  places:
                    ferma:
                      name: "Ferma"
                      type: "farm"
                      min: { x: 20, y: 60, z: 20 }
                      max: { x: 40, y: 75, z: 40 }
                      tags: [farm, workplace]
                      metadata:
                        profession: "farmer"
            """), profile())

        val template = ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.BOUNTY)
        template.setObjectives(
            listOf(
                FeaturePackLoader.QuestEntryDefinition(
                    "visit_place",
                    "tag:farm",
                    1,
                    "",
                    mapOf("entry_id" to "visit_farm_edge"),
                    mapOf(),
                    mapOf()
                )
            )
        )

        val result = QuestAnchorResolver(service, listOf()).resolve(template, null, null)

        assertTrue(result.valid())
        val variables = result.toQuestVariables()
        assertEquals("demo_sat:ferma", variables["anchor.visit_farm_edge.id"])
    }

    @Test
    @Throws(Exception::class)
    fun resolvesRitualAnchorsFromTagAndNodeSemantic() {
        service.reloadFromConfig(loadConfig("""
            world_admin:
              enabled: true
              regions:
                demo_sat:
                  name: "Demo Village"
                  world: "world"
                  type: "settlement"
                  min: { x: 0, y: 50, z: 0 }
                  max: { x: 100, y: 90, z: 100 }
                  places:
                    altar:
                      name: "Altarul satului"
                      type: "custom"
                      min: { x: 20, y: 60, z: 20 }
                      max: { x: 40, y: 75, z: 40 }
                      tags: [ritual, altar, shrine, public]
                      metadata:
                        role: "ritual"
                      nodes:
                        ritual_circle:
                          type: "progression"
                          x: 30
                          y: 65
                          z: 30
                          radius: 3.0
                          metadata:
                            semantic: "ritual_circle"
            """), profile())

        val template = ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.RITUAL)
        template.setObjectives(
            listOf(
                FeaturePackLoader.QuestEntryDefinition(
                    "visit_place",
                    "tag:ritual",
                    1,
                    "",
                    mapOf("entry_id" to "visit_ritual_altar"),
                    mapOf(),
                    mapOf()
                ),
                FeaturePackLoader.QuestEntryDefinition(
                    "inspect_node",
                    "ritual_circle",
                    1,
                    "",
                    mapOf("entry_id" to "inspect_ritual_circle"),
                    mapOf(),
                    mapOf()
                )
            )
        )

        val result = QuestAnchorResolver(service, listOf()).resolve(template, null, null)

        assertTrue(result.valid())
        val variables = result.toQuestVariables()
        assertEquals("demo_sat:altar", variables["anchor.visit_ritual_altar.id"])
        assertEquals("demo_sat:altar:ritual_circle", variables["anchor.inspect_ritual_circle.id"])
    }

    @Test
    @Throws(Exception::class)
    fun reportsMissingSemanticAnchor() {
        service.reloadFromConfig(loadConfig("""
            world_admin:
              enabled: true
              regions:
                satul_central:
                  name: "Satul Central"
                  world: "world"
                  type: "settlement"
                  min: { x: 0, y: 50, z: 0 }
                  max: { x: 100, y: 90, z: 100 }
            """), profile())

        val template = ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.QUEST)
        template.setObjectives(
            listOf(
                FeaturePackLoader.QuestEntryDefinition("visit_place", "tag:blacksmith", 1, "")
            )
        )

        val result = QuestAnchorResolver(service, listOf()).resolve(template, null, null)

        assertFalse(result.valid())
        assertEquals(1, result.issues().size)
        assertEquals("place", result.issues()[0].anchorType())
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
