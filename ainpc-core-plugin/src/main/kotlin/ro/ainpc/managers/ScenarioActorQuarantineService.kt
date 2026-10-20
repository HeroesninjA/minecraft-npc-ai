package ro.ainpc.managers

import com.google.gson.Gson
import ro.ainpc.npc.NpcLifecycleType
import ro.ainpc.npc.NpcPersistenceMode
import java.sql.Connection
import java.util.UUID

internal class ScenarioActorQuarantineService(
    private val connection: Connection,
    private val isLoaded: (Int) -> Boolean,
    private val gson: Gson = Gson(),
) {
    enum class Reason(val label: String) {
        TRANSIENT("tranzitoriu confirmat"),
        LEGACY_OR_INVALID("metadata veche sau invalida"),
        INVALID_PERSISTENCE("persistenta runtime intr-un rand DB"),
    }

    data class Entry(val databaseId: Int, val uuid: String, val reason: Reason)

    data class Preview(val total: Int, val repairable: Int, val entries: List<Entry>)

    enum class DeleteResult {
        DELETED,
        NOT_FOUND,
        UUID_MISMATCH,
        NOT_REPAIRABLE,
        LOADED,
        CHANGED,
    }

    fun preview(limit: Int = 10, offset: Long = 0): Preview {
        val entries = ArrayList<Entry>()
        val previewLimit = limit.coerceAtLeast(0)
        val previewOffset = offset.coerceAtLeast(0)
        var total = 0
        var repairable = 0
        val sql = """
            SELECT n.id, n.uuid, pr.profile_data
            FROM npcs n
            JOIN npc_profiles pr ON pr.npc_id = n.id
            WHERE LOWER(pr.profile_source) = 'scenario_actor'
            ORDER BY n.id
        """
        connection.prepareStatement(sql).use { statement ->
            statement.executeQuery().use { rows ->
                while (rows.next()) {
                    if (isLoaded(rows.getInt("id"))) continue
                    val reason = quarantineReason(rows.getString("profile_data")) ?: continue
                    total++
                    if (reason == Reason.TRANSIENT) repairable++
                    if (total.toLong() > previewOffset && entries.size < previewLimit) {
                        entries.add(Entry(rows.getInt("id"), rows.getString("uuid"), reason))
                    }
                }
            }
        }
        return Preview(total, repairable, entries)
    }

    fun deleteConfirmed(databaseId: Int, expectedUuid: UUID): DeleteResult {
        if (databaseId <= 0) return DeleteResult.NOT_FOUND
        if (isLoaded(databaseId)) return DeleteResult.LOADED
        val sql = """
            SELECT n.uuid, pr.profile_data
            FROM npcs n
            JOIN npc_profiles pr ON pr.npc_id = n.id
            WHERE n.id = ? AND LOWER(pr.profile_source) = 'scenario_actor'
        """
        val row = connection.prepareStatement(sql).use { statement ->
            statement.setInt(1, databaseId)
            statement.executeQuery().use { rows ->
                if (rows.next()) rows.getString("uuid") to rows.getString("profile_data") else null
            }
        } ?: return DeleteResult.NOT_FOUND

        val storedUuid = runCatching { UUID.fromString(row.first) }.getOrNull()
        if (storedUuid != expectedUuid) return DeleteResult.UUID_MISMATCH
        if (quarantineReason(row.second) != Reason.TRANSIENT) return DeleteResult.NOT_REPAIRABLE

        val deleteSql = """
            DELETE FROM npcs
            WHERE id = ? AND uuid = ? AND EXISTS (
                SELECT 1 FROM npc_profiles pr
                WHERE pr.npc_id = npcs.id
                  AND LOWER(pr.profile_source) = 'scenario_actor'
                  AND pr.profile_data = ?
            )
        """
        val deleted = connection.prepareStatement(deleteSql).use { statement ->
            statement.setInt(1, databaseId)
            statement.setString(2, row.first)
            statement.setString(3, row.second)
            statement.executeUpdate()
        }
        return if (deleted == 1) DeleteResult.DELETED else DeleteResult.CHANGED
    }

    private fun quarantineReason(profileData: String?): Reason? {
        val metadata = ScenarioActorProfileData.read(profileData, gson) ?: return Reason.LEGACY_OR_INVALID
        if (metadata.persistenceMode == NpcPersistenceMode.RUNTIME_ONLY) return Reason.INVALID_PERSISTENCE
        return if (metadata.lifecycleType == NpcLifecycleType.PERMANENT) null else Reason.TRANSIENT
    }
}
