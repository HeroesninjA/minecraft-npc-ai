package ro.ainpc.world;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.ainpc.engine.FeaturePackLoader;
import ro.ainpc.engine.QuestAnchorResolver;
import ro.ainpc.engine.ScenarioEngine;
import ro.ainpc.platform.PlatformProfile;
import ro.ainpc.platform.RuntimeMode;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestAnchorResolverTest {

    private WorldAdminService service;

    @BeforeEach
    void setUp() {
        service = new WorldAdminService(message -> { }, Logger.getLogger("QuestAnchorResolverTest"));
    }

    @Test
    void resolvesPlaceAndNodeAnchorsFromQuestObjectives() throws Exception {
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
            """), profile());

        ScenarioEngine.ScenarioTemplate template = new ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.QUEST);
        template.setObjectives(List.of(
            new FeaturePackLoader.QuestEntryDefinition("visit_place", "tag:blacksmith", 1, ""),
            new FeaturePackLoader.QuestEntryDefinition("inspect_node", "node:satul_central:fierarie:anvil", 1, "")
        ));

        QuestAnchorResolver.ResolvedQuestAnchors result = new QuestAnchorResolver(service, List.of())
            .resolve(template, null, null);

        assertTrue(result.valid());
        Map<String, String> variables = result.toQuestVariables();
        assertEquals("2", variables.get("quest_anchor_count"));
        assertEquals("satul_central:fierarie", variables.get("anchor.visit_place:tag:blacksmith:0.id"));
        assertEquals("satul_central:fierarie:anvil", variables.get("anchor.inspect_node:node:satul_central:fierarie:anvil:1.id"));
    }

    @Test
    void usesStableYamlEntryIdsForAnchorKeys() throws Exception {
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
            """), profile());

        ScenarioEngine.ScenarioTemplate template = new ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.QUEST);
        template.setObjectives(List.of(new FeaturePackLoader.QuestEntryDefinition(
            "visit_place",
            "tag:blacksmith",
            1,
            "",
            Map.of("entry_id", "visit_forge"),
            Map.of(),
            Map.of()
        )));

        QuestAnchorResolver.ResolvedQuestAnchors result = new QuestAnchorResolver(service, List.of())
            .resolve(template, null, null);

        assertTrue(result.valid());
        Map<String, String> variables = result.toQuestVariables();
        assertEquals("satul_central:fierarie", variables.get("anchor.visit_forge.id"));
        assertFalse(variables.containsKey("anchor.visit_place:tag:blacksmith:0.id"));
    }

    @Test
    void resolvesStableRegionAnchorFromTypeReference() throws Exception {
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
            """), profile());

        ScenarioEngine.ScenarioTemplate template = new ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.QUEST);
        template.setObjectives(List.of(new FeaturePackLoader.QuestEntryDefinition(
            "visit_region",
            "type:settlement",
            1,
            "",
            Map.of("entry_id", "patrol_region"),
            Map.of(),
            Map.of()
        )));

        QuestAnchorResolver.ResolvedQuestAnchors result = new QuestAnchorResolver(service, List.of())
            .resolve(template, null, null);

        assertTrue(result.valid());
        Map<String, String> variables = result.toQuestVariables();
        assertEquals("satul_central", variables.get("anchor.patrol_region.id"));
        assertFalse(variables.containsKey("anchor.visit_region:type:settlement:0.id"));
    }

    @Test
    void resolvesMarketContractAnchorsFromTagsAndNodeSemantic() throws Exception {
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
            """), profile());

        ScenarioEngine.ScenarioTemplate template = new ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.TRADE_DEAL);
        template.setObjectives(List.of(
            new FeaturePackLoader.QuestEntryDefinition(
                "visit_place",
                "tag:market",
                1,
                "",
                Map.of("entry_id", "visit_market"),
                Map.of(),
                Map.of()
            ),
            new FeaturePackLoader.QuestEntryDefinition(
                "inspect_node",
                "quest_board",
                1,
                "",
                Map.of("entry_id", "check_market_board"),
                Map.of(),
                Map.of()
            )
        ));

        QuestAnchorResolver.ResolvedQuestAnchors result = new QuestAnchorResolver(service, List.of())
            .resolve(template, null, null);

        assertTrue(result.valid());
        Map<String, String> variables = result.toQuestVariables();
        assertEquals("demo_sat:piata", variables.get("anchor.visit_market.id"));
        assertEquals("demo_sat:piata:quest_board", variables.get("anchor.check_market_board.id"));
    }

    @Test
    void resolvesBountyFarmAnchorFromPlaceTag() throws Exception {
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
            """), profile());

        ScenarioEngine.ScenarioTemplate template = new ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.BOUNTY);
        template.setObjectives(List.of(new FeaturePackLoader.QuestEntryDefinition(
            "visit_place",
            "tag:farm",
            1,
            "",
            Map.of("entry_id", "visit_farm_edge"),
            Map.of(),
            Map.of()
        )));

        QuestAnchorResolver.ResolvedQuestAnchors result = new QuestAnchorResolver(service, List.of())
            .resolve(template, null, null);

        assertTrue(result.valid());
        Map<String, String> variables = result.toQuestVariables();
        assertEquals("demo_sat:ferma", variables.get("anchor.visit_farm_edge.id"));
    }

    @Test
    void resolvesRitualAnchorsFromTagAndNodeSemantic() throws Exception {
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
            """), profile());

        ScenarioEngine.ScenarioTemplate template = new ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.RITUAL);
        template.setObjectives(List.of(
            new FeaturePackLoader.QuestEntryDefinition(
                "visit_place",
                "tag:ritual",
                1,
                "",
                Map.of("entry_id", "visit_ritual_altar"),
                Map.of(),
                Map.of()
            ),
            new FeaturePackLoader.QuestEntryDefinition(
                "inspect_node",
                "ritual_circle",
                1,
                "",
                Map.of("entry_id", "inspect_ritual_circle"),
                Map.of(),
                Map.of()
            )
        ));

        QuestAnchorResolver.ResolvedQuestAnchors result = new QuestAnchorResolver(service, List.of())
            .resolve(template, null, null);

        assertTrue(result.valid());
        Map<String, String> variables = result.toQuestVariables();
        assertEquals("demo_sat:altar", variables.get("anchor.visit_ritual_altar.id"));
        assertEquals("demo_sat:altar:ritual_circle", variables.get("anchor.inspect_ritual_circle.id"));
    }

    @Test
    void reportsMissingSemanticAnchor() throws Exception {
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
            """), profile());

        ScenarioEngine.ScenarioTemplate template = new ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.QUEST);
        template.setObjectives(List.of(
            new FeaturePackLoader.QuestEntryDefinition("visit_place", "tag:blacksmith", 1, "")
        ));

        QuestAnchorResolver.ResolvedQuestAnchors result = new QuestAnchorResolver(service, List.of())
            .resolve(template, null, null);

        assertFalse(result.valid());
        assertEquals(1, result.issues().size());
        assertEquals("place", result.issues().get(0).anchorType());
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
