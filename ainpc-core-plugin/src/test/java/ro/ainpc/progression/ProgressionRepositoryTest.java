package ro.ainpc.progression;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionRepositoryTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE player_quests (
                    player_uuid TEXT NOT NULL,
                    template_id TEXT NOT NULL,
                    quest_code TEXT NOT NULL DEFAULT '',
                    status TEXT NOT NULL,
                    started_at INTEGER,
                    completed_at INTEGER,
                    current_phase TEXT NOT NULL DEFAULT '',
                    current_stage_id TEXT NOT NULL DEFAULT '',
                    objective_progress TEXT NOT NULL DEFAULT '{}',
                    quest_variables TEXT NOT NULL DEFAULT '{}',
                    updated_at INTEGER NOT NULL,
                    tracked INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (player_uuid, template_id)
                )
            """);
            statement.execute("""
                CREATE TABLE quest_anchor_bindings (
                    player_uuid TEXT NOT NULL,
                    template_id TEXT NOT NULL,
                    objective_key TEXT NOT NULL,
                    quest_code TEXT NOT NULL DEFAULT '',
                    objective_type TEXT NOT NULL,
                    reference TEXT NOT NULL DEFAULT '',
                    anchor_type TEXT NOT NULL,
                    anchor_id TEXT NOT NULL,
                    anchor_label TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY (player_uuid, template_id, objective_key)
                )
            """);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void readsStoredProgressionsWithResolvedDefinitionMetadata() throws Exception {
        insertProgression(
            "player-1",
            "medieval:C01",
            "C01",
            "active",
            "RETURN",
            "RETURN",
            "{\"return_goods\":1}",
            "{\"npc_id\":42}",
            100L,
            0L,
            150L,
            true
        );
        ProgressionRepository repository = new ProgressionRepository(
            connection::prepareStatement,
            () -> List.of(contractDefinition())
        );

        List<StoredProgression> rows = repository.findAll();

        assertEquals(1, rows.size());
        StoredProgression progression = rows.get(0);
        assertEquals("player-1", progression.playerUuid());
        assertEquals("medieval:village_contracts:C01", progression.progressionId());
        assertEquals("village_contracts", progression.mechanicId());
        assertEquals("contract", progression.kind());
        assertEquals("side", progression.category());
        assertEquals("trade", progression.scenarioKind());
        assertEquals("TRADE_DEAL", progression.baseType());
        assertEquals("C01", progression.definitionId());
        assertEquals("{\"return_goods\":1}", progression.objectiveProgressJson());
        assertTrue(progression.definitionResolved());
        assertTrue(progression.current());
        assertTrue(progression.tracked());
    }

    @Test
    void usesStableFallbackMetadataWhenDefinitionIsMissing() throws Exception {
        insertProgression(
            "player-2",
            "unknown_pack:missing_entry",
            "",
            "completed",
            "",
            "",
            "{}",
            "{}",
            100L,
            120L,
            130L,
            false
        );
        ProgressionRepository repository = new ProgressionRepository(
            connection::prepareStatement,
            List::of
        );

        StoredProgression progression = repository.findAll().get(0);

        assertEquals("unknown_pack:missing_entry", progression.progressionId());
        assertEquals("missing_entry", progression.definitionId());
        assertFalse(progression.definitionResolved());
        assertTrue(progression.archived());
        assertFalse(progression.current());
    }

    @Test
    void filtersStoredProgressionsByPlayerStatusAndMechanic() throws Exception {
        insertProgression(
            "player-1",
            "medieval:C01",
            "C01",
            "active",
            "RETURN",
            "RETURN",
            "{}",
            "{}",
            100L,
            0L,
            150L,
            true
        );
        insertProgression(
            "player-2",
            "medieval:C01",
            "C01",
            "completed",
            "RETURN",
            "RETURN",
            "{}",
            "{}",
            80L,
            120L,
            130L,
            false
        );
        ProgressionRepository repository = new ProgressionRepository(
            connection::prepareStatement,
            () -> List.of(contractDefinition())
        );

        assertEquals(1, repository.find("player-1", "active", 10).size());
        assertEquals(2, repository.find("", "village_contracts", 10).size());
        assertEquals(2, repository.find("", "trade", 10).size());
        assertEquals(2, repository.find("", "TRADE_DEAL", 10).size());
        assertEquals(2, repository.find("", "mechanic:village_contracts", 10).size());
        assertEquals(2, repository.find("", "scenario:trade", 10).size());
        assertEquals(2, repository.find("", "base:trade-deal", 10).size());
        assertEquals(2, repository.find("", "category:side", 10).size());
        assertEquals(1, repository.find("", "status:active", 10).size());
        assertEquals(1, repository.find("", "tracked", 10).size());
        assertEquals(1, repository.find("", "tracked:true", 10).size());
        assertEquals(1, repository.find("", "contract", 1).size());
    }

    @Test
    void summarizesStoredProgressions() throws Exception {
        insertProgression(
            "player-1",
            "medieval:C01",
            "C01",
            "active",
            "RETURN",
            "RETURN",
            "{}",
            "{}",
            100L,
            0L,
            150L,
            true
        );
        insertProgression(
            "player-2",
            "unknown_pack:missing_entry",
            "",
            "completed",
            "",
            "",
            "{}",
            "{}",
            100L,
            120L,
            130L,
            false
        );
        ProgressionRepository repository = new ProgressionRepository(
            connection::prepareStatement,
            () -> List.of(contractDefinition())
        );

        StoredProgressionSummary summary = repository.summarize("", "all");

        assertEquals(2, summary.rowCount());
        assertEquals(2, summary.playerCount());
        assertEquals(1, summary.currentCount());
        assertEquals(1, summary.archivedCount());
        assertEquals(1, summary.trackedCount());
        assertEquals(1, summary.unresolvedDefinitionCount());
        assertEquals(1, summary.byTemplate().get("medieval:C01"));
        assertEquals(1, summary.byMechanic().get("village_contracts"));
        assertEquals(1, summary.byMechanic().get("unknown"));
        assertEquals(1, summary.byCategory().get("side"));
        assertEquals(1, summary.byScenarioKind().get("trade"));
        assertEquals(1, summary.byBaseType().get("TRADE_DEAL"));
    }

    @Test
    void readsAnchorBindingsByAnchorAndPlayer() throws Exception {
        insertProgression(
            "player-1",
            "medieval:C01",
            "C01",
            "active",
            "RETURN",
            "RETURN",
            "{}",
            "{}",
            100L,
            0L,
            150L,
            true
        );
        insertProgression(
            "player-2",
            "medieval:C01",
            "C01",
            "offered",
            "",
            "",
            "{}",
            "{}",
            100L,
            0L,
            140L,
            false
        );
        insertAnchorBinding("player-1", "medieval:C01", "visit_market", "C01",
            "visit_place", "tag:market", "place", "market_square", "Piata", 100L, 160L);
        insertAnchorBinding("player-1", "medieval:C01", "inspect_board", "C01",
            "inspect_node", "node:quest_board", "node", "quest_board", "Avizier", 100L, 155L);
        insertAnchorBinding("player-2", "medieval:C01", "visit_market", "C01",
            "visit_place", "tag:market", "place", "market_square", "Piata", 100L, 145L);
        ProgressionRepository repository = new ProgressionRepository(
            connection::prepareStatement,
            () -> List.of(contractDefinition())
        );

        List<ProgressionAnchorBinding> playerRows =
            repository.findAnchorBindingsForAnchor("player-1", "place", "market_square", 10);
        List<ProgressionAnchorBinding> allRows =
            repository.findAnchorBindingsForAnchor("", "place", "market_square", 10);

        assertEquals(1, playerRows.size());
        ProgressionAnchorBinding row = playerRows.get(0);
        assertEquals("visit_market", row.objectiveKey());
        assertEquals("active", row.status());
        assertEquals("place:market_square", row.anchorSelector());
        assertTrue(row.matchesAnchor("PLACE", "market_square"));
        assertEquals("Piata", row.displayLabel());
        assertEquals(2, allRows.size());
    }

    private void insertProgression(String playerUuid,
                                   String templateId,
                                   String questCode,
                                   String status,
                                   String currentPhase,
                                   String currentStageId,
                                   String objectiveProgress,
                                   String questVariables,
                                   long startedAt,
                                   long completedAt,
                                   long updatedAt,
                                   boolean tracked) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO player_quests (
                player_uuid, template_id, quest_code, status, started_at, completed_at,
                current_phase, current_stage_id, objective_progress, quest_variables, updated_at, tracked
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """)) {
            statement.setString(1, playerUuid);
            statement.setString(2, templateId);
            statement.setString(3, questCode);
            statement.setString(4, status);
            statement.setLong(5, startedAt);
            statement.setLong(6, completedAt);
            statement.setString(7, currentPhase);
            statement.setString(8, currentStageId);
            statement.setString(9, objectiveProgress);
            statement.setString(10, questVariables);
            statement.setLong(11, updatedAt);
            statement.setInt(12, tracked ? 1 : 0);
            statement.executeUpdate();
        }
    }

    private void insertAnchorBinding(String playerUuid,
                                     String templateId,
                                     String objectiveKey,
                                     String questCode,
                                     String objectiveType,
                                     String reference,
                                     String anchorType,
                                     String anchorId,
                                     String anchorLabel,
                                     long createdAt,
                                     long updatedAt) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO quest_anchor_bindings (
                player_uuid, template_id, objective_key, quest_code, objective_type,
                reference, anchor_type, anchor_id, anchor_label, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """)) {
            statement.setString(1, playerUuid);
            statement.setString(2, templateId);
            statement.setString(3, objectiveKey);
            statement.setString(4, questCode);
            statement.setString(5, objectiveType);
            statement.setString(6, reference);
            statement.setString(7, anchorType);
            statement.setString(8, anchorId);
            statement.setString(9, anchorLabel);
            statement.setLong(10, createdAt);
            statement.setLong(11, updatedAt);
            statement.executeUpdate();
        }
    }

    private ProgressionDefinition contractDefinition() {
        return new ProgressionDefinition(
            "medieval:village_contracts:C01",
            "medieval",
            "village_contracts",
            "contract",
            "C01",
            "medieval:C01",
            "C01",
            "Hartie pentru negustor",
            "",
            "side",
            "trade",
            "TRADE_DEAL",
            "Contracte de sat",
            "contract",
            "contracte",
            3,
            1,
            1,
            1,
            false,
            true
        );
    }
}
