package ro.ainpc.spawn

import ro.ainpc.npc.AINPC
import ro.ainpc.spawn.HouseholdPersistenceService.HouseholdBackfillReport
import ro.ainpc.spawn.HouseholdPersistenceService.HouseholdRecord
import ro.ainpc.spawn.HouseholdPersistenceService.HouseholdResidentRecord
import ro.ainpc.spawn.HouseholdPersistenceService.HouseholdResidentRepairReport
import ro.ainpc.spawn.HouseholdPersistenceService.MetadataResidentBackfillInput
import java.sql.ResultSet
import java.sql.SQLException
import java.util.Locale
import java.util.Optional
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min

class HouseholdPersistenceServiceState(
    private val statements: HouseholdPersistenceService.StatementProvider?,
    logger: Logger?
) {
    private val logger: Logger = logger ?: Logger.getLogger(HouseholdPersistenceService::class.java.name)

    @Throws(SQLException::class)
    fun saveHousehold(
        allocation: HouseAllocation?,
        plans: List<NpcSpawnPlan>?,
        spawnResults: List<NpcSpawnResult?>?,
        source: String?
    ): Int {
        val safeAllocation = requireValid(allocation)
        val safePlans = plans?.toList() ?: emptyList()
        val safeResults = spawnResults?.toList() ?: emptyList()
        val householdId = safeAllocation.householdId()
        val now = System.currentTimeMillis()

        saveHouseholdRow(safeAllocation, householdId, safePlans, source, now)
        val activeResidentKeys = ArrayList<String>()
        var savedResidents = 0
        val limit = min(safePlans.size, safeResults.size)

        for (index in 0 until limit) {
            val plan = safePlans[index]
            val result = safeResults[index]
            val npc = result?.npc()
            if (result == null || !result.success() || npc == null) {
                continue
            }

            val residentPlan = findResidentPlan(safeAllocation, plan)
            if (residentPlan == null) {
                logger.fine("Sar peste resident fara ResidentPlan pentru ${plan.npcKey()}")
                continue
            }

            val residentKey = normalizeResidentKey(residentPlan.npcKey())
            if (residentKey.isBlank()) {
                continue
            }

            saveResidentRow(householdId, residentKey, safeAllocation, residentPlan, plan, npc, source, now)
            activeResidentKeys.add(residentKey)
            savedResidents++
        }

        deleteStaleResidents(householdId, activeResidentKeys)
        updateResidentCount(householdId, savedResidents)
        return savedResidents
    }

    @Throws(SQLException::class)
    fun listResidents(householdId: String?): List<HouseholdResidentRecord> {
        if (householdId.isNullOrBlank()) {
            return emptyList()
        }

        val sql = """
            SELECT household_id, resident_key, npc_id, npc_uuid, npc_name, source_key,
                   relation_role, home_place_id, spawn_node_id, home_node_id,
                   work_place_id, work_node_id, social_place_id, social_node_id,
                   status, created_at, updated_at
            FROM household_residents
            WHERE household_id = ?
            ORDER BY resident_key ASC
        """.trimIndent()
        val residents = ArrayList<HouseholdResidentRecord>()
        requireStatements().prepareStatement(sql).use { statement ->
            statement.setString(1, householdId.trim())
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    residents.add(readResident(resultSet))
                }
            }
        }
        return residents
    }

    @Throws(SQLException::class)
    fun listHouseholds(limit: Int): List<HouseholdRecord> {
        val safeLimit = max(1, min(100, limit))
        val sql = """
            SELECT household_id, family_id, home_place_id, primary_owner_key,
                   max_residents, resident_count, plan_hash, source, created_at, updated_at
            FROM households
            ORDER BY updated_at DESC, household_id ASC
            LIMIT ?
        """.trimIndent()
        val households = ArrayList<HouseholdRecord>()
        requireStatements().prepareStatement(sql).use { statement ->
            statement.setInt(1, safeLimit)
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    households.add(readHousehold(resultSet))
                }
            }
        }
        return households
    }

    @Throws(SQLException::class)
    fun getHousehold(householdId: String?): Optional<HouseholdRecord> {
        if (householdId.isNullOrBlank()) {
            return Optional.empty()
        }
        val sql = """
            SELECT household_id, family_id, home_place_id, primary_owner_key,
                   max_residents, resident_count, plan_hash, source, created_at, updated_at
            FROM households
            WHERE household_id = ?
            LIMIT 1
        """.trimIndent()
        requireStatements().prepareStatement(sql).use { statement ->
            statement.setString(1, householdId.trim())
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) Optional.of(readHousehold(resultSet)) else Optional.empty()
            }
        }
    }

    @Throws(SQLException::class)
    fun findHouseholdByHomePlace(homePlaceId: String?): Optional<HouseholdRecord> {
        if (homePlaceId.isNullOrBlank()) {
            return Optional.empty()
        }
        val sql = """
            SELECT household_id, family_id, home_place_id, primary_owner_key,
                   max_residents, resident_count, plan_hash, source, created_at, updated_at
            FROM households
            WHERE home_place_id = ?
            ORDER BY updated_at DESC, household_id ASC
            LIMIT 1
        """.trimIndent()
        requireStatements().prepareStatement(sql).use { statement ->
            statement.setString(1, homePlaceId.trim())
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) Optional.of(readHousehold(resultSet)) else Optional.empty()
            }
        }
    }

    @Throws(SQLException::class)
    fun findResidentByNpcId(npcId: Int): Optional<HouseholdResidentRecord> {
        if (npcId <= 0) {
            return Optional.empty()
        }
        val sql = """
            SELECT household_id, resident_key, npc_id, npc_uuid, npc_name, source_key,
                   relation_role, home_place_id, spawn_node_id, home_node_id,
                   work_place_id, work_node_id, social_place_id, social_node_id,
                   status, created_at, updated_at
            FROM household_residents
            WHERE npc_id = ?
            ORDER BY updated_at DESC, household_id ASC, resident_key ASC
            LIMIT 1
        """.trimIndent()
        requireStatements().prepareStatement(sql).use { statement ->
            statement.setInt(1, npcId)
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) Optional.of(readResident(resultSet)) else Optional.empty()
            }
        }
    }

    @Throws(SQLException::class)
    fun backfillFromNpcWorldBindings(apply: Boolean, limit: Int): HouseholdBackfillReport {
        val safeLimit = max(1, min(1000, limit))
        val rows = loadBindingBackfillRows(safeLimit)
        val report = HouseholdBackfillAccumulator(apply, rows.size)
        if (rows.isEmpty()) {
            report.warning("Nu exista npc_world_bindings cu home_place_id pentru backfill household.")
            return report.toReport()
        }

        val rowsByHousehold = LinkedHashMap<String, MutableList<BindingBackfillRow>>()
        for (row in rows) {
            val householdId = backfillHouseholdId(row.familyId, row.homePlaceId)
            if (householdId.isBlank()) {
                report.warning("Sar peste npc_id=${row.npcId}: nu pot deriva household_id din family_id/home_place_id.")
                continue
            }
            rowsByHousehold.computeIfAbsent(householdId) { ArrayList() }.add(row)
        }
        report.candidateHouseholds = rowsByHousehold.size

        for ((householdId, householdRows) in rowsByHousehold) {
            backfillHouseholdCandidate(householdId, householdRows, apply, report)
        }

        return report.toReport()
    }

    @Throws(SQLException::class)
    fun backfillFromMetadataResidents(
        apply: Boolean,
        limit: Int,
        inputs: List<MetadataResidentBackfillInput>?
    ): HouseholdBackfillReport {
        val safeLimit = max(1, min(1000, limit))
        val safeInputs = (inputs ?: emptyList())
            .asSequence()
            .filter { input -> input.npcId() > 0 && input.homePlaceId().isNotBlank() }
            .take(safeLimit)
            .toList()
        val report = HouseholdBackfillAccumulator(apply, safeInputs.size)
        if (safeInputs.isEmpty()) {
            report.warning("Nu exista metadata resident_npc_ids valida pentru backfill household.")
            return report.toReport()
        }

        val rowsByKey = LinkedHashMap<String, BindingBackfillRow>()
        for (input in safeInputs) {
            val row = loadMetadataBackfillRow(input)
            if (row.isEmpty) {
                report.warning("Sar peste metadata resident npc_id=${input.npcId()} pentru ${input.homePlaceId()}: NPC-ul nu exista in DB.")
                continue
            }
            val value = row.get()
            rowsByKey.putIfAbsent("${value.homePlaceId}:${value.npcId}", value)
        }

        val rowsByHousehold = LinkedHashMap<String, MutableList<BindingBackfillRow>>()
        for (row in rowsByKey.values) {
            val householdId = backfillHouseholdId(row.familyId, row.homePlaceId)
            if (householdId.isBlank()) {
                report.warning("Sar peste metadata resident npc_id=${row.npcId}: nu pot deriva household_id din family_id/home_place_id.")
                continue
            }
            rowsByHousehold.computeIfAbsent(householdId) { ArrayList() }.add(row)
        }
        report.candidateHouseholds = rowsByHousehold.size

        for ((householdId, householdRows) in rowsByHousehold) {
            backfillHouseholdCandidate(householdId, householdRows, apply, report)
        }

        return report.toReport()
    }

    @Throws(SQLException::class)
    fun countHouseholds(): Int {
        requireStatements().prepareStatement("SELECT COUNT(*) FROM households").use { statement ->
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) resultSet.getInt(1) else 0
            }
        }
    }

    @Throws(SQLException::class)
    fun countResidents(): Int {
        requireStatements().prepareStatement("SELECT COUNT(*) FROM household_residents").use { statement ->
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) resultSet.getInt(1) else 0
            }
        }
    }

    @Throws(SQLException::class)
    fun repairDuplicateResidents(apply: Boolean, limit: Int): HouseholdResidentRepairReport {
        val safeLimit = max(1, min(1000, limit))
        val report = HouseholdResidentRepairAccumulator(apply)
        val deleteKeys = LinkedHashSet<String>()
        val affectedHouseholds = LinkedHashSet<String>()

        for (npcId in loadDuplicateResidentNpcIds(safeLimit)) {
            val rows = loadRepairResidentRowsForNpc(npcId)
            if (rows.size <= 1) {
                continue
            }
            report.duplicateNpcGroups++
            repairDuplicateResidentGroup("npc_id=$npcId", rows, deleteKeys, affectedHouseholds, report)
        }

        for (sourceKey in loadDuplicateResidentSourceKeys(safeLimit)) {
            val rows = loadRepairResidentRowsForSourceKey(sourceKey)
                .filter { row -> !deleteKeys.contains(row.identityKey()) }
            if (rows.size <= 1) {
                continue
            }
            report.duplicateSourceKeyGroups++
            repairDuplicateResidentGroup("source_key=$sourceKey", rows, deleteKeys, affectedHouseholds, report)
        }

        if (apply) {
            for (householdId in affectedHouseholds) {
                updateResidentCount(householdId, 0)
                report.updatedHouseholds++
            }
        }

        return report.toReport()
    }

    @Throws(SQLException::class)
    fun recalculateResidentCounts(householdIds: Collection<String>?): Int {
        requireStatements()
        val safeHouseholdIds = LinkedHashSet<String>()
        if (householdIds != null) {
            for (householdId in householdIds) {
                val safeHouseholdId = clean(householdId)
                if (safeHouseholdId.isNotBlank()) {
                    safeHouseholdIds.add(safeHouseholdId)
                }
            }
        }

        var updatedHouseholds = 0
        for (householdId in safeHouseholdIds) {
            updatedHouseholds += updateResidentCount(householdId, 0)
        }
        return updatedHouseholds
    }

    @Throws(SQLException::class)
    private fun repairDuplicateResidentGroup(
        groupLabel: String,
        rows: List<RepairResidentRow>,
        deleteKeys: MutableSet<String>,
        affectedHouseholds: MutableSet<String>,
        report: HouseholdResidentRepairAccumulator
    ) {
        val safeRows = rows.sortedWith(RepairResidentRow::compareCanonical)
        val canonical = safeRows[0]
        report.action("Pastrez resident canonic pentru $groupLabel: ${canonical.label()}.")

        for (index in 1 until safeRows.size) {
            val duplicate = safeRows[index]
            if (!deleteKeys.add(duplicate.identityKey())) {
                continue
            }
            report.duplicateResidentRows++
            affectedHouseholds.add(duplicate.householdId)
            affectedHouseholds.add(canonical.householdId)
            if (report.apply) {
                if (deleteRepairResidentRow(duplicate)) {
                    report.deletedResidentRows++
                    report.action("Am sters resident duplicat ${duplicate.label()}; canonic=${canonical.label()}.")
                } else {
                    report.warning("Nu am gasit pentru stergere residentul duplicat ${duplicate.label()}.")
                }
            } else {
                report.action("As sterge resident duplicat ${duplicate.label()}; canonic=${canonical.label()}.")
            }
        }
    }

    @Throws(SQLException::class)
    private fun loadDuplicateResidentNpcIds(limit: Int): List<Int> {
        val sql = """
            SELECT npc_id
            FROM household_residents
            WHERE npc_id > 0
            GROUP BY npc_id
            HAVING COUNT(*) > 1
            ORDER BY npc_id ASC
            LIMIT ?
        """.trimIndent()
        val npcIds = ArrayList<Int>()
        requireStatements().prepareStatement(sql).use { statement ->
            statement.setInt(1, limit)
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    npcIds.add(resultSet.getInt("npc_id"))
                }
            }
        }
        return npcIds
    }

    @Throws(SQLException::class)
    private fun loadDuplicateResidentSourceKeys(limit: Int): List<String> {
        val sql = """
            SELECT source_key
            FROM household_residents
            WHERE TRIM(COALESCE(source_key, '')) <> ''
            GROUP BY source_key
            HAVING COUNT(*) > 1
            ORDER BY source_key ASC
            LIMIT ?
        """.trimIndent()
        val sourceKeys = ArrayList<String>()
        requireStatements().prepareStatement(sql).use { statement ->
            statement.setInt(1, limit)
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    sourceKeys.add(text(resultSet, "source_key"))
                }
            }
        }
        return sourceKeys
    }

    @Throws(SQLException::class)
    private fun loadRepairResidentRowsForNpc(npcId: Int): List<RepairResidentRow> {
        val sql = repairResidentRowSelectSql() + """
            WHERE r.npc_id = ?
            ORDER BY CASE WHEN r.status = 'active' THEN 0 ELSE 1 END,
                     r.updated_at DESC, r.created_at DESC, r.household_id ASC, r.resident_key ASC
        """.trimIndent()
        val rows = ArrayList<RepairResidentRow>()
        requireStatements().prepareStatement(sql).use { statement ->
            statement.setInt(1, npcId)
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    rows.add(readRepairResidentRow(resultSet))
                }
            }
        }
        return rows
    }

    @Throws(SQLException::class)
    private fun loadRepairResidentRowsForSourceKey(sourceKey: String?): List<RepairResidentRow> {
        val sql = repairResidentRowSelectSql() + """
            WHERE r.source_key = ?
            ORDER BY CASE WHEN r.status = 'active' THEN 0 ELSE 1 END,
                     r.updated_at DESC, r.created_at DESC, r.household_id ASC, r.resident_key ASC
        """.trimIndent()
        val rows = ArrayList<RepairResidentRow>()
        requireStatements().prepareStatement(sql).use { statement ->
            statement.setString(1, clean(sourceKey))
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    rows.add(readRepairResidentRow(resultSet))
                }
            }
        }
        return rows
    }

    private fun repairResidentRowSelectSql(): String = """
        SELECT r.household_id, r.resident_key, r.npc_id, r.npc_name,
               r.source_key, r.status, r.created_at, r.updated_at,
               h.home_place_id
        FROM household_residents r
        LEFT JOIN households h ON h.household_id = r.household_id
    """.trimIndent() + "\n"

    @Throws(SQLException::class)
    private fun readRepairResidentRow(resultSet: ResultSet): RepairResidentRow =
        RepairResidentRow(
            text(resultSet, "household_id"),
            text(resultSet, "resident_key"),
            resultSet.getInt("npc_id"),
            text(resultSet, "npc_name"),
            text(resultSet, "source_key"),
            text(resultSet, "status"),
            text(resultSet, "home_place_id"),
            resultSet.getLong("created_at"),
            resultSet.getLong("updated_at")
        )

    @Throws(SQLException::class)
    private fun deleteRepairResidentRow(row: RepairResidentRow): Boolean {
        requireStatements().prepareStatement(
            """
            DELETE FROM household_residents
            WHERE household_id = ?
              AND resident_key = ?
              AND npc_id = ?
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, row.householdId)
            statement.setString(2, row.residentKey)
            statement.setInt(3, row.npcId)
            return statement.executeUpdate() > 0
        }
    }

    @Throws(SQLException::class)
    private fun loadBindingBackfillRows(limit: Int): List<BindingBackfillRow> {
        val sql = """
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
        """.trimIndent()
        val rows = ArrayList<BindingBackfillRow>()
        requireStatements().prepareStatement(sql).use { statement ->
            statement.setInt(1, limit)
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    rows.add(
                        BindingBackfillRow(
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
                        )
                    )
                }
            }
        }
        return rows
    }

    @Throws(SQLException::class)
    private fun loadMetadataBackfillRow(input: MetadataResidentBackfillInput): Optional<BindingBackfillRow> {
        val sql = """
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
        """.trimIndent()
        requireStatements().prepareStatement(sql).use { statement ->
            statement.setInt(1, input.npcId())
            statement.executeQuery().use { resultSet ->
                if (!resultSet.next()) {
                    return Optional.empty()
                }
                return Optional.of(
                    BindingBackfillRow(
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
                    )
                )
            }
        }
    }

    @Throws(SQLException::class)
    private fun backfillHouseholdCandidate(
        householdId: String,
        rows: List<BindingBackfillRow>?,
        apply: Boolean,
        report: HouseholdBackfillAccumulator
    ) {
        val safeRows = rows?.toList() ?: emptyList()
        if (safeRows.isEmpty()) {
            return
        }

        val homePlaceIds = LinkedHashSet<String>()
        for (row in safeRows) {
            if (row.homePlaceId.isNotBlank()) {
                homePlaceIds.add(row.homePlaceId)
            }
        }
        if (homePlaceIds.size != 1) {
            report.warning("Sar peste household $householdId: family_id-ul are mai multe home_place_id=$homePlaceIds.")
            report.skippedResidents += safeRows.size
            return
        }

        val homePlaceId = homePlaceIds.iterator().next()
        val existingByHome = findHouseholdByHomePlace(homePlaceId)
        if (existingByHome.isPresent && existingByHome.get().householdId() != householdId) {
            report.warning("Sar peste household $householdId: casa $homePlaceId apartine deja de ${existingByHome.get().householdId()}.")
            report.skippedResidents += safeRows.size
            return
        }

        val existingById = getHousehold(householdId)
        if (existingById.isPresent &&
            existingById.get().homePlaceId().isNotBlank() &&
            existingById.get().homePlaceId() != homePlaceId
        ) {
            report.warning("Sar peste household $householdId: exista deja cu home_place_id=${existingById.get().homePlaceId()}, nu $homePlaceId.")
            report.skippedResidents += safeRows.size
            return
        }

        val familyId = firstNonBlank(safeRows.map { row -> row.familyId })
        val primaryOwnerKey = "npc_${safeRows[0].npcId}"
        if (apply) {
            upsertBackfilledHousehold(householdId, familyId, homePlaceId, primaryOwnerKey, safeRows.size)
            if (existingById.isPresent) {
                report.householdsUpdated++
            } else {
                report.householdsCreated++
            }
        } else {
            report.action(
                "As ${if (existingById.isPresent) "actualiza" else "crea"} household $householdId " +
                    "pentru casa $homePlaceId cu ${safeRows.size} rezidenti."
            )
        }

        var residentsAccepted = 0
        for (row in safeRows) {
            if (backfillResident(householdId, row, apply, report)) {
                residentsAccepted++
            }
        }

        if (apply) {
            updateResidentCount(householdId, residentsAccepted)
        }
    }

    @Throws(SQLException::class)
    private fun backfillResident(
        householdId: String,
        row: BindingBackfillRow,
        apply: Boolean,
        report: HouseholdBackfillAccumulator
    ): Boolean {
        val existingByNpc = findResidentByNpcId(row.npcId)
        if (existingByNpc.isPresent) {
            if (existingByNpc.get().householdId() == householdId) {
                report.residentsAlreadyPresent++
                return true
            }
            report.warning("Sar peste npc_id=${row.npcId} pentru household $householdId: este deja in household ${existingByNpc.get().householdId()}.")
            report.skippedResidents++
            return false
        }

        if (row.sourceKey.isNotBlank()) {
            val existingBySource = findResidentBySourceKey(row.sourceKey)
            if (existingBySource.isPresent &&
                (existingBySource.get().householdId() != householdId || existingBySource.get().npcId() != row.npcId)
            ) {
                report.warning(
                    "Sar peste npc_id=${row.npcId} pentru household $householdId: source_key este deja folosit de " +
                        "${existingBySource.get().householdId()}/${existingBySource.get().residentKey()}."
                )
                report.skippedResidents++
                return false
            }
        }

        if (apply) {
            upsertBackfilledResident(householdId, row)
            report.residentsCreated++
        } else {
            report.action("As adauga resident npc_${row.npcId} in household $householdId home=${row.homePlaceId}.")
        }
        return true
    }

    @Throws(SQLException::class)
    private fun upsertBackfilledHousehold(
        householdId: String,
        familyId: String?,
        homePlaceId: String?,
        primaryOwnerKey: String?,
        maxResidents: Int
    ) {
        val now = System.currentTimeMillis()
        val sql = """
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
        """.trimIndent()
        requireStatements().prepareStatement(sql).use { statement ->
            statement.setString(1, householdId)
            statement.setString(2, clean(familyId))
            statement.setString(3, clean(homePlaceId))
            statement.setString(4, clean(primaryOwnerKey))
            statement.setInt(5, max(1, maxResidents))
            statement.setInt(6, max(0, maxResidents))
            statement.setString(7, "binding_backfill:$householdId")
            statement.setLong(8, now)
            statement.setLong(9, now)
            statement.executeUpdate()
        }
    }

    @Throws(SQLException::class)
    private fun upsertBackfilledResident(householdId: String, row: BindingBackfillRow) {
        val now = System.currentTimeMillis()
        val residentKey = "npc_${row.npcId}"
        val sql = """
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
        """.trimIndent()
        requireStatements().prepareStatement(sql).use { statement ->
            statement.setString(1, householdId)
            statement.setString(2, residentKey)
            statement.setInt(3, row.npcId)
            statement.setString(4, row.npcUuid)
            statement.setString(5, row.npcName)
            statement.setString(6, row.sourceKey)
            statement.setString(7, row.homePlaceId)
            statement.setString(8, row.homeNodeId)
            statement.setString(9, row.homeNodeId)
            statement.setString(10, row.workPlaceId)
            statement.setString(11, row.workNodeId)
            statement.setString(12, row.socialPlaceId)
            statement.setString(13, row.socialNodeId)
            statement.setLong(14, now)
            statement.setLong(15, now)
            statement.executeUpdate()
        }
    }

    @Throws(SQLException::class)
    private fun saveHouseholdRow(
        allocation: HouseAllocation,
        householdId: String,
        plans: List<NpcSpawnPlan>,
        source: String?,
        now: Long
    ) {
        removeConflictingHousehold(allocation.placeId(), householdId)
        val sql = """
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
        """.trimIndent()

        requireStatements().prepareStatement(sql).use { statement ->
            statement.setString(1, householdId)
            statement.setString(2, allocation.familyId())
            statement.setString(3, allocation.placeId())
            statement.setString(4, allocation.primaryOwnerNpcKey())
            statement.setInt(5, allocation.maxResidents())
            statement.setInt(6, plans.size)
            statement.setString(7, SpawnBatchPlanHasher.householdPlanHash(allocation))
            statement.setString(8, clean(source))
            statement.setLong(9, now)
            statement.setLong(10, now)
            statement.executeUpdate()
        }
    }

    @Throws(SQLException::class)
    private fun removeConflictingHousehold(homePlaceId: String?, householdId: String) {
        if (homePlaceId.isNullOrBlank()) {
            return
        }
        requireStatements().prepareStatement(
            """
            DELETE FROM households
            WHERE home_place_id = ?
              AND household_id <> ?
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, homePlaceId)
            statement.setString(2, householdId)
            statement.executeUpdate()
        }
    }

    @Throws(SQLException::class)
    private fun saveResidentRow(
        householdId: String,
        residentKey: String,
        allocation: HouseAllocation,
        residentPlan: HouseAllocation.ResidentPlan,
        plan: NpcSpawnPlan,
        npc: AINPC,
        source: String?,
        now: Long
    ) {
        removeMovedResident(npc.databaseId, plan.sourceKey(), householdId, residentKey)

        val sql = """
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
        """.trimIndent()

        requireStatements().prepareStatement(sql).use { statement ->
            statement.setString(1, householdId)
            statement.setString(2, residentKey)
            statement.setInt(3, npc.databaseId)
            statement.setString(4, npc.uuid.toString())
            statement.setString(5, npc.name)
            statement.setString(6, plan.sourceKey())
            statement.setString(7, residentPlan.relationRole())
            statement.setString(8, allocation.placeId())
            statement.setString(9, residentPlan.spawnNodeId())
            statement.setString(10, residentPlan.effectiveHomeNodeId())
            statement.setString(11, residentPlan.workPlaceId())
            statement.setString(12, residentPlan.workNodeId())
            statement.setString(13, residentPlan.socialPlaceId())
            statement.setString(14, residentPlan.socialNodeId())
            statement.setLong(15, now)
            statement.setLong(16, now)
            statement.executeUpdate()
        }
    }

    @Throws(SQLException::class)
    private fun removeMovedResident(npcId: Int, sourceKey: String?, householdId: String, residentKey: String) {
        if (npcId <= 0) {
            return
        }

        requireStatements().prepareStatement(
            """
            DELETE FROM household_residents
            WHERE (npc_id = ? OR (source_key <> '' AND source_key = ?))
              AND NOT (household_id = ? AND resident_key = ?)
            """.trimIndent()
        ).use { statement ->
            statement.setInt(1, npcId)
            statement.setString(2, clean(sourceKey))
            statement.setString(3, householdId)
            statement.setString(4, residentKey)
            statement.executeUpdate()
        }
    }

    @Throws(SQLException::class)
    private fun deleteStaleResidents(householdId: String, activeResidentKeys: List<String>) {
        if (activeResidentKeys.isEmpty()) {
            requireStatements().prepareStatement("DELETE FROM household_residents WHERE household_id = ?").use { statement ->
                statement.setString(1, householdId)
                statement.executeUpdate()
            }
            return
        }

        val placeholders = activeResidentKeys.joinToString(", ") { "?" }
        requireStatements().prepareStatement(
            "DELETE FROM household_residents WHERE household_id = ? AND resident_key NOT IN ($placeholders)"
        ).use { statement ->
            statement.setString(1, householdId)
            for (index in activeResidentKeys.indices) {
                statement.setString(index + 2, activeResidentKeys[index])
            }
            statement.executeUpdate()
        }
    }

    @Throws(SQLException::class)
    private fun updateResidentCount(householdId: String, fallbackCount: Int): Int {
        requireStatements().prepareStatement(
            """
            UPDATE households
            SET resident_count = COALESCE((
                SELECT COUNT(*)
                FROM household_residents
                WHERE household_id = households.household_id
                  AND status = 'active'
            ), ?),
                updated_at = ?
            WHERE household_id = ?
            """.trimIndent()
        ).use { statement ->
            statement.setInt(1, max(0, fallbackCount))
            statement.setLong(2, System.currentTimeMillis())
            statement.setString(3, householdId)
            return statement.executeUpdate()
        }
    }

    private fun requireValid(allocation: HouseAllocation?): HouseAllocation {
        if (allocation == null) {
            throw IllegalArgumentException("HouseAllocation nu poate fi null.")
        }
        if (allocation.householdId().isBlank()) {
            throw IllegalArgumentException("HouseAllocation nu are household_id valid.")
        }
        return allocation
    }

    private fun findResidentPlan(allocation: HouseAllocation, plan: NpcSpawnPlan): HouseAllocation.ResidentPlan? =
        allocation.residentPlans()
            .firstOrNull { resident -> normalizeResidentKey(resident.npcKey()) == normalizeResidentKey(plan.npcKey()) }

    @Throws(SQLException::class)
    private fun readResident(resultSet: ResultSet): HouseholdResidentRecord =
        HouseholdResidentRecord(
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
        )

    @Throws(SQLException::class)
    private fun findResidentBySourceKey(sourceKey: String?): Optional<HouseholdResidentRecord> {
        if (sourceKey.isNullOrBlank()) {
            return Optional.empty()
        }
        val sql = """
            SELECT household_id, resident_key, npc_id, npc_uuid, npc_name, source_key,
                   relation_role, home_place_id, spawn_node_id, home_node_id,
                   work_place_id, work_node_id, social_place_id, social_node_id,
                   status, created_at, updated_at
            FROM household_residents
            WHERE source_key = ?
            ORDER BY updated_at DESC, household_id ASC, resident_key ASC
            LIMIT 1
        """.trimIndent()
        requireStatements().prepareStatement(sql).use { statement ->
            statement.setString(1, clean(sourceKey))
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) Optional.of(readResident(resultSet)) else Optional.empty()
            }
        }
    }

    @Throws(SQLException::class)
    private fun readHousehold(resultSet: ResultSet): HouseholdRecord =
        HouseholdRecord(
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
        )

    @Throws(SQLException::class)
    private fun requireStatements(): HouseholdPersistenceService.StatementProvider {
        return statements ?: throw SQLException("HouseholdPersistenceService nu are acces la baza de date.")
    }

    private class BindingBackfillRow(
        val npcId: Int,
        npcUuid: String?,
        npcName: String?,
        homePlaceId: String?,
        workPlaceId: String?,
        socialPlaceId: String?,
        homeNodeId: String?,
        workNodeId: String?,
        socialNodeId: String?,
        familyId: String?,
        sourceKey: String?
    ) {
        val npcUuid: String = clean(npcUuid)
        val npcName: String = clean(npcName)
        val homePlaceId: String = clean(homePlaceId)
        val workPlaceId: String = clean(workPlaceId)
        val socialPlaceId: String = clean(socialPlaceId)
        val homeNodeId: String = clean(homeNodeId)
        val workNodeId: String = clean(workNodeId)
        val socialNodeId: String = clean(socialNodeId)
        val familyId: String = clean(familyId)
        val sourceKey: String = clean(sourceKey)
    }

    private class RepairResidentRow(
        householdId: String?,
        residentKey: String?,
        val npcId: Int,
        npcName: String?,
        sourceKey: String?,
        status: String?,
        homePlaceId: String?,
        val createdAt: Long,
        val updatedAt: Long
    ) {
        val householdId: String = clean(householdId)
        val residentKey: String = clean(residentKey)
        val npcName: String = clean(npcName)
        val sourceKey: String = clean(sourceKey)
        val status: String = clean(status)
        val homePlaceId: String = clean(homePlaceId)

        fun identityKey(): String = "$householdId|$residentKey|$npcId"

        fun label(): String =
            "$householdId/$residentKey npc_id=$npcId name=$npcName home=$homePlaceId updated_at=$updatedAt"

        companion object {
            fun compareCanonical(left: RepairResidentRow, right: RepairResidentRow): Int {
                val activeCompare = activeRank(left.status).compareTo(activeRank(right.status))
                if (activeCompare != 0) {
                    return activeCompare
                }
                val updatedCompare = right.updatedAt.compareTo(left.updatedAt)
                if (updatedCompare != 0) {
                    return updatedCompare
                }
                val createdCompare = right.createdAt.compareTo(left.createdAt)
                if (createdCompare != 0) {
                    return createdCompare
                }
                val householdCompare = left.householdId.compareTo(right.householdId)
                if (householdCompare != 0) {
                    return householdCompare
                }
                return left.residentKey.compareTo(right.residentKey)
            }

            private fun activeRank(status: String): Int = if ("active".equals(status, ignoreCase = true)) 0 else 1
        }
    }

    private class HouseholdBackfillAccumulator(
        val apply: Boolean,
        private val scannedBindings: Int
    ) {
        var candidateHouseholds: Int = 0
        var householdsCreated: Int = 0
        var householdsUpdated: Int = 0
        var residentsCreated: Int = 0
        var residentsAlreadyPresent: Int = 0
        var skippedResidents: Int = 0
        private val actions: MutableList<String> = ArrayList()
        private val warnings: MutableList<String> = ArrayList()
        private val errors: MutableList<String> = ArrayList()

        fun action(message: String) {
            addLimited(actions, message)
        }

        fun warning(message: String) {
            addLimited(warnings, message)
        }

        fun toReport(): HouseholdBackfillReport =
            HouseholdBackfillReport(
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
            )
    }

    private class HouseholdResidentRepairAccumulator(
        val apply: Boolean
    ) {
        var duplicateNpcGroups: Int = 0
        var duplicateSourceKeyGroups: Int = 0
        var duplicateResidentRows: Int = 0
        var deletedResidentRows: Int = 0
        var updatedHouseholds: Int = 0
        private val actions: MutableList<String> = ArrayList()
        private val warnings: MutableList<String> = ArrayList()
        private val errors: MutableList<String> = ArrayList()

        fun action(message: String) {
            addLimited(actions, message)
        }

        fun warning(message: String) {
            addLimited(warnings, message)
        }

        fun toReport(): HouseholdResidentRepairReport =
            HouseholdResidentRepairReport(
                apply,
                duplicateNpcGroups,
                duplicateSourceKeyGroups,
                duplicateResidentRows,
                deletedResidentRows,
                updatedHouseholds,
                actions,
                warnings,
                errors
            )
    }

    companion object {
        private fun normalizeResidentKey(value: String?): String =
            clean(value).lowercase(Locale.ROOT).replace(' ', '_').replace('-', '_')

        private fun backfillHouseholdId(familyId: String?, homePlaceId: String?): String {
            val cleanFamilyId = clean(familyId)
            if (cleanFamilyId.isNotBlank()) {
                return cleanFamilyId
            }
            val normalizedPlaceId = normalizeResidentKey(homePlaceId)
            return if (normalizedPlaceId.isBlank()) "" else "household_$normalizedPlaceId"
        }

        private fun firstNonBlank(values: List<String>?): String {
            if (values == null) {
                return ""
            }
            for (value in values) {
                val cleanValue = clean(value)
                if (cleanValue.isNotBlank()) {
                    return cleanValue
                }
            }
            return ""
        }

        private fun addLimited(target: MutableList<String>, value: String) {
            if (target.size < 50) {
                target.add(value)
            }
        }

        @Throws(SQLException::class)
        private fun text(resultSet: ResultSet, column: String): String = resultSet.getString(column) ?: ""

        private fun clean(value: String?): String = value?.trim().orEmpty()
    }
}
