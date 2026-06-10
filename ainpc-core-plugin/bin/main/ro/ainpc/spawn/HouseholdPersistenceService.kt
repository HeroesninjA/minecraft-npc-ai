package ro.ainpc.spawn

import ro.ainpc.AINPCPlugin
import ro.ainpc.database.DatabaseManager
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.Optional
import java.util.logging.Logger

class HouseholdPersistenceService {
    fun interface StatementProvider {
        @Throws(SQLException::class)
        fun prepareStatement(sql: String): PreparedStatement
    }

    private val state: HouseholdPersistenceServiceState

    constructor(plugin: AINPCPlugin?) : this(plugin?.databaseManager, plugin?.logger)

    constructor(databaseManager: DatabaseManager?, logger: Logger?) : this(
        databaseManager?.let { manager -> StatementProvider { sql -> manager.prepareStatement(sql) } },
        logger,
    )

    constructor(statements: StatementProvider?, logger: Logger?) {
        state = HouseholdPersistenceServiceState(statements, logger)
    }

    @Throws(SQLException::class)
    fun saveHousehold(
        allocation: HouseAllocation?,
        plans: List<NpcSpawnPlan>?,
        spawnResults: List<NpcSpawnResult?>?,
        source: String?,
    ): Int = state.saveHousehold(allocation, plans, spawnResults, source)

    @Throws(SQLException::class)
    fun listResidents(householdId: String?): List<HouseholdResidentRecord> = state.listResidents(householdId)

    @Throws(SQLException::class)
    fun listHouseholds(limit: Int): List<HouseholdRecord> = state.listHouseholds(limit)

    @Throws(SQLException::class)
    fun getHousehold(householdId: String?): Optional<HouseholdRecord> = state.getHousehold(householdId)

    @Throws(SQLException::class)
    fun findHouseholdByHomePlace(homePlaceId: String?): Optional<HouseholdRecord> =
        state.findHouseholdByHomePlace(homePlaceId)

    @Throws(SQLException::class)
    fun findResidentByNpcId(npcId: Int): Optional<HouseholdResidentRecord> = state.findResidentByNpcId(npcId)

    @Throws(SQLException::class)
    fun backfillFromNpcWorldBindings(apply: Boolean, limit: Int): HouseholdBackfillReport =
        state.backfillFromNpcWorldBindings(apply, limit)

    @Throws(SQLException::class)
    fun backfillFromMetadataResidents(
        apply: Boolean,
        limit: Int,
        inputs: List<MetadataResidentBackfillInput>?,
    ): HouseholdBackfillReport = state.backfillFromMetadataResidents(apply, limit, inputs)

    @Throws(SQLException::class)
    fun countHouseholds(): Int = state.countHouseholds()

    @Throws(SQLException::class)
    fun countResidents(): Int = state.countResidents()

    @Throws(SQLException::class)
    fun repairDuplicateResidents(apply: Boolean, limit: Int): HouseholdResidentRepairReport =
        state.repairDuplicateResidents(apply, limit)

    @Throws(SQLException::class)
    fun recalculateResidentCounts(householdIds: Collection<String>?): Int =
        state.recalculateResidentCounts(householdIds)

    class HouseholdRecord(
        householdId: String?,
        familyId: String?,
        homePlaceId: String?,
        primaryOwnerKey: String?,
        maxResidents: Int,
        residentCount: Int,
        planHash: String?,
        source: String?,
        val createdAt: Long,
        val updatedAt: Long,
    ) {
        val householdId: String = clean(householdId)
        val familyId: String = clean(familyId)
        val homePlaceId: String = clean(homePlaceId)
        val primaryOwnerKey: String = clean(primaryOwnerKey)
        val maxResidents: Int = maxResidents.coerceAtLeast(0)
        val residentCount: Int = residentCount.coerceAtLeast(0)
        val planHash: String = clean(planHash)
        val source: String = clean(source)

        fun householdId(): String = householdId
        fun familyId(): String = familyId
        fun homePlaceId(): String = homePlaceId
        fun primaryOwnerKey(): String = primaryOwnerKey
        fun maxResidents(): Int = maxResidents
        fun residentCount(): Int = residentCount
        fun planHash(): String = planHash
        fun source(): String = source
        fun createdAt(): Long = createdAt
        fun updatedAt(): Long = updatedAt
    }

