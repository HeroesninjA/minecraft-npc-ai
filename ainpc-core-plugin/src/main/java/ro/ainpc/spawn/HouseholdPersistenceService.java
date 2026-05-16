package ro.ainpc.spawn;

import ro.ainpc.AINPCPlugin;
import ro.ainpc.database.DatabaseManager;
import ro.ainpc.npc.AINPC;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Logger;

public class HouseholdPersistenceService {

    @FunctionalInterface
    interface StatementProvider {
        PreparedStatement prepareStatement(String sql) throws SQLException;
    }

    private final StatementProvider statements;
    private final Logger logger;

    public HouseholdPersistenceService(AINPCPlugin plugin) {
        this(plugin != null ? plugin.getDatabaseManager() : null, plugin != null ? plugin.getLogger() : null);
    }

    public HouseholdPersistenceService(DatabaseManager databaseManager, Logger logger) {
        this(databaseManager != null ? databaseManager::prepareStatement : null, logger);
    }

    HouseholdPersistenceService(StatementProvider statements, Logger logger) {
        this.statements = statements;
        this.logger = logger != null ? logger : Logger.getLogger(HouseholdPersistenceService.class.getName());
    }

    public int saveHousehold(HouseAllocation allocation,
                             List<NpcSpawnPlan> plans,
                             List<NpcSpawnResult> spawnResults,
                             String source) throws SQLException {
        HouseAllocation safeAllocation = requireValid(allocation);
        List<NpcSpawnPlan> safePlans = List.copyOf(plans != null ? plans : List.of());
        List<NpcSpawnResult> safeResults = List.copyOf(spawnResults != null ? spawnResults : List.of());
        String householdId = safeAllocation.householdId();
        long now = System.currentTimeMillis();

        saveHouseholdRow(safeAllocation, householdId, safePlans, source, now);
        List<String> activeResidentKeys = new ArrayList<>();
        int savedResidents = 0;
        int limit = Math.min(safePlans.size(), safeResults.size());

        for (int index = 0; index < limit; index++) {
            NpcSpawnPlan plan = safePlans.get(index);
            NpcSpawnResult result = safeResults.get(index);
            if (result == null || !result.success() || result.npc() == null) {
                continue;
            }

            HouseAllocation.ResidentPlan residentPlan = findResidentPlan(safeAllocation, plan);
            if (residentPlan == null) {
                logger.fine("Sar peste resident fara ResidentPlan pentru " + plan.npcKey());
                continue;
            }

            String residentKey = normalizeResidentKey(residentPlan.npcKey());
            if (residentKey.isBlank()) {
                continue;
            }

            saveResidentRow(householdId, residentKey, safeAllocation, residentPlan, plan, result.npc(), source, now);
            activeResidentKeys.add(residentKey);
            savedResidents++;
        }

        deleteStaleResidents(householdId, activeResidentKeys);
        updateResidentCount(householdId, savedResidents);
        return savedResidents;
    }

