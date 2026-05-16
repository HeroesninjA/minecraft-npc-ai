package ro.ainpc.world;

import ro.ainpc.AINPCPlugin;
import ro.ainpc.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class NpcWorldBindingService {

    @FunctionalInterface
    interface StatementProvider {
        PreparedStatement prepareStatement(String sql) throws SQLException;
    }

    private final StatementProvider statements;
    private final Logger logger;

    public NpcWorldBindingService(AINPCPlugin plugin) {
        this(plugin != null ? plugin.getDatabaseManager() : null, plugin != null ? plugin.getLogger() : null);
    }

    public NpcWorldBindingService(DatabaseManager databaseManager, Logger logger) {
        this(databaseManager != null ? databaseManager::prepareStatement : null, logger);
    }

    NpcWorldBindingService(StatementProvider statements, Logger logger) {
        this.statements = statements;
        this.logger = logger != null ? logger : Logger.getLogger(NpcWorldBindingService.class.getName());
    }

    public NpcWorldBinding saveBinding(NpcWorldBinding binding) throws SQLException {
        NpcWorldBinding normalized = requireValid(binding);
        long now = System.currentTimeMillis();
        long createdAt = normalized.createdAt() > 0L ? normalized.createdAt() : now;
        String sql = """
            INSERT INTO npc_world_bindings (
                npc_id, npc_uuid, npc_name,
                home_place_id, work_place_id, social_place_id,
                home_node_id, work_node_id, social_node_id,
                family_id, source, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(npc_id) DO UPDATE SET
                npc_uuid = excluded.npc_uuid,
                npc_name = excluded.npc_name,
                home_place_id = excluded.home_place_id,
                work_place_id = excluded.work_place_id,
                social_place_id = excluded.social_place_id,
                home_node_id = excluded.home_node_id,
                work_node_id = excluded.work_node_id,
                social_node_id = excluded.social_node_id,
                family_id = excluded.family_id,
                source = excluded.source,
                updated_at = excluded.updated_at
        """;

        try (PreparedStatement statement = requireStatements().prepareStatement(sql)) {
            statement.setInt(1, normalized.npcId());
            statement.setString(2, normalized.npcUuid());
            statement.setString(3, normalized.npcName());
            statement.setString(4, normalized.homePlaceId());
            statement.setString(5, normalized.workPlaceId());
            statement.setString(6, normalized.socialPlaceId());
            statement.setString(7, normalized.homeNodeId());
            statement.setString(8, normalized.workNodeId());
            statement.setString(9, normalized.socialNodeId());
            statement.setString(10, normalized.familyId());
            statement.setString(11, normalized.source());
            statement.setLong(12, createdAt);
            statement.setLong(13, now);
            statement.executeUpdate();
        }

        return new NpcWorldBinding(
            normalized.npcId(),
            normalized.npcUuid(),
            normalized.npcName(),
            normalized.homePlaceId(),
            normalized.workPlaceId(),
            normalized.socialPlaceId(),
            normalized.homeNodeId(),
            normalized.workNodeId(),
            normalized.socialNodeId(),
            normalized.familyId(),
            normalized.source(),
            createdAt,
            now
        );
    }

    public Optional<NpcWorldBinding> getBinding(int npcId) throws SQLException {
        if (npcId <= 0) {
            return Optional.empty();
        }

        String sql = """
            SELECT npc_id, npc_uuid, npc_name,
                   home_place_id, work_place_id, social_place_id,
                   home_node_id, work_node_id, social_node_id,
                   family_id, source, created_at, updated_at
            FROM npc_world_bindings
            WHERE npc_id = ?
        """;

        try (PreparedStatement statement = requireStatements().prepareStatement(sql)) {
            statement.setInt(1, npcId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readBinding(resultSet)) : Optional.empty();
            }
        }
    }

    public List<NpcWorldBinding> listBindings(int limit) throws SQLException {
        int safeLimit = Math.max(1, limit);
        String sql = """
            SELECT npc_id, npc_uuid, npc_name,
                   home_place_id, work_place_id, social_place_id,
                   home_node_id, work_node_id, social_node_id,
                   family_id, source, created_at, updated_at
            FROM npc_world_bindings
            ORDER BY updated_at DESC, npc_id ASC
            LIMIT ?
        """;

        List<NpcWorldBinding> bindings = new ArrayList<>();
        try (PreparedStatement statement = requireStatements().prepareStatement(sql)) {
            statement.setInt(1, safeLimit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    bindings.add(readBinding(resultSet));
                }
            }
        }
        return bindings;
    }

    public int countBindings() throws SQLException {
        try (PreparedStatement statement = requireStatements()
                 .prepareStatement("SELECT COUNT(*) FROM npc_world_bindings");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    public boolean deleteBinding(int npcId) throws SQLException {
        if (npcId <= 0) {
            return false;
        }

        try (PreparedStatement statement = requireStatements()
                 .prepareStatement("DELETE FROM npc_world_bindings WHERE npc_id = ?")) {
            statement.setInt(1, npcId);
            return statement.executeUpdate() > 0;
        }
    }

    private NpcWorldBinding requireValid(NpcWorldBinding binding) {
        if (binding == null) {
            throw new IllegalArgumentException("Binding-ul NPC-world nu poate fi null.");
        }
        if (binding.npcId() <= 0) {
            throw new IllegalArgumentException("Binding-ul NPC-world are npc_id invalid: " + binding.npcId());
        }
        if (!binding.hasAnyPlaceBinding()) {
            logger.fine("Salvez binding NPC-world fara place IDs pentru npc_id=" + binding.npcId());
        }
        return binding;
    }

    private NpcWorldBinding readBinding(ResultSet resultSet) throws SQLException {
        return new NpcWorldBinding(
            resultSet.getInt("npc_id"),
            text(resultSet, "npc_uuid"),
            text(resultSet, "npc_name"),
            text(resultSet, "home_place_id"),
            text(resultSet, "work_place_id"),
            text(resultSet, "social_place_id"),
            text(resultSet, "home_node_id"),
            text(resultSet, "work_node_id"),
            text(resultSet, "social_node_id"),
            text(resultSet, "family_id"),
            text(resultSet, "source"),
            resultSet.getLong("created_at"),
            resultSet.getLong("updated_at")
        );
    }

    private StatementProvider requireStatements() throws SQLException {
        if (statements == null) {
            throw new SQLException("NpcWorldBindingService nu are acces la baza de date.");
        }
        return statements;
    }

    private static String text(ResultSet resultSet, String column) throws SQLException {
        String value = resultSet.getString(column);
        return value == null ? "" : value;
    }
}