    class HouseholdBackfillReport(
        val apply: Boolean,
        val scannedBindings: Int,
        val candidateHouseholds: Int,
        val householdsCreated: Int,
        val householdsUpdated: Int,
        val residentsCreated: Int,
        val residentsAlreadyPresent: Int,
        val skippedResidents: Int,
        actions: List<String>?,
        warnings: List<String>?,
        errors: List<String>?,
    ) {
        val actions: List<String> = immutableList(actions)
        val warnings: List<String> = immutableList(warnings)
        val errors: List<String> = immutableList(errors)

        fun apply(): Boolean = apply
        fun scannedBindings(): Int = scannedBindings
        fun candidateHouseholds(): Int = candidateHouseholds
        fun householdsCreated(): Int = householdsCreated
        fun householdsUpdated(): Int = householdsUpdated
        fun residentsCreated(): Int = residentsCreated
        fun residentsAlreadyPresent(): Int = residentsAlreadyPresent
        fun skippedResidents(): Int = skippedResidents
        fun actions(): List<String> = actions
        fun warnings(): List<String> = warnings
        fun errors(): List<String> = errors
        fun success(): Boolean = errors.isEmpty()
    }

    class MetadataResidentBackfillInput(
        homePlaceId: String?,
        familyId: String?,
        val npcId: Int,
    ) {
        val homePlaceId: String = clean(homePlaceId)
        val familyId: String = clean(familyId)

        fun homePlaceId(): String = homePlaceId
        fun familyId(): String = familyId
        fun npcId(): Int = npcId
    }

    class HouseholdResidentRepairReport(
        val apply: Boolean,
        val duplicateNpcGroups: Int,
        val duplicateSourceKeyGroups: Int,
        val duplicateResidentRows: Int,
        val deletedResidentRows: Int,
        val updatedHouseholds: Int,
        actions: List<String>?,
        warnings: List<String>?,
        errors: List<String>?,
    ) {
        val actions: List<String> = immutableList(actions)
        val warnings: List<String> = immutableList(warnings)
        val errors: List<String> = immutableList(errors)

        fun apply(): Boolean = apply
        fun duplicateNpcGroups(): Int = duplicateNpcGroups
        fun duplicateSourceKeyGroups(): Int = duplicateSourceKeyGroups
        fun duplicateResidentRows(): Int = duplicateResidentRows
        fun deletedResidentRows(): Int = deletedResidentRows
        fun updatedHouseholds(): Int = updatedHouseholds
        fun actions(): List<String> = actions
        fun warnings(): List<String> = warnings
        fun errors(): List<String> = errors
    }

    class HouseholdResidentRecord(
        householdId: String?,
        residentKey: String?,
        val npcId: Int,
        npcUuid: String?,
        npcName: String?,
        sourceKey: String?,
        relationRole: String?,
        homePlaceId: String?,
        spawnNodeId: String?,
        homeNodeId: String?,
        workPlaceId: String?,
        workNodeId: String?,
        socialPlaceId: String?,
        socialNodeId: String?,
        status: String?,
        val createdAt: Long,
        val updatedAt: Long,
    ) {
        val householdId: String = clean(householdId)
        val residentKey: String = clean(residentKey)
        val npcUuid: String = clean(npcUuid)
        val npcName: String = clean(npcName)
        val sourceKey: String = clean(sourceKey)
        val relationRole: String = clean(relationRole)
        val homePlaceId: String = clean(homePlaceId)
        val spawnNodeId: String = clean(spawnNodeId)
        val homeNodeId: String = clean(homeNodeId)
        val workPlaceId: String = clean(workPlaceId)
        val workNodeId: String = clean(workNodeId)
        val socialPlaceId: String = clean(socialPlaceId)
        val socialNodeId: String = clean(socialNodeId)
        val status: String = clean(status)

        fun householdId(): String = householdId
        fun residentKey(): String = residentKey
        fun npcId(): Int = npcId
        fun npcUuid(): String = npcUuid
        fun npcName(): String = npcName
        fun sourceKey(): String = sourceKey
        fun relationRole(): String = relationRole
        fun homePlaceId(): String = homePlaceId
        fun spawnNodeId(): String = spawnNodeId
        fun homeNodeId(): String = homeNodeId
        fun workPlaceId(): String = workPlaceId
        fun workNodeId(): String = workNodeId
        fun socialPlaceId(): String = socialPlaceId
        fun socialNodeId(): String = socialNodeId
        fun status(): String = status
        fun createdAt(): Long = createdAt
        fun updatedAt(): Long = updatedAt
    }

    companion object {
        @JvmStatic
        internal fun clean(value: String?): String = value?.trim().orEmpty()

        private fun immutableList(values: List<String>?): List<String> =
            java.util.Collections.unmodifiableList(ArrayList(values ?: emptyList()))
    }
}