    public List<HouseholdResidentRecord> listResidents(String householdId) throws SQLException {
        if (householdId == null || householdId.isBlank()) {
            return List.of();
        }

        String sql = """
            SELECT household_id, resident_key, npc_id, npc_uuid, npc_name, source_key,
                   relation_role, home_place_id, spawn_node_id, home_node_id,
                   work_place_id, work_node_id, social_place_id, social_node_id,
                   status, created_at, updated_at
            FROM household_residents
            WHERE household_id = ?
            ORDER BY resident_key ASC
        """;
        List<HouseholdResidentRecord> residents = new ArrayList<>();
        try (PreparedStatement statement = requireStatements().prepareStatement(sql)) {
            statement.setString(1, householdId.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    residents.add(readResident(resultSet));
                }
            }
        }
        return residents;
    }

    public List<HouseholdRecord> listHouseholds(int limit) throws SQLException {
        int safeLimit = Math.max(1, Math.min(100, limit));
        String sql = """
            SELECT household_id, family_id, home_place_id, primary_owner_key,
                   max_residents, resident_count, plan_hash, source, created_at, updated_at
            FROM households
            ORDER BY updated_at DESC, household_id ASC
            LIMIT ?
        """;
        List<HouseholdRecord> households = new ArrayList<>();
        try (PreparedStatement statement = requireStatements().prepareStatement(sql)) {
            statement.setInt(1, safeLimit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    households.add(readHousehold(resultSet));
                }
            }
        }
        return households;
    }

    public Optional<HouseholdRecord> getHousehold(String householdId) throws SQLException {
        if (householdId == null || householdId.isBlank()) {
            return Optional.empty();
        }
        String sql = """
            SELECT household_id, family_id, home_place_id, primary_owner_key,
                   max_residents, resident_count, plan_hash, source, created_at, updated_at
            FROM households
            WHERE household_id = ?
            LIMIT 1
        """;
        try (PreparedStatement statement = requireStatements().prepareStatement(sql)) {
            statement.setString(1, householdId.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readHousehold(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<HouseholdRecord> findHouseholdByHomePlace(String homePlaceId) throws SQLException {
        if (homePlaceId == null || homePlaceId.isBlank()) {
            return Optional.empty();
        }
        String sql = """
            SELECT household_id, family_id, home_place_id, primary_owner_key,
                   max_residents, resident_count, plan_hash, source, created_at, updated_at
            FROM households
            WHERE home_place_id = ?
            ORDER BY updated_at DESC, household_id ASC
            LIMIT 1
        """;
        try (PreparedStatement statement = requireStatements().prepareStatement(sql)) {
            statement.setString(1, homePlaceId.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readHousehold(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<HouseholdResidentRecord> findResidentByNpcId(int npcId) throws SQLException {
        if (npcId <= 0) {
            return Optional.empty();
        }
        String sql = """
            SELECT household_id, resident_key, npc_id, npc_uuid, npc_name, source_key,
                   relation_role, home_place_id, spawn_node_id, home_node_id,
                   work_place_id, work_node_id, social_place_id, social_node_id,
                   status, created_at, updated_at
            FROM household_residents
            WHERE npc_id = ?
            ORDER BY updated_at DESC, household_id ASC, resident_key ASC
            LIMIT 1
        """;
        try (PreparedStatement statement = requireStatements().prepareStatement(sql)) {
            statement.setInt(1, npcId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readResident(resultSet)) : Optional.empty();
            }
        }
    }

    public HouseholdBackfillReport backfillFromNpcWorldBindings(boolean apply, int limit) throws SQLException {
        int safeLimit = Math.max(1, Math.min(1000, limit));
        List<BindingBackfillRow> rows = loadBindingBackfillRows(safeLimit);
        HouseholdBackfillAccumulator report = new HouseholdBackfillAccumulator(apply, rows.size());
        if (rows.isEmpty()) {
            report.warning("Nu exista npc_world_bindings cu home_place_id pentru backfill household.");
            return report.toReport();
        }

        Map<String, List<BindingBackfillRow>> rowsByHousehold = new LinkedHashMap<>();
        for (BindingBackfillRow row : rows) {
            String householdId = backfillHouseholdId(row.familyId(), row.homePlaceId());
            if (householdId.isBlank()) {
                report.warning("Sar peste npc_id=" + row.npcId()
                    + ": nu pot deriva household_id din family_id/home_place_id.");
                continue;
            }
            rowsByHousehold.computeIfAbsent(householdId, ignored -> new ArrayList<>()).add(row);
        }
        report.candidateHouseholds = rowsByHousehold.size();

        for (Map.Entry<String, List<BindingBackfillRow>> entry : rowsByHousehold.entrySet()) {
            backfillHouseholdCandidate(entry.getKey(), entry.getValue(), apply, report);
        }

        return report.toReport();
    }

    public HouseholdBackfillReport backfillFromMetadataResidents(boolean apply,
                                                                 int limit,
                                                                 List<MetadataResidentBackfillInput> inputs)
        throws SQLException {
        int safeLimit = Math.max(1, Math.min(1000, limit));
        List<MetadataResidentBackfillInput> safeInputs = List.copyOf(inputs != null ? inputs : List.of())
            .stream()
            .filter(input -> input.npcId() > 0 && !input.homePlaceId().isBlank())
            .limit(safeLimit)
            .toList();
        HouseholdBackfillAccumulator report = new HouseholdBackfillAccumulator(apply, safeInputs.size());
        if (safeInputs.isEmpty()) {
            report.warning("Nu exista metadata resident_npc_ids valida pentru backfill household.");
            return report.toReport();
        }

        Map<String, BindingBackfillRow> rowsByKey = new LinkedHashMap<>();
        for (MetadataResidentBackfillInput input : safeInputs) {
            Optional<BindingBackfillRow> row = loadMetadataBackfillRow(input);
            if (row.isEmpty()) {
                report.warning("Sar peste metadata resident npc_id=" + input.npcId()
                    + " pentru " + input.homePlaceId() + ": NPC-ul nu exista in DB.");
                continue;
            }
            String uniqueKey = row.get().homePlaceId() + ":" + row.get().npcId();
            rowsByKey.putIfAbsent(uniqueKey, row.get());
        }

        Map<String, List<BindingBackfillRow>> rowsByHousehold = new LinkedHashMap<>();
        for (BindingBackfillRow row : rowsByKey.values()) {
            String householdId = backfillHouseholdId(row.familyId(), row.homePlaceId());
            if (householdId.isBlank()) {
                report.warning("Sar peste metadata resident npc_id=" + row.npcId()
                    + ": nu pot deriva household_id din family_id/home_place_id.");
                continue;
            }
            rowsByHousehold.computeIfAbsent(householdId, ignored -> new ArrayList<>()).add(row);
        }
        report.candidateHouseholds = rowsByHousehold.size();

        for (Map.Entry<String, List<BindingBackfillRow>> entry : rowsByHousehold.entrySet()) {
            backfillHouseholdCandidate(entry.getKey(), entry.getValue(), apply, report);
        }

        return report.toReport();
    }

    public int countHouseholds() throws SQLException {
        try (PreparedStatement statement = requireStatements().prepareStatement("SELECT COUNT(*) FROM households");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    public int countResidents() throws SQLException {
        try (PreparedStatement statement = requireStatements().prepareStatement("SELECT COUNT(*) FROM household_residents");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    public HouseholdResidentRepairReport repairDuplicateResidents(boolean apply, int limit) throws SQLException {
        int safeLimit = Math.max(1, Math.min(1000, limit));
        HouseholdResidentRepairAccumulator report = new HouseholdResidentRepairAccumulator(apply);
        Set<String> deleteKeys = new LinkedHashSet<>();
        Set<String> affectedHouseholds = new LinkedHashSet<>();

        for (Integer npcId : loadDuplicateResidentNpcIds(safeLimit)) {
            List<RepairResidentRow> rows = loadRepairResidentRowsForNpc(npcId);
            if (rows.size() <= 1) {
                continue;
            }
            report.duplicateNpcGroups++;
            repairDuplicateResidentGroup("npc_id=" + npcId, rows, deleteKeys, affectedHouseholds, report);
        }

        for (String sourceKey : loadDuplicateResidentSourceKeys(safeLimit)) {
            List<RepairResidentRow> rows = loadRepairResidentRowsForSourceKey(sourceKey).stream()
                .filter(row -> !deleteKeys.contains(row.identityKey()))
                .toList();
            if (rows.size() <= 1) {
                continue;
            }
            report.duplicateSourceKeyGroups++;
            repairDuplicateResidentGroup("source_key=" + sourceKey, rows, deleteKeys, affectedHouseholds, report);
        }

        if (apply) {
            for (String householdId : affectedHouseholds) {
                updateResidentCount(householdId, 0);
                report.updatedHouseholds++;
            }
        }

        return report.toReport();
    }

    public int recalculateResidentCounts(Collection<String> householdIds) throws SQLException {
        requireStatements();
        Set<String> safeHouseholdIds = new LinkedHashSet<>();
        if (householdIds != null) {
            for (String householdId : householdIds) {
                String safeHouseholdId = clean(householdId);
                if (!safeHouseholdId.isBlank()) {
                    safeHouseholdIds.add(safeHouseholdId);
                }
            }
        }

        int updatedHouseholds = 0;
        for (String householdId : safeHouseholdIds) {
            updatedHouseholds += updateResidentCount(householdId, 0);
        }
        return updatedHouseholds;
    }

    private void repairDuplicateResidentGroup(String groupLabel,
                                              List<RepairResidentRow> rows,
                                              Set<String> deleteKeys,
                                              Set<String> affectedHouseholds,
                                              HouseholdResidentRepairAccumulator report) throws SQLException {
        List<RepairResidentRow> safeRows = rows.stream()
            .sorted(RepairResidentRow::compareCanonical)
            .toList();
        RepairResidentRow canonical = safeRows.get(0);
        report.action("Pastrez resident canonic pentru " + groupLabel + ": " + canonical.label() + ".");

        for (int index = 1; index < safeRows.size(); index++) {
            RepairResidentRow duplicate = safeRows.get(index);
            if (!deleteKeys.add(duplicate.identityKey())) {
                continue;
            }
            report.duplicateResidentRows++;
            affectedHouseholds.add(duplicate.householdId());
            affectedHouseholds.add(canonical.householdId());
            if (report.apply) {
                if (deleteRepairResidentRow(duplicate)) {
                    report.deletedResidentRows++;
                    report.action("Am sters resident duplicat " + duplicate.label()
                        + "; canonic=" + canonical.label() + ".");
                } else {
                    report.warning("Nu am gasit pentru stergere residentul duplicat " + duplicate.label() + ".");
                }
            } else {
                report.action("As sterge resident duplicat " + duplicate.label()
                    + "; canonic=" + canonical.label() + ".");
            }
        }
    }

    private List<Integer> loadDuplicateResidentNpcIds(int limit) throws SQLException {
        String sql = """
            SELECT npc_id
            FROM household_residents
            WHERE npc_id > 0
            GROUP BY npc_id
            HAVING COUNT(*) > 1
            ORDER BY npc_id ASC
            LIMIT ?
        """;
        List<Integer> npcIds = new ArrayList<>();
        try (PreparedStatement statement = requireStatements().prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    npcIds.add(resultSet.getInt("npc_id"));
                }
            }
        }
        return npcIds;
    }

    private List<String> loadDuplicateResidentSourceKeys(int limit) throws SQLException {
        String sql = """
            SELECT source_key
            FROM household_residents
            WHERE TRIM(COALESCE(source_key, '')) <> ''
            GROUP BY source_key
            HAVING COUNT(*) > 1
            ORDER BY source_key ASC
            LIMIT ?
        """;
        List<String> sourceKeys = new ArrayList<>();
        try (PreparedStatement statement = requireStatements().prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    sourceKeys.add(text(resultSet, "source_key"));
                }
            }
        }
        return sourceKeys;
    }

    private List<RepairResidentRow> loadRepairResidentRowsForNpc(int npcId) throws SQLException {
        String sql = repairResidentRowSelectSql() + """
            WHERE r.npc_id = ?
            ORDER BY CASE WHEN r.status = 'active' THEN 0 ELSE 1 END,
                     r.updated_at DESC, r.created_at DESC, r.household_id ASC, r.resident_key ASC
        """;
        List<RepairResidentRow> rows = new ArrayList<>();
        try (PreparedStatement statement = requireStatements().prepareStatement(sql)) {
            statement.setInt(1, npcId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(readRepairResidentRow(resultSet));
                }
            }
        }
        return rows;
    }

    private List<RepairResidentRow> loadRepairResidentRowsForSourceKey(String sourceKey) throws SQLException {
        String sql = repairResidentRowSelectSql() + """
            WHERE r.source_key = ?
            ORDER BY CASE WHEN r.status = 'active' THEN 0 ELSE 1 END,
                     r.updated_at DESC, r.created_at DESC, r.household_id ASC, r.resident_key ASC
        """;
        List<RepairResidentRow> rows = new ArrayList<>();
        try (PreparedStatement statement = requireStatements().prepareStatement(sql)) {
            statement.setString(1, clean(sourceKey));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(readRepairResidentRow(resultSet));
                }
            }
        }
        return rows;
    }

    private String repairResidentRowSelectSql() {
        return """
            SELECT r.household_id, r.resident_key, r.npc_id, r.npc_name,
                   r.source_key, r.status, r.created_at, r.updated_at,
                   h.home_place_id
            FROM household_residents r
            LEFT JOIN households h ON h.household_id = r.household_id
        """;
    }

    private RepairResidentRow readRepairResidentRow(ResultSet resultSet) throws SQLException {
        return new RepairResidentRow(
            text(resultSet, "household_id"),
            text(resultSet, "resident_key"),
            resultSet.getInt("npc_id"),
            text(resultSet, "npc_name"),
            text(resultSet, "source_key"),
            text(resultSet, "status"),
            text(resultSet, "home_place_id"),
            resultSet.getLong("created_at"),
            resultSet.getLong("updated_at")
        );
    }

    private boolean deleteRepairResidentRow(RepairResidentRow row) throws SQLException {
        try (PreparedStatement statement = requireStatements().prepareStatement("""
                 DELETE FROM household_residents
                 WHERE household_id = ?
                   AND resident_key = ?
                   AND npc_id = ?
                 """)) {
            statement.setString(1, row.householdId());
            statement.setString(2, row.residentKey());
            statement.setInt(3, row.npcId());
            return statement.executeUpdate() > 0;
        }
    }

    private List<BindingBackfillRow> loadBindingBackfillRows(int limit) throws SQLException {
        String sql = """
            SELECT b.npc_id, b.npc_uuid, b.npc_name,
                   b.home_place_id, b.work_place_id, b.social_place_id,
                   b.home_node_id, b.work_node_id, b.social_node_id,
                   b.family_id,
                   COALESCE((
                       SELECT s.source_key
                       FROM npc_source_keys s
                       WHERE s.npc_id = b.npc_id
                       ORDER BY s.updated_at DESC, s.source_key ASC
                       LIMIT 1
                   ), '') AS source_key
            FROM npc_world_bindings b
            JOIN npcs n ON n.id = b.npc_id
            WHERE TRIM(COALESCE(b.home_place_id, '')) <> ''
            ORDER BY COALESCE(NULLIF(TRIM(b.family_id), ''), b.home_place_id), b.npc_id
            LIMIT ?
        """;
        List<BindingBackfillRow> rows = new ArrayList<>();
        try (PreparedStatement statement = requireStatements().prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new BindingBackfillRow(
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
                        text(resultSet, "source_key")
                    ));
                }
            }
        }
        return rows;
    }

    private Optional<BindingBackfillRow> loadMetadataBackfillRow(MetadataResidentBackfillInput input)
        throws SQLException {
        String sql = """
            SELECT n.id AS npc_id, n.uuid AS npc_uuid, n.name AS npc_name,
                   COALESCE((
                       SELECT s.source_key
                       FROM npc_source_keys s
                       WHERE s.npc_id = n.id
                       ORDER BY s.updated_at DESC, s.source_key ASC
                       LIMIT 1
                   ), '') AS source_key
            FROM npcs n
            WHERE n.id = ?
            LIMIT 1
        """;
        try (PreparedStatement statement = requireStatements().prepareStatement(sql)) {
            statement.setInt(1, input.npcId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new BindingBackfillRow(
                    resultSet.getInt("npc_id"),
                    text(resultSet, "npc_uuid"),
                    text(resultSet, "npc_name"),
                    input.homePlaceId(),
                    "",
                    "",
                    "",
                    "",
                    "",
                    input.familyId(),
                    text(resultSet, "source_key")
                ));
            }
        }
    }

    private void backfillHouseholdCandidate(String householdId,
                                            List<BindingBackfillRow> rows,
                                            boolean apply,
                                            HouseholdBackfillAccumulator report) throws SQLException {
        List<BindingBackfillRow> safeRows = List.copyOf(rows != null ? rows : List.of());
        if (safeRows.isEmpty()) {
            return;
        }

        Set<String> homePlaceIds = new LinkedHashSet<>();
        for (BindingBackfillRow row : safeRows) {
            if (!row.homePlaceId().isBlank()) {
                homePlaceIds.add(row.homePlaceId());
            }
        }
        if (homePlaceIds.size() != 1) {
            report.warning("Sar peste household " + householdId
                + ": family_id-ul are mai multe home_place_id=" + homePlaceIds + ".");
            report.skippedResidents += safeRows.size();
            return;
        }

        String homePlaceId = homePlaceIds.iterator().next();
        Optional<HouseholdRecord> existingByHome = findHouseholdByHomePlace(homePlaceId);
        if (existingByHome.isPresent() && !existingByHome.get().householdId().equals(householdId)) {
            report.warning("Sar peste household " + householdId + ": casa " + homePlaceId
                + " apartine deja de " + existingByHome.get().householdId() + ".");
            report.skippedResidents += safeRows.size();
            return;
        }

        Optional<HouseholdRecord> existingById = getHousehold(householdId);
        if (existingById.isPresent()
            && !existingById.get().homePlaceId().isBlank()
            && !existingById.get().homePlaceId().equals(homePlaceId)) {
            report.warning("Sar peste household " + householdId + ": exista deja cu home_place_id="
                + existingById.get().homePlaceId() + ", nu " + homePlaceId + ".");
            report.skippedResidents += safeRows.size();
            return;
        }

        String familyId = firstNonBlank(safeRows.stream().map(BindingBackfillRow::familyId).toList());
        String primaryOwnerKey = "npc_" + safeRows.get(0).npcId();
        if (apply) {
            upsertBackfilledHousehold(householdId, familyId, homePlaceId, primaryOwnerKey, safeRows.size());
            if (existingById.isPresent()) {
                report.householdsUpdated++;
            } else {
                report.householdsCreated++;
            }
        } else {
            report.action("As " + (existingById.isPresent() ? "actualiza" : "crea")
                + " household " + householdId + " pentru casa " + homePlaceId
                + " cu " + safeRows.size() + " rezidenti.");
        }

        int residentsAccepted = 0;
        for (BindingBackfillRow row : safeRows) {
            if (backfillResident(householdId, row, apply, report)) {
                residentsAccepted++;
            }
        }

        if (apply) {
            updateResidentCount(householdId, residentsAccepted);
        }
    }

    private boolean backfillResident(String householdId,
                                     BindingBackfillRow row,
                                     boolean apply,
                                     HouseholdBackfillAccumulator report) throws SQLException {
        Optional<HouseholdResidentRecord> existingByNpc = findResidentByNpcId(row.npcId());
        if (existingByNpc.isPresent()) {
            if (existingByNpc.get().householdId().equals(householdId)) {
                report.residentsAlreadyPresent++;
                return true;
            }
            report.warning("Sar peste npc_id=" + row.npcId() + " pentru household " + householdId
                + ": este deja in household " + existingByNpc.get().householdId() + ".");
            report.skippedResidents++;
            return false;
        }

        if (!row.sourceKey().isBlank()) {
            Optional<HouseholdResidentRecord> existingBySource = findResidentBySourceKey(row.sourceKey());
            if (existingBySource.isPresent()
                && (!existingBySource.get().householdId().equals(householdId)
                    || existingBySource.get().npcId() != row.npcId())) {
                report.warning("Sar peste npc_id=" + row.npcId() + " pentru household " + householdId
                    + ": source_key este deja folosit de " + existingBySource.get().householdId()
                    + "/" + existingBySource.get().residentKey() + ".");
                report.skippedResidents++;
                return false;
            }
        }

        if (apply) {
            upsertBackfilledResident(householdId, row);
            report.residentsCreated++;
        } else {
            report.action("As adauga resident npc_" + row.npcId() + " in household " + householdId
                + " home=" + row.homePlaceId() + ".");
        }
        return true;
    }

    private void upsertBackfilledHousehold(String householdId,
                                           String familyId,
                                           String homePlaceId,
                                           String primaryOwnerKey,
                                           int maxResidents) throws SQLException {
        long now = System.currentTimeMillis();
        String sql = """
            INSERT INTO households (
                household_id, family_id, home_place_id, primary_owner_key,
                max_residents, resident_count, plan_hash, source, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, 'binding_backfill', ?, ?)
            ON CONFLICT(household_id) DO UPDATE SET
                family_id = CASE
                    WHEN households.family_id = '' THEN excluded.family_id
                    ELSE households.family_id
                END,
                home_place_id = CASE
                    WHEN households.home_place_id = '' THEN excluded.home_place_id
                    ELSE households.home_place_id
                END,
                primary_owner_key = CASE
                    WHEN households.primary_owner_key = '' THEN excluded.primary_owner_key
                    ELSE households.primary_owner_key
                END,
                max_residents = MAX(households.max_residents, excluded.max_residents),
                source = CASE
                    WHEN households.source = '' THEN excluded.source
                    ELSE households.source
                END,
                updated_at = excluded.updated_at
        """;
        try (PreparedStatement statement = requireStatements().prepareStatement(sql)) {
            statement.setString(1, householdId);
            statement.setString(2, clean(familyId));
            statement.setString(3, clean(homePlaceId));
            statement.setString(4, clean(primaryOwnerKey));
            statement.setInt(5, Math.max(1, maxResidents));
            statement.setInt(6, Math.max(0, maxResidents));
            statement.setString(7, "binding_backfill:" + householdId);
            statement.setLong(8, now);
            statement.setLong(9, now);
            statement.executeUpdate();
        }
    }

    private void upsertBackfilledResident(String householdId, BindingBackfillRow row) throws SQLException {
        long now = System.currentTimeMillis();
        String residentKey = "npc_" + row.npcId();
        String sql = """
            INSERT INTO household_residents (
                household_id, resident_key, npc_id, npc_uuid, npc_name, source_key,
                relation_role, home_place_id, spawn_node_id, home_node_id,
                work_place_id, work_node_id, social_place_id, social_node_id,
                status, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, 'resident', ?, ?, ?, ?, ?, ?, ?, 'active', ?, ?)
            ON CONFLICT(household_id, resident_key) DO UPDATE SET
                npc_uuid = excluded.npc_uuid,
                npc_name = excluded.npc_name,
                source_key = CASE
                    WHEN household_residents.source_key = '' THEN excluded.source_key
                    ELSE household_residents.source_key
                END,
                home_place_id = excluded.home_place_id,
                spawn_node_id = excluded.spawn_node_id,
                home_node_id = excluded.home_node_id,
                work_place_id = excluded.work_place_id,
                work_node_id = excluded.work_node_id,
                social_place_id = excluded.social_place_id,
                social_node_id = excluded.social_node_id,
                status = excluded.status,
                updated_at = excluded.updated_at
        """;
        try (PreparedStatement statement = requireStatements().prepareStatement(sql)) {
            statement.setString(1, householdId);
            statement.setString(2, residentKey);
            statement.setInt(3, row.npcId());
            statement.setString(4, row.npcUuid());
            statement.setString(5, row.npcName());
            statement.setString(6, row.sourceKey());
            statement.setString(7, row.homePlaceId());
            statement.setString(8, row.homeNodeId());
            statement.setString(9, row.homeNodeId());
            statement.setString(10, row.workPlaceId());
            statement.setString(11, row.workNodeId());
            statement.setString(12, row.socialPlaceId());
            statement.setString(13, row.socialNodeId());
            statement.setLong(14, now);
            statement.setLong(15, now);
            statement.executeUpdate();
        }
    }

    private void saveHouseholdRow(HouseAllocation allocation,
                                  String householdId,
                                  List<NpcSpawnPlan> plans,
                                  String source,
                                  long now) throws SQLException {
        removeConflictingHousehold(allocation.placeId(), householdId);
        String sql = """
            INSERT INTO households (
                household_id, family_id, home_place_id, primary_owner_key,
                max_residents, resident_count, plan_hash, source, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(household_id) DO UPDATE SET
                family_id = excluded.family_id,
                home_place_id = excluded.home_place_id,
                primary_owner_key = excluded.primary_owner_key,
                max_residents = excluded.max_residents,
                resident_count = excluded.resident_count,
                plan_hash = excluded.plan_hash,
                source = excluded.source,
                updated_at = excluded.updated_at
        """;

        try (PreparedStatement statement = requireStatements().prepareStatement(sql)) {
            statement.setString(1, householdId);
            statement.setString(2, allocation.familyId());
            statement.setString(3, allocation.placeId());
            statement.setString(4, allocation.primaryOwnerNpcKey());
            statement.setInt(5, allocation.maxResidents());
            statement.setInt(6, plans.size());
            statement.setString(7, SpawnBatchPlanHasher.householdPlanHash(allocation));
            statement.setString(8, clean(source));
            statement.setLong(9, now);
            statement.setLong(10, now);
            statement.executeUpdate();
        }
    }

    private void removeConflictingHousehold(String homePlaceId, String householdId) throws SQLException {
        if (homePlaceId == null || homePlaceId.isBlank()) {
            return;
        }
        try (PreparedStatement statement = requireStatements().prepareStatement("""
                 DELETE FROM households
                 WHERE home_place_id = ?
                   AND household_id <> ?
                 """)) {
            statement.setString(1, homePlaceId);
            statement.setString(2, householdId);
            statement.executeUpdate();
        }
    }

    private void saveResidentRow(String householdId,
                                 String residentKey,
                                 HouseAllocation allocation,
                                 HouseAllocation.ResidentPlan residentPlan,
                                 NpcSpawnPlan plan,
                                 AINPC npc,
                                 String source,
                                 long now) throws SQLException {
        removeMovedResident(npc.getDatabaseId(), plan.sourceKey(), householdId, residentKey);

        String sql = """
            INSERT INTO household_residents (
                household_id, resident_key, npc_id, npc_uuid, npc_name, source_key,
                relation_role, home_place_id, spawn_node_id, home_node_id,
                work_place_id, work_node_id, social_place_id, social_node_id,
                status, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'active', ?, ?)
            ON CONFLICT(household_id, resident_key) DO UPDATE SET
                npc_id = excluded.npc_id,
                npc_uuid = excluded.npc_uuid,
                npc_name = excluded.npc_name,
                source_key = excluded.source_key,
                relation_role = excluded.relation_role,
                home_place_id = excluded.home_place_id,
                spawn_node_id = excluded.spawn_node_id,
                home_node_id = excluded.home_node_id,
                work_place_id = excluded.work_place_id,
                work_node_id = excluded.work_node_id,
                social_place_id = excluded.social_place_id,
                social_node_id = excluded.social_node_id,
                status = excluded.status,
                updated_at = excluded.updated_at
        """;

        try (PreparedStatement statement = requireStatements().prepareStatement(sql)) {
            statement.setString(1, householdId);
            statement.setString(2, residentKey);
            statement.setInt(3, npc.getDatabaseId());
            statement.setString(4, npc.getUuid() != null ? npc.getUuid().toString() : "");
            statement.setString(5, npc.getName());
            statement.setString(6, plan.sourceKey());
            statement.setString(7, residentPlan.relationRole());
            statement.setString(8, allocation.placeId());
            statement.setString(9, residentPlan.spawnNodeId());
            statement.setString(10, residentPlan.effectiveHomeNodeId());
            statement.setString(11, residentPlan.workPlaceId());
            statement.setString(12, residentPlan.workNodeId());
            statement.setString(13, residentPlan.socialPlaceId());
            statement.setString(14, residentPlan.socialNodeId());
            statement.setLong(15, now);
            statement.setLong(16, now);
            statement.executeUpdate();
        }
    }

    private void removeMovedResident(int npcId, String sourceKey, String householdId, String residentKey) throws SQLException {
        if (npcId <= 0) {
            return;
        }

        try (PreparedStatement statement = requireStatements().prepareStatement("""
                 DELETE FROM household_residents
                 WHERE (npc_id = ? OR (source_key <> '' AND source_key = ?))
                   AND NOT (household_id = ? AND resident_key = ?)
                 """)) {
            statement.setInt(1, npcId);
            statement.setString(2, clean(sourceKey));
            statement.setString(3, householdId);
            statement.setString(4, residentKey);
            statement.executeUpdate();
        }
    }

    private void deleteStaleResidents(String householdId, List<String> activeResidentKeys) throws SQLException {
        if (activeResidentKeys.isEmpty()) {
            try (PreparedStatement statement = requireStatements()
                     .prepareStatement("DELETE FROM household_residents WHERE household_id = ?")) {
                statement.setString(1, householdId);
                statement.executeUpdate();
            }
            return;
        }

        StringJoiner placeholders = new StringJoiner(", ");
        for (int i = 0; i < activeResidentKeys.size(); i++) {
            placeholders.add("?");
        }

        try (PreparedStatement statement = requireStatements().prepareStatement(
                 "DELETE FROM household_residents WHERE household_id = ? AND resident_key NOT IN ("
                     + placeholders + ")")) {
            statement.setString(1, householdId);
            for (int i = 0; i < activeResidentKeys.size(); i++) {
                statement.setString(i + 2, activeResidentKeys.get(i));
            }
            statement.executeUpdate();
        }
    }

    private int updateResidentCount(String householdId, int fallbackCount) throws SQLException {
        try (PreparedStatement statement = requireStatements().prepareStatement("""
                 UPDATE households
                 SET resident_count = COALESCE((
                     SELECT COUNT(*)
                     FROM household_residents
                     WHERE household_id = households.household_id
                       AND status = 'active'
                 ), ?),
                     updated_at = ?
                 WHERE household_id = ?
                 """)) {
            statement.setInt(1, Math.max(0, fallbackCount));
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, householdId);
            return statement.executeUpdate();
        }
    }

    private HouseAllocation requireValid(HouseAllocation allocation) {
        if (allocation == null) {
            throw new IllegalArgumentException("HouseAllocation nu poate fi null.");
        }
        if (allocation.householdId().isBlank()) {
            throw new IllegalArgumentException("HouseAllocation nu are household_id valid.");
        }
        return allocation;
    }

    private HouseAllocation.ResidentPlan findResidentPlan(HouseAllocation allocation, NpcSpawnPlan plan) {
        return allocation.residentPlans().stream()
            .filter(resident -> normalizeResidentKey(resident.npcKey()).equals(normalizeResidentKey(plan.npcKey())))
            .findFirst()
            .orElse(null);
    }

    private HouseholdResidentRecord readResident(ResultSet resultSet) throws SQLException {
        return new HouseholdResidentRecord(
            resultSet.getString("household_id"),
            resultSet.getString("resident_key"),
            resultSet.getInt("npc_id"),
            text(resultSet, "npc_uuid"),
            text(resultSet, "npc_name"),
            text(resultSet, "source_key"),
            text(resultSet, "relation_role"),
            text(resultSet, "home_place_id"),
            text(resultSet, "spawn_node_id"),
            text(resultSet, "home_node_id"),
            text(resultSet, "work_place_id"),
            text(resultSet, "work_node_id"),
            text(resultSet, "social_place_id"),
            text(resultSet, "social_node_id"),
            text(resultSet, "status"),
            resultSet.getLong("created_at"),
            resultSet.getLong("updated_at")
        );
    }

    private Optional<HouseholdResidentRecord> findResidentBySourceKey(String sourceKey) throws SQLException {
        if (sourceKey == null || sourceKey.isBlank()) {
            return Optional.empty();
        }
        String sql = """
            SELECT household_id, resident_key, npc_id, npc_uuid, npc_name, source_key,
                   relation_role, home_place_id, spawn_node_id, home_node_id,
                   work_place_id, work_node_id, social_place_id, social_node_id,
                   status, created_at, updated_at
            FROM household_residents
            WHERE source_key = ?
            ORDER BY updated_at DESC, household_id ASC, resident_key ASC
            LIMIT 1
        """;
        try (PreparedStatement statement = requireStatements().prepareStatement(sql)) {
            statement.setString(1, clean(sourceKey));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readResident(resultSet)) : Optional.empty();
            }
        }
    }

    private HouseholdRecord readHousehold(ResultSet resultSet) throws SQLException {
        return new HouseholdRecord(
            resultSet.getString("household_id"),
            text(resultSet, "family_id"),
            text(resultSet, "home_place_id"),
            text(resultSet, "primary_owner_key"),
            resultSet.getInt("max_residents"),
            resultSet.getInt("resident_count"),
            text(resultSet, "plan_hash"),
            text(resultSet, "source"),
            resultSet.getLong("created_at"),
            resultSet.getLong("updated_at")
        );
    }

    private StatementProvider requireStatements() throws SQLException {
        if (statements == null) {
            throw new SQLException("HouseholdPersistenceService nu are acces la baza de date.");
        }
        return statements;
    }

    private static String normalizeResidentKey(String value) {
        return clean(value).toLowerCase(java.util.Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private static String backfillHouseholdId(String familyId, String homePlaceId) {
        String cleanFamilyId = clean(familyId);
        if (!cleanFamilyId.isBlank()) {
            return cleanFamilyId;
        }
        String normalizedPlaceId = normalizeResidentKey(homePlaceId);
        return normalizedPlaceId.isBlank() ? "" : "household_" + normalizedPlaceId;
    }

    private static String firstNonBlank(List<String> values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String cleanValue = clean(value);
            if (!cleanValue.isBlank()) {
                return cleanValue;
            }
        }
        return "";
    }

    private static void addLimited(List<String> target, String value) {
        if (target.size() < 50) {
            target.add(value);
        }
    }

    private static String text(ResultSet resultSet, String column) throws SQLException {
        String value = resultSet.getString(column);
        return value == null ? "" : value;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public record HouseholdRecord(
        String householdId,
        String familyId,
        String homePlaceId,
        String primaryOwnerKey,
        int maxResidents,
        int residentCount,
        String planHash,
        String source,
        long createdAt,
        long updatedAt
    ) {
        public HouseholdRecord {
            householdId = clean(householdId);
            familyId = clean(familyId);
            homePlaceId = clean(homePlaceId);
            primaryOwnerKey = clean(primaryOwnerKey);
            planHash = clean(planHash);
            source = clean(source);
            maxResidents = Math.max(0, maxResidents);
            residentCount = Math.max(0, residentCount);
        }
    }

    private record BindingBackfillRow(
        int npcId,
        String npcUuid,
        String npcName,
        String homePlaceId,
        String workPlaceId,
        String socialPlaceId,
        String homeNodeId,
        String workNodeId,
        String socialNodeId,
        String familyId,
        String sourceKey
    ) {
        private BindingBackfillRow {
            npcUuid = clean(npcUuid);
            npcName = clean(npcName);
            homePlaceId = clean(homePlaceId);
            workPlaceId = clean(workPlaceId);
            socialPlaceId = clean(socialPlaceId);
            homeNodeId = clean(homeNodeId);
            workNodeId = clean(workNodeId);
            socialNodeId = clean(socialNodeId);
            familyId = clean(familyId);
            sourceKey = clean(sourceKey);
        }
    }

    public record HouseholdBackfillReport(
        boolean apply,
        int scannedBindings,
        int candidateHouseholds,
        int householdsCreated,
        int householdsUpdated,
        int residentsCreated,
        int residentsAlreadyPresent,
        int skippedResidents,
        List<String> actions,
        List<String> warnings,
        List<String> errors
    ) {
        public HouseholdBackfillReport {
            actions = List.copyOf(actions != null ? actions : List.of());
            warnings = List.copyOf(warnings != null ? warnings : List.of());
            errors = List.copyOf(errors != null ? errors : List.of());
        }

        public boolean success() {
            return errors.isEmpty();
        }
    }

    public record MetadataResidentBackfillInput(
        String homePlaceId,
        String familyId,
        int npcId
    ) {
        public MetadataResidentBackfillInput {
            homePlaceId = clean(homePlaceId);
            familyId = clean(familyId);
        }
    }

    public record HouseholdResidentRepairReport(
        boolean apply,
        int duplicateNpcGroups,
        int duplicateSourceKeyGroups,
        int duplicateResidentRows,
        int deletedResidentRows,
        int updatedHouseholds,
        List<String> actions,
        List<String> warnings,
        List<String> errors
    ) {
        public HouseholdResidentRepairReport {
            actions = List.copyOf(actions != null ? actions : List.of());
            warnings = List.copyOf(warnings != null ? warnings : List.of());
            errors = List.copyOf(errors != null ? errors : List.of());
        }
    }

    private record RepairResidentRow(
        String householdId,
        String residentKey,
        int npcId,
        String npcName,
        String sourceKey,
        String status,
        String homePlaceId,
        long createdAt,
        long updatedAt
    ) {
        private RepairResidentRow {
            householdId = clean(householdId);
            residentKey = clean(residentKey);
            npcName = clean(npcName);
            sourceKey = clean(sourceKey);
            status = clean(status);
            homePlaceId = clean(homePlaceId);
        }

        private String identityKey() {
            return householdId + "|" + residentKey + "|" + npcId;
        }

        private String label() {
            return householdId + "/" + residentKey
                + " npc_id=" + npcId
                + " name=" + npcName
                + " home=" + homePlaceId
                + " updated_at=" + updatedAt;
        }

        private static int compareCanonical(RepairResidentRow left, RepairResidentRow right) {
            int activeCompare = Integer.compare(activeRank(left.status), activeRank(right.status));
            if (activeCompare != 0) {
                return activeCompare;
            }
            int updatedCompare = Long.compare(right.updatedAt, left.updatedAt);
            if (updatedCompare != 0) {
                return updatedCompare;
            }
            int createdCompare = Long.compare(right.createdAt, left.createdAt);
            if (createdCompare != 0) {
                return createdCompare;
            }
            int householdCompare = left.householdId.compareTo(right.householdId);
            if (householdCompare != 0) {
                return householdCompare;
            }
            return left.residentKey.compareTo(right.residentKey);
        }

        private static int activeRank(String status) {
            return "active".equalsIgnoreCase(status) ? 0 : 1;
        }
    }

    private static final class HouseholdBackfillAccumulator {
        private final boolean apply;
        private final int scannedBindings;
        private int candidateHouseholds;
        private int householdsCreated;
        private int householdsUpdated;
        private int residentsCreated;
        private int residentsAlreadyPresent;
        private int skippedResidents;
        private final List<String> actions = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        private HouseholdBackfillAccumulator(boolean apply, int scannedBindings) {
            this.apply = apply;
            this.scannedBindings = scannedBindings;
        }

        private void action(String message) {
            addLimited(actions, message);
        }

        private void warning(String message) {
            addLimited(warnings, message);
        }

        private HouseholdBackfillReport toReport() {
            return new HouseholdBackfillReport(
                apply,
                scannedBindings,
                candidateHouseholds,
                householdsCreated,
                householdsUpdated,
                residentsCreated,
                residentsAlreadyPresent,
                skippedResidents,
                actions,
                warnings,
                errors
            );
        }
    }

    private static final class HouseholdResidentRepairAccumulator {
        private final boolean apply;
        private int duplicateNpcGroups;
        private int duplicateSourceKeyGroups;
        private int duplicateResidentRows;
        private int deletedResidentRows;
        private int updatedHouseholds;
        private final List<String> actions = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        private HouseholdResidentRepairAccumulator(boolean apply) {
            this.apply = apply;
        }

        private void action(String message) {
            addLimited(actions, message);
        }

        private void warning(String message) {
            addLimited(warnings, message);
        }

        private HouseholdResidentRepairReport toReport() {
            return new HouseholdResidentRepairReport(
                apply,
                duplicateNpcGroups,
                duplicateSourceKeyGroups,
                duplicateResidentRows,
                deletedResidentRows,
                updatedHouseholds,
                actions,
                warnings,
                errors
            );
        }
    }

    public record HouseholdResidentRecord(
        String householdId,
        String residentKey,
        int npcId,
        String npcUuid,
        String npcName,
        String sourceKey,
        String relationRole,
        String homePlaceId,
        String spawnNodeId,
        String homeNodeId,
        String workPlaceId,
        String workNodeId,
        String socialPlaceId,
        String socialNodeId,
        String status,
        long createdAt,
        long updatedAt
    ) {
        public HouseholdResidentRecord {
            householdId = clean(householdId);
            residentKey = clean(residentKey);
            npcUuid = clean(npcUuid);
            npcName = clean(npcName);
            sourceKey = clean(sourceKey);
            relationRole = clean(relationRole);
            homePlaceId = clean(homePlaceId);
            spawnNodeId = clean(spawnNodeId);
            homeNodeId = clean(homeNodeId);
            workPlaceId = clean(workPlaceId);
            workNodeId = clean(workNodeId);
            socialPlaceId = clean(socialPlaceId);
            socialNodeId = clean(socialNodeId);
            status = clean(status);
        }
    }
}
